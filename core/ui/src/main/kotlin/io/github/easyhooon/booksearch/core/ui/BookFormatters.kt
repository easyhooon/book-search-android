package io.github.easyhooon.booksearch.core.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 화면 표시 형식입니다. 값이 없을 때의 대체 문구는 호출하는 쪽에서 리소스로 채웁니다. */
object BookFormatters {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)

    /** `#,###원` 형식입니다. 0원도 그대로 표시합니다. */
    fun price(value: Int): String =
        DecimalFormat("#,###", DecimalFormatSymbols(Locale.KOREA)).format(value.toLong()).let {
            "${if (value == 0) "0" else it}원"
        }

    fun date(value: LocalDate): String = value.format(dateFormatter)

    fun names(values: List<String>): String? = values.joinToString(", ").takeIf { it.isNotBlank() }
}
