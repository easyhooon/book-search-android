plugins {
    id("booksearch.android.library.compose")
}

dependencies {
    api(projects.core.model)

    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    implementation(libs.coil.compose)
    implementation(libs.soil.query.core)
    implementation(libs.soil.query.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)

    testImplementation(projects.core.data)
    testImplementation(projects.core.database)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
}
