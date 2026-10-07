plugins {
    id("booksearch.android.library.compose")
}

/** feature 테스트가 공유하는 Presenter 실행 도구와 테스트 데이터입니다. 테스트 의존성으로만 씁니다. */
dependencies {
    api(projects.core.model)

    api(libs.soil.query.core)
    api(libs.soil.query.compose)
    api(libs.molecule.runtime)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.junit)
}
