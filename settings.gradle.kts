pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        // Declared once here so the per-version script can apply it without
        // repeating a version. The snapshot line is what the official Fabric
        // template uses for 26.x, and it still handles 1.21.x.
        id("fabric-loom") version "1.17-SNAPSHOT"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        // Only 1.21.11 is built at the moment. The features added after the
        // multi-version release — combat hitboxes, the keybind list and the
        // colour picker — use APIs that arrived in 1.21.9 and changed again in
        // 26.x, so each needs its own work before those versions compile again.
        // Every version's properties file is still here, ready to be listed.
        versions("1.21.11")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "vantage"
