plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.legacy.looming)
}

group = "com.graphite"
version = "1.0-SNAPSHOT"

val lwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            else if (arch.startsWith("ppc"))
                "natives-linux-ppc64le"
            else if (arch.startsWith("riscv"))
                "natives-linux-riscv64"
            else
                "natives-linux"

        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            "natives-macos"

        arrayOf("Windows").any { name.startsWith(it) } ->
            "natives-windows"

        else ->
            throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft(libs.minecraft)
    mappings(libs.yarn)

    modImplementation(libs.fabric.loader)

    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.2")
    implementation(libs.bundles.wgpu4k) {
        exclude(group = "ch.qos.logback")
    }
    implementation(libs.kotlin.coroutines)
    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.glfw)
    implementation("org.lwjgl", "lwjgl", version = libs.versions.lwjgl.get(), classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-glfw", version = libs.versions.lwjgl.get(), classifier = lwjglNatives)

    implementation(project(":math"))
}

loom {
    accessWidenerPath.set(file("src/main/resources/graphite.accesswidener"))
}

kotlin {
    jvmToolchain(24)
}

configurations.all {
    exclude(group = "org.lwjgl.lwjgl")
}