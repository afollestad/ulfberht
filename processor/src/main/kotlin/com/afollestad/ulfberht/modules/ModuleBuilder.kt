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

import com.afollestad.ulfberht.annotation.Module
import com.afollestad.ulfberht.util.Annotations.SUPPRESS_UNCHECKED_CAST
import com.afollestad.ulfberht.util.BindOrProvide.BIND
import com.afollestad.ulfberht.util.BindOrProvide.PROVIDE
import com.afollestad.ulfberht.util.BinderOrProvider
import com.afollestad.ulfberht.util.DependencyGraph
import com.afollestad.ulfberht.util.Names.CACHED_PROVIDERS_NAME
import com.afollestad.ulfberht.util.Names.CALLED_BY
import com.afollestad.ulfberht.util.Names.CLASS_HEADER
import com.afollestad.ulfberht.util.Names.COMPONENT_PARAM_NAME
import com.afollestad.ulfberht.util.Names.FACTORY_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.GENERIC_ARGS
import com.afollestad.ulfberht.util.Names.GET_PROVIDER_NAME
import com.afollestad.ulfberht.util.Names.IS_SUBCLASS_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.LIBRARY_COMMON_PACKAGE
import com.afollestad.ulfberht.util.Names.LIBRARY_PACKAGE
import com.afollestad.ulfberht.util.Names.MODULE_NAME_SUFFIX
import com.afollestad.ulfberht.util.Names.QUALIFIER
import com.afollestad.ulfberht.util.Names.SINGLETON_PROVIDER_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.WANTED_TYPE
import com.afollestad.ulfberht.util.ProcessorUtil.applyIf
import com.afollestad.ulfberht.util.ProcessorUtil.asFileName
import com.afollestad.ulfberht.util.ProcessorUtil.error
import com.afollestad.ulfberht.util.ProcessorUtil.getFullClassName
import com.afollestad.ulfberht.util.ProcessorUtil.getPackage
import com.afollestad.ulfberht.util.ProcessorUtil.isAbstractClass
import com.afollestad.ulfberht.util.ProcessorUtil.warn
import com.afollestad.ulfberht.util.TypeAndArgs
import com.afollestad.ulfberht.util.Types.BASE_COMPONENT
import com.afollestad.ulfberht.util.Types.BASE_MODULE
import com.afollestad.ulfberht.util.Types.KCLASS_OF_ANY
import com.afollestad.ulfberht.util.Types.NULLABLE_BASE_COMPONENT
import com.afollestad.ulfberht.util.Types.NULLABLE_KOTLIN_STRING
import com.afollestad.ulfberht.util.Types.PROVIDER
import com.afollestad.ulfberht.util.Types.PROVIDER_OF_ANY
import com.afollestad.ulfberht.util.Types.PROVIDER_OF_T
import com.afollestad.ulfberht.util.Types.PROVIDER_OF_T_NULLABLE
import com.afollestad.ulfberht.util.Types.TYPE_VARIABLE_T
import com.afollestad.ulfberht.util.getBindsAndProvidesMethods
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.INTERFACE

/**
 * Generates module implementations from [Module] annotated interfaces and  abstract classes.
 *
 * @author Aidan Follestad (@afollestad)
 */
