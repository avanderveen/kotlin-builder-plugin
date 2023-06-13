plugins {
    kotlin("jvm")
}

dependencies {
    val kspVersion: String by project
    val kotlinPoetVersion: String by project
    implementation(project(":kotlin-builder-annotation"))
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")
}
