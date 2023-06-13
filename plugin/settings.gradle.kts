rootProject.name = "plugin"

include(
    "kotlin-builder-annotation",
    "kotlin-builder-plugin",
    "kotlin-builder-gradle-plugin"
)

pluginManagement {
    repositories {
        gradlePluginPortal {
            content {
                includeGroupByRegex("com.google.*")
                includeGroupByRegex("com.gradle.*")
                includeGroupByRegex("com.squareup.*")
                includeGroupByRegex("net.java.*")
                includeGroupByRegex("org.apache.*")
                includeGroupByRegex("org.jetbrains.*")
            }
        }
    }
}
