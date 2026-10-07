package io.github.easyhooon.booksearch.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureCompose(extension: CommonExtension) {
    extension.apply {
        buildFeatures.compose = true
    }

    dependencies {
        val bom = platform(libs.androidx.compose.bom)
        implementation(bom)
        implementation(libs.androidx.compose.runtime)
        implementation(libs.androidx.compose.runtime.retain)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.ui.tooling.preview)
        debugImplementation(libs.androidx.compose.ui.tooling)

        androidTestImplementation(bom)
        testImplementation(bom)
    }
}
