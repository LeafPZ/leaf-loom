@file:Suppress("AvoidDuplicateDependencies")

import com.diffplug.spotless.LineEnding
import groovy.json.JsonOutput
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.nio.charset.Charset
import java.util.*

val isCiBuild = providers.environmentVariable("CI").map { it.toBoolean() }.orElse(false).get()
val isSnapshot = providers.gradleProperty("isSnapshot").map { it.toBoolean() }.orElse(false).get()

var groupUrl = rootProject.group.toString().replace(".", "/")

val baseVersion = project.version.toString()
project.version = if (isSnapshot) "$baseVersion-SNAPSHOT" else if (!isCiBuild) "$baseVersion.local" else baseVersion

// We must build against the version of Kotlin Gradle ships with.
val props = Properties()
Project::class.java.classLoader.getResource("gradle-kotlin-dsl-versions.properties")!!
    .openStream().use { output ->
        props.load(output)
    }

val kotlinVersion: String? = props.getProperty("kotlin")
if (libs.versions.kotlin.get() != kotlinVersion) {
    throw IllegalStateException("Requires Kotlin version: $kotlinVersion")
}

repositories {
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "aoqia"
        url = uri("https://maven.aoqia.dev/releases")

        mavenContent {
            releasesOnly()
        }
    }
    mavenCentral()
}

plugins {
    java
    idea
    eclipse
    groovy
    checkstyle

    alias(libs.plugins.kotlin)
    alias(libs.plugins.spotless)
    alias(libs.plugins.retry)

    // Generating the Gradle plugin marker POM
    alias(libs.plugins.gradle.plugin.publish)

    `maven-publish`
    signing

    id("versions")
    id("package-info")
}

configurations.configureEach {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
}

configurations.named(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME) {
    attributes {
        attribute(
            GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
            objects.named(
                GradlePluginApiVersion::class.java,
                GradleVersion.current().version
            )
        )
    }
}

sourceSets {
    create("commonDecompiler") {
        java {
            srcDir("src/decompilers/common")
        }
    }

    create("cfr") {
        java {
            srcDir("src/decompilers/cfr")
        }
    }

    create("vineflower") {
        java {
            srcDir("src/decompilers/vineflower")
        }
    }
}

