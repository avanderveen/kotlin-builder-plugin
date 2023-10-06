package com.vndrvn.kotlin.builder

import org.gradle.api.provider.Property

interface KotlinBuilderPluginExtension {
    val casing: Property<Casing>
}
