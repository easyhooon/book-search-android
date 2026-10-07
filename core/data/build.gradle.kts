plugins {
    id("booksearch.android.library")
    alias(libs.plugins.metro)
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.database)

    api(libs.soil.query.core)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.soil.query.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
}
