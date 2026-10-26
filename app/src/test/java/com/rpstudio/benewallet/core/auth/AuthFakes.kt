package com.rpstudio.benewallet.core.auth

/** Reversible and recognizable as "not plain text", without needing an Android Keystore. */
internal class FakeTokenCipher : TokenCipher {
    var failToDecrypt = false
    var encryptCalls = 0

    override fun encrypt(plain: ByteArray): ByteArray {
        encryptCalls++
        return plain.reversedArray()
    }

    override fun decrypt(encrypted: ByteArray): ByteArray? = if (failToDecrypt) null else encrypted.reversedArray()
}

internal class FakeTokenStore(var tokens: AuthTokens? = null) : TokenStore {
    override suspend fun read(): AuthTokens? = tokens

    override suspend fun save(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}

internal fun tokenResponse(access: String = "access-1", refresh: String = "refresh-1", expiresInSeconds: Long = 900) =
    TokenResponseDto(access, refresh, expiresInSeconds)

internal class FakeAuthService : AuthService {
    var loginResult: () -> TokenResponseDto = { error("login not stubbed") }
    var refreshResult: (String) -> TokenResponseDto = { error("refresh not stubbed") }
    var logoutResult: (String) -> Unit = {}
    val loginCalls = mutableListOf<LoginRequestDto>()
    val refreshCalls = mutableListOf<String>()
    val logoutCalls = mutableListOf<String>()

    override suspend fun login(request: LoginRequestDto): TokenResponseDto {
        loginCalls.add(request)
        return loginResult()
    }

    override suspend fun refresh(request: RefreshRequestDto): TokenResponseDto {
        refreshCalls.add(request.refreshToken)
        return refreshResult(request.refreshToken)
    }

    override suspend fun logout(request: RefreshRequestDto) {
        logoutCalls.add(request.refreshToken)
        logoutResult(request.refreshToken)
    }
}

internal class FakeSessionManager(var token: String? = "token-1") : SessionManager {
    var refreshedTo: String? = "token-2"
    val rejected = mutableListOf<String>()
    override val state = kotlinx.coroutines.flow.MutableStateFlow<SessionState>(SessionState.SignedIn)

    override suspend fun restore() = Unit

    override suspend fun login(email: String, password: String) = LoginResult.Success

    override suspend fun logout() {
        state.value = SessionState.SignedOut
    }

    override suspend fun accessToken(): String? = token

    override suspend fun refresh(rejectedAccessToken: String): String? {
        rejected.add(rejectedAccessToken)
        return refreshedTo
    }
}
