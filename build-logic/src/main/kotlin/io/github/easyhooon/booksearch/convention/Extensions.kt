package io.github.easyhooon.booksearch.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import java.util.Properties

internal val Project.libs
    get() = the<LibrariesForLibs>()

internal fun Project.applyPlugins(vararg plugins: String) {
    plugins.forEach(pluginManager::apply)
}

/**
 * local.properties에서 값을 읽습니다. 파일이나 키가 없으면 null을 돌려 빌드를 막지 않습니다.
 * AGP 내부 API(gradleLocalProperties)에 의존하지 않도록 직접 읽습니다.
 */
fun Project.localProperty(propertyKey: String): String? {
    val file = rootProject.file("local.properties")
    if (!file.isFile) return null
    val properties = Properties()
    file.inputStream().use(properties::load)
    return properties.getProperty(propertyKey)?.trim()?.takeIf(String::isNotEmpty)
}
