import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    base
    idea
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.maven.publish) apply false
}

val funclientVersion = libs.versions.funclient.get()

allprojects {
    group = "net.igsoft.funclient"
    version = funclientVersion
}

// Only these two projects produce libraries. The root and adapter group are aggregators.
configure(listOf(project(":funclient-core"), project(":funclient-serializer:serializer-kotlinx"))) {
    repositories {
        mavenCentral()
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(libs.versions.jvm.get().toInt())
        }

        dependencies {
            "testImplementation"(libs.junit.jupiter.api)
            "testImplementation"(libs.junit.jupiter.params)
            "testRuntimeOnly"(libs.junit.jupiter.engine)
            "testRuntimeOnly"(libs.junit.platform.launcher)
            "testImplementation"(libs.assertk)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            val artifactId = if (project.name == "serializer-kotlinx") "funclient-serializer-kotlinx" else project.name
            coordinates(group.toString(), artifactId, version.toString())
            pom {
                name.set(artifactId)
                description.set(if (project.name == "funclient-core") "FunClient HTTP transport and serialization contract" else "FunClient kotlinx.serialization JSON adapter")
                url.set("https://github.com/aartiPl/funclient")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("aartiPl")
                        name.set("Marcin Kuszczak")
                        email.set("aarti@interia.pl")
                        organization.set("Marcin Kuszczak")
                        organizationUrl.set("https://github.com/aartiPl")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/aartiPl/funclient.git")
                    developerConnection.set("scm:git:ssh://git@github.com/aartiPl/funclient.git")
                    url.set("https://github.com/aartiPl/funclient")
                }
            }
            publishToMavenCentral()
            signAllPublications()
        }
    }
}

tasks.named("build") {
    dependsOn(":funclient-core:build", ":funclient-serializer:serializer-kotlinx:build")
}

tasks.named("check") {
    dependsOn(":funclient-core:check", ":funclient-serializer:serializer-kotlinx:check")
}
