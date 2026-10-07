import io.github.easyhooon.booksearch.convention.ApplicationConstants
import io.github.easyhooon.booksearch.convention.Plugins
import io.github.easyhooon.booksearch.convention.applyPlugins
import io.github.easyhooon.booksearch.convention.libs
import io.github.easyhooon.booksearch.convention.testImplementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins(
                Plugins.JAVA_LIBRARY,
                Plugins.KOTLIN_JVM,
            )

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = ApplicationConstants.javaVersion
                targetCompatibility = ApplicationConstants.javaVersion
            }

            extensions.configure<KotlinProjectExtension> {
                jvmToolchain(ApplicationConstants.JAVA_VERSION_INT)
            }

            dependencies {
                testImplementation(libs.junit)
            }
        }
    }
}
