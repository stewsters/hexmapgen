plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.stewsters"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.processing:core:4.3.1")
    implementation("com.github.Hexworks.mixite:mixite.core-jvm:2018.2.0-RELEASE")
    implementation("com.github.stewsters:kaiju:1.6")

}

tasks.test {
    useJUnitPlatform()
}