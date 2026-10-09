package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.network.MissingApiKeyException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/**
 * 화면 문구와 재시도 여부를 정하기 위한 오류 분류입니다.
 * 취소 예외는 Soil과 코루틴이 재전파하므로 이 분류에 들어오지 않아야 합니다.
 */
enum class ErrorKind {
    MissingApiKey,
    Unauthorized,
    RateLimited,
    Server,
    Client,
    Network,
    Parse,
    Unknown,
    ;

    /** 일시적인 오류만 자동 재시도합니다. */
    val isRetryable: Boolean
        get() = this == Network || this == Server || this == RateLimited
}

fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is SearchPageFetchException -> cause.toErrorKind()
    // IOException 하위이므로 네트워크 오류보다 먼저 판정합니다.
    is MissingApiKeyException -> ErrorKind.MissingApiKey
    is HttpException -> when (code()) {
        401, 403 -> ErrorKind.Unauthorized
        429 -> ErrorKind.RateLimited
        in 500..599 -> ErrorKind.Server
        else -> ErrorKind.Client
    }
    is SerializationException -> ErrorKind.Parse
    is IOException -> ErrorKind.Network
    else -> ErrorKind.Unknown
}
