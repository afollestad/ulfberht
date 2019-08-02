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
import com.afollestad.ulfberht.annotation.Module
import com.afollestad.ulfberht.annotation.Provides
import com.afollestad.ulfberht.annotation.Singleton
import com.afollestad.ulfberht.util.Annotations.SUPPRESS_UNCHECKED_CAST
import com.afollestad.ulfberht.util.DependencyGraph
import com.afollestad.ulfberht.util.Names.CACHED_PROVIDERS_NAME
import com.afollestad.ulfberht.util.Names.CALLED_BY
import com.afollestad.ulfberht.util.Names.CLASS_HEADER
import com.afollestad.ulfberht.util.Names.COMPONENT_PARAM_NAME
import com.afollestad.ulfberht.util.Names.GET_PROVIDER_NAME
import com.afollestad.ulfberht.util.Names.IS_SUBCLASS_OF_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.LIBRARY_PACKAGE
import com.afollestad.ulfberht.util.Names.MODULE_NAME_SUFFIX
import com.afollestad.ulfberht.util.Names.FACTORY_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.QUALIFIER
import com.afollestad.ulfberht.util.Names.SINGLETON_PROVIDER_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.WANTED_TYPE
import com.afollestad.ulfberht.util.ProcessorUtil.applyIf
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
import com.afollestad.ulfberht.util.ProcessorUtil.error
import com.afollestad.ulfberht.util.ProcessorUtil.getMethodParamsAndQualifiers
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

/**
 * Generates module implementations from [Module] annotated interfaces and  abstract classes.
 *
 * @author Aidan Follestad (@afollestad)
 */
