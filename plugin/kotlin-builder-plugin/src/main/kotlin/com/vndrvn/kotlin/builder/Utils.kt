package com.vndrvn.kotlin.builder

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName

fun <T, U> T?.pairWith(other: U) = this?.let { Pair(it, other) }

fun String.withCasing(casing: Casing) = when (casing) {
    Casing.PascalCase -> first().uppercase() + skip(1)
    Casing.CamelCase -> first().lowercase() + skip(1)
    else -> this
}

fun String.skip(n: Int) = if (length < n) "" else substring(n)

@OptIn(KspExperimental::class)
val KSClassDeclaration.builderConstructor: KSFunctionDeclaration get() = getConstructors().filter {
    it.getAnnotationsByType(Builder.Constructor::class).any()
}.toList().ifEmpty {
    listOf(primaryConstructor!!)
}.singleOrNull() ?: throw Exception(
    """
        Multiple @Builder.Constructor annotations for ${simpleName.asString()} in file:
        ${(location as? FileLocation)?.filePath}
        """.trimIndent()
)

val KSClassDeclaration.typeVariables: List<TypeVariableName> get() = typeParameters.map {
    it.toTypeVariableName(typeParameters.toTypeParameterResolver())
}

val KSClassDeclaration.typeName: TypeName get() =
    asType(emptyList()).toTypeName(typeParameters.toTypeParameterResolver())

val KSClassDeclaration.builderTypeName: TypeName get() {
    val simpleNames = mutableListOf(simpleName.asString())
    var parent: KSClassDeclaration? = this
    while (true) {
        parent = parent?.parentDeclaration as? KSClassDeclaration ?: break
        simpleNames += parent.simpleName.asString()
    }

    val className = ClassName(
        (parent ?: this).packageName.asString(),
        *simpleNames.map { "${it}Builder" }.reversed().toTypedArray()
    )

    if (typeVariables.isEmpty()) {
        return className
    }

    return className.parameterizedBy(typeVariables)
}