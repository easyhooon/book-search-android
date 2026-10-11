package io.github.easyhooon.booksearch

import android.content.Context
import androidx.room.Room
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import io.github.easyhooon.booksearch.core.data.DefaultFavoriteBookSubscriptionKeyFactory
import io.github.easyhooon.booksearch.core.data.DefaultFavoriteBooksSubscriptionKey
import io.github.easyhooon.booksearch.core.data.DefaultFavoriteIdsSubscriptionKey
import io.github.easyhooon.booksearch.core.data.DefaultFavoriteMutationKeyFactory
import io.github.easyhooon.booksearch.core.data.DefaultSearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.database.BookSearchDatabase
import io.github.easyhooon.booksearch.core.network.NetworkConfig
import io.github.easyhooon.booksearch.core.network.createBaseOkHttpClient
import io.github.easyhooon.booksearch.core.network.createKakaoBookService
import io.github.easyhooon.booksearch.feature.detail.BookDetailScreenContext
import io.github.easyhooon.booksearch.feature.detail.BookDetailScreenGraph
import io.github.easyhooon.booksearch.feature.favorites.FavoritesScreenContext
import io.github.easyhooon.booksearch.feature.favorites.FavoritesScreenGraph
import io.github.easyhooon.booksearch.feature.search.SearchScreenContext
import io.github.easyhooon.booksearch.feature.search.SearchScreenGraph
import soil.query.AndroidMemoryPressure
import soil.query.AndroidNetworkConnectivity
import soil.query.SwrCachePlus
import soil.query.SwrCachePlusPolicy
import soil.query.SwrCacheScope
import soil.query.SwrClientPlus
import soil.query.annotation.ExperimentalSoilQueryApi

/** 앱 자원은 이 그래프에서 공유하고, 화면 Context와 key는 화면 그래프마다 새로 조립합니다. */
internal class ManualAppGraph private constructor(
    context: Context,
    networkConfig: NetworkConfig,
) : AppGraph {
    private val applicationContext = context.applicationContext
    private val baseClient by lazy { createBaseOkHttpClient() }
    private val bookService by lazy { createKakaoBookService(baseClient, networkConfig) }
    private val database by lazy {
        Room.databaseBuilder(applicationContext, BookSearchDatabase::class.java, BookSearchDatabase.NAME).build()
    }
    private val favoriteBookDao get() = database.favoriteBookDao()

    @OptIn(ExperimentalSoilQueryApi::class)
    override val swrClient: SwrClientPlus by lazy {
        SwrCachePlus(
            SwrCachePlusPolicy(
                coroutineScope = SwrCacheScope(),
                memoryPressure = AndroidMemoryPressure(applicationContext),
                networkConnectivity = AndroidNetworkConnectivity(applicationContext),
            ),
        )
    }

    /** 이미지 요청에는 Kakao 인증 interceptor가 없는 기본 클라이언트를 공유합니다. */
    override val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(applicationContext)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { baseClient })) }
            .crossfade(true)
            .build()
    }

    override fun createSearchScreenGraph(): SearchScreenGraph = object : SearchScreenGraph {
        override val searchScreenContext by lazy {
            SearchScreenContext(
                searchBooksQueryKeyFactory = DefaultSearchBooksQueryKeyFactory(bookService),
                favoriteIdsSubscriptionKey = DefaultFavoriteIdsSubscriptionKey(favoriteBookDao),
                favoriteMutationKeyFactory = DefaultFavoriteMutationKeyFactory(favoriteBookDao),
            )
        }
    }

    override fun createFavoritesScreenGraph(): FavoritesScreenGraph = object : FavoritesScreenGraph {
        override val favoritesScreenContext by lazy {
            FavoritesScreenContext(
                favoriteBooksSubscriptionKey = DefaultFavoriteBooksSubscriptionKey(favoriteBookDao),
                favoriteMutationKeyFactory = DefaultFavoriteMutationKeyFactory(favoriteBookDao),
            )
        }
    }

    override fun createBookDetailScreenGraph(): BookDetailScreenGraph = object : BookDetailScreenGraph {
        override val bookDetailScreenContext by lazy {
            BookDetailScreenContext(
                favoriteBookSubscriptionKeyFactory = DefaultFavoriteBookSubscriptionKeyFactory(favoriteBookDao),
                favoriteMutationKeyFactory = DefaultFavoriteMutationKeyFactory(favoriteBookDao),
            )
        }
    }

    companion object Factory : AppGraph.Factory {
        override fun create(context: Context, networkConfig: NetworkConfig): AppGraph =
            ManualAppGraph(context, networkConfig)
    }
}
