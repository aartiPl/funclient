val kotlinVersion = "2.4.0"

plugins {
    idea
    kotlin("jvm") version "2.4.0"
    id("com.adarshr.test-logger") version "4.0.0"
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "net.igsoft"
version = "0.6.2"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

testlogger {
    showStandardStreams = true
    showFullStackTraces = false
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    pom {
        name.set("funclient")
        description.set("FunClient - HTTP client - functional way")
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
            }
        }
        scm {
            connection.set("scm:git:git://https://github.com/aartiPl/funclient.git")
            developerConnection.set("scm:git:ssh:https://github.com/aartiPl/funclient.git")
            url.set("https://github.com/aartiPl/funclient/tree/master")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("com.google.code.gson:gson:2.14.0")

    val junitVersion = "6.1.0"
    testImplementation("org.junit.platform:junit-platform-suite-engine:$junitVersion")
    testImplementation("org.junit.platform:junit-platform-suite-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testImplementation("io.mockk:mockk:1.14.11")
}
