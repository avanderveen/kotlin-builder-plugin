package com.vndrvn.kotlin.builder

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo
import java.nio.file.Path
import kotlin.io.path.readText

private val suppress = listOf(
    "PrivatePropertyName",
    "RedundantVisibilityModifier",
    "MemberVisibilityCanBePrivate"
)

class BuilderCodeGenerator(
    private val codeGenerator: CodeGenerator
) {
    fun generate(
        filePath: Path,
        classDeclarations: List<KSClassDeclaration>
    ) {
        val fileContent = filePath.readText()
        classDeclarations.forEach { classDeclaration ->
            generate(fileContent, classDeclaration)
        }
    }

    @OptIn(KspExperimental::class)
    private fun generate(
        fileContent: String,
        classDeclaration: KSClassDeclaration
    ) {
        // TODO: move all of this argument collecting out into BuilderClassArgs and BuilderFunctionArgs classes
        // - constructors can accept a KSClassDeclaration (and fileContent if needed)
        val annotation = classDeclaration.getAnnotationsByType(Builder::class).single()
        val name = classDeclaration.simpleName.asString()
        val builderName = annotation.name.ifBlank { name }
        val packageName = classDeclaration.packageName.asString()
        val typeParameters = classDeclaration.typeParameters
        val typeParameterResolver = typeParameters.toTypeParameterResolver()
        val typeVariables = typeParameters.map { it.toTypeVariableName(typeParameterResolver) }
        val typeName = classDeclaration.asType(emptyList()).toTypeName(typeParameterResolver)
        val params = classDeclaration.primaryConstructor!!.parameters
        val dependencies = Dependencies(aggregating = true, classDeclaration.parent as KSFile)
        val builderTypeName = ClassName(packageName, "${name}Builder").run {
            if (typeVariables.isEmpty()) this else parameterizedBy(typeVariables)
        }

        val paramsWithBuilders = classDeclaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.getAnnotationsByType(Builder::class).any() }
            .mapNotNull { params.firstOfTypeOrNull(it.toClassName(), typeParameterResolver).pairWith(it) }
            .toMap()

        FileSpec.builder(packageName, "${name}Builder")
            .addAnnotation(suppressAnnotation())
            .addType(builderClass(name, typeName, fileContent, params, paramsWithBuilders, typeVariables, typeParameterResolver))
            .addFunction(builderFunction(name, builderName, builderTypeName, typeVariables))
            .build()
            .writeTo(codeGenerator, dependencies)
    }
}

private fun suppressAnnotation() = AnnotationSpec.builder(Suppress::class)
    .useSiteTarget(FILE)
    .apply { suppress.forEach { addMember("%S", it) } }
    .build()

private fun builderClass(
    name: String,
    typeName: TypeName,
    fileContent: String,
    params: List<KSValueParameter>,
    paramsWithBuilders: Map<KSValueParameter, KSClassDeclaration>,
    typeVariables: List<TypeVariableName>,
    typeParameterResolver: TypeParameterResolver
) = TypeSpec.classBuilder("${name}Builder").apply {
    addTypeVariables(typeVariables)

    // default no-args constructor
    primaryConstructor(FunSpec.constructorBuilder().build())

    params.forEach { parameter ->
        val paramName = parameter.name!!.asString()
        val paramType = parameter.type.toTypeName(typeParameterResolver)
        addProperty(backingProperty(paramName, paramType))
        addProperty(publicProperty(paramName, paramType))
        addProperty(wasSetProperty(paramName))

        // TODO: generate nested builder classes based on `paramsWithBuilders`
    }

    addFunction(buildFunction(fileContent, name, typeName, params))
}.build()

private fun builderFunction(
    name: String,
    builderName: String,
    builderTypeName: TypeName,
    typeVariables: List<TypeVariableName>
): FunSpec {
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
                    receiver = builderTypeName,
                    returnType = ClassName("kotlin", "Unit")
                )
            ).build()
        )
        .addStatement("return ${name}Builder$typeArgs().apply(builder).build()")
        .build()
}

private fun publicProperty(
    paramName: String,
    paramType: TypeName
) = PropertySpec.builder(paramName, paramType)
    .mutable(true)
    .getter(
        FunSpec.getterBuilder()
            .addStatement(
                "return ${paramName}_backing${if (paramType.isNullable) "" else "!!"}"
            )
            .build()
    )
    .setter(
        FunSpec.setterBuilder()
            .addParameter(
                ParameterSpec.builder("value", paramType)
                    .build()
            )
            .addStatement("${paramName}_backing = value")
            .addStatement("${paramName}_wasSet = true")
            .build()
    )
    .build()

private fun backingProperty(
    paramName: String,
    paramType: TypeName
) = PropertySpec.builder("${paramName}_backing", paramType.copy(nullable = true))
    .addModifiers(PRIVATE)
    .mutable(true)
    .initializer("null")
    .build()

private fun wasSetProperty(
    paramName: String
) = PropertySpec.builder("${paramName}_wasSet", BOOLEAN)
    .addModifiers(PRIVATE)
    .mutable(true)
    .initializer("false")
    .build()

private fun buildFunction(
    fileContent: String,
    name: String,
    typeName: TypeName,
    parameters: List<KSValueParameter>
): FunSpec {
    val builderStatementParts = parameters.map { it.defaultValue(fileContent) }
    val builderParams = builderStatementParts.joinToString(",\n\t") { (format, _) -> format }
    val builderStatementFormat = "return $name(\n\t$builderParams\n)"
    val builderStatementArgs = builderStatementParts.map { (_, args) -> args }.toTypedArray()
    return FunSpec.builder("build")
        .returns(typeName)
        .addStatement(builderStatementFormat, *builderStatementArgs)
        .build()
}

private fun KSValueParameter.defaultValue(fileContent: String): Pair<String, String>  {
    val (unset, arg) = if (hasDefault) {
        val value = defaultValueRegex(fileContent)
        val (format, defaultArg) = if (value.startsWith('"') && value.endsWith('"')) {
            Pair("%S", value.removePrefix("\"").removeSuffix("\""))
        } else {
            Pair("%L", value)
        }

        Pair(format, defaultArg)
    } else {
        Pair("throw IllegalStateException(%S)", "${name!!.asString()} was not set")
    }

    val paramName = name!!.asString()
    return Pair("$paramName = if (${paramName}_wasSet) $paramName else $unset", arg)
}

private const val MATCH_GROUP = "value"

/**
 * TODO
 *
 * This regex needs to be a bit more advanced. It currently looks for `<paramName>: <any string> = <default value>`,
 * without considering context. It should ensure that the parameter it is looking for occurs within the primary
 * constructor's parameter list.
 */
private fun KSValueParameter.defaultValueRegex(fileContent: String) = Regex(
    """${name!!.asString()}\s*:\s*\w+\s*=\s*(?<$MATCH_GROUP>[^,)]+)""",
    RegexOption.MULTILINE
).find(fileContent)?.groups?.get(MATCH_GROUP)?.value?.trim() ?: throw IllegalStateException(
    "Could not find default value for parameter ${name!!.asString()}:\r\n$fileContent"
)

/**
 * Utils
 */

private fun List<KSValueParameter>.firstOfTypeOrNull(
    typeName: TypeName,
    typeParameterResolver: TypeParameterResolver
) = firstOrNull { param ->
    typeName == param.type.toTypeName(typeParameterResolver)
}

private fun <T, U> T?.pairWith(other: U) = this?.let { Pair(it, other) }