dependencies {
    implementation(gradleApi())

    // libraries
    implementation(libs.commons.io)
    implementation(libs.gson)
    implementation(libs.bundles.asm)

    // game handling utils
    implementation(libs.fabric.stitch) {
        exclude(module = "enigma")
        exclude(module = "guava")
    }

    // tinyfile management
    implementation(libs.fabric.clazz.tweaker)
    implementation(libs.fabric.tiny.remapper)
    implementation(libs.fabric.mapping.io)
    implementation(libs.fabric.lorenz.tiny) {
        isTransitive = false
    }

    // decompilers
    "cfrCompileOnly"(runtimeLibs.cfr)
    "cfrCompileOnly"(libs.fabric.mapping.io)

    "vineflowerCompileOnly"(runtimeLibs.vineflower)
    "vineflowerCompileOnly"(libs.fabric.mapping.io)

    "cfrApi"(sourceSets.named("commonDecompiler").get().output)
    "vineflowerApi"(sourceSets.named("commonDecompiler").get().output)

    implementation(sourceSets.named("commonDecompiler").get().output)
    implementation(sourceSets.named("cfr").get().output)
    implementation(sourceSets.named("vineflower").get().output)

    // source code remapping
    implementation(libs.fabric.mercury)
    implementation(libs.fabric.mercury.mixin)

    implementation(libs.fabric.unpick)
    implementation(libs.fabric.unpick.utils)

    // Kotlin
    implementation(libs.kotlin.metadata) {
        isTransitive = false
    }

    // Kapt integration
    compileOnly(libs.kotlin.gradle.plugin)

    // Testing
    testImplementation(gradleTestKit())
    testImplementation(testLibs.spock) {
        exclude(module = "groovy-all")
    }
    testImplementation(testLibs.junit.jupiter.engine)
    testRuntimeOnly(testLibs.junit.platform.launcher)
    testImplementation(testLibs.javalin) {
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation(testLibs.mockito)
    testImplementation(testLibs.java.debug)
    testImplementation(testLibs.bcprov)
    testImplementation(testLibs.bcutil)
    testImplementation(testLibs.bcpkix)
    testImplementation(testLibs.leaf.loader)
    testImplementation(runtimeLibs.cfr)
    testImplementation(runtimeLibs.vineflower)

    compileOnly(runtimeLibs.jetbrains.annotations)
    testCompileOnly(runtimeLibs.jetbrains.annotations)

    testCompileOnly(testLibs.mixin) {
        isTransitive = false
    }
}

java {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
}

// The publish tasks appear to write to the same files, so we need to ensure they don't run in parallel.
val publishTasks = tasks.withType<PublishToMavenLocal>()
publishTasks.configureEach {
    mustRunAfter(publishTasks.matching { it.name < this.name })
}

// Workaround for https://youtrack.jetbrains.com/issue/KT-46466
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

tasks.withType<Sign>().configureEach {
    enabled = isCiBuild && !isSnapshot
}

tasks.jar {
    manifest {
        attributes("Implementation-Version" to project.version)
    }

    from(sourceSets.named("commonDecompiler").get().output.classesDirs)
    from(sourceSets.named("cfr").get().output.classesDirs)
    from(sourceSets.named("vineflower").get().output.classesDirs)
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as CoreJavadocOptions).addBooleanOption("html5", true)
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register<DownloadGradleSources>("downloadGradleSources") {
    description = "Downloads the Gradle API sources next to the API jar." +
        " May require you to manually attach the sources jar."

    gradleApi.from(configurations.detachedConfiguration(dependencies.gradleApi()))
    gradleVersion.set(GradleVersion.current().version)
}

tasks.named<Test>("test") {
    description = "Run tests"

    maxHeapSize = "2560m"
    jvmArgs = listOf("-XX:+HeapDumpOnOutOfMemoryError")
    useJUnitPlatform()

    // Forward system prop onto tests.
    val prop = System.getProperty("leaf.${rootProject.name}.test.homeDir")
    if (prop != null && prop.isNotEmpty()) {
        systemProperty("leaf.${rootProject.name}.test.homeDir", prop)
    }

    if (isCiBuild) {
        retry {
            maxRetries = 3
        }
    }

    testLogging {
        // Log everything to the console
        setEvents(TestLogEvent.entries)
    }
}

tasks.register<PrintActionsTestName>("printActionsTestName") {
    description = "Replaces invalid characters in test names for GitHub Actions artifacts."
}

tasks.register("exportVersions") {
    description = "Exports the project version to a file for CI"

    val versionFile = file("${layout.buildDirectory.get()}/version.txt")
    outputs.file(versionFile)

    val v = "${project.version}"
    doLast {
        versionFile.writeText(v)
        println("Exported version: $v")
    }
}

tasks.register("writeActionsTestMatrix") {
    description = "Outputs a JSON file with a list of all the tests to run."

    doLast {
        val testMatrix = mutableListOf<String>()
        val extendedTests = System.getenv("EXTENDED_TESTS").toBoolean()

        file("src/test/groovy/${groupUrl}/${rootProject.name}/test/integration").listFiles()?.forEach {
            if (it.name.endsWith("Test.groovy")) {
                if (it.name.endsWith("ReproducibleBuildTest.groovy")) {
                    // This test gets a special case to run across all os's
                    return@forEach
                }

                if (it.name.endsWith("DebugLineNumbersTest.groovy") && !extendedTests) {
                    // Known flakey test
                    return@forEach
                }

                val className = it.name.replace(".groovy", "")
                testMatrix.add("${rootProject.group}.${rootProject.name}.test.integration.${className}")
            } else if (it.name.endsWith("noRemap")) {
                testMatrix.add("${rootProject.group}.${rootProject.name}.test.integration.noRemap.*")
            }
        }

        // Run all the unit tests together
        testMatrix.add("net.fabricmc.loom.test.unit.*")

        val output = file("build/test_matrix.json")
        output.parentFile.mkdir()
        output.writeText(JsonOutput.toJson(testMatrix))
    }
}

// Workaround https://github.com/gradle/gradle/issues/25898
tasks.withType<Test>().configureEach {
    jvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED"
    )
}

