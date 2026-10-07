import com.android.build.api.dsl.LibraryExtension
import io.github.easyhooon.booksearch.convention.Plugins
import io.github.easyhooon.booksearch.convention.applyPlugins
import io.github.easyhooon.booksearch.convention.configureAndroid
import io.github.easyhooon.booksearch.convention.libs
import io.github.easyhooon.booksearch.convention.testImplementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins(Plugins.ANDROID_LIBRARY)

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
            }

            dependencies {
                testImplementation(libs.junit)
                testImplementation(libs.kotlinx.coroutines.test)
                testImplementation(libs.turbine)
            }
        }
    }
}
