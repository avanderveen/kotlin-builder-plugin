package com.vndrvn.kotlin.builder.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.vndrvn.kotlin.builder.Builder
import com.vndrvn.kotlin.builder.Casing
import com.vndrvn.kotlin.builder.withCasing

class BuilderFunctionGenerator(
    private val classDeclaration: KSClassDeclaration,
    private val casingOverride: Casing?
) {
    private val name: String = classDeclaration.simpleName.asString()

    @OptIn(KspExperimental::class)
    private val builderName: String = classDeclaration.getAnnotationsByType(Builder::class).single().let {
        it.name.ifBlank {
            name.withCasing(
                casingOverride?.let { override ->
                    if (it.casing == Casing.Default) override else null
                } ?: it.casing
            )
        }
    }

    private val typeVariables: List<TypeVariableName> = classDeclaration.typeParameters.map {
        it.toTypeVariableName(classDeclaration.typeParameters.toTypeParameterResolver())
    }

    fun generate(): FunSpec {
        val typeArgs = typeVariables.joinToString(
            prefix = "<",
            postfix = ">"
        ) { it.name }.replace("<>", "")

        return FunSpec.builder(builderName)
            .addTypeVariables(typeVariables)
            .addParameter(
                ParameterSpec.builder(
                    "builder",
                    LambdaTypeName.get(
                        receiver = builderTypeName(),
                        returnType = ClassName("kotlin", "Unit")
                    )
                ).build()
            )
            .addStatement("return ${name}Builder$typeArgs().apply(builder).build()")
            .build()
    }

    private fun builderTypeName(): TypeName {
        val simpleNames = mutableListOf(classDeclaration.simpleName.asString())
        var parent: KSClassDeclaration? = classDeclaration
        while (true) {
            parent = parent?.parentDeclaration as? KSClassDeclaration ?: break
            simpleNames += parent.simpleName.asString()
        }

        val className = ClassName(
            (parent ?: classDeclaration).packageName.asString(),
            *simpleNames.map { "${it}Builder" }.reversed().toTypedArray()
        )

        if (typeVariables.isEmpty()) {
            return className
        }

        return className.parameterizedBy(typeVariables)
    }
}
