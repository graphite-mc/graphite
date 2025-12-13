plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.graphite.renderer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}