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
package com.afollestad.ulfberht.generators

import com.afollestad.ulfberht.graph.BindingModel
import com.afollestad.ulfberht.graph.BindingModel.AssociationBinding
import com.afollestad.ulfberht.graph.BindingModel.FactoryBinding
import com.afollestad.ulfberht.graph.BindingModel.ProviderBinding
import com.afollestad.ulfberht.graph.ComponentModel
import com.afollestad.ulfberht.graph.ComponentModel.ComponentMember
import com.afollestad.ulfberht.graph.ComponentModel.ComponentMember.Getter
import com.afollestad.ulfberht.graph.ComponentModel.ComponentMember.Injector
import com.afollestad.ulfberht.graph.DependencyGraph
import com.afollestad.ulfberht.graph.getterName
import com.afollestad.ulfberht.utilities.Names.CLASS_HEADER
import com.afollestad.ulfberht.utilities.Names.COMPONENT_NAME_SUFFIX
import com.afollestad.ulfberht.utilities.Types.COMPONENT_IMPL
import com.afollestad.ulfberht.utilities.Types.FACTORY
import com.afollestad.ulfberht.utilities.Types.PROVIDER
import com.afollestad.ulfberht.utilities.Types.PROVIDER_CREATOR
import com.afollestad.ulfberht.utilities.Types.PROVIDER_SINGLETON_CREATOR
import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.originatingKSFiles
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.withIndent

/**
 * TODO
 *
 * @author Aidan Follestad (afollestad)
 */
internal class ComponentGenerator(
  private val codeGenerator: CodeGenerator,
  private val dependencyGraph: DependencyGraph,
) {

  /**
   * TODO
   */
  @Throws(IllegalStateException::class)
  fun generate(models: Sequence<ComponentModel>) {
    models.forEach(::generate)
  }

  @Throws(IllegalStateException::class)
  private fun generate(model: ComponentModel) {
    val componentName = model.className
    val pkg = componentName.packageName
    val componentSuffixedName = componentName.simpleName + COMPONENT_NAME_SUFFIX

    val factoryBindings = model.members.asSequence()
      .flatMap {
        when (it) {
          is Getter -> listOf(it.providedKey)
          is Injector -> it.targetProperties.values
        }
      }
      .flatMap { setOf(it) + dependencyGraph.requireDependencies(it, transitive = true) }
      .distinct()
      .map(dependencyGraph::requireBinding)

    val factoryPropertySpecs = factoryBindings
      .map { it.toPropertySpec() }
      .toList()

    val destroyMethod = FunSpec.builder("destroy")
      .addModifiers(OVERRIDE)
      .addCode(
        CodeBlock.builder().apply {
          factoryBindings.forEach {
            addStatement("%L.destroy()", it.factoryParameterName)
          }
        }.build(),
      )
      .build()

    val typeSpec = TypeSpec.classBuilder(componentSuffixedName)
      .addOriginatingKSFile(model.containingFile)
      .addKdoc(CLASS_HEADER)
      .addSuperinterface(COMPONENT_IMPL)
      .addSuperinterface(model.className)
      .addProperties(factoryPropertySpecs)
      .addFunctions(model.members.map { it.toFunSpec() } + destroyMethod)
      .build()

    val fileSpec = FileSpec.builder(pkg, componentSuffixedName)
      .addType(typeSpec)
      .build()

    fileSpec.writeTo(
      codeGenerator = codeGenerator,
      aggregating = true,
      originatingKSFiles = fileSpec.originatingKSFiles(),
    )
  }

  private fun ComponentMember.toFunSpec(): FunSpec =
    when (this) {
      is Injector -> FunSpec.builder("inject")
        .addModifiers(OVERRIDE)
        .addParameter(targetParamName, targetType)
        .addCode(
          CodeBlock.builder().apply {
            targetProperties
              .mapValues { dependencyGraph.requireBinding(it.value) }
              .forEach { (name, binding) ->
                addStatement(
                  "%L.%L = %L.%L()",
                  targetParamName,
                  name,
                  binding.factoryParameterName,
                  binding.getterName,
                )
              }
          }.build(),
        )
        .build()

      is Getter -> FunSpec.builder(functionName)
        .addModifiers(OVERRIDE)
        .apply { providedKey.qualifier?.let(::addAnnotation) }
        .returns(providedKey.type)
        .addCode(
          CodeBlock.builder().apply {
            val binding = dependencyGraph.requireBinding(providedKey)
            addStatement(
              "return %L.%L()",
              binding.factoryParameterName,
              binding.getterName,
            )
          }.build(),
        )
        .build()
    }

  private fun BindingModel.toPropertySpec(): PropertySpec =
    when (this) {
      is AssociationBinding,
      is FactoryBinding,
      -> {
        toFactoryPropertySpec()
      }

      is ProviderBinding -> {
        toProviderPropertySpec()
      }
    }

  private fun BindingModel.toFactoryPropertySpec(): PropertySpec {
    val constructionCode = CodeBlock.builder().apply {
      addStatement("lazy {")
      withIndent {
        if (parameters.isEmpty()) {
          addStatement("%T()", factoryKey!!.type)
        } else {
          addStatement("%T(", factoryKey!!.type)
          withIndent {
            parameters
              .map { dependencyGraph.requireBinding(it.value) }
              .forEach { binding ->
                addStatement(
                  "%L = %L,",
                  binding.factoryParameterName,
                  binding.factoryParameterName,
                )
              }
          }
          addStatement(")")
        }
      }
      add("}")
    }.build()

    return PropertySpec
      .builder(
        name = factoryParameterName,
        type = FACTORY.parameterizedBy(providedKey.type),
        modifiers = listOf(PRIVATE),
      )
      .delegate(constructionCode)
      .build()
  }

  private fun ProviderBinding.toProviderPropertySpec(): PropertySpec {
    val providerCreator = when {
      isSingleton -> PROVIDER_SINGLETON_CREATOR
      else -> PROVIDER_CREATOR
    }

    val constructionCode = CodeBlock.builder().apply {
      addStatement("lazy {")
      withIndent {
        beginControlFlow("%M {", providerCreator)
        when {
          parameters.isNotEmpty() -> {
            if (factoryKey != null) {
              addStatement("%T.%N(", factoryKey.type, functionName)
            } else {
              addStatement("%M(", functionName)
            }
            parameters.mapValues { dependencyGraph.requireBinding(it.value) }
              .forEach { (name, binding) ->
                withIndent {
                  addStatement(
                    "%L = %L.%L(),",
                    name,
                    binding.factoryParameterName,
                    binding.getterName,
                  )
                }
              }
            addStatement(")")
          }

          factoryKey != null -> {
            addStatement("%T.%N()", factoryKey.type, functionName)
          }

          else -> {
            addStatement("%M()", functionName)
          }
        }
        endControlFlow()
      }
      add("}")
    }.build()

    return PropertySpec
      .builder(
        name = factoryParameterName,
        type = PROVIDER.parameterizedBy(providedKey.type),
        modifiers = listOf(PRIVATE),
      )
      .delegate(constructionCode)
      .build()
  }
}
