pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net")
        maven {
            name = "LegacyFabric"
            url = uri("https://repo.legacyfabric.net/legacyfabric")
        }
    }
}

rootProject.name = "graphite"
include("math")