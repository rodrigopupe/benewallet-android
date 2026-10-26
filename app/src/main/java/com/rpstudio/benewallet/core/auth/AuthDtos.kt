package com.rpstudio.benewallet.core.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequestDto(val email: String, val password: String)

@Serializable
internal data class RefreshRequestDto(val refreshToken: String)

@Serializable
internal data class TokenResponseDto(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long)
