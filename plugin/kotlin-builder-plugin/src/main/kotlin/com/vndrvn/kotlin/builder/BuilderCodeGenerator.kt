package com.vndrvn.kotlin.builder

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import com.vndrvn.kotlin.builder.generators.BuilderClassGenerator
import com.vndrvn.kotlin.builder.generators.BuilderFunctionGenerator
import com.vndrvn.kotlin.builder.generators.CopyExtensionFunctionGenerator
import java.nio.file.Path
import kotlin.io.path.readLines

private val suppress = listOf(
    "PrivatePropertyName",
    "RedundantVisibilityModifier",
    "MemberVisibilityCanBePrivate"
)

class BuilderCodeGenerator(
    private val codeGenerator: CodeGenerator,
    private val casingOverride: Casing?
) {
    fun generate(
        filePath: Path,
        classDeclarations: List<KSClassDeclaration>
    ) {
        val fileContent = filePath.readLines()
        classDeclarations.forEach { classDeclaration ->
            generate(fileContent, classDeclaration)
        }
    }

    private fun generate(
        fileContent: List<String>,
        classDeclaration: KSClassDeclaration
    ) {
        val name = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()
        val dependencies = Dependencies(aggregating = true, classDeclaration.parent as KSFile)
        val copyExtension = CopyExtensionFunctionGenerator(classDeclaration).generate()
        FileSpec.builder(packageName, "${name}Builder")
            .addAnnotation(suppressAnnotation())
            .addType(BuilderClassGenerator(fileContent, classDeclaration, casingOverride).generate())
            .addFunction(BuilderFunctionGenerator(classDeclaration, casingOverride).generate())
            .also { copyExtension?.let(it::addFunction) }
            .build()
            .writeTo(codeGenerator, dependencies)
    }
}

private fun suppressAnnotation() = AnnotationSpec.builder(Suppress::class)
    .useSiteTarget(FILE)
    .apply { suppress.forEach { addMember("%S", it) } }
    .build()
