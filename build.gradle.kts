plugins {
    kotlin("jvm") version "1.8.10" apply false
    id("com.gradle.plugin-publish") version "1.1.0" apply false
}

allprojects {
    group = "com.vndrvn"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
