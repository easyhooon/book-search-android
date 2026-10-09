package io.github.easyhooon.booksearch.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.ui.BookFormatters
import io.github.easyhooon.booksearch.core.ui.component.BookCover
import io.github.easyhooon.booksearch.core.ui.component.FavoriteButton
import io.github.easyhooon.booksearch.core.ui.R as CoreUiR

private val ContentMaxWidth = 720.dp
private val CoverWidth = 180.dp
private const val NORMAL_SALE_STATUS = "정상판매"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    uiState: BookDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage = uiState.userMessage?.let { stringResource(it.resId) }
    val eventSink by rememberUpdatedState(uiState.eventSink)
    val book = uiState.book

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(userMessage)
            eventSink(BookDetailEvent.UserMessageShown)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = book.title.ifBlank { stringResource(CoreUiR.string.title_unavailable) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
                actions = {
                    FavoriteButton(
                        isFavorite = uiState.isFavorite,
                        enabled = uiState.isFavoriteEnabled,
                        onClick = { uiState.eventSink(BookDetailEvent.ToggleFavorite) },
                        modifier = Modifier.testTag(BookDetailTestTags.FAVORITE_BUTTON),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BookDetailBody(
                book = book,
                modifier = Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun BookDetailBody(book: Book, modifier: Modifier = Modifier) {
    val unavailable = stringResource(CoreUiR.string.info_unavailable)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BookCover(
            thumbnailUrl = book.thumbnailUrl,
            title = book.title,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(CoverWidth)
                .aspectRatio(CoverAspectRatio),
        )
        Text(
            text = book.title.ifBlank { stringResource(CoreUiR.string.title_unavailable) },
            style = MaterialTheme.typography.headlineSmall,
        )
        HorizontalDivider()
        InfoRow(stringResource(R.string.detail_authors), BookFormatters.names(book.authors) ?: unavailable)
        BookFormatters.names(book.translators)?.let { InfoRow(stringResource(R.string.detail_translators), it) }
        InfoRow(stringResource(R.string.detail_publisher), book.publisher.ifBlank { unavailable })
        InfoRow(stringResource(R.string.detail_published_date), book.publishedDate?.let(BookFormatters::date) ?: unavailable)
        InfoRow(stringResource(R.string.detail_isbn), listOfNotNull(book.isbn13, book.isbn10).joinToString(" / ").ifBlank { unavailable })
        InfoRow(
            stringResource(R.string.detail_price),
            book.availablePrice?.let(BookFormatters::price) ?: stringResource(CoreUiR.string.price_unavailable),
        )
        InfoRow(
            stringResource(R.string.detail_sale_price),
            book.discountedPrice?.let(BookFormatters::price) ?: stringResource(CoreUiR.string.no_discount),
        )
        if (book.status.isNotBlank() && book.status != NORMAL_SALE_STATUS) {
            InfoRow(stringResource(R.string.detail_status), book.status)
        }
        HorizontalDivider()
        Text(stringResource(R.string.detail_contents), style = MaterialTheme.typography.titleMedium)
        Text(
            text = book.contents.ifBlank { unavailable },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** 국내 도서 표지의 일반적인 세로 비율(약 1:1.45)입니다. */
private const val CoverAspectRatio = 1f / 1.45f

object BookDetailTestTags {
    const val FAVORITE_BUTTON = "detail-favorite-button"
}
