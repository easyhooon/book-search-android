package io.github.easyhooon.booksearch.core.network

/**
 * app 모듈이 BuildConfig에서 읽어 앱 그래프에 넘기는 네트워크 설정입니다.
 * 키 값이 로그나 예외 메시지에 노출되지 않도록 [toString]에서 가립니다.
 */
class NetworkConfig(
    val apiKey: String,
    val enableHttpLog: Boolean,
    val baseUrl: String = KAKAO_BASE_URL,
) {
    override fun toString(): String {
        val key = if (apiKey.isBlank()) "<missing>" else "<redacted>"
        return "NetworkConfig(apiKey=$key, enableHttpLog=$enableHttpLog, baseUrl=$baseUrl)"
    }

    companion object {
        const val KAKAO_BASE_URL = "https://dapi.kakao.com/"
    }
}