internal class ModuleBuilder(
  private val environment: ProcessingEnvironment,
  private val dependencyGraph: DependencyGraph
) {
  var haveViewModels: Boolean = false

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
    haveViewModels = false

    val pkg = element.getPackage(environment)
        .also { fullClassName = element.getFullClassName(environment, it) }
    val fileName = fullClassName.asFileName(MODULE_NAME_SUFFIX)
    val typeBuilder = moduleTypeBuilder(fileName, element.isAbstractClass(), fullClassName)
    val providedTypeMethodNameMap = mutableMapOf<TypeAndArgs, BinderOrProvider>()

    element.getBindsAndProvidesMethods(environment, dependencyGraph)
        .map {
          when (it.mode) {
            BIND -> addBindsFunction(it, providedTypeMethodNameMap)
            PROVIDE -> addProvidesFunction(it, providedTypeMethodNameMap)
          }
        }
        .filterNotNull()
        .forEach { typeBuilder.addFunction(it) }

    val typeSpec = typeBuilder
        .addFunction(getProviderFunction(providedTypeMethodNameMap))
        .build()
    val fileSpec = FileSpec.builder(pkg, fileName)
        .addImport(LIBRARY_PACKAGE, IS_SUBCLASS_EXTENSION_NAME)
        .applyIf(haveNonSingletons) { addImport(LIBRARY_COMMON_PACKAGE, FACTORY_EXTENSION_NAME) }
        .applyIf(haveSingletons) {
          addImport(LIBRARY_COMMON_PACKAGE, SINGLETON_PROVIDER_EXTENSION_NAME)
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
    providedTypeMethodNameMap: MutableMap<TypeAndArgs, BinderOrProvider>
  ): FunSpec {
    val code = CodeBlock.builder()
        .add("return when {\n")

    for ((typeAndArgs, binderOrProvider) in providedTypeMethodNameMap) {
      val getterName = binderOrProvider.getterName
      check(getterName.isNotBlank())
      val qualifier = binderOrProvider.qualifier

      code.add("  $WANTED_TYPE.$IS_SUBCLASS_EXTENSION_NAME(%T::class)", typeAndArgs.erasedType)
      code.applyIf(typeAndArgs.hasGenericArgs) {
        add(" && $GENERIC_ARGS == setOf(")
        for ((index, typeArg) in typeAndArgs.genericArgs.withIndex()) {
          if (index > 0) add(", ")
          add("%T::class", typeArg)
        }
        add(")")
      }
      code.applyIf(!typeAndArgs.hasGenericArgs) {
        add(" && $GENERIC_ARGS.isEmpty()")
      }
      code.applyIf(qualifier != null) {
        add(" && $QUALIFIER == %S", qualifier)
      }
      code.applyIf(qualifier == null) {
        add(" && $QUALIFIER == null")
      }
      code.add(" -> %N() as %T\n", getterName, PROVIDER_OF_T)
    }
    code.apply {
      addStatement(
          "  else -> %N.$GET_PROVIDER_NAME($WANTED_TYPE, $GENERIC_ARGS, $QUALIFIER, $CALLED_BY)",
          COMPONENT_PARAM_NAME
      )
      add("}\n")
    }

    return FunSpec.builder(GET_PROVIDER_NAME)
        .addAnnotation(SUPPRESS_UNCHECKED_CAST)
        .addModifiers(OVERRIDE)
        .addTypeVariable(TYPE_VARIABLE_T)
        .addParameter(WANTED_TYPE, KCLASS_OF_ANY)
        .addParameter(GENERIC_ARGS, SET.parameterizedBy(KCLASS_OF_ANY))
        .addParameter(QUALIFIER, NULLABLE_KOTLIN_STRING)
        .addParameter(CALLED_BY, NULLABLE_BASE_COMPONENT)
        .returns(PROVIDER_OF_T_NULLABLE)
        .addCode(code.build())
        .build()
  }

  private fun addBindsFunction(
    method: BinderOrProvider,
    providedTypeMethodNameMap: MutableMap<TypeAndArgs, BinderOrProvider>
  ): FunSpec? {
    providedTypeMethodNameMap[method.providedType] = method
    val code = CodeBlock.builder()
        .apply {
          val fillArgumentTypes = method.fillArgumentTypes
          val paramBreak = fillArgumentTypes.lineBreak
          val factoryNamePrefix = if (fillArgumentTypes.count() > 1) "  " else " "
          add(
              "return %N {$paramBreak$factoryNamePrefix%T(",
              method.providerGetName(),
              method.concreteType.fullType
          )
          if (!construct(fillArgumentTypes, method.providedType)) {
            return null
          }

          if (fillArgumentTypes.count() > 1) add("\n  ")
          add(") $paramBreak}\n")
        }
        .build()

    return FunSpec.builder(method.getterName)
        .addModifiers(PRIVATE)
        .returns(PROVIDER.parameterizedBy(method.providedType.fullType))
        .addCode(code)
        .build()
  }

  private fun addProvidesFunction(
    method: BinderOrProvider,
    providedTypeMethodNameMap: MutableMap<TypeAndArgs, BinderOrProvider>
  ): FunSpec? {
    providedTypeMethodNameMap[method.providedType] = method
    val code = CodeBlock.builder()
        .apply {
          val fillArgumentTypes = method.fillArgumentTypes
          val paramBreak = fillArgumentTypes.lineBreak
          val factoryNamePrefix = if (fillArgumentTypes.count() > 1) "  " else " "

          add(
              "return %N {$paramBreak$factoryNamePrefix%N(",
              method.providerGetName(),
              method.methodName
          )
          if (!construct(fillArgumentTypes, method.providedType)) {
            return null
          }

          if (fillArgumentTypes.count() > 1) add("\n  ")
          add(") $paramBreak}\n")
        }
        .build()

    return FunSpec.builder(method.getterName)
        .addModifiers(PRIVATE)
        .returns(PROVIDER.parameterizedBy(method.providedType.fullType))
        .addCode(code)
        .build()
  }

  private fun CodeBlock.Builder.construct(
    params: Sequence<TypeAndArgs>,
    returnType: TypeAndArgs
  ): Boolean {
    apply {
      val indent = params.indent
      val paramBreak = params.lineBreak

      for ((paramIndex, typeAndArgs) in params.withIndex()) {
        val type = typeAndArgs.erasedType
        val qualifier = typeAndArgs.qualifier
        val getterName = typeAndArgs.getterName
        val doubleBang = if (typeAndArgs.isProvider) "!!" else ""
        if (paramIndex > 0) add(",")

        add("$paramBreak$indent")
        add("$getterName(%T::class", type)
        if (typeAndArgs.hasGenericArgs) {
          add(", setOf(")
          for ((argIndex, typeArg) in typeAndArgs.genericArgs.withIndex()) {
            if (argIndex > 0) add(", ")
            add("%T::class", typeArg)
          }
          add(")")
        }
        if (qualifier != null) {
          add(", $QUALIFIER = %S", qualifier)
        }
        add(")$doubleBang")

        if (!dependencyGraph.put(returnType, typeAndArgs)) {
          // Dependency issue detected
          return false
        }
      }
    }
    return true
  }

  private val Sequence<*>.indent get() = if (count() > 1) "    " else ""

  private val Sequence<*>.lineBreak get() = if (count() > 1) "\n" else ""

  private fun BinderOrProvider.providerGetName(): String {
    return when {
      providedType.isViewModel -> {
        if (isSingleton) {
          environment.warn("$this: ViewModels cannot be @Singleton. Annotation ignored.")
        }
        haveNonSingletons = true
        haveViewModels = true
        FACTORY_EXTENSION_NAME
      }
      isSingleton -> {
        haveSingletons = true
        SINGLETON_PROVIDER_EXTENSION_NAME
      }
      else -> {
        haveNonSingletons = true
        FACTORY_EXTENSION_NAME
      }
    }
  }
}
