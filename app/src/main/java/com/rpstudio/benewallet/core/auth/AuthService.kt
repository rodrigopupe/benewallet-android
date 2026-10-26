package com.rpstudio.benewallet.core.auth

import retrofit2.http.Body
import retrofit2.http.POST

/** Talks to the BFF through the unauthenticated client: these calls never carry (or need) an access token. */
internal interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): TokenResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): TokenResponseDto

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequestDto)
}
