package io.github.easyhooon.booksearch

import android.content.Context
import coil3.ImageLoader
import io.github.easyhooon.booksearch.core.network.NetworkConfig
import io.github.easyhooon.booksearch.feature.detail.BookDetailScreenGraph
import io.github.easyhooon.booksearch.feature.favorites.FavoritesScreenGraph
import io.github.easyhooon.booksearch.feature.search.SearchScreenGraph
import soil.query.SwrClientPlus

/**
 * 앱 전체 그래프의 계약입니다. 화면 엔트리는 이 Factory로 만든 그래프를 retain합니다.
 */
interface AppGraph :
    SearchScreenGraph.Factory,
    FavoritesScreenGraph.Factory,
    BookDetailScreenGraph.Factory {

    val swrClient: SwrClientPlus
    val imageLoader: ImageLoader

    fun interface Factory {
        fun create(
            context: Context,
            networkConfig: NetworkConfig,
        ): AppGraph
    }
}
