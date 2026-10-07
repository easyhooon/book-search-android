plugins {
    id("booksearch.android.library")
    id("booksearch.kotlin.library.serialization")
    alias(libs.plugins.metro)
}

dependencies {
    implementation(projects.core.model)

    api(libs.okhttp)
    api(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.timber)

    testImplementation(libs.okhttp.mockwebserver)
}