internal class ModuleBuilder(
  private val environment: ProcessingEnvironment,
  private val dependencyGraph: DependencyGraph
) {
  private lateinit var fullClassName: ClassName
  private var haveNonSingletons: Boolean = false
  private var haveSingletons: Boolean = false

  fun generate(element: Element) {
    if (element.kind != INTERFACE && !element.isAbstractClass()) {
      environment.error(
          "$element: @Module annotation can only decorate interfaces or abstract classes."
      )
      return
    }
    haveNonSingletons = false
    haveSingletons = false

    val pkg = element.getPackage(environment)
        .also { fullClassName = element.getFullClassName(environment, it) }

    val fileName = fullClassName.asFileName(MODULE_NAME_SUFFIX)
    val typeBuilder = moduleTypeBuilder(fileName, element.isAbstractClass(), fullClassName)
    val providedTypeMethodNameMap = mutableMapOf<TypeName, MethodNameAndQualifier>()

    element.enclosedElements
        .filterMethods()
        .map { processBindsOrProvidesMethod(element, it, providedTypeMethodNameMap) }
        .filter { it != null }
        .forEach { typeBuilder.addFunction(it!!) }

    val typeSpec = typeBuilder
        .addFunction(getProviderFunction(providedTypeMethodNameMap))
        .build()
    val fileSpec = FileSpec.builder(pkg, fileName)
        .addImport(LIBRARY_PACKAGE, IS_SUBCLASS_OF_EXTENSION_NAME)
        .applyIf(haveNonSingletons) { addImport(LIBRARY_PACKAGE, FACTORY_EXTENSION_NAME) }
        .applyIf(haveSingletons) { addImport(LIBRARY_PACKAGE, SINGLETON_PROVIDER_EXTENSION_NAME) }
        .addType(typeSpec)
        .build()
    fileSpec.writeTo(environment.filer)
  }

  private fun processBindsOrProvidesMethod(
    element: Element,
    method: ExecutableElement,
    providedTypeMethodNameMap: MutableMap<TypeName, MethodNameAndQualifier>
  ): FunSpec? {
    return when {
      method.getAnnotationMirror<Binds>() != null -> if (element.kind != INTERFACE) {
        environment.error("$method: @Binds methods can only be used in an interface.")
        null
      } else {
        bindsFunction(method, providedTypeMethodNameMap)
      }
      method.getAnnotationMirror<Provides>() != null -> if (!element.isAbstractClass()) {
        environment.error("$method: @Provides methods can only be used in an abstract class.")
        null
      } else {
        providesFunction(method, providedTypeMethodNameMap)
      }
      else -> null
    }
  }

  private fun moduleTypeBuilder(
    fileName: String,
    isAbstractClass: Boolean,
    fullClassName: ClassName
  ): TypeSpec.Builder {
    return TypeSpec.classBuilder(fileName)
        .addKdoc(CLASS_HEADER)
        .addSuperinterface(BASE_MODULE)
        .applyIf(isAbstractClass) { superclass(fullClassName) }
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(COMPONENT_PARAM_NAME, BASE_COMPONENT, OVERRIDE)
                .build()
        )
        .addProperties(
            listOf(
                cachedProvidersProperty(),
                PropertySpec.builder(COMPONENT_PARAM_NAME, BASE_COMPONENT)
                    .addModifiers(OVERRIDE)
                    .initializer(COMPONENT_PARAM_NAME)
                    .build()
            )
        )
  }

  private fun cachedProvidersProperty(): PropertySpec {
    val cachedProviderType = MUTABLE_MAP.parameterizedBy(STRING, PROVIDER_OF_ANY)
    return PropertySpec.builder(CACHED_PROVIDERS_NAME, cachedProviderType)
        .addModifiers(OVERRIDE)
        .initializer("mutableMapOf()")
        .build()
  }

  private fun getProviderFunction(
    providedTypeMethodNameMap: MutableMap<TypeName, MethodNameAndQualifier>
  ): FunSpec {
    val code = CodeBlock.builder()
        .add("return when {\n")

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

    code.apply {
      addStatement(
          "  else -> %N.getProvider($WANTED_TYPE, $QUALIFIER, $CALLED_BY)",
          COMPONENT_PARAM_NAME
      )
      add("}\n")
    }

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
  ): FunSpec? {
    if (method.parameters.size != 1) {
      environment.error("$method: @Binds methods must have a single parameter.")
      return null
    }
    val parameterType = method.parameters.single()
        .asType()

    val returnType = method.returnType
    if (!environment.typeUtils.isSubtype(parameterType, returnType)) {
      environment.error(
          "@Binds method ${method.simpleName}() parameter of type " +
              "$parameterType must be a subclass of $returnType"
      )
      return null
    }

    val correctedReturnType = returnType.correct()
    val methodName = method.simpleName.toString()

    val providerMethodName = method.providerGetName()
    providedTypeMethodNameMap[correctedReturnType] = MethodNameAndQualifier(
        name = methodName,
        qualifier = method.getAnnotationMirror<Binds>().qualifier
    )

    val fieldTypeConstructorParams = parameterType
        .asTypeElement()
        .getConstructorParamsAndQualifiers()

    val code = CodeBlock.builder()
        .apply {
          val paramBreak = fieldTypeConstructorParams.lineBreak
          val factoryNamePrefix = if (fieldTypeConstructorParams.size > 1) "  " else " "

          add("return %N {$paramBreak$factoryNamePrefix%T(", providerMethodName, parameterType)
          if (!construct(fieldTypeConstructorParams, correctedReturnType)) {
            return null
          }
          if (fieldTypeConstructorParams.size > 1) add("\n  ")
          add(") $paramBreak}\n")
        }
        .build()

    return FunSpec.builder(methodName)
        .addModifiers(PRIVATE)
        .returns(PROVIDER.parameterizedBy(correctedReturnType))
        .addCode(code)
        .build()
  }

  private fun providesFunction(
    method: ExecutableElement,
    providedTypeMethodNameMap: MutableMap<TypeName, MethodNameAndQualifier>
  ): FunSpec? {
    val originalMethodName = method.simpleName.toString()
    val newMethodName = "$PROVIDE_FUNCTION_PREFIX${originalMethodName.capitalize()}"
    val correctedReturnType = method.returnType.correct()

    providedTypeMethodNameMap[correctedReturnType] = MethodNameAndQualifier(
        name = newMethodName,
        qualifier = method.getAnnotationMirror<Provides>().qualifier
    )

    val code = CodeBlock.builder()
        .apply {
          val paramBreak = method.parameters.lineBreak
          val factoryNamePrefix = if (method.parameters.size > 1) "  " else " "

          add(
              "return %N {$paramBreak$factoryNamePrefix%N(",
              method.providerGetName(),
              originalMethodName
          )
          if (!construct(method.getMethodParamsAndQualifiers(), correctedReturnType)) {
            return null
          }
          if (method.parameters.size > 1) add("\n  ")
          add(") $paramBreak}\n")
        }
        .build()

    return FunSpec.builder(newMethodName)
        .addModifiers(PRIVATE)
        .returns(PROVIDER.parameterizedBy(correctedReturnType))
        .addCode(code)
        .build()
  }

  private fun CodeBlock.Builder.construct(
    params: List<Pair<TypeName, String?>>,
    returnType: TypeName
  ): Boolean {
    apply {
      val indent = params.indent
      val paramBreak = params.lineBreak

      for ((index, typeAndQualifier) in params.withIndex()) {
        val (type, qualifier) = typeAndQualifier
        if (index > 0) add(",")
        if (qualifier != null) {
          add("$paramBreak${indent}get(%T::class, qualifier = %S)", type, qualifier)
        } else {
          add("$paramBreak${indent}get(%T::class)", type)
        }
        if (!dependencyGraph.put(returnType, type)) {
          // Dependency issue detected
          return false
        }
      }
    }
    return true
  }

  private val List<*>.indent get() = if (size > 1) "    " else ""

  private val List<*>.lineBreak get() = if (size > 1) "\n" else ""

  private fun Element.providerGetName(): String {
    return if (hasAnnotationMirror<Singleton>()) {
      haveSingletons = true
      SINGLETON_PROVIDER_EXTENSION_NAME
    } else {
      haveNonSingletons = true
      FACTORY_EXTENSION_NAME
    }
  }

  private data class MethodNameAndQualifier(
    val name: String,
    val qualifier: String?
  )

  companion object {
    const val PROVIDE_FUNCTION_PREFIX = "_provides"
  }
}
