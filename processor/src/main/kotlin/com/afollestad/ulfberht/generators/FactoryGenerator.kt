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
import com.afollestad.ulfberht.graph.BindingModel.Key
import com.afollestad.ulfberht.graph.BindingModel.ProviderBinding
import com.afollestad.ulfberht.graph.DependencyGraph
import com.afollestad.ulfberht.graph.toConstructorParameterSpec
import com.afollestad.ulfberht.graph.toConstructorPropertySpec
import com.afollestad.ulfberht.utilities.Names.CLASS_HEADER
import com.afollestad.ulfberht.utilities.Types.FACTORY
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
internal class FactoryGenerator(
  private val codeGenerator: CodeGenerator,
  private val dependencyGraph: DependencyGraph,
) {

  /**
   * TODO
   */
  @Throws(IllegalStateException::class)
  fun generate(bindings: Sequence<BindingModel>) {
    bindings.forEach(::generate)
  }

  @Throws(IllegalStateException::class)
  private fun generate(binding: BindingModel) {
    val implementationKey = when (binding) {
      is AssociationBinding -> binding.implementationKey
      else -> binding.providedKey
    }
    val providedKey = binding.providedKey
    val factoryKey = binding.factoryKey!!

    val typeSpec = TypeSpec.classBuilder(factoryKey.type).apply {
      addOriginatingKSFile(binding.containingFile)
      addKdoc(CLASS_HEADER)
      addSuperinterface(FACTORY.parameterizedBy(providedKey.type))

      val parameterAndPropertySpecs = dependencyGraph.requireDependencies(implementationKey)
        .asSequence()
        .map(dependencyGraph::requireBinding)
        .map { it.toConstructorParameterSpec() to it.toConstructorPropertySpec() }
        .toList()

      if (parameterAndPropertySpecs.isNotEmpty()) {
        primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameters(parameterAndPropertySpecs.map { it.first })
            .build(),
        )
        addProperties(parameterAndPropertySpecs.map { it.second })
      }

      if (binding.isSingleton) {
        addProperty(
          PropertySpec.builder("cached", providedKey.type.copy(nullable = true), PRIVATE)
            .mutable(true)
            .initializer("null")
            .build(),
        )
      }

      addFunctions(
        listOfNotNull(
          binding.toCreateFunSpec(),
          binding.toDestroyFunSpec(),
          if (binding.isSingleton) binding.toCreateNewFunSpec() else null,
        ),
      )
    }.build()

    val fileSpec = FileSpec
      .builder(
        packageName = factoryKey.type.packageName,
        fileName = factoryKey.type.simpleName,
      )
      .addType(typeSpec)
      .build()

    fileSpec.writeTo(
      codeGenerator = codeGenerator,
      aggregating = true,
      originatingKSFiles = fileSpec.originatingKSFiles(),
    )
  }

  @Throws(IllegalStateException::class)
  private fun CodeBlock.Builder.construct(
    constructedKey: Key,
    parameterKeys: Map<String, Key>,
    dependencyGraph: DependencyGraph,
  ) = apply {
    if (parameterKeys.isEmpty()) {
      add("%T()", constructedKey.type)
    } else {
      add("%T(\n", constructedKey.type)
      parameterKeys.forEach { (name, key) ->
        withIndent {
          assignParameterFromBinding(
            parameterName = name,
            provided = dependencyGraph.requireBinding(key),
          )
        }
      }
      add(")")
    }
  }

  private fun CodeBlock.Builder.assignParameterFromBinding(
    parameterName: String,
    provided: BindingModel,
    trailingComma: Boolean = true,
  ) = apply {
    val getterName = when (provided) {
      is FactoryBinding, is AssociationBinding -> "create"
      is ProviderBinding -> "get"
    }
    addStatement(
      "%L = %L.%L()%L",
      parameterName,
      provided.factoryParameterName,
      getterName,
      if (trailingComma) "," else "",
    )
  }

  private fun BindingModel.toCreateFunSpec(): FunSpec =
    FunSpec.builder("create")
      .addModifiers(OVERRIDE)
      .apply { implementationKey.qualifier?.let(::addAnnotation) }
      .returns(providedKey.type)
      .addCode(
        if (isSingleton) {
          CodeBlock.of("return cached ?: createNew().also { cached = it }")
        } else {
          toConstructionCode()
        },
      )
      .build()

  private fun BindingModel.toCreateNewFunSpec(): FunSpec =
    FunSpec.builder("createNew")
      .addModifiers(PRIVATE)
      .returns(providedKey.type)
      .addCode(toConstructionCode())
      .build()

  private fun BindingModel.toDestroyFunSpec(): FunSpec =
    FunSpec.builder("destroy")
      .addModifiers(OVERRIDE)
      .addCode(
        if (isSingleton) {
          CodeBlock.of("cached = null")
        } else {
          CodeBlock.of("return Unit")
        },
      )
      .build()

  private val BindingModel.implementationKey: Key
    get() = when (this) {
      is AssociationBinding -> implementationKey
      else -> providedKey
    }

  private fun BindingModel.toConstructionCode(): CodeBlock =
    CodeBlock.builder().apply {
      add("return ")
      construct(
        constructedKey = implementationKey,
        parameterKeys = parameters,
        dependencyGraph = dependencyGraph,
      )
    }.build()
}
