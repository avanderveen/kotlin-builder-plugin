package com.vndrvn.kotlin.builder

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo
import java.nio.file.Path
import kotlin.io.path.readText

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

    private fun generate(
        fileContent: String,
        classDeclaration: KSClassDeclaration
    ) {
        val name = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()
        val typeParameters = classDeclaration.typeParameters
        val typeParameterResolver = typeParameters.toTypeParameterResolver()
        val typeVariables = typeParameters.map { it.toTypeVariableName(typeParameterResolver) }
        val typeName = classDeclaration.asType(emptyList()).toTypeName(typeParameterResolver)
        val builderTypeName = ClassName(packageName, "${name}Builder").parameterizedBy(typeVariables)
        val params = classDeclaration.primaryConstructor!!.parameters
        val dependencies = Dependencies(aggregating = true, classDeclaration.parent as KSFile)

        FileSpec.builder(packageName, "${name}Builder")
            .addType(
                TypeSpec.classBuilder("${name}Builder").apply {
                    addTypeVariables(typeVariables)

                    // no-args constructor
                    primaryConstructor(FunSpec.constructorBuilder().build())

                    val builderConstructorParams = params.map { parameter ->
                        val paramName = parameter.name!!.asString()
                        val paramType = parameter.type.toTypeName(typeParameterResolver)

                        addProperty(
                            PropertySpec.builder("${paramName}_backing", paramType.copy(nullable = true))
                                .mutable(true)
                                .initializer("null")
                                .build()
                        )

                        addProperty(
                            PropertySpec.Companion.builder(paramName, paramType)
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
                        )

                        addProperty(
                            PropertySpec.builder("${paramName}_wasSet", BOOLEAN)
                                .addModifiers(PRIVATE)
                                .mutable(true)
                                .initializer("false")
                                .build()
                        )

                        val (defaultFormat, defaultArgs) = parameter.defaultValue(fileContent)
                        val format = "$paramName = if (${paramName}_wasSet) $paramName else $defaultFormat"
                        Pair(format, defaultArgs)
                    }

                    val builderConstructorParamsFormat = builderConstructorParams
                        .joinToString(",\n\t") { (format, _) -> format }
                    val builderConstructorArgs = builderConstructorParams
                        .map { (_, args) -> args }.toTypedArray()

                    addFunction(
                        FunSpec.builder("build")
                            .returns(typeName)
                            .addStatement(
                                "return $name(\n\t$builderConstructorParamsFormat\n)",
                                *builderConstructorArgs
                            )
                            .build()
                    )
                }.build()
            )
            .addFunction(
                FunSpec.builder(name)
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
                    .addStatement("return ${name}Builder<T>().apply(builder).build()")
                    .build()
            )
            .build()
            .writeTo(codeGenerator, dependencies)
    }
}

private fun KSValueParameter.defaultValue(fileContent: String) = if (hasDefault) {
    val value = defaultValueRegex(fileContent)
    if (value.startsWith('"') && value.endsWith('"')) "%S" to value.removePrefix("\"").removeSuffix("\"")
    else "%L" to value
} else {
    "throw IllegalStateException(%S)" to "${name!!.asString()} was not set"
}

private const val MATCH_GROUP = "value"

private fun KSValueParameter.defaultValueRegex(fileContent: String) = Regex(
    """${name!!.asString()}\s*:\s*\w+\s*=\s*(?<$MATCH_GROUP>[^,)]+)""",
    RegexOption.MULTILINE
).find(fileContent)?.groups?.get(MATCH_GROUP)?.value?.trim()
    ?: throw IllegalStateException("Could not find default value for parameter ${name!!.asString()}:\r\n$fileContent")
