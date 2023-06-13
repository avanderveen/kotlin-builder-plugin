tasks.register("clean") {
    dependsOn(
        gradle.includedBuild("plugin").task(":kotlin-builder-annotation:clean"),
        gradle.includedBuild("plugin").task(":kotlin-builder-gradle-plugin:clean"),
        gradle.includedBuild("plugin").task(":kotlin-builder-plugin:clean"),
        gradle.includedBuild("test").task(":kotlin-builder-test:clean")
    )
}

tasks.register("test") {
    dependsOn(
        gradle.includedBuild("test").task(":kotlin-builder-test:test")
    )
}
