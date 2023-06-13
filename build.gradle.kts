tasks.register("test") {
    dependsOn(gradle.includedBuild("test").task(":kotlin-builder-test:test"))
}
