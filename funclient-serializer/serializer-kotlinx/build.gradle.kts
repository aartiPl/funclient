plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":funclient-core"))
    api(libs.serialization.json)
}
