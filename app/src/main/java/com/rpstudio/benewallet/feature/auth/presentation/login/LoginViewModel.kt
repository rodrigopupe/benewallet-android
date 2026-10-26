package com.rpstudio.benewallet.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpstudio.benewallet.core.auth.LoginResult
import com.rpstudio.benewallet.core.auth.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LoginViewModel(private val sessionManager: SessionManager) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = mutableState.asStateFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> mutableState.update { it.copy(email = intent.email, error = null) }
            is LoginIntent.PasswordChanged -> mutableState.update { it.copy(password = intent.password, error = null) }
            LoginIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val form = mutableState.value
        if (!form.canSubmit) return

        mutableState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = sessionManager.login(form.email, form.password)
            // On success the session turns SignedIn and the screen is replaced; the form must not keep the password
            mutableState.value = when (result) {
                LoginResult.Success -> LoginState()
                else -> form.copy(isLoading = false, error = result.toError())
            }
        }
    }

    private fun LoginResult.toError(): LoginError? = when (this) {
        LoginResult.Success -> null
        LoginResult.InvalidCredentials -> LoginError.INVALID_CREDENTIALS
        LoginResult.TooManyAttempts -> LoginError.TOO_MANY_ATTEMPTS
        LoginResult.NetworkError -> LoginError.NETWORK
        LoginResult.UnexpectedError -> LoginError.UNEXPECTED
    }
}
