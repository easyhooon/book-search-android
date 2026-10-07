import io.github.easyhooon.booksearch.convention.Plugins
import io.github.easyhooon.booksearch.convention.applyPlugins
import io.github.easyhooon.booksearch.convention.implementation
import io.github.easyhooon.booksearch.convention.libs
import io.github.easyhooon.booksearch.convention.project
import io.github.easyhooon.booksearch.convention.testImplementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 화면 모듈 공통 설정입니다. feature는 core:model·core:data·core:ui만 보고,
 * Retrofit·Room 타입은 core:data 뒤에 숨겨 둡니다. NavKey 직렬화를 위해 serialization 플러그인을 씁니다.
 */
internal class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins(
                "booksearch.android.library.compose",
                Plugins.METRO,
                Plugins.KOTLINX_SERIALIZATION,
            )

            dependencies {
                implementation(project(path = ":core:model"))
                implementation(project(path = ":core:data"))
                implementation(project(path = ":core:ui"))

                implementation(libs.soil.query.core)
                implementation(libs.soil.query.compose)
                implementation(libs.androidx.navigation3.runtime)
                implementation(libs.kotlinx.coroutines.core)

                testImplementation(project(path = ":core:testing"))
                testImplementation(libs.molecule.runtime)
                testImplementation(libs.soil.query.test)
                testImplementation(libs.robolectric)
                testImplementation(libs.androidx.compose.ui.test.junit4)
                testImplementation(libs.androidx.compose.ui.test.manifest)
            }
        }
    }
}
