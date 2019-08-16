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
import com.afollestad.ulfberht.util.Names.CHILDREN_NAME
import com.afollestad.ulfberht.util.Names.CLASS_HEADER
import com.afollestad.ulfberht.util.Names.COMPONENT_NAME_SUFFIX
import com.afollestad.ulfberht.util.Names.GET_NAME
import com.afollestad.ulfberht.util.Names.MODULES_LIST_NAME
import com.afollestad.ulfberht.util.Names.PARENT_NAME
import com.afollestad.ulfberht.util.Names.PARENT_TYPE_NAME
import com.afollestad.ulfberht.util.Names.RUNTIME_DEPS_NAME
import com.afollestad.ulfberht.util.Names.VIEW_MODEL_FACTORY_CREATE
import com.afollestad.ulfberht.util.Annotations.SUPPRESS_UNCHECKED_CAST
import com.afollestad.ulfberht.util.ProcessorUtil.asFileName
import com.afollestad.ulfberht.util.ProcessorUtil.asTypeElement
import com.afollestad.ulfberht.util.ProcessorUtil.error
import com.afollestad.ulfberht.util.ProcessorUtil.filterMethods
import com.afollestad.ulfberht.util.ProcessorUtil.getAnnotationMirror
import com.afollestad.ulfberht.util.ProcessorUtil.applyIf
import com.afollestad.ulfberht.util.ProcessorUtil.getFullClassName
import com.afollestad.ulfberht.util.ProcessorUtil.getModulesTypes
import com.afollestad.ulfberht.util.ProcessorUtil.getPackage
import com.afollestad.ulfberht.util.ProcessorUtil.getParameter
import com.afollestad.ulfberht.util.ProcessorUtil.injectingFields
import com.afollestad.ulfberht.util.ProcessorUtil.hasInterface
import com.afollestad.ulfberht.util.ProcessorUtil.name
import com.afollestad.ulfberht.util.ProcessorUtil.isVoid
import com.afollestad.ulfberht.util.Types.BASE_COMPONENT
import com.afollestad.ulfberht.util.Types.BASE_MODULE
import com.afollestad.ulfberht.util.Types.GET_SCOPE_METHOD
import com.afollestad.ulfberht.util.Types.KCLASS_OF_ANY
import com.afollestad.ulfberht.util.Types.LIFECYCLE_EVENT_ON_DESTROY
import com.afollestad.ulfberht.util.Types.LIFECYCLE_OBSERVER
import com.afollestad.ulfberht.util.Types.LIFECYCLE_OWNER
import com.afollestad.ulfberht.util.Types.LOGGER
import com.afollestad.ulfberht.util.Types.NULLABLE_BASE_COMPONENT
import com.afollestad.ulfberht.util.Types.ON_LIFECYCLE_EVENT
import com.afollestad.ulfberht.util.Types.VIEW_MODEL
import com.afollestad.ulfberht.util.Types.VIEW_MODEL_FACTORY
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.INTERFACE
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

/**
 * Generates component implementations from [Component] annotated interfaces.
 *
 * @author Aidan Follestad (@afollestad)
 */
