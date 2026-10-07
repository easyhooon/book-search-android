package io.github.easyhooon.booksearch.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureAndroid(extension: CommonExtension) {
    extension.apply {
        namespace = ApplicationConstants.NAMESPACE_PREFIX + path.replace(':', '.').replace('-', '_')
            .removePrefix(".app")
        compileSdk = libs.versions.compileSdk.get().toInt()

        defaultConfig.apply {
            minSdk = libs.versions.minSdk.get().toInt()
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions.apply {
            sourceCompatibility = ApplicationConstants.javaVersion
            targetCompatibility = ApplicationConstants.javaVersion
        }

        testOptions.apply {
            unitTests.isIncludeAndroidResources = true
            unitTests.all {
                // Robolectric 4.17이 SDK 36 이상에서 FileDescriptor 내부를 다룰 때 필요합니다.
                it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
            }
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
}