spotless {
    setEncoding(Charset.forName("UTF-8"))
    setLineEndings(LineEnding.UNIX)

    java {
        targetExclude("**/generated/**")

        eclipse().configFile(file("eclipse-formatter.xml"))
            .sortMembersEnabled(true)
            // SF,SI,SM,F,I,C,M,T = Static Fields, Static Initializers, Static Methods,
            //   Fields, Initializers, Constructors, Methods, (Nested) Types
            .sortMembersOrder("SF,F,SI,I,C,SM,M,T")
            .sortMembersDoNotSortFields(true)
            .sortMembersVisibilityOrderEnabled(true)
            // B,R,D,V = Public, Protected, Package, Private
            .sortMembersVisibilityOrder("B,R,D,V")

        cleanthat()
        // formatAnnotations()

        importOrder(
            "java|javax",
            "",
            "net.fabricmc",
            "\\#net.fabricmc",
            "$group",
            "\\#",
            "\\#$group",
            "zombie",
            "\\#zombie"
        )
        removeUnusedImports()
        forbidWildcardImports()
        forbidModuleImports()

        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()

        toggleOffOn()
        licenseHeaderFile(file("HEADER")).yearSeparator("-").onlyIfContentMatches("^[^/]{2}")
    }

//    groovy {
//        importOrder("java|javax", "groovy", "", "net.fabricmc", "\\#net.fabricmc", "$group", "\\#", "\\#$group")
//
//        removeSemicolons()
//        greclipse()
//
//        trimTrailingWhitespace()
//        endWithNewline()
//
//        licenseHeaderFile(file("HEADER")).yearSeparator("-").updateYearWithLatest(true)
//    }
//
//    groovyGradle {
//        target("src/**/*.gradle", "*.gradle")
//        targetExclude("**/build.gradle")
//
//        greclipse()
//
//        trimTrailingWhitespace()
//        endWithNewline()
//    }

    kotlin {
        targetExclude("src/test/resources/projects/*/**")

        ktlint()

        trimTrailingWhitespace()
        endWithNewline()

        toggleOffOn()
        licenseHeaderFile(file("HEADER")).yearSeparator("-").onlyIfContentMatches("^[^/]{2}")
    }

//    kotlinGradle {
//        ktlint()
//    }
}

checkstyle {
    configFile = file("checkstyle.xml")
}

gradlePlugin {
    website = property("url").toString()
    vcsUrl = property("url").toString()

    plugins {
        register("leafLoom") {
            id = "${rootProject.group}.${rootProject.name}"
            implementationClass = "${rootProject.group}.${rootProject.name}.LoomGradlePlugin"
        }
        register("leafLoomRemap") {
            id = "${rootProject.group}.${rootProject.name}-remap"
            implementationClass = "${rootProject.group}.${rootProject.name}.LoomRemapGradlePlugin"
        }
        register("leafLoomCompanion") {
            id = "${rootProject.group}.${rootProject.name}-companion"
            implementationClass = "${rootProject.group}.${rootProject.name}.LoomCompanionGradlePlugin"
        }
        register("leafLoomRepositories") {
            id = "${rootProject.group}.${rootProject.name}-repositories"
            implementationClass = "${rootProject.group}.${rootProject.name}.LoomRepositoryPlugin"
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = rootProject.name
            group = rootProject.group
            description = rootProject.description
            url = property("url").toString()
            inceptionYear = "2025"

            developers {
                developer {
                    id = "aoqia"
                    name = "aoqia"
                    email = "aoqia@aoqia.dev"
                }
            }

            issueManagement {
                system = "GitHub"
                url = "${property("url").toString()}/issues"
            }

            licenses {
                license {
                    name = "MIT"
                    url = "https://spdx.org/licenses/MIT.html"
                }
            }

            scm {
                connection = "scm:git:${property("url").toString()}.git"
                developerConnection = "scm:git:${property("url").toString().replace("https", "ssh")}.git"
                url = property("url").toString()
            }
        }
    }

    repositories {
        maven {
            name = "leaf"
            url = uri("https://maven.aoqia.dev/${if (isSnapshot) "snapshots" else "releases"}")

            credentials {
                username = providers.gradleProperty("mavenUsername").orNull
                password = providers.gradleProperty("mavenPassword").orNull
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

signing {
    isRequired = isCiBuild and !isSnapshot

    val signingKey = providers.gradleProperty("signingKey")
    val signingPassword = providers.gradleProperty("signingPassword")
    if (signingKey.isPresent && signingPassword.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
    }

    sign(publishing.publications)
}
