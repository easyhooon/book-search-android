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
    // Gradle daemon용 JDK 21을 자동으로 찾거나 내려받기 위한 toolchain resolver입니다.
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
