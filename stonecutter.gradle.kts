plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21.11"

/**
 * Builds every version and gathers the finished jars into one folder.
 *
 * <p>The per-version jars are otherwise scattered under versions/<ver>/build/libs,
 * which is awkward to hand to anyone. Collecting them is a task rather than a
 * manual copy so the folder cannot quietly go stale: it is emptied and refilled
 * from whatever the build just produced.
 */
val collectJars by tasks.registering(Copy::class) {
    group = "build"
    description = "Builds all versions and copies their jars into build/dist."

    dependsOn(stonecutter.versions.map { ":${it.project}:build" })

    into(layout.buildDirectory.dir("dist"))
    // Sources jars only exist on the obfuscated versions, so including them
    // would make the folder look inconsistent for no benefit.
    from(stonecutter.versions.map { rootProject.file("versions/${it.project}/build/libs") }) {
        include("*.jar")
        exclude("*-sources.jar")
    }

    doNotTrackState("Output is a plain folder of build products, refilled each run.")
}
