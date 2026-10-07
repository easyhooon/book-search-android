package io.github.easyhooon.booksearch.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.easyhooon.booksearch.core.model.PriceFilter
import kotlinx.coroutines.launch

/**
 * 정가 최소·최대 금액을 입력받아 "적용"으로 확정합니다.
 * 입력 중인 값은 시트 안에만 두고, 최소가 최대보다 크면 적용 버튼을 막습니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PriceFilterSheet(
    current: PriceFilter,
    onApply: (PriceFilter) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    var minText by rememberSaveable { mutableStateOf(current.min?.toString().orEmpty()) }
    var maxText by rememberSaveable { mutableStateOf(current.max?.toString().orEmpty()) }
    val draft = PriceFilter(min = PriceInput.parse(minText), max = PriceInput.parse(maxText))

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.price_filter), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.price_filter_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriceField(
                    value = minText,
                    label = stringResource(R.string.price_filter_min),
                    onValueChange = { minText = PriceInput.sanitize(it) },
                    isError = !draft.isValid,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(FavoritesTestTags.PRICE_MIN_FIELD),
                )
                Text("~", modifier = Modifier.padding(horizontal = 8.dp))
                PriceField(
                    value = maxText,
                    label = stringResource(R.string.price_filter_max),
                    onValueChange = { maxText = PriceInput.sanitize(it) },
                    isError = !draft.isValid,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(FavoritesTestTags.PRICE_MAX_FIELD),
                )
            }
            if (!draft.isValid) {
                Text(
                    text = stringResource(R.string.price_filter_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(
                    onClick = {
                        onClear()
                        close()
                    },
                    enabled = current.isActive || draft.isActive,
                ) {
                    Text(stringResource(R.string.price_filter_clear))
                }
                Button(
                    onClick = {
                        onApply(draft)
                        close()
                    },
                    enabled = draft.isValid,
                ) {
                    Text(stringResource(R.string.price_filter_apply))
                }
            }
        }
    }
}

@Composable
private fun PriceField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        suffix = { Text(stringResource(R.string.price_filter_unit)) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
    )
}
