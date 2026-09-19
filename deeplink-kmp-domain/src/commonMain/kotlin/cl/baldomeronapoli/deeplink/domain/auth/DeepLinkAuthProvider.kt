package cl.baldomeronapoli.deeplink.domain.auth

/**
 * Minimal session check the deep link handler needs to decide whether to
 * navigate immediately or defer until login. Implemented by the consumer
 * app on top of its real session/auth repository.
 */
interface DeepLinkAuthProvider {
    fun currentUserId(): String?
}
