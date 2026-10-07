package io.github.easyhooon.booksearch.initializer

import android.content.Context
import androidx.startup.Initializer
import io.github.easyhooon.booksearch.BuildConfig
import timber.log.Timber

/**
 * debug 빌드에서만 [Timber.DebugTree]를 심습니다.
 *
 * release 빌드에는 Tree를 심지 않으므로 Timber 호출이 아무 출력도 남기지 않습니다.
 * `InitializationProvider`가 `Application.onCreate`보다 먼저 실행합니다.
 */
class TimberInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
