package io.github.easyhooon.booksearch.core.model

/** 원격 검색 정렬. API 파라미터 값은 core:data에서 대응시킵니다. */
enum class SearchSort {
    Accuracy,
    Latest,
}

/** 즐겨찾기 목록 정렬. 기본값은 제목 오름차순입니다. */
enum class FavoriteSort {
    TitleAscending,
    TitleDescending,
}
