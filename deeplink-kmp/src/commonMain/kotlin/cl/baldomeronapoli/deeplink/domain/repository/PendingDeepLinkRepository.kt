package cl.baldomeronapoli.deeplink.domain.repository

/**
 * Single-slot persistent buffer for a deep link captured before the user is
 * authenticated. The handler stores the raw URI here when the session is
 * missing; the post-login flow consumes it (one-shot read + clear) so the
 * user lands at the originally requested screen instead of the default.
 *
 * Persistence is up to the consumer app (DataStore, NSUserDefaults, etc).
 */
interface PendingDeepLinkRepository {
    suspend fun save(uri: String)
    suspend fun consume(): String?
    suspend fun clear()
}
