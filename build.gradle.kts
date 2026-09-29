import org.jetbrains.compose.desktop.application.dsl.TargetFormat
plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.3"
}
repositories { google(); mavenCentral() }
dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.datastore:datastore-preferences-core:1.1.1")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
    implementation("net.java.dev.jna:jna-platform:5.15.0")
    testImplementation("junit:junit:4.13.2")
}
val appVersion = providers.gradleProperty("appVersion").getOrElse("1.0.0")
val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildConfig")
    outputs.dir(outputDir)
    inputs.property("appVersion", appVersion)
    doLast {
        val output = outputDir.get().file("com/mgafk/app/BuildConfig.kt").asFile
        output.parentFile.mkdirs()
        output.writeText("package com.mgafk.app\nobject BuildConfig { const val VERSION_NAME = \"$appVersion\"; const val DEBUG = false }\n")
    }
}
kotlin {
    jvmToolchain(17)
    sourceSets.main { kotlin.srcDir(layout.buildDirectory.dir("generated/buildConfig")) }
}
tasks.named("compileKotlin") { dependsOn(generateBuildConfig) }
compose.desktop {
    application {
        mainClass = "com.mgafk.app.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "MGAUTO"
            packageVersion = appVersion
            description = "Magic Garden desktop companion"
            vendor = "GertiMecaj"
            includeAllModules = true
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            windows {
                menu = true
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "4294f025-a307-4e95-9584-c3c6e512caa9"
            }
        }
    }
}
tasks.test { testLogging { events("passed", "skipped", "failed") } }
