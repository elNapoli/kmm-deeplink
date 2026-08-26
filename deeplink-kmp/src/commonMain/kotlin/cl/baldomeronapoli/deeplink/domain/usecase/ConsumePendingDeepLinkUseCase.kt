package cl.baldomeronapoli.deeplink.domain.usecase

import cl.baldomeronapoli.base.domain.models.Destination
import cl.baldomeronapoli.deeplink.domain.repository.PendingDeepLinkRepository

/**
 * Consumes the buffered deep link saved during the unauthenticated path.
 * Returns the [Destination] the caller should navigate to right after a
 * successful login, or null if there was no pending link (or the URI cannot
 * be mapped any more).
 *
 * Idempotent: the URI is cleared from storage as soon as it is read, so
 * callers may invoke this multiple times safely (the second call just
 * returns null).
 */
class ConsumePendingDeepLinkUseCase<Route>(
    private val parse: (String) -> Route?,
    private val mapToDestination: (Route) -> Destination?,
    private val pendingRepository: PendingDeepLinkRepository,
) {

    suspend operator fun invoke(): Destination? {
        val uri = pendingRepository.consume() ?: return null
        val route = parse(uri) ?: return null
        return mapToDestination(route)
    }
}
