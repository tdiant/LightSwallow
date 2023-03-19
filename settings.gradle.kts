rootProject.name = "LightSwallow"

pluginManagement {
    val quarkusPluginVersion: String by settings

    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    plugins {
        id("io.quarkus") version quarkusPluginVersion
    }
}

include(":lightswallow-core")
include(":lightswallow-server")