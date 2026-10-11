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
        buildConfigField("String", "KAKAO_API_BASE_URL", "\"https://dapi.kakao.com/\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        create("e2e") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".e2e"
            matchingFallbacks += "debug"
            val realApiSmoke = providers.gradleProperty("bookSearchE2eRealApi").orNull == "true"
            val apiKey = if (realApiSmoke) localProperty("KAKAO_REST_API_KEY").orEmpty() else "fixture-only"
            check(!realApiSmoke || apiKey.isNotBlank()) { "Real API smoke requires a local API key" }
            val baseUrl = if (realApiSmoke) "https://dapi.kakao.com/" else "http://127.0.0.1:8877/"
            buildConfigField("String", "KAKAO_REST_API_KEY", "\"$apiKey\"")
            buildConfigField("String", "KAKAO_API_BASE_URL", "\"$baseUrl\"")
        }
        create("e2eNoKey") {
            initWith(getByName("e2e"))
            applicationIdSuffix = ".e2e"
            buildConfigField("String", "KAKAO_REST_API_KEY", "\"\"")
            buildConfigField("String", "KAKAO_API_BASE_URL", "\"http://127.0.0.1:8877/\"")
        }
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
    implementation(libs.androidx.startup.runtime)
    implementation(libs.timber)

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
