package io.github.easyhooon.booksearch.core.ui.component

import androidx.compose.ui.unit.dp

/** 두 목록 탭이 같은 폭 규칙을 쓰도록 모아 둡니다. */
object BookListDefaults {
    /** 넓은 화면에서 검색창과 정렬 줄이 지나치게 늘어나지 않게 합니다. */
    val QueryBarMaxWidth = 640.dp

    /** 이 폭을 기준으로 화면 너비에 맞춰 열 수가 늘어납니다. */
    val GridMinCellWidth = 360.dp
}
