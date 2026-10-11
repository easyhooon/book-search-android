package io.github.easyhooon.booksearch

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dev.zacsweers.metro.createGraphFactory
import io.github.easyhooon.booksearch.core.network.NetworkConfig

class BookSearchApplication : Application(), SingletonImageLoader.Factory {

    val appGraph: AppGraph by lazy {
        createGraphFactory<AppGraph.Factory>().create(
            context = applicationContext,
            networkConfig = NetworkConfig(
                apiKey = BuildConfig.KAKAO_REST_API_KEY,
                enableHttpLog = BuildConfig.DEBUG,
                baseUrl = BuildConfig.KAKAO_API_BASE_URL,
            ),
        )
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = appGraph.imageLoader
}
