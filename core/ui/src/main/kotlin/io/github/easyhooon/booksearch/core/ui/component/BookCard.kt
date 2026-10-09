package io.github.easyhooon.booksearch.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.ui.BookFormatters
import io.github.easyhooon.booksearch.core.ui.R

/**
 * 검색·즐겨찾기 탭이 함께 쓰는 도서 카드입니다. 카드를 누르면 [onClick], 하트를 누르면
 * [onFavoriteClick]만 호출합니다. 빈 정보는 줄을 숨기지 않고 "정보 없음"으로 채웁니다.
 * [selected]는 2-pane에서 오른쪽에 상세가 열린 도서임을 나타냅니다.
 */
@Composable
fun BookCard(
    book: Book,
    isFavorite: Boolean,
    isFavoritePending: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val unavailable = stringResource(R.string.info_unavailable)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.selected = selected },
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            BookCover(
                thumbnailUrl = book.thumbnailUrl,
                title = book.title,
                modifier = Modifier.size(width = 72.dp, height = 104.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = book.title.ifBlank { stringResource(R.string.title_unavailable) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                InfoLine(BookFormatters.names(book.authors) ?: unavailable)
                InfoLine(book.publisher.ifBlank { unavailable })
                InfoLine(book.publishedDate?.let(BookFormatters::date) ?: unavailable)
                Spacer(Modifier.height(4.dp))
                PriceLine(book)
            }
            FavoriteButton(
                isFavorite = isFavorite,
                enabled = !isFavoritePending,
                onClick = onFavoriteClick,
            )
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 정가를 기본으로 보여 주고, 할인 판매가가 있으면 정가에 취소선을 긋고 함께 표시합니다. */
@Composable
fun PriceLine(book: Book, modifier: Modifier = Modifier) {
    val price = book.availablePrice

    if (price == null) {
        Text(
            text = stringResource(R.string.price_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    val discounted = book.discountedPrice

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = BookFormatters.price(price),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (discounted == null) FontWeight.SemiBold else FontWeight.Normal,
            textDecoration = if (discounted == null) null else TextDecoration.LineThrough,
            color = if (discounted == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (discounted != null) {
            Text(
                text = BookFormatters.price(discounted),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
