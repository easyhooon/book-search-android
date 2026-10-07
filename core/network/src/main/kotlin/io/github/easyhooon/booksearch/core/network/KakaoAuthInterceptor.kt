package io.github.easyhooon.booksearch.core.network

import okhttp3.Interceptor
import okhttp3.Response

/** Kakao API 요청에만 붙이는 인증 interceptor입니다. 이미지 클라이언트에는 추가하지 않습니다. */
internal class KakaoAuthInterceptor(
    private val apiKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (apiKey.isBlank()) throw MissingApiKeyException()
        val request = chain.request().newBuilder()
            .header(AUTHORIZATION, "KakaoAK $apiKey")
            .build()
        return chain.proceed(request)
    }

    companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
