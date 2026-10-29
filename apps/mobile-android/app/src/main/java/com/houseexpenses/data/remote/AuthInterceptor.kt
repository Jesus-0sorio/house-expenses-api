package com.houseexpenses.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // 1. Inyectar Bearer token si el usuario está autenticado
        val token = sessionManager.authToken
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        // 2. Re-escribir dinámicamente la URL base si fue modificada en la configuración
        val configuredUrl = sessionManager.serverUrl.toHttpUrlOrNull()
        if (configuredUrl != null) {
            val newUrl = original.url.newBuilder()
                .scheme(configuredUrl.scheme)
                .host(configuredUrl.host)
                .port(configuredUrl.port)
                .build()
            requestBuilder.url(newUrl)
        }

        return chain.proceed(requestBuilder.build())
    }
}
