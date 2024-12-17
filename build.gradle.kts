plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.stewsters"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.processing:core:4.3.1")

}

tasks.test {
    useJUnitPlatform()
}