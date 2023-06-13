plugins {
    kotlin("jvm")
//    id("com.vndrvn.kotlin.builder")
    id("com.google.devtools.ksp") version("1.8.10-1.0.9")
}

dependencies {
    implementation(project(":kotlin-builder-annotation"))
    ksp(project(":kotlin-builder-plugin"))
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useKotlinTest()
        }
    }
}
