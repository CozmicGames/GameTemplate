import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10-RC"
}

group = "com.gratedgames"
version = "0.0.1"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("com.gratedgames:Kore:0.1.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}