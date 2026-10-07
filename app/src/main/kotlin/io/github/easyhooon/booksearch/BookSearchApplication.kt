package io.github.easyhooon.booksearch

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dev.zacsweers.metro.createGraphFactory
import io.github.easyhooon.booksearch.core.network.NetworkConfig
import timber.log.Timber

class BookSearchApplication : Application(), SingletonImageLoader.Factory {

    val appGraph: AppGraph by lazy {
        createGraphFactory<AppGraph.Factory>().create(
            context = applicationContext,
            networkConfig = NetworkConfig(
                apiKey = BuildConfig.KAKAO_REST_API_KEY,
                enableHttpLog = BuildConfig.DEBUG,
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
        // release 빌드에는 Tree를 심지 않으므로 Timber 호출이 아무 출력도 남기지 않습니다.
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = appGraph.imageLoader
}
