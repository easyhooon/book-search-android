package io.github.easyhooon.booksearch

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import io.github.easyhooon.booksearch.core.network.NetworkConfig

class BookSearchApplication : Application(), SingletonImageLoader.Factory {

    val appGraph: AppGraph by lazy {
        ManualAppGraph.create(
            context = applicationContext,
            networkConfig = NetworkConfig(
                apiKey = BuildConfig.KAKAO_REST_API_KEY,
                enableHttpLog = BuildConfig.DEBUG,
            ),
        )
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = appGraph.imageLoader
}
