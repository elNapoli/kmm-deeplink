package cl.baldomeronapoli.deeplink.domain.bus

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * In-memory pipe for raw deep link URIs flowing from the platform entry
 * points (Android `MainActivity.onCreate` / `onNewIntent`, iOS app delegate)
 * to the Compose root that owns the NavController.
 *
 * Why a bus instead of calling the deep link use case directly from the
 * activity: on a cold start the intent fires before the root composable
 * mounts the NavHost. Calling `coordinator.navigate` at that moment is a
 * no-op (NavController is null), and persisting straight to storage has a
 * race — the async write may not have committed by the time the root
 * composable consumes it.
 *
 * BUFFERED channel + Flow consumption guarantees that a URI emitted before
 * the consumer subscribes is still delivered as soon as the root composable
 * starts collecting. One consumer at a time (the root composable).
 */
class DeepLinkBus {

    private val channel = Channel<String>(capacity = Channel.BUFFERED)

    suspend fun emit(uri: String) {
        channel.send(uri)
    }

    /**
     * Variante no-suspend para callers que no pueden invocar `suspend fun`
     * directamente (ej. Swift via cinterop). Capacity es BUFFERED (ilimitada),
     * por lo que `trySend` nunca falla por backpressure.
     */
    fun emitFromPlatform(uri: String) {
        channel.trySend(uri)
    }

    fun asFlow(): Flow<String> = channel.receiveAsFlow()
}
