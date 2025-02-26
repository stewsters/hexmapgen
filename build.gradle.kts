plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.stewsters"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven("https://jogamp.org/deployment/maven")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.processing:core:4.3.2")
    implementation("com.github.Hexworks.mixite:mixite.core-jvm:2018.2.0-RELEASE")
    implementation("com.github.stewsters:kaiju:1.6")
}

application {
    mainClass = "com.stewsters.hexmapgen.MainKt"
}

tasks.test {
    useJUnitPlatform()
}