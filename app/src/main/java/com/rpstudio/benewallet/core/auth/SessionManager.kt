package com.rpstudio.benewallet.core.auth

import kotlinx.coroutines.flow.StateFlow

internal sealed interface SessionState {
    /** The saved session has not been read yet. */
    object Loading : SessionState

    object SignedOut : SessionState

    object SignedIn : SessionState
}

internal sealed interface LoginResult {
    object Success : LoginResult

    object InvalidCredentials : LoginResult

    object TooManyAttempts : LoginResult

    object NetworkError : LoginResult

    object UnexpectedError : LoginResult
}

/** Implemented by whoever keeps data of the signed-in user (database, cached flags), so a session end wipes it. */
internal fun interface SessionDataCleaner {
    suspend fun clear()
}

internal interface SessionManager {
    val state: StateFlow<SessionState>

    /** Reads the saved session. Until then the state is [SessionState.Loading]. */
    suspend fun restore()

    suspend fun login(email: String, password: String): LoginResult

    /** Ends the session on this device right away and tells the server on a best-effort basis. */
    suspend fun logout()

    /** The token to send, renewed first when it is about to expire. Null when signed out. */
    suspend fun accessToken(): String?

    /**
     * Called after the server rejected [rejectedAccessToken]. Returns a token to retry with, or null when the
     * request must fail (and the session has ended if the server refused the refresh token).
     */
    suspend fun refresh(rejectedAccessToken: String): String?
}
