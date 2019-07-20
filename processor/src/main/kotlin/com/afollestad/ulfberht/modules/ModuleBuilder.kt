/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.afollestad.ulfberht.modules

import com.afollestad.ulfberht.annotation.Binds
import com.afollestad.ulfberht.annotation.Param
import com.afollestad.ulfberht.annotation.Provides
import com.afollestad.ulfberht.annotation.Singleton
import com.afollestad.ulfberht.util.Annotations.SUPPRESS_UNCHECKED_CAST
import com.afollestad.ulfberht.util.Names.CACHED_PROVIDERS_NAME
import com.afollestad.ulfberht.util.Names.CALLED_BY
import com.afollestad.ulfberht.util.Names.GET_PROVIDER_NAME
import com.afollestad.ulfberht.util.Names.IS_SUBCLASS_OF_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.LIBRARY_PACKAGE
import com.afollestad.ulfberht.util.Names.MODULE_NAME_SUFFIX
import com.afollestad.ulfberht.util.Names.PROVIDER_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.QUALIFIER
import com.afollestad.ulfberht.util.Names.SINGLETON_PROVIDER_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.WANTED_TYPE
import com.afollestad.ulfberht.util.ProcessorUtil.asTypeElement
import com.afollestad.ulfberht.util.ProcessorUtil.correct
import com.afollestad.ulfberht.util.ProcessorUtil.filterMethods
import com.afollestad.ulfberht.util.ProcessorUtil.getAnnotationMirror
import com.afollestad.ulfberht.util.ProcessorUtil.getFullClassName
import com.afollestad.ulfberht.util.ProcessorUtil.getPackage
import com.afollestad.ulfberht.util.ProcessorUtil.getConstructorParamsAndQualifiers
import com.afollestad.ulfberht.util.ProcessorUtil.hasAnnotationMirror
import com.afollestad.ulfberht.util.ProcessorUtil.isAbstractClass
import com.afollestad.ulfberht.util.ProcessorUtil.qualifier
import com.afollestad.ulfberht.util.ProcessorUtil.asFileName
import com.afollestad.ulfberht.util.Types.BASE_COMPONENT
import com.afollestad.ulfberht.util.Types.BASE_MODULE
import com.afollestad.ulfberht.util.Types.KCLASS_OF_T
import com.afollestad.ulfberht.util.Types.NULLABLE_BASE_COMPONENT
import com.afollestad.ulfberht.util.Types.NULLABLE_KOTLIN_STRING
import com.afollestad.ulfberht.util.Types.PROVIDER
import com.afollestad.ulfberht.util.Types.PROVIDER_OF_ANY
import com.afollestad.ulfberht.util.Types.PROVIDER_OF_T
import com.afollestad.ulfberht.util.Types.PROVIDER_OF_T_NULLABLE
import com.afollestad.ulfberht.util.Types.TYPE_VARIABLE_T
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.INTERFACE
import javax.lang.model.element.ExecutableElement

