import com.vndrvn.kotlin.builder.Casing.PascalCase

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

builderKt {
    casing.set(PascalCase)
}
