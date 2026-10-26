package com.rpstudio.benewallet.feature.auth.presentation.login

import androidx.core.util.PatternsCompat

internal enum class LoginError { INVALID_CREDENTIALS, TOO_MANY_ATTEMPTS, NETWORK, UNEXPECTED }

internal data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: LoginError? = null
) {
    val canSubmit: Boolean get() = !isLoading && isLoginFormValid(email, password)
}

internal sealed interface LoginIntent {
    data class EmailChanged(val email: String) : LoginIntent

    data class PasswordChanged(val password: String) : LoginIntent

    object Submit : LoginIntent
}

/**
 * Whether the form may be sent. Decides if the "Entrar" button is enabled and if Submit does anything.
 *
 * The server is the one that really validates the credentials, so this only avoids requests that cannot possibly work:
 * an e-mail that is not shaped like one, or no password.
 */
internal fun isLoginFormValid(email: String, password: String): Boolean =
    PatternsCompat.EMAIL_ADDRESS.matcher(email.trim()).matches() && password.isNotEmpty()
