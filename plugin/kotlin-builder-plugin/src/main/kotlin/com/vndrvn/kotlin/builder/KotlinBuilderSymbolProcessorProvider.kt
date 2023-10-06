package com.vndrvn.kotlin.builder

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSValueParameter

class KotlinBuilderSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ) = KotlinBuilderSymbolProcessor(
        BuilderCodeGenerator(
            environment.codeGenerator,
            environment.casing
        )
    )
}

private val SymbolProcessorEnvironment.casing: Casing? get() {
    return options["${javaClass.packageName}.casing"]?.let {
        if (it.isBlank()) null else Casing.valueOf(it)
    }
}
