package io.github.easyhooon.booksearch.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okhttp3.OkHttpClient

@ContributesTo(AppScope::class)
interface NetworkBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun provideBaseOkHttpClient(): OkHttpClient = createBaseOkHttpClient()

    @Provides
    @SingleIn(AppScope::class)
    fun provideKakaoBookService(
        baseClient: OkHttpClient,
        config: NetworkConfig,
    ): KakaoBookService = createKakaoBookService(baseClient, config)
}
