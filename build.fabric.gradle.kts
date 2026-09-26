@file:Suppress("AvoidDuplicateDependencies")
import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("dev.kikugie.fletching-table.fabric")
    id("me.modmuss50.mod-publish-plugin")
    id("dev.kikugie.loom-back-compat")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-fabric"

repositories {
    mavenCentral()
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://maven.blamejared.com", "BlameJared", "mezz.jei", "net.mezzdev.config")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }
val runtimeOptionals: List<String> = sc.properties.rawOrNull("dev", "runtime_optionals")
    ?.asList().orEmpty().map { it.toString() }

data class ModDep(val key: String, val version: String) {
    private fun meta(suffix: String): String? = findProperty("dep.$key.$suffix")?.toString()?.takeIf { it.isNotBlank() }
    val id: String get() = meta("id") ?: key
    val coords: String? get() = meta("coords")?.replace($$"$id", id)?.replace($$"$loader", "fabric")?.replace($$"$mc", sc.current.version)
    val base: String get() = version.substringBefore('+').substringBefore("-beta")
    val range: String get() = meta("range") ?: ">=$base"
    fun slug(platform: String): String = meta("slug.$platform") ?: meta("slug") ?: key
}

val requiredDeps = project.ext.properties
    .filterKeys { it.startsWith("required.") }
    .map { (k, v) -> ModDep(
        key = k.substringAfter('.'),
        version = v.toString()
    ) }.sortedBy { it.key }
val includeDeps = project.ext.properties
    .filterKeys { it.startsWith("include.") }
    .map { (k, v) -> ModDep(
        key = k.substringAfter('.'),
        version = v.toString()
    ) }.sortedBy { it.key }
val optionalDeps = project.ext.properties
    .filterKeys { it.startsWith("optional.") }
    .map { (k, v) -> ModDep(
        key = k.substringAfter('.'),
        version = v.toString()
    ) }.sortedBy { it.key }
val runtimeDeps = project.ext.properties
    .filterKeys { it.startsWith("runtime.") }
    .map { (k, v) -> ModDep(
        key = k.substringAfter('.'),
        version = v.toString()
    ) }.sortedBy { it.key }

fun jsonObject(entries: List<Pair<String, String>>): String =
    if (entries.isEmpty()) "{}"
    else entries.joinToString(",\n    ", "{\n    ", "\n  }") { (k, v) -> "\"$k\": \"$v\"" }

val fabricDepends = jsonObject(
    buildList {
        add("minecraft" to sc.properties["mod.mc_compat"])
        add("fabricloader" to ">=${property("loader.fabric")}")
        add("java" to ">=${requiredJava.majorVersion}")
        requiredDeps.forEach { add(it.id to it.range) }
    }
)
val fabricSuggests = jsonObject(optionalDeps.map { it.id to it.range })

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("loader.fabric")}")
    fun ModDep.declare(vararg configurations: String) {
        val notation = (coords ?: return).replace($$"$version", version)
        configurations.forEach {
            conf -> conf(notation) {
                if (id != "fabric-api") exclude(group = "net.fabricmc.fabric-api")
            }
        }
    }
    requiredDeps.forEach { it.declare("modImplementation") }
    includeDeps.forEach { it.declare("modImplementation", "include")}
    optionalDeps.forEach {
        it.declare("modCompileOnly")
        if (runtimeOptionals.contains(it.key)) {
            it.declare("modRuntimeOnly")
        }
    }
    runtimeDeps.forEach { it.declare("modRuntimeOnly") }

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/aw/${sc.current.project.substringBefore('-')}.ct")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run/${sc.current.version.replace(".", "_")}_fabric/")
        // `-PmixinAudit=true` loads every mixin target at startup, so a bad target fails the smoke test
        vmArg("-Ddooles_core_crucible.debug.audit=${findProperty("mixinAudit") ?: "false"}")
        // List each untranslated item tag by name instead of Fabric's one-line summary
        vmArg("-Dfabric-tag-conventions-v2.missingTagTranslationWarning=VERBOSE")
    }    // `-PquickPlay=<save>` loads straight into that singleplayer world (smoke tests)
    runConfigs.named("client") {
        findProperty("quickPlay")?.let { programArgs("--quickPlaySingleplayer", it.toString()) }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.MICROSOFT
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

fletchingTable {
    mixins.create("main") {
        mixin("default", "${property("mod.id")}.mixins.json") {
            env("CLIENT", "${property("mod.package")}.mixin.client")
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val ct = "aw/${sc.current.project.substringBefore('-')}.ct"
        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        val depends = fabricDepends
        val suggests = fabricSuggests

        val props = buildMap {
            register("id", "mod.id")
            register("group", "mod.group")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
            register("description", "mod.description")
            register("license", "mod.license")
            register("sources_url", "mod.sources_url")
            register("homepage_url", "mod.homepage_url")
            register("issues_url", "mod.issues_url")
            register("discord_url", "mod.discord_url")
            register("authors", "mod.authors")
            register("contributors", "mod.contributors")
            inputs.property("ct", ct)
            put("ct", ct)
            inputs.property("depends", depends)
            inputs.property("suggests", suggests)
        }

        filesMatching("fabric.mod.json") { expand(props) }
        filesMatching("fabric.mod.json") {
            filter { line -> line
                .replace("\"depends\": {}", "\"depends\": $depends")
                .replace("\"suggests\": {}", "\"suggests\": $suggests")
            }
        }

        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
        // Per-version pack formats (1.21.1: resource 34 / data 48; 26.x: a min/max range over 26.2 and 26.3)
        val packFormat = if (sc.current.parsed >= "1.21.2") "\"min_format\": [88, 0], \"max_format\": [121, 0]"
            else "\"pack_format\": 34, \"supported_formats\": [34, 48]"
        inputs.property("pack_format", packFormat)
        filesMatching("pack.mcmeta") { expand("pack_format" to packFormat) }

        exclude("META-INF/neoforge.mods.toml")

        // Data whose JSON differs by version (vanilla ingredient syntax); see tools/gen_data.py.
        from(rootProject.file("src/main/versioned/${if (sc.current.parsed >= "1.21.2") "26" else "1.21.1"}"))
        // Armor trim atlases and trim materials, whose format changed again in 26.3; see gen_trim_overrides in tools/gen_data.py.
        if (sc.current.parsed >= "1.21.2") from(rootProject.file("src/main/versioned/${sc.current.version}"))
        exclude { it.path.startsWith("aw/") && it.path != ct }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
    // `build` also drops the mod jar straight into `versions/`, next to the per-target folders, so it's easy to find
    val collectJar = register("collectJar") {
        group = "build"
        description = "Copies the mod jar to `versions/`"
        val source = loomx.modJar.flatMap { it.archiveFile }
        val target = rootProject.layout.projectDirectory.dir("versions").file(loomx.modJar.flatMap { it.archiveFileName })
        inputs.file(source)
        outputs.file(target)
        doLast { source.get().asFile.copyTo(target.get().asFile, overwrite = true) }
    }
    named("build") { finalizedBy(collectJar) }
}

publishMods {
    file.set(loomx.modJar.get().archiveFile)
    additionalFiles.from(loomx.modSourcesJar.get().archiveFile)
    changelog.set(rootProject.file("CHANGELOG.md").readText())
    type.set(STABLE)
    modLoaders.add("fabric")
    displayName = "${property("mod.version")} for Fabric ${sc.current.version}"
    dryRun = (property("publish.dry_run") as String).toBooleanStrict()

    val mrRequired = requiredDeps.map { it.slug("modrinth") }
    val cfRequired = requiredDeps.map { it.slug("curseforge") }

    modrinth {
        projectId.set("${property("publish.modrinth")}")
        accessToken.set(providers.environmentVariable("MR_KEY"))
        minecraftVersions.addAll(compatibleVersions)
        environment.set(ModrinthEnvironment.valueOf(property("publish.env.mr") as String))
        requires(*mrRequired.toTypedArray())
    }

    curseforge {
        projectId.set("${property("publish.curseforge")}")
        accessToken.set(providers.environmentVariable("CF_KEY"))
        minecraftVersions.addAll(compatibleVersions)
        client = (property("publish.env.cf.client") as String).toBooleanStrict()
        server = (property("publish.env.cf.server") as String).toBooleanStrict()
        requires(*cfRequired.toTypedArray())
    }
}
