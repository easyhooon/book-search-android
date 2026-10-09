rootProject.name = "book-search-android"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // 필요한 Java toolchain이 설치되지 않은 환경에서 다운로드를 지원합니다.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(
    ":app",

    ":core:model",
    ":core:network",
    ":core:database",
    ":core:data",
    ":core:ui",
    ":core:testing",

    ":feature:search",
    ":feature:favorites",
    ":feature:detail",
)
