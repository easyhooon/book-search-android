package io.github.easyhooon.booksearch

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.easyhooon.booksearch.core.network.NetworkConfig
import io.github.easyhooon.booksearch.feature.detail.BookDetailScreenGraph
import io.github.easyhooon.booksearch.feature.favorites.FavoritesScreenGraph
import io.github.easyhooon.booksearch.feature.search.SearchScreenGraph
import okhttp3.OkHttpClient
import soil.query.AndroidMemoryPressure
import soil.query.AndroidNetworkConnectivity
import soil.query.SwrCachePlus
import soil.query.SwrCachePlusPolicy
import soil.query.SwrCacheScope
import soil.query.SwrClientPlus
import soil.query.annotation.ExperimentalSoilQueryApi

/**
 * 앱 전체 그래프입니다. 화면별 그래프는 각 feature의 GraphExtension이며, 이 그래프가 그 Factory를 구현합니다.
 */
@DependencyGraph(AppScope::class)
interface AppGraph :
    SearchScreenGraph.Factory,
    FavoritesScreenGraph.Factory,
    BookDetailScreenGraph.Factory {

    val swrClient: SwrClientPlus
    val imageLoader: ImageLoader

    @OptIn(ExperimentalSoilQueryApi::class)
    @Provides
    @SingleIn(AppScope::class)
    fun provideSwrClient(context: Context): SwrClientPlus = SwrCachePlus(
        SwrCachePlusPolicy(
            coroutineScope = SwrCacheScope(),
            memoryPressure = AndroidMemoryPressure(context),
            networkConnectivity = AndroidNetworkConnectivity(context),
        ),
    )

    /** 이미지 요청에는 Kakao 인증 interceptor가 없는 기본 클라이언트를 씁니다. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideImageLoader(context: Context, baseClient: OkHttpClient): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { baseClient })) }
            .crossfade(true)
            .build()

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides context: Context,
            @Provides networkConfig: NetworkConfig,
        ): AppGraph
    }
}
