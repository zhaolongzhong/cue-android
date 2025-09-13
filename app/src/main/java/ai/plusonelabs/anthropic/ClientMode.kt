package ai.plusonelabs.anthropic

/**
 * Defines the connection mode for API access
 *
 * This enum helps organize different ways to connect to external services:
 * - Direct: Connect directly to the official API
 * - Proxy: Connect through backend server that proxies requests
 */
sealed class ClientMode {
    /**
     * Direct connection to the official API
     * @param apiKey The user's API key
     * @param baseUrl The API base URL
     */
    data class Direct(
        val apiKey: String,
        val baseUrl: String,
    ) : ClientMode()

    /**
     * Proxy connection through our backend server
     * @param baseUrl The backend server base URL (e.g., http://10.0.2.2:8000/api/v1)
     * @param authToken Optional authentication token for the backend server
     */
    data class Proxy(
        val baseUrl: String,
        val authToken: String? = null,
    ) : ClientMode()
}
