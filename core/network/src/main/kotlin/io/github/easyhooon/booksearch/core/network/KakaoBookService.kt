package io.github.easyhooon.booksearch.core.network

import io.github.easyhooon.booksearch.core.network.model.SearchBookResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface KakaoBookService {

    /** 실패는 예외로 전파합니다. Soil fetch가 그대로 받아 상태로 바꿉니다. */
    @GET("v3/search/book")
    suspend fun searchBooks(
        @Query("query") query: String,
        @Query("sort") sort: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): SearchBookResponse
}
