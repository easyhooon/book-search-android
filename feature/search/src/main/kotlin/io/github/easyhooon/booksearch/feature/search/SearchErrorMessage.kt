package io.github.easyhooon.booksearch.feature.search

import io.github.easyhooon.booksearch.core.data.ErrorKind
import io.github.easyhooon.booksearch.core.data.toErrorKind
import io.github.easyhooon.booksearch.core.ui.ErrorMessage

internal fun Throwable.toSearchErrorMessage(): ErrorMessage = when (toErrorKind()) {
    ErrorKind.MissingApiKey -> ErrorMessage.MissingApiKey
    ErrorKind.Unauthorized -> ErrorMessage.Unauthorized
    ErrorKind.Network -> ErrorMessage.Network
    ErrorKind.Server, ErrorKind.RateLimited -> ErrorMessage.Temporary
    ErrorKind.Client, ErrorKind.Parse, ErrorKind.Unknown -> ErrorMessage.Generic
}
