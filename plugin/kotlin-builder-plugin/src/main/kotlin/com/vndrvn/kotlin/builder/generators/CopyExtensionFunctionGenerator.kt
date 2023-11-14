package com.vndrvn.kotlin.builder.generators

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.withIndent
import com.vndrvn.kotlin.builder.builderConstructor
import com.vndrvn.kotlin.builder.builderTypeName
import com.vndrvn.kotlin.builder.typeName
import com.vndrvn.kotlin.builder.typeVariables
import java.lang.System.lineSeparator

/**
 * Issues:
 *  1. Doesn't work when a secondary constructor is annotated with @Builder.Constructor
 */
class CopyExtensionFunctionGenerator(
    private val classDeclaration: KSClassDeclaration
) {
    fun generate(): FunSpec? {
        // all params must be properties to generate a copy builder
        val params = classDeclaration.builderConstructor.parameters
        if (!params.all { it.name != null && (it.isVal || it.isVar) }) {
            return null
        }

        val builderTypeName = classDeclaration.builderTypeName
        return FunSpec.builder("copy")
            .receiver(classDeclaration.typeName)
            .addTypeVariables(classDeclaration.typeVariables)
            .addParameter(
                ParameterSpec.Companion.builder(
                    "builder",
                    LambdaTypeName.get(
                        receiver = builderTypeName,
                        returnType = ClassName("kotlin", "Unit")
                    )
                ).build()
            )
            .addCode(
                CodeBlock.builder()
                    .add("return $builderTypeName().also { copy ->\n")
                    .withIndent {
                        params.map { it.name!!.asString() }.forEach { param ->
                            addStatement("copy.$param = $param")
                        }
                    }
                    .add("}.apply(builder).build()")
                    .build()
            )
            .build()
    }
}
