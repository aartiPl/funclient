val kotlinVersion: String = "1.8.22"

plugins {
    kotlin("jvm") version "1.8.22"
    id("com.adarshr.test-logger") version "3.2.0"
    `maven-publish`
    signing
    idea
}

group = "net.igsoft"
version = "0.5.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    withJavadocJar()
    withSourcesJar()
}

testlogger {
    showStandardStreams = true
    showFullStackTraces = false
}

tasks.test {
    useJUnitPlatform()
}

val licencesSpec = Action<MavenPomLicenseSpec> {
    license {
        name.set("MIT License")
        url.set("https://opensource.org/licenses/MIT")
    }
}

val developersSpec = Action<MavenPomDeveloperSpec> {
    developer {
        id.set("aartiPl")
        name.set("Marcin Kuszczak")
        email.set("aarti@interia.pl")
    }
}

val scmSpec = Action<MavenPomScm> {
    connection.set("scm:git:git://https://github.com/aartiPl/funclient.git")
    developerConnection.set("scm:git:ssh:https://github.com/aartiPl/funclient.git")
    url.set("https://github.com/aartiPl/funclient/tree/master")
}

publishing {
    publications {
        create<MavenPublication>("funclient") {
            artifactId = "funclient"
            from(components["java"])

            pom {
                name.set("funclient")
                description.set("FunClient - HTTP client - functional way")
                url.set("https://github.com/aartiPl/funclient")

                licenses(licencesSpec)
                developers(developersSpec)
                scm(scmSpec)
            }
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (project.version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = project.findProperty("sonatype.user") as String? ?: System.getenv("SONATYPE_USER")
                password = project.findProperty("sonatype.password") as String? ?: System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}

signing {
    sign(publishing.publications["funclient"])
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.konghq:unirest-java:3.13.13")
    implementation("com.google.code.gson:gson:2.10")

    testImplementation("org.junit.platform:junit-platform-suite-engine:1.9.0")
    testImplementation("org.junit.platform:junit-platform-suite-api:1.9.0")
    testImplementation("org.junit.platform:junit-platform-suite-commons:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.26.1")
    testImplementation("io.mockk:mockk:1.13.2")
}
