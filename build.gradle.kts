plugins {
    id("fabric-loom")
}

/**
 * Central build script, run once per Minecraft version under versions/.
 *
 * Everything that differs between versions comes from that version's own
 * gradle.properties rather than from branching here, so adding a version means
 * adding a properties file and a line in settings.gradle.kts.
 */

fun prop(name: String): String = project.property(name).toString()

val minecraftVersion = prop("minecraft_version")
val javaVersion = prop("java_version").toInt()

/**
 * Minecraft ships obfuscated up to 1.21.11 and unobfuscated from 26.1 onwards.
 * That single fact decides three things: whether mappings are needed at all,
 * whether dependencies have to be remapped, and whether the jar does.
 */
val obfuscated = prop("obfuscated").toBoolean()

version = "${prop("mod_version")}+mc$minecraftVersion"
group = prop("maven_group")

base {
    archivesName = "${prop("archives_base_name")}-$minecraftVersion"
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    // Configurations are named as strings rather than through Kotlin's
    // generated accessors: with obfuscation off, Loom never creates "mappings"
    // or "modImplementation", and the accessor for a missing configuration
    // fails to compile even on the branch that would not run.
    "minecraft"("com.mojang:minecraft:$minecraftVersion")

    if (obfuscated) {
        "mappings"(loom.officialMojangMappings())
        "modImplementation"("net.fabricmc:fabric-loader:${prop("loader_version")}")
        "modImplementation"("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")
    } else {
        // Nothing to map, so these are ordinary dependencies.
        "implementation"("net.fabricmc:fabric-loader:${prop("loader_version")}")

        // The fabric-api bundle carries no classes of its own — it only nests
        // the real module jars, which Loom unpacks as part of remapping. With
        // remapping off that never happens, so the modules this mod uses are
        // named individually. fabricApi resolves each one's version from the
        // bundle, keeping fabric_version the single place a version is stated.
        listOf(
            "fabric-lifecycle-events-v1",
            "fabric-key-mapping-api-v1",
            "fabric-message-api-v1",
            "fabric-networking-api-v1",
            "fabric-rendering-v1",
            "fabric-events-interaction-v0"
        ).forEach { module ->
            "implementation"(fabricApi.module(module, prop("fabric_version")))
        }
    }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

/*
 * MC 26.x renamed the class GUI drawing goes through, along with several of its
 * methods, without changing what any of them do. Pure renames are applied as
 * source replacements: spelling them out at each of the forty-odd call sites
 * would bury the code they sit in for no gain.
 */
stonecutter.replacements {
    if (!obfuscated) {
        // 26.x splits drawing into an extract pass and a submit pass, which
        // renamed every override that draws. The bodies are unchanged, so these
        // match full parameter lists: that keeps them off same-named methods
        // that are ours rather than overrides, such as StatusEffectHud.render.
        //
        // Each rule spells out the renamed parameter type as well, because only
        // one replacement is applied per line — leaving the type to the general
        // rule below would silently skip it on exactly these lines.
        string(true) {
            replace(
                "render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)",
                "extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)"
            )
        }
        string(true) {
            replace(
                "super.render(graphics, mouseX, mouseY, partialTick)",
                "super.extractRenderState(graphics, mouseX, mouseY, partialTick)"
            )
        }
        string(true) {
            replace(
                "render(GuiGraphics graphics, DeltaTracker deltaTracker)",
                "extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker)"
            )
        }
        string(true) {
            replace(
                "renderImage(Font font, int x, int y, int width, int height, GuiGraphics graphics)",
                "extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics)"
            )
        }

        string(true) { replace("GuiGraphics", "GuiGraphicsExtractor") }
        string(true) { replace("drawCenteredString", "centeredText") }
        string(true) { replace("drawString", "text") }
        string(true) { replace("graphics.renderItemDecorations(", "graphics.itemDecorations(") }
        string(true) { replace("graphics.renderItem(", "graphics.item(") }

        string(true) { replace("setScreen(", "setScreenAndShow(") }
        string(true) { replace("resizeDisplay(", "resizeGui(") }
        // Before the plain class rename, so the call site gets both halves: only
        // one replacement lands per line.
        string(true) {
            replace("PlayerFaceRenderer.draw(", "PlayerFaceExtractor.extractRenderState(")
        }
        string(true) { replace("PlayerFaceRenderer", "PlayerFaceExtractor") }
        string(true) { replace("nonEmptyStream()", "nonEmptyItemCopyStream()") }
        string(true) { replace("renderer.state.CameraRenderState", "renderer.state.level.CameraRenderState") }

        // Fabric renamed these alongside Minecraft's own key-binding rename.
        string(true) { replace("keybinding.v1", "keymapping.v1") }
        string(true) { replace("KeyBindingHelper", "KeyMappingHelper") }
        string(true) { replace("registerKeyBinding(", "registerKeyMapping(") }
        string(true) { replace("TooltipComponentCallback", "ClientTooltipComponentCallback") }
    }

    /*
     * 1.21.11 renamed ResourceLocation to Identifier and moved a couple of
     * helpers. The mod is written against the newer names, so older versions get
     * them translated back.
     */
    if (stonecutter.eval(minecraftVersion, "<1.21.11")) {
        // Ahead of the type rename: this method's name contains the type's, and
        // only one replacement lands per line, so the broader rule would other-
        // wise turn getIdentifier into getResourceLocation and miss the mark.
        string(true) { replace("getIdentifier()", "getLocation()") }
        string(true) { replace("Identifier", "ResourceLocation") }

        string(true) { replace("net.minecraft.util.Util", "net.minecraft.Util") }
        string(true) { replace("org.jspecify.annotations", "org.jetbrains.annotations") }
    }

    /*
     * Before 1.21.9 the GLFW window handle was reached differently and player
     * skins were a single texture rather than a set of parts.
     */
    if (stonecutter.eval(minecraftVersion, "<1.21.9")) {
        string(true) { replace(".handle()", ".getWindow()") }
        string(true) { replace("isKeyDown(window,", "isKeyDown(window.getWindow(),") }
        string(true) { replace(".getSkin().body().texturePath()", ".getSkin().texture()") }

        // The collector that 1.21.9 introduced took the place of the buffer
        // source, in the same package and the same argument position, so this
        // one rule covers the import, the descriptor and the parameter alike.
        // Where the surrounding signature changed shape as well, the mixin says
        // so itself rather than relying on this.
        string(true) { replace("SubmitNodeCollector", "MultiBufferSource") }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)

    // Only on obfuscated versions: Loom pairs a sources jar with a source
    // remapping step, and that step demands a mapping set that does not exist
    // for the unobfuscated releases.
    if (obfuscated) {
        withSourcesJar()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVersion
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_range", prop("minecraft_range"))
    inputs.property("loader_version", prop("loader_version"))
    inputs.property("java_version", javaVersion)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        // Placeholder names match the properties they carry. They did not once:
        // minecraft_range was fed to a ${minecraft_version} placeholder that the
        // template prefixed with "~", producing the range "~~1.21.11", which
        // Fabric parses as a predicate no version can satisfy.
        //
        // Each version names itself exactly rather than using a "~" range. A
        // jar is built and verified against one Minecraft version, and "~1.21.6"
        // would claim every 1.21.x from .6 up — versions whose mixin targets it
        // was never checked against.
        expand(
            "version" to project.version,
            "minecraft_range" to prop("minecraft_range"),
            "loader_version" to prop("loader_version"),
            "java_version" to javaVersion
        )
    }
}

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${prop("archives_base_name")}" }
    }
}

