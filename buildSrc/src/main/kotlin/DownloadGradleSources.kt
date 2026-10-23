import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI

abstract class DownloadGradleSources : DefaultTask() {
    @get:Classpath
    abstract val gradleApi: ConfigurableFileCollection

    @get:Input
    abstract val gradleVersion: Property<String>

    @TaskAction
    fun run() {
        // Awful hack to find the gradle api location
        val gradleApiFile = gradleApi.files.find {
            it.name.startsWith("gradle-api")
        }

        if (gradleApiFile == null) {
            throw GradleException("Unable to find Gradle API jar")
        }

        val gradleApiSources = File(gradleApiFile.absolutePath.replace(".jar", "-sources.jar"))
        val url = "https://services.gradle.org/distributions/gradle-${gradleVersion.get()}-src.zip"

        gradleApiSources.delete()

        println("Downloading (${url}) to (${gradleApiSources})")

        gradleApiSources.outputStream().use { output ->
            URI(url).toURL().file.byteInputStream().use { input -> output.write(input.readBytes()) }
        }
    }
}
