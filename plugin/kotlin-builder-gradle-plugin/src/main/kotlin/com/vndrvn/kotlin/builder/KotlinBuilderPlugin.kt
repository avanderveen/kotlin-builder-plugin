package com.vndrvn.kotlin.builder

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

@Suppress("unused")
class KotlinBuilderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
            project.plugins.apply("com.google.devtools.ksp")
        }

        val version = project.artifactVersion("com.vndrvn", "kotlin-builder-gradle-plugin")
        if (!project.hasDependency("implementation", "com.vndrvn", "kotlin-builder-annotation")) {
            project.dependencies.add("implementation", "com.vndrvn:kotlin-builder-annotation:$version")
        }

        if (!project.hasDependency("ksp", "com.vndrvn", "kotlin-builder-plugin")) {
            project.dependencies.add("ksp", "com.vndrvn:kotlin-builder-plugin:$version")
        }

        project.extensions.configure("kotlin") { kotlin: KotlinJvmProjectExtension ->
            kotlin.sourceSets.getByName("main").kotlin.srcDir("build/generated/ksp/main/kotlin")
            kotlin.sourceSets.getByName("test").kotlin.srcDir("build/generated/ksp/test/kotlin")
        }
    }
}

private fun Project.artifactVersion(group: String, artifact: String): String {
    fun artifactVersion(project: Project, group: String, artifact: String): String? {
        return project.buildscript.configurations.firstNotNullOfOrNull { configuration ->
            if (!configuration.isCanBeResolved) null
            else configuration.resolvedConfiguration.resolvedArtifacts.firstNotNullOfOrNull { resolved ->
                val id = resolved.moduleVersion.id
                if (id.group == group && id.name == artifact) id.version else null
            }
        } ?: project.parent?.artifactVersion(group, artifact)
    }

    return artifactVersion(this, group, artifact) ?: throw IllegalStateException(
        "Plugin missing or version not specified: $group:$artifact"
    )
}

@Suppress("UNNECESSARY_SAFE_CALL")
private fun Project.hasDependency(configuration: String, group: String, artifact: String) = true == configurations
    .getByName(configuration)?.dependencies?.any { it.group == group && it.name == artifact }
