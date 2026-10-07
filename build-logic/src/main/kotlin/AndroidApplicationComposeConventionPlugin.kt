import com.android.build.api.dsl.ApplicationExtension
import io.github.easyhooon.booksearch.convention.Plugins
import io.github.easyhooon.booksearch.convention.applyPlugins
import io.github.easyhooon.booksearch.convention.configureCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins(
                "booksearch.android.application",
                Plugins.KOTLIN_COMPOSE,
            )

            extensions.configure<ApplicationExtension> {
                configureCompose(this)
            }
        }
    }
}
