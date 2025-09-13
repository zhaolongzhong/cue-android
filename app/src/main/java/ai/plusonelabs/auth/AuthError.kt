package ai.plusonelabs.auth

sealed class AuthError : Exception() {
    object InvalidCredentials : AuthError()
    object NetworkError : AuthError()
    object InvalidResponse : AuthError()
    object EmailAlreadyExists : AuthError()
    object Unauthorized : AuthError()
    object Unknown : AuthError()
    object TokenGenerationFailed : AuthError()

    override val message: String
        get() = when (this) {
            is InvalidCredentials -> "Invalid email or password"
            is NetworkError -> "Network error occurred"
            is InvalidResponse -> "Invalid response from server"
            is EmailAlreadyExists -> "Email already exists"
            is Unauthorized -> "Unauthorized access"
            is TokenGenerationFailed -> "Failed to generate access token"
            is Unknown -> "An unknown error occurred"
        }
}
