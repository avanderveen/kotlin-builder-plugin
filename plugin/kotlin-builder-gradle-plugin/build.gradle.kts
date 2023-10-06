plugins {
    kotlin("jvm")
    id("com.gradle.plugin-publish")
}

repositories {
    mavenCentral()
}

dependencies {
    val kspVersion: String by project
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-plugin"))
    implementation(project(":kotlin-builder-annotation"))
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$kspVersion")
}

gradlePlugin {
    plugins {
        create("kotlinBuilderPlugin") {
            id = "com.vndrvn.kotlin.builder"
            displayName = "Kotlin Builder Plugin"
            description = "Generate Kotlin builder DSLs"
            implementationClass = "com.vndrvn.kotlin.builder.KotlinBuilderPlugin"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
