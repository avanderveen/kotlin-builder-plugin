# Kotlin Builder DSL Plugin

> TODO: this readme needs to be updated

This plugin generates idiomatic [type-safe builders](https://kotlinlang.org/docs/type-safe-builders.html) for Kotlin
classes.

## Usage

Add the Gradle plugin:
```kotlin
plugins {
    id("com.vndrvn.kotlin.builder")
}
```

Annotate your class with `@Builder`:
```kotlin
@Builder
data class Example(
    val property: String
)
```

Build your project, and the plugin generates a builder DSL implementation:
```kotlin
val example = Example {
    property = "hello world"
}
```

## Features

### Builder Function Name

By default, the builder function has the same name (and casing) as the class. The function name can be overridden by
passing a `name` to the `@Builder` annotation. For example, if you prefer that the builder be lowercase:
```kotlin
@Builder(name = "example")
data class Example(
    val property: String
)
```

### Required and Default Values

All properties must be assigned values unless they have default values declared in the constructor. If a property is not
assigned a value, an `IllegalStateException` will be thrown.

## Future

### Nested Builders

Given a set of nested classes:
```kotlin
@Builder
data class Foo(val bar: Bar) {
    data class Bar(val baz: Int)
}
```

It would be convenient to have nested builders as well:
```kotlin
val foo = Foo {
    // build `Bar` and set `Foo::bar` property at the same time
    bar { baz = 123 }
}
```

### Collections

Given collection properties:
```kotlin
@Builder
data class Foo(
    val list: List<Int>,
    val map: Map<String, Int>,
    val set: Set<Bar>
) {
    data class Bar(val baz: Int)
}
```

It would be convenient to have nested helpers that can be called multiple times for records in the collections:
```kotlin
val example = Example {
    list(1)
    list(2)
    list(4, 5, 6)
    list += 7
    
    map("a", 1)
    map("b" to 2, "c" to 3)
    map += "d" to 4
    
    set(bar { baz = 1 })
    set += bar { baz = 2 }
    set {
        bar { baz = 3 }
        bar { baz = 4 }
    }
}
```

## Generated Code

Generated code is written into `build/generated/ksp/main/kotlin/{package}/{class}Builder.kt` where `{package}` is the
same as the package directory tree for the annotated class and `{class}` is the name of the annotated class.

As an example, this class:
```kotlin
package com.vndrvn.kotlin.builder

@Builder
data class Example(
    val requiredProperty: String,
    val optionalProperty: Int = -1
)
```

results in this code in `build/generated/ksp/main/kotlin/com/vndrvn/kotlin/builder/ExampleBuilder.kt`:
```kotlin
@file:Suppress(
  "PrivatePropertyName",
  "RedundantVisibilityModifier",
  "MemberVisibilityCanBePrivate",
)

package com.vndrvn.kotlin.builder

import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit

public class ExampleBuilder() {
  private var requiredProperty_backing: String? = null

  public var requiredProperty: String
    get() = requiredProperty_backing!!
    set(`value`) {
      requiredProperty_backing = value
      requiredProperty_wasSet = true
    }

  private var requiredProperty_wasSet: Boolean = false

  private var optionalProperty_backing: Int? = null

  public var optionalProperty: Int
    get() = optionalProperty_backing!!
    set(`value`) {
      optionalProperty_backing = value
      optionalProperty_wasSet = true
    }

  private var optionalProperty_wasSet: Boolean = false

  public fun build(): Example = Example(
  	requiredProperty = if (requiredProperty_wasSet) requiredProperty else throw
      IllegalStateException("requiredProperty was not set"),
  	optionalProperty = if (optionalProperty_wasSet) optionalProperty else -1
  )
}

public fun Example(builder: ExampleBuilder.() -> Unit) = ExampleBuilder().apply(builder).build()
```
