import com.android.build.api.dsl.ApplicationExtension
import io.github.easyhooon.booksearch.convention.Plugins
import io.github.easyhooon.booksearch.convention.applyPlugins
import io.github.easyhooon.booksearch.convention.configureAndroid
import io.github.easyhooon.booksearch.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins(Plugins.ANDROID_APPLICATION)

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)

                defaultConfig {
                    targetSdk = libs.versions.targetSdk.get().toInt()
                    versionName = libs.versions.versionName.get()
                    versionCode = libs.versions.versionCode.get().toInt()
                }
            }
        }
    }
}
