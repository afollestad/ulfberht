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
package com.afollestad.ulfberht.components

import com.afollestad.ulfberht.annotation.Component
import com.afollestad.ulfberht.annotation.ScopeOwner
import com.afollestad.ulfberht.util.Annotations.SUPPRESS_UNCHECKED_CAST
import com.afollestad.ulfberht.util.Names.CALLED_BY
import com.afollestad.ulfberht.util.Names.FACTORY_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.GET_PROVIDER_NAME
import com.afollestad.ulfberht.util.Names.GET_RUNTIME_DEP_NAME
import com.afollestad.ulfberht.util.Names.LIBRARY_PACKAGE
import com.afollestad.ulfberht.util.Names.MODULES_LIST_NAME
import com.afollestad.ulfberht.util.Names.RUNTIME_DEPS_NAME
import com.afollestad.ulfberht.util.Names.QUALIFIER
import com.afollestad.ulfberht.util.Names.WANTED_TYPE
import com.afollestad.ulfberht.util.ProcessorUtil.asFileName
import com.afollestad.ulfberht.util.ProcessorUtil.asTypeElement
import com.afollestad.ulfberht.util.ProcessorUtil.error
import com.afollestad.ulfberht.util.ProcessorUtil.filterMethods
import com.afollestad.ulfberht.util.ProcessorUtil.getAnnotationMirror
import com.afollestad.ulfberht.util.ProcessorUtil.getFieldTypeName
import com.afollestad.ulfberht.util.ProcessorUtil.getFullClassName
import com.afollestad.ulfberht.util.ProcessorUtil.getModulesTypes
import com.afollestad.ulfberht.util.ProcessorUtil.getPackage
import com.afollestad.ulfberht.util.ProcessorUtil.getParameter
import com.afollestad.ulfberht.util.ProcessorUtil.injectedFieldsAndQualifiers
import com.afollestad.ulfberht.util.ProcessorUtil.isLifecycleOwner
import com.afollestad.ulfberht.util.ProcessorUtil.name
import com.afollestad.ulfberht.util.Types.BASE_COMPONENT
import com.afollestad.ulfberht.util.Types.BASE_MODULE
import com.afollestad.ulfberht.util.Types.GET_SCOPE_METHOD
import com.afollestad.ulfberht.util.Types.KCLASS_OF_ANY
import com.afollestad.ulfberht.util.Types.KCLASS_OF_T
import com.afollestad.ulfberht.util.Types.LIFECYCLE_EVENT_ON_DESTROY
import com.afollestad.ulfberht.util.Types.LIFECYCLE_OBSERVER
import com.afollestad.ulfberht.util.Types.LOGGER
import com.afollestad.ulfberht.util.Types.NULLABLE_BASE_COMPONENT
import com.afollestad.ulfberht.util.Types.NULLABLE_KOTLIN_STRING
import com.afollestad.ulfberht.util.Types.ON_LIFECYCLE_EVENT
import com.afollestad.ulfberht.util.Types.PROVIDER_OF_T_NULLABLE
import com.afollestad.ulfberht.util.Types.SCOPE
import com.afollestad.ulfberht.util.Types.TYPE_VARIABLE_T
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.INTERFACE
import javax.lang.model.element.ExecutableElement

/**
 * Generates component implementations from [Component] annotated interfaces.
 *
 * @author Aidan Follestad (@afollestad)
 */
