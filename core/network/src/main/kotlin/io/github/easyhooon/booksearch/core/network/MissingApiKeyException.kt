package io.github.easyhooon.booksearch.core.network

import java.io.IOException

/**
 * API 키가 설정되지 않아 요청을 보내지 않았음을 뜻합니다.
 *
 * OkHttp interceptor에서 던질 수 있도록 [IOException]을 상속하지만,
 * 오류 분류에서는 네트워크 오류보다 먼저 판정해 재시도하지 않습니다.
 */
class MissingApiKeyException : IOException("Kakao REST API key is not configured")
