import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    kotlin("jvm") version "2.4.0"
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
}

val modGroup: String by project
val modID: String by project
val modName: String by project
val modVersion: String by project

val minecraftVersion: String by project
val loaderVersion: String by project
val fabricApiVersion: String by project
val fabricLanguageKotlinVersion: String by project

group = modGroup
version = modVersion

base {
    archivesName.set(modID)
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$fabricLanguageKotlinVersion")

    implementation("com.google.code.gson:gson:2.11.0")
}

loom {
    mods {
        create(modID) {
            sourceSet(sourceSets["main"])
        }
    }

    runs {
        named("client") {
            client()
        }
    }
}

tasks.processResources {
    inputs.property("version", modVersion)

    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-Xjvm-default=all"
        )
    }
}
