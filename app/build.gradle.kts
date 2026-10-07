import io.github.easyhooon.booksearch.convention.localProperty

plugins {
    id("booksearch.android.application.compose")
    alias(libs.plugins.metro)
}

android {
    defaultConfig {
        applicationId = "io.github.easyhooon.booksearch"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 키가 없어도 빌드는 성공해야 하므로 빈 문자열을 넣고, 검색 화면에서 키 없음 상태로 안내합니다.
        val kakaoRestApiKey = localProperty("KAKAO_REST_API_KEY").orEmpty()
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"$kakaoRestApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(projects.feature.search)
    implementation(projects.feature.favorites)
    implementation(projects.feature.detail)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.runtime.retain)
    implementation(libs.soil.query.core)
    implementation(libs.soil.query.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
