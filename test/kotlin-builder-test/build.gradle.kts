plugins {
    kotlin("jvm")
    id("com.vndrvn.kotlin.builder")
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useKotlinTest()
        }
    }
}
