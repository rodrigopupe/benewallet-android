package com.rpstudio.benewallet.core.auth

import java.time.Instant

internal data class AuthTokens(val accessToken: String, val refreshToken: String, val accessExpiresAt: Instant)
