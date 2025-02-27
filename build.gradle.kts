plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id ("org.beryx.runtime") version "1.13.1"
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

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.stewsters.hexmapgen.MainKt"
    }
}

tasks.test {
    useJUnitPlatform()
}

runtime {
    options = listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
//    modules =listOf ("java.naming", "java.xml")

    launcher {
        jvmArgs = listOf("-Djava.awt.headless=false")
    }
}

tasks.register<Copy>("copyAssets") {
    mustRunAfter("installShadowDist")
    from("assets/")
    include("**/*.*")
    into(layout.buildDirectory.dir("install/hexmapgen-shadow/assets"))
}

tasks.named("installShadowDist"){
   finalizedBy("copyAssets")
}



//jlink {
//    options =listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages", "--bind-services")
//    launcher {
//        name = "MyApplication"
//    }
//}