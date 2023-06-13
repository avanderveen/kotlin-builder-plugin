package com.vndrvn.kotlin.builder

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import java.nio.file.Paths

class KotlinBuilderSymbolProcessor(
    private val codeGenerator: BuilderCodeGenerator
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(Builder::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.parent is KSFile }
            .groupBy {
                (it.parent as KSFile).run {
                    Paths.get(filePath)
                }
            }
            .forEach { (filePath, classDeclarations) ->
                codeGenerator.generate(filePath, classDeclarations)
            }

        return emptyList()
    }
}
