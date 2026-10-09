package io.github.easyhooon.booksearch.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import io.github.easyhooon.booksearch.core.ui.R

/** 썸네일이 비었거나 불러오지 못하면 대체 표지를 보여 줍니다. */
@Composable
fun BookCover(
    thumbnailUrl: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.book_cover_description, title.ifBlank { stringResource(R.string.title_unavailable) })
    val shapeModifier = modifier
        .clip(RoundedCornerShape(4.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (thumbnailUrl.isBlank()) {
        CoverPlaceholder(shapeModifier)
        return
    }

    SubcomposeAsyncImage(
        model = thumbnailUrl,
        contentDescription = description,
        contentScale = ContentScale.Crop,
        modifier = shapeModifier,
        error = { CoverPlaceholder(Modifier) },
    )
}

@Composable
private fun CoverPlaceholder(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.cover_unavailable),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(4.dp),
        )
    }
}
