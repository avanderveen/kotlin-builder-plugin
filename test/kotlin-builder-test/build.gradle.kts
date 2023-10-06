import com.vndrvn.kotlin.builder.Casing

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

// TODO change this to "builderKt" as part of renaming project to BuilderKt
builder {
    casing.set(Casing.CamelCase)
}