internal class ModuleBuilder(
  private val environment: ProcessingEnvironment
) {
  private lateinit var fullClassName: ClassName
  private var haveNonSingletons: Boolean = false
  private var haveSingletons: Boolean = false

  fun generate(element: Element) {
    check(element.kind == INTERFACE || element.isAbstractClass()) {
      "@Module annotation can only decorate interfaces or abstract classes."
    }
    haveNonSingletons = false
    haveSingletons = false

    val pkg = element.getPackage(environment)
    fullClassName = element.getFullClassName(environment, pkg)

    val fileName = fullClassName.asFileName(MODULE_NAME_SUFFIX)
    val typeBuilder = moduleTypeBuilder(fileName, element.isAbstractClass(), fullClassName)
    val providedTypeMethodNameMap = mutableMapOf<TypeName, MethodNameAndQualifier>()

    element.enclosedElements
        .filterMethods()
        .forEach { method ->
          val bindsAnnotation = method.getAnnotationMirror<Binds>()
          bindsAnnotation?.let {
            check(element.kind == INTERFACE) {
              "@Binds methods can only be used in an interface."
            }
            typeBuilder.addFunction(bindsFunction(method, providedTypeMethodNameMap))
            return@forEach
          }
          val providesAnnotation = method.getAnnotationMirror<Provides>()
          providesAnnotation?.let {
            check(element.isAbstractClass()) {
              "@Provides methods can only be used in an abstract class."
            }
            typeBuilder.addFunction(providesFunction(method, providedTypeMethodNameMap))
            return@forEach
          }
        }

    val typeSpec = typeBuilder
        .addFunction(getProviderFunction(providedTypeMethodNameMap))
        .build()
    val fileSpec = FileSpec.builder(pkg, fileName)
        .apply {
          addImport(LIBRARY_PACKAGE, IS_SUBCLASS_OF_EXTENSION_NAME)
          if (haveNonSingletons) {
            addImport(LIBRARY_PACKAGE, PROVIDER_EXTENSION_NAME)
          }
          if (haveSingletons) {
            addImport(LIBRARY_PACKAGE, SINGLETON_PROVIDER_EXTENSION_NAME)
          }
        }
        .addType(typeSpec)
        .build()
    fileSpec.writeTo(environment.filer)
  }

  private fun moduleTypeBuilder(
    fileName: String,
    isAbstractClass: Boolean,
    fullClassName: ClassName
  ): TypeSpec.Builder {
    val builder = TypeSpec.classBuilder(fileName)
    return builder
        .addSuperinterface(BASE_MODULE)
        .apply {
          if (isAbstractClass) {
            superclass(fullClassName)
          }
        }
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(COMPONENT_PARAM_NAME, BASE_COMPONENT)
                .build()
        )
        .addProperties(
            listOf(
                cachedProvidersProperty(),
                PropertySpec.builder(COMPONENT_PARAM_NAME, BASE_COMPONENT)
                    .addModifiers(PRIVATE)
                    .initializer(COMPONENT_PARAM_NAME)
                    .build()
            )
        )
  }

  private fun cachedProvidersProperty(): PropertySpec {
    val cachedProviderType = MUTABLE_MAP.parameterizedBy(STRING, PROVIDER_OF_ANY)
    return PropertySpec.builder(CACHED_PROVIDERS_NAME, cachedProviderType)
        .addModifiers(OVERRIDE)
        .initializer("hashMapOf()")
        .build()
  }

  private fun getProviderFunction(
    providedTypeMethodNameMap: MutableMap<TypeName, MethodNameAndQualifier>
  ): FunSpec {
    val code = CodeBlock.builder().add("return when {\n")

    for ((key, value) in providedTypeMethodNameMap) {
      if (value.qualifier != null) {
        code.addStatement(
            "  $WANTED_TYPE.$IS_SUBCLASS_OF_EXTENSION_NAME<%T>() " +
                "&& %S == $QUALIFIER -> %N() as %T",
            key, value.qualifier, value.name, PROVIDER_OF_T
        )
      } else {
        code.addStatement(
            "  $WANTED_TYPE.$IS_SUBCLASS_OF_EXTENSION_NAME<%T>() -> %N() as %T",
            key, value.name, PROVIDER_OF_T
        )
      }
    }

    code.addStatement(
        "  else -> %N.getProvider($WANTED_TYPE, $QUALIFIER, $CALLED_BY)",
        COMPONENT_PARAM_NAME
    )
    code.add("}\n")

    return FunSpec.builder(GET_PROVIDER_NAME)
        .addAnnotation(SUPPRESS_UNCHECKED_CAST)
        .addModifiers(OVERRIDE)
        .addTypeVariable(TYPE_VARIABLE_T)
        .addParameter(WANTED_TYPE, KCLASS_OF_T)
        .addParameter(QUALIFIER, NULLABLE_KOTLIN_STRING)
        .addParameter(CALLED_BY, NULLABLE_BASE_COMPONENT)
        .returns(PROVIDER_OF_T_NULLABLE)
        .addCode(code.build())
        .build()
  }

  private fun bindsFunction(
    method: ExecutableElement,
    providedTypeMethodNameMap: MutableMap<TypeName, MethodNameAndQualifier>
  ): FunSpec {
    require(method.parameters.size == 1) {
      "@Binds methods must have a single parameter."
    }
    val parameter = method.parameters.single()
    val parameterType = parameter.asType()
    val returnType = method.returnType

    require(environment.typeUtils.isSubtype(parameterType, returnType)) {
      "@Binds method ${method.simpleName}() parameter of type " +
          "$parameterType must be a subclass of $returnType"
    }

    val correctedReturnType = returnType.correct()
    val providerReturnType = PROVIDER.parameterizedBy(correctedReturnType)
    val methodName = method.simpleName.toString()

    val providerMethodName = method.providerGetName()
    val qualifierName = method.getAnnotationMirror<Binds>()
        .qualifier
    providedTypeMethodNameMap[correctedReturnType] = MethodNameAndQualifier(
        name = methodName,
        qualifier = qualifierName
    )

    val fieldTypeConstructorParams = parameterType
        .asTypeElement()
        .getConstructorParamsAndQualifiers()

    val code = CodeBlock.builder()
    if (fieldTypeConstructorParams.isEmpty()) {
      code.addStatement("return %N { %T() }", providerMethodName, parameterType)
    } else {
      code.apply {
        add("return %N {\n", providerMethodName)
        addStatement("  %T(", parameterType)
        for ((index, typeAndQualifier) in fieldTypeConstructorParams.withIndex()) {
          val (type, qualifier) = typeAndQualifier
          if (index > 0) add(",\n")
          if (qualifier != null) {
            add("    get(%T::class, qualifier = %S)", type, qualifier)
          } else {
            add("    get(%T::class)", type)
          }
        }
        add("\n  )")
        add("\n}\n")
      }
    }

    return FunSpec.builder(methodName)
        .addModifiers(PRIVATE)
        .returns(providerReturnType)
        .addCode(code.build())
        .build()
  }

  private fun providesFunction(
    method: ExecutableElement,
    providedTypeMethodNameMap: MutableMap<TypeName, MethodNameAndQualifier>
  ): FunSpec {
    val originalMethodName = method.simpleName.toString()
    val newMethodName = "$PROVIDE_FUNCTION_PREFIX${originalMethodName.capitalize()}"
    val correctedReturnType = method.returnType.correct()
    val providerReturnType = PROVIDER.parameterizedBy(correctedReturnType)

    val providerMethodName = method.providerGetName()
    val qualifierName = method.getAnnotationMirror<Provides>()
        .qualifier
    providedTypeMethodNameMap[correctedReturnType] = MethodNameAndQualifier(
        name = newMethodName,
        qualifier = qualifierName
    )

    val code = CodeBlock.builder()
    when {
      method.parameters.isEmpty() -> {
        code.addStatement("return %N { %N() }", providerMethodName, originalMethodName)
      }
      method.parameters.size == 1 -> {
        val param = method.parameters.single()
        val qualifier = method.getAnnotationMirror<Param>()
            .qualifier
        code.add("return %N {\n  %N(", providerMethodName, originalMethodName)
        if (qualifier != null) {
          code.add("get(%T::class, qualifier = %S)", param.asType(), qualifier)
        } else {
          code.add("get(%T::class)", param.asType())
        }
        code.add(")\n}\n")
      }
      else -> {
        code.apply {
          add("return %N {\n", providerMethodName)
          add("  %N(\n", originalMethodName)
        }
        for (param in method.parameters) {
          val qualifier = method.getAnnotationMirror<Param>()
              .qualifier
          if (qualifier != null) {
            code.addStatement("    get(%T::class, qualifier = %S)", param.asType(), qualifier)
          } else {
            code.addStatement("    get(%T::class)", param.asType())
          }
        }
        code.apply {
          add("  )\n")
          add("}\n")
        }
      }
    }

    return FunSpec.builder(newMethodName)
        .addModifiers(PRIVATE)
        .returns(providerReturnType)
        .addCode(code.build())
        .build()
  }

  private fun Element.providerGetName(): String {
    return if (hasAnnotationMirror<Singleton>()) {
      haveSingletons = true
      SINGLETON_PROVIDER_EXTENSION_NAME
    } else {
      haveNonSingletons = true
      PROVIDER_EXTENSION_NAME
    }
  }

  private data class MethodNameAndQualifier(
    val name: String,
    val qualifier: String?
  )

  companion object {
    private const val COMPONENT_PARAM_NAME = "component"
    const val PROVIDE_FUNCTION_PREFIX = "provides"
  }
}
