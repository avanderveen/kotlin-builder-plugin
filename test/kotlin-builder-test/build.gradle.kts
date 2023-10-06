import com.vndrvn.kotlin.builder.Casing
import com.vndrvn.kotlin.builder.KotlinBuilderPluginExtension

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

configure<KotlinBuilderPluginExtension> {
    casing.set(Casing.CamelCase)
}
