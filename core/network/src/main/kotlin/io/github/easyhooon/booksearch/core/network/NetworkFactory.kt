package io.github.easyhooon.booksearch.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal val NetworkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

/** 이미지 로딩과 API가 공유하는 기본 클라이언트입니다. 인증 정보가 없습니다. */
fun createBaseOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

/**
 * 기본 클라이언트의 연결 풀을 공유하되 인증 interceptor는 API 클라이언트에만 추가합니다.
 */
fun createKakaoBookService(
    baseClient: OkHttpClient,
    config: NetworkConfig,
): KakaoBookService {
    val apiClient = baseClient.newBuilder()
        .addInterceptor(KakaoAuthInterceptor(config.apiKey))
        .apply {
            if (config.enableHttpLog) {
                addInterceptor(
                    HttpLoggingInterceptor { message -> Timber.tag(HTTP_LOG_TAG).d(message) }.apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader(KakaoAuthInterceptor.AUTHORIZATION)
                    },
                )
            }
        }
        .build()
    return Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(apiClient)
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(KakaoBookService::class.java)
}

private const val HTTP_LOG_TAG = "OkHttp"