internal class ComponentBuilder(
  private val environment: ProcessingEnvironment,
  private val parentTypeMap: MutableMap<TypeName, TypeName>
) {
  private lateinit var fullClassName: ClassName

  fun generate(
    element: Element,
    haveViewModels: Boolean
  ) {
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
    val typeBuilder = componentTypeBuilder(
        haveViewModels = haveViewModels,
        originalType = element.asType().asTypeElement().asClassName(),
        fileName = fileName,
        superInterface = fullClassName,
        scope = scopeName,
        moduleTypes = moduleTypes
    )

    element.enclosedElements
        .filterMethods()
        .filter { it.simpleName.toString() == INJECT_METHOD_NAME }
        .map(::injectFunction)
        .filterNotNull()
        .forEach { typeBuilder.addFunction(it) }

    val fileSpec = FileSpec.builder(pkg, fileName)
        .addType(typeBuilder.build())
        .build()
    fileSpec.writeTo(environment.filer)
  }

  private fun componentTypeBuilder(
    haveViewModels: Boolean,
    originalType: ClassName,
    fileName: String,
    superInterface: TypeName,
    scope: String,
    moduleTypes: Sequence<TypeName>
  ): Builder {
    return TypeSpec.classBuilder(fileName)
        .addKdoc(CLASS_HEADER)
        .addSuperinterface(superInterface)
        .addSuperinterface(BASE_COMPONENT)
        .applyIf(haveViewModels) { addSuperinterface(VIEW_MODEL_FACTORY) }
        .addProperties(
            listOf(
                propertyScope(scope),
                propertyOriginalType(superInterface),
                propertyParent(),
                propertyParentType(originalType),
                propertyChildren(),
                propertyModuleList(moduleTypes),
                propertyRuntimeDependencies()
            )
        )
        .applyIf(haveViewModels) { addFunction(viewModelFactoryCreateFunction()) }
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
        .mutable()
        .addModifiers(OVERRIDE)
        .initializer("null")
        .build()
  }

  private fun propertyParentType(generatingComponent: ClassName): PropertySpec {
    return PropertySpec.builder(PARENT_TYPE_NAME, KCLASS_OF_ANY.copy(nullable = true))
        .addModifiers(OVERRIDE)
        .apply {
          val parentType = parentTypeMap[generatingComponent]
          if (parentType != null) {
            initializer("%T::class", parentType)
          } else {
            initializer("null")
          }
        }
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
        .parameterizedBy(STRING, ANY)
        .copy(nullable = true)
    return PropertySpec.builder(RUNTIME_DEPS_NAME, propertyType, OVERRIDE)
        .mutable()
        .initializer("null")
        .build()
  }

  private fun injectFunction(method: ExecutableElement): FunSpec? {
    if (method.parameters.size != 1) {
      environment.error("$method: $INJECT_METHOD_NAME() methods must have a single parameter.")
      return null
    }
    val parameter = method.parameters.single()
    if (!method.returnType.isVoid()) {
      environment.error("$method: $INJECT_METHOD_NAME() methods must have no return value.")
      return null
    }

    val paramName = parameter.simpleName.toString()
    val paramElement = parameter.asType()
        .asTypeElement()
    val code = CodeBlock.builder()

    paramElement.enclosedElements
        .injectingFields(environment)
        .forEach {
          val result = it.setFieldInTarget(
              environment = environment,
              targetParamName = paramName,
              targetClassElement = paramElement,
              code = code
          )
          if (!result) return@injectFunction null
        }

    if (!maybeAddLifecycleObserver(paramElement, paramName, code)) {
      return null
    }

    return FunSpec.builder(method.simpleName.toString())
        .addAnnotation(SUPPRESS_UNCHECKED_CAST)
        .addModifiers(OVERRIDE)
        .addParameter(paramName, parameter.asType().asTypeName())
        .addCode(code.build())
        .build()
  }

  private fun viewModelFactoryCreateFunction(): FunSpec {
    val typeVariableT = TypeVariableName("T", VIEW_MODEL)
    val classOfT = Class::class.asTypeName()
        .parameterizedBy(typeVariableT)
    return FunSpec.builder(VIEW_MODEL_FACTORY_CREATE)
        .addModifiers(OVERRIDE)
        .addTypeVariable(typeVariableT)
        .addParameter("modelClass", classOfT)
        .returns(typeVariableT)
        .addCode("return $GET_NAME(modelClass.kotlin)\n")
        .build()
  }

  private fun maybeAddLifecycleObserver(
    paramElement: TypeElement,
    paramName: String,
    code: CodeBlock.Builder
  ): Boolean = paramElement.getAnnotationMirror<ScopeOwner>()?.let { scopeOwner ->
    if (!paramElement.hasInterface(environment, LIFECYCLE_OWNER)) {
      environment.error(
          "$paramElement: @ScopeOwner can only be used on classes which implement $LIFECYCLE_OWNER."
      )
      return false
    }

    val ownedScope = scopeOwner.name
    code.add(
        "\n" + """
            $paramName.lifecycle.addObserver(object : %T {
              @%T(%T)
              fun onDestroy() {
                %T(%S).exit()
                %T.log(%P)
              }
            })
            %T.log(%P)
            """.trimIndent() + "\n",
        LIFECYCLE_OBSERVER,
        ON_LIFECYCLE_EVENT, LIFECYCLE_EVENT_ON_DESTROY,
        GET_SCOPE_METHOD, ownedScope,
        LOGGER, "$$paramName destroyed scope $ownedScope",
        LOGGER, "$$paramName is now the owner of scope $ownedScope"
    )

    return true
  } ?: true

  private companion object {
    const val INJECT_METHOD_NAME = "inject"
    const val ORIGINAL_TYPE_NAME = "originalType"
    const val SCOPE_NAME = "scope"
  }
}
