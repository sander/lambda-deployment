plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.22"
    application
}

group = "nl.sanderdijkhuis.deployment"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.hashicorp:cdktf:0.16.3")
    implementation("software.constructs:constructs:10.2.48")
    implementation("com.hashicorp:cdktf-provider-random:7.0.0")
    implementation("com.hashicorp:cdktf-provider-aws:14.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}