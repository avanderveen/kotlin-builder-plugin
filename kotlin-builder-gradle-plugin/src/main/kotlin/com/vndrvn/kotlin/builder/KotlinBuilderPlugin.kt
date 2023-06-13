package com.vndrvn.kotlin.builder

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class KotlinBuilderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("com.google.devtools.ksp")

        val version = project.artifactVersion("com.vndrvn", "kotlin-builder-gradle-plugin")
        project.dependencies.add("implementation", "com.vndrvn:kotlin-builder-annotation:$version")
        project.dependencies.add("ksp", "com.vndrvn:kotlin-builder-plugin:$version")

        project.extensions.configure("kotlin") { kotlin: KotlinJvmProjectExtension ->
            kotlin.sourceSets.getByName("main").kotlin.srcDir("build/generated/ksp/main/kotlin")
            kotlin.sourceSets.getByName("test").kotlin.srcDir("build/generated/ksp/test/kotlin")
        }
    }
}

private fun Project.artifactVersion(group: String, artifact: String): String {
    fun artifactVersion(project: Project, group: String, artifact: String): String? {
        return project.configurations.firstNotNullOfOrNull { configuration ->
            configuration.dependencies.toList().firstOrNull { dependency ->
                dependency.group == group && dependency.name == artifact
            }?.version
        } ?: project.parent?.artifactVersion(group, artifact)
    }

    return artifactVersion(this, group, artifact) ?: throw IllegalStateException(
        "Plugin missing or version not specified: $group:$artifact"
    )
}