internal class ComponentBuilder(
  private val environment: ProcessingEnvironment
) {
  private lateinit var fullClassName: ClassName

  fun generate(element: Element) {
    if (element.kind != INTERFACE) {
      environment.error(
          "${element.simpleName}: @Component annotation can only decorate an interface."
      )
      return
    }

    val pkg = element.getPackage(environment)
        .also { fullClassName = element.getFullClassName(environment, it) }

    val fileName = fullClassName.asFileName(COMPONENT_NAME_SUFFIX)
    val component = element.getAnnotationMirror<Component>()!!
    val scopeName = component.getParameter<String>(SCOPE_NAME) ?: ""
    val moduleTypes = component.getModulesTypes(environment)
    val typeBuilder = componentTypeBuilder(fileName, fullClassName, scopeName, moduleTypes)

    element.enclosedElements
        .filterMethods()
        .filter { it.simpleName.toString() == INJECT_METHOD_NAME }
        .map { injectFunction(it) }
        .filter { it != null }
        .forEach { typeBuilder.addFunction(it!!) }

    val fileSpec = FileSpec.builder(pkg, fileName)
        .addImport(LIBRARY_PACKAGE, FACTORY_EXTENSION_NAME)
        .addType(typeBuilder.build())
        .build()
    fileSpec.writeTo(environment.filer)
  }

  private fun componentTypeBuilder(
    fileName: String,
    superInterface: TypeName,
    scope: String,
    moduleTypes: Sequence<TypeName>
  ): Builder {
    return TypeSpec.classBuilder(fileName)
        .addSuperinterface(superInterface)
        .addSuperinterface(BASE_COMPONENT)
        .primaryConstructor(typeConstructor())
        .addProperties(
            listOf(
                propertyScope(scope),
                propertyOriginalType(superInterface),
                propertyParent(),
                propertyChildren(),
                propertyModuleList(moduleTypes),
                propertyRuntimeDependencies()
            )
        )
        .addFunction(getProviderFunction(moduleTypes))
  }

  private fun propertyScope(value: String): PropertySpec {
    return PropertySpec.builder(SCOPE_NAME, STRING)
        .addModifiers(OVERRIDE)
        .mutable()
        .initializer("%S", value)
        .build()
  }

  private fun propertyOriginalType(type: TypeName): PropertySpec {
    return PropertySpec.builder(ORIGINAL_TYPE_NAME, KCLASS_OF_ANY)
        .addModifiers(OVERRIDE)
        .initializer("%T::class", type)
        .build()
  }

  private fun propertyParent(): PropertySpec {
    return PropertySpec.builder(PARENT_NAME, NULLABLE_BASE_COMPONENT)
        .addModifiers(OVERRIDE)
        .initializer(PARENT_NAME)
        .build()
  }

  private fun parameterParent(): ParameterSpec {
    return ParameterSpec.builder(PARENT_NAME, NULLABLE_BASE_COMPONENT)
        .defaultValue("null")
        .build()
  }

  private fun typeConstructor(): FunSpec {
    return FunSpec.constructorBuilder()
        .addParameter(parameterParent())
        .build()
  }

  private fun propertyChildren(): PropertySpec {
    val propertyType = MUTABLE_SET.parameterizedBy(BASE_COMPONENT)
    return PropertySpec.builder(CHILDREN_NAME, propertyType)
        .addModifiers(OVERRIDE)
        .initializer("mutableSetOf()")
        .build()
  }

  private fun propertyModuleList(
    moduleTypes: Sequence<TypeName>
  ): PropertySpec {
    val propertyType = SET.parameterizedBy(BASE_MODULE)
    val initializer = if (moduleTypes.firstOrNull() != null) {
      CodeBlock.builder()
          .apply {
            addStatement("setOf(")
            moduleTypes.forEachIndexed { index, typeName ->
              if (index > 0) addStatement(", ")
              add("  %T(this)", typeName)
            }
            add("\n)")
          }
          .build()
    } else {
      CodeBlock.of("emptySet()")
    }

    return PropertySpec.builder(MODULES_LIST_NAME, propertyType)
        .addModifiers(OVERRIDE)
        .initializer(initializer)
        .build()
  }

  private fun propertyRuntimeDependencies(): PropertySpec {
    val propertyType = MAP
        .parameterizedBy(NULLABLE_KOTLIN_STRING, ANY)
        .copy(nullable = true)
    return PropertySpec.builder(RUNTIME_DEPS_NAME, propertyType, OVERRIDE)
        .mutable()
        .initializer("null")
        .build()
  }

  private fun getProviderFunction(moduleTypes: Sequence<TypeName>): FunSpec {
    val code = CodeBlock.builder()
    if (moduleTypes.firstOrNull() != null) {
      code.add(
          CodeBlock.of(
              """
              if ($CALLED_BY === this) return null
              $MODULES_LIST_NAME.forEach { module ->
                module.getProvider($WANTED_TYPE, $QUALIFIER, $CALLED_BY ?: this)
                    ?.let { return it }
              }
              """.trimIndent() + "\n"
          )
      )
    }
    code.add(
        CodeBlock.of(
            """
            if ($PARENT_NAME != null && $CALLED_BY === $PARENT_NAME) return null
            val runtimeProvider = $GET_RUNTIME_DEP_NAME<%T>($QUALIFIER)
                  ?.run { $FACTORY_EXTENSION_NAME { this } }
            return runtimeProvider ?: $PARENT_NAME?.getProvider($WANTED_TYPE, $QUALIFIER, $CALLED_BY)
            """.trimIndent() + "\n",
            TYPE_VARIABLE_T
        )
    )

    return FunSpec.builder(GET_PROVIDER_NAME)
        .addAnnotation(SUPPRESS_UNCHECKED_CAST)
        .addParameter(WANTED_TYPE, KCLASS_OF_T)
        .addParameter(QUALIFIER, NULLABLE_KOTLIN_STRING)
        .addParameter(CALLED_BY, NULLABLE_BASE_COMPONENT)
        .addModifiers(OVERRIDE)
        .addTypeVariable(TYPE_VARIABLE_T)
        .returns(PROVIDER_OF_T_NULLABLE)
        .addCode(code.build())
        .build()
  }

  private fun injectFunction(method: ExecutableElement): FunSpec? {
    if (method.parameters.size != 1) {
      environment.error("$method: $INJECT_METHOD_NAME() methods must have a single parameter.")
      return null
    }
    val parameter = method.parameters.single()
    if (method.returnType.toString() != VOID_TYPE_NAME) {
      environment.error("$method: $INJECT_METHOD_NAME() methods must have no return value.")
      return null
    }

    val paramName = parameter.simpleName.toString()
    val paramClass = parameter.asType()
        .asTypeElement()

    val code = CodeBlock.builder()
    paramClass.enclosedElements
        .injectedFieldsAndQualifiers()
        .forEach { (field, qualifier) ->
          if (qualifier != null) {
            code.addStatement(
                "$paramName.%N = get(%T::class, qualifier = %S)",
                field.simpleName.toString(),
                field.getFieldTypeName(),
                qualifier
            )
          } else {
            code.addStatement(
                "$paramName.%N = get(%T::class)",
                field.simpleName.toString(), field.getFieldTypeName()
            )
          }
        }

    paramClass.getAnnotationMirror<ScopeOwner>()
        ?.let { scopeOwner ->
          if (!paramClass.isLifecycleOwner()) {
            environment.error(
                "$paramClass: @ScopeOwner can only be used on classes which implement LifecycleOwner."
            )
            return null
          }

          val ownedScope = scopeOwner.name
          code.add(
            "\n" + """
            val scope: %T = %T(%S)
            $paramName.lifecycle.addObserver(object : %T {
              @%T(%T)
              fun onDestroy() {
                scope.exit()
                %T.log(%P)
              }
            })
            %T.log(%P)
            """.trimIndent() + "\n",
              SCOPE, GET_SCOPE_METHOD, ownedScope,
              LIFECYCLE_OBSERVER,
              ON_LIFECYCLE_EVENT, LIFECYCLE_EVENT_ON_DESTROY,
              LOGGER, "$$paramName destroyed scope $ownedScope",
              LOGGER, "$$paramName is now the owner of scope $ownedScope"
          )
        }

    return FunSpec.builder(method.simpleName.toString())
        .addModifiers(OVERRIDE)
        .addParameter(paramName, parameter.asType().asTypeName())
        .addCode(code.build())
        .build()
  }

  companion object {
    const val PARENT_NAME = "parent"
    const val CHILDREN_NAME = "children"

    private const val COMPONENT_NAME_SUFFIX = "_Component"
    private const val INJECT_METHOD_NAME = "inject"
    private const val VOID_TYPE_NAME = "void"
    private const val ORIGINAL_TYPE_NAME = "originalType"
    private const val SCOPE_NAME = "scope"
  }
}
