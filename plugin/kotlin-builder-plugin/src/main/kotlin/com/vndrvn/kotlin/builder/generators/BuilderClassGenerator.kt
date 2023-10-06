package com.vndrvn.kotlin.builder.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.vndrvn.kotlin.builder.Builder
import com.vndrvn.kotlin.builder.Casing
import com.vndrvn.kotlin.builder.pairWith

@OptIn(KspExperimental::class)
class BuilderClassGenerator(
    private val fileContent: List<String>,
    classDeclaration: KSClassDeclaration,
    private val casingOverride: Casing?
) {
    private val name: String = classDeclaration.simpleName.asString()

    private val typeParameterResolver: TypeParameterResolver = classDeclaration.typeParameters.toTypeParameterResolver()

    private val typeVariables: List<TypeVariableName> = classDeclaration.typeParameters.map {
        it.toTypeVariableName(typeParameterResolver)
    }

    private val typeName: TypeName = classDeclaration.asType(emptyList()).toTypeName(typeParameterResolver)

    private val params: List<KSValueParameter> = classDeclaration.getConstructors().filter {
        it.getAnnotationsByType(Builder.Constructor::class).any()
    }.toList().ifEmpty {
        listOf(classDeclaration.primaryConstructor!!)
    }.singleOrNull()?.parameters ?: throw Exception(
        """
        Multiple @Builder.Constructor annotations for $name in file:
        ${(classDeclaration.location as? FileLocation)?.filePath}
        """.trimIndent()
    )

    private val paramsWithBuilders: Map<KSValueParameter, KSClassDeclaration> = classDeclaration.declarations
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.getAnnotationsByType(Builder::class).any() }
        .mapNotNull { params.firstOfTypeOrNull(it.toClassName(), typeParameterResolver).pairWith(it) }
        .toMap()

    fun generate() = TypeSpec.classBuilder("${name}Builder").apply {
        addTypeVariables(this@BuilderClassGenerator.typeVariables)

        // default no-args constructor
        primaryConstructor(FunSpec.constructorBuilder().build())

        params.forEach { parameter ->
            val paramName = parameter.name!!.asString()
            val paramType = parameter.type.toTypeName(typeParameterResolver)
            addProperty(backingProperty(paramName, paramType))
            addProperty(publicProperty(paramName, paramType))
            addProperty(wasSetProperty(paramName))

            if (paramsWithBuilders.containsKey(parameter)) {
                val paramClassDeclaration = paramsWithBuilders.getValue(parameter)
                addType(generate(fileContent, paramClassDeclaration, casingOverride))
                addFunction(BuilderFunctionGenerator(paramClassDeclaration, casingOverride).generate())
            }
        }

        addFunction(buildFunction())
    }.build()

    private fun buildFunction(): FunSpec {
        val builderStatementParts = params.map { it.defaultValue() }
        val builderParams = builderStatementParts.joinToString(",\n\t") { (format, _) -> format }
        val builderStatementFormat = "return ${typeName}(\n\t$builderParams\n)"
        val builderStatementArgs = builderStatementParts.map { (_, args) -> args }.toTypedArray()
        return FunSpec.builder("build")
            .returns(typeName)
            .addStatement(builderStatementFormat, *builderStatementArgs)
            .build()
    }

    private fun publicProperty(
        paramName: String,
        paramType: TypeName
    ) = PropertySpec.builder(paramName, paramType)
        .mutable(true)
        .getter(
            FunSpec.getterBuilder()
                .addGetterImplementation(paramName, paramType)
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

    private fun FunSpec.Builder.addGetterImplementation(
        paramName: String,
        paramType: TypeName
    ) = when (
        val className = paramType as? ClassName ?: (paramType as? ParameterizedTypeName)?.rawType
    ) {
        LIST,
        SET,
        MAP -> addStatement("return ${paramName}_backing ?: empty${className.simpleName}()")
        MUTABLE_LIST,
        MUTABLE_SET,
        MUTABLE_MAP -> CodeBlock.builder()
            .beginControlFlow("if (${paramName}_backing == null)")
            .addStatement("${paramName}_backing = mutable${className.simpleName.removePrefix("Mutable")}Of()")
            .addStatement("${paramName}_wasSet = true")
            .endControlFlow()
            .addStatement("return ${paramName}_backing!!")
            .build()
            .let { addCode(it) }
        else -> addStatement("return ${paramName}_backing${if (paramType.isNullable) "" else "!!"}")
    }

    private fun backingProperty(
        paramName: String,
        paramType: TypeName
    ) = PropertySpec.builder("${paramName}_backing", paramType.copy(nullable = true))
        .addModifiers(KModifier.PRIVATE)
        .mutable(true)
        .initializer("null")
        .build()

    private fun wasSetProperty(
        paramName: String
    ) = PropertySpec.builder("${paramName}_wasSet", BOOLEAN)
        .addModifiers(KModifier.PRIVATE)
        .mutable(true)
        .initializer("false")
        .build()

    private fun KSValueParameter.defaultValue(): Pair<String, String>  {
        val (unset, arg) = if (hasDefault) {
            val value = defaultValueRegex()
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

    private val matchGroup = "value"

    private fun KSValueParameter.defaultValueRegex(): String {
        val pattern = """${name!!.asString()}\s*:\s*\w+\s*=\s*(?<$matchGroup>[^,)]+)"""
        val fileLocation = location as FileLocation
        val line = fileContent.getOrElse(fileLocation.lineNumber - 1) { "" }
        val regex = Regex(pattern, RegexOption.MULTILINE)
        val defaultValue = regex.find(line)?.groups?.get(matchGroup)?.value?.trim()
        return defaultValue ?: throw IllegalStateException(
            """
            Could not find default value for parameter ${name!!.asString()}

            Line ${fileLocation.lineNumber} in ${fileLocation.filePath}:

                $line
            """.trimIndent()
        )
    }

    private fun List<KSValueParameter>.firstOfTypeOrNull(
        typeName: TypeName,
        typeParameterResolver: TypeParameterResolver
    ) = firstOrNull { param ->
        typeName == param.type.toTypeName(typeParameterResolver)
    }
}

private fun generate(
    fileContent: List<String>,
    classDeclaration: KSClassDeclaration,
    casingOverride: Casing?
): TypeSpec = BuilderClassGenerator(
    fileContent,
    classDeclaration,
    casingOverride
).generate()
