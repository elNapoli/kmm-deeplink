package cl.baldomeronapoli.deeplink.domain.usecase

import cl.baldomeronapoli.base.domain.models.Destination
import cl.baldomeronapoli.base.navigation.NavigationCoordinator
import cl.baldomeronapoli.deeplink.domain.auth.DeepLinkAuthProvider
import cl.baldomeronapoli.deeplink.domain.repository.PendingDeepLinkRepository
import cl.baldomeronapoli.navigation.domain.model.NavigateToRoute

/**
 * Top-level entry point for incoming deep links. Decides whether to navigate
 * immediately, defer until the user logs in, or drop the URI entirely.
 *
 * Behaviour matrix:
 *  - [parse] returns null (unmappable/unrecognized route) -> log + drop.
 *  - [mapToDestination] returns null (pass-through route, e.g. a payment
 *    bridge callback that isn't a navigation target) -> drop.
 *  - logged in + mappable route  -> coordinator.navigate(...).
 *  - NOT logged in + mappable route -> persist URI, navigate to the auth
 *    graph; the consumer app drains the URI on successful login.
 *
 * [parse] and [mapToDestination] are plain functions so the consumer app's
 * route model and URI shape stay entirely out of this module — this class
 * only knows the auth-gate + persist-or-navigate policy.
 */
class HandleDeepLinkUseCase<Route>(
    private val parse: (String) -> Route?,
    private val mapToDestination: (Route) -> Destination?,
    private val authProvider: DeepLinkAuthProvider,
    private val pendingRepository: PendingDeepLinkRepository,
    private val navigationCoordinator: NavigationCoordinator,
    private val authGraphDestination: Destination,
) {

    suspend operator fun invoke(uri: String) {
        val route = parse(uri) ?: return
        val destination = mapToDestination(route) ?: return
        val isLogged = authProvider.currentUserId() != null

        if (!isLogged) {
            // Persist + bounce to auth. The post-login flow consumes the URI.
            pendingRepository.save(uri)
            navigationCoordinator.navigate(NavigateToRoute(authGraphDestination))
            return
        }

        // Logged user. Try direct navigation; the coordinator returns false
        // when the NavController is not attached yet (cold start race: the
        // intent fires before the root composable mounts). In that case
        // persist so the root composable can drain the URI right after
        // setNavController.
        val handled = navigationCoordinator.navigate(NavigateToRoute(destination))
        if (!handled) {
            pendingRepository.save(uri)
        }
    }
}
