package com.alvinhkh.buseta

import okhttp3.Interceptor
import okhttp3.Response


class UserAgentInterceptor(private val userAgent: String = System.getProperty("http.agent")?:"") : Interceptor {
    @Throws(Throwable::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build()
        return chain.proceed(requestWithUserAgent)
    }
}