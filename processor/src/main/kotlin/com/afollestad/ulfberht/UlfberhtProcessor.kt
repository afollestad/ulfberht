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
package com.afollestad.ulfberht

import com.afollestad.ulfberht.annotation.BindsTo
import com.afollestad.ulfberht.annotation.Component
import com.afollestad.ulfberht.annotation.Inject
import com.afollestad.ulfberht.annotation.Provides
import com.afollestad.ulfberht.generators.ComponentGenerator
import com.afollestad.ulfberht.generators.FactoryGenerator
import com.afollestad.ulfberht.graph.BindingModel.ProviderBinding
import com.afollestad.ulfberht.graph.DependencyGraph
import com.afollestad.ulfberht.graph.toAssociationBinding
import com.afollestad.ulfberht.graph.toComponentModel
import com.afollestad.ulfberht.graph.toFactoryBindingModel
import com.afollestad.ulfberht.graph.toProviderBindingModel
import com.afollestad.ulfberht.utilities.getSymbolsWithAnnotation
import com.afollestad.ulfberht.utilities.isAnnotationPresent
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind.CLASS
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion

/**
 * Ulfberht's annotation processor. Registered by [UlfberhtProcessorProvider].
 *
 * @author Aidan Follestad (@afollestad)
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class UlfberhtProcessor(
  private val logger: KSPLogger,
  private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

  private val dependencyGraph: DependencyGraph by lazy {
    DependencyGraph()
  }
  private val factoryGenerator: FactoryGenerator by lazy {
    FactoryGenerator(
      codeGenerator = codeGenerator,
      dependencyGraph = dependencyGraph,
    )
  }
  private val componentGenerator: ComponentGenerator by lazy {
    ComponentGenerator(
      codeGenerator = codeGenerator,
      dependencyGraph = dependencyGraph,
    )
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    try {
      val bindings = resolver.getSymbolsWithAnnotation<BindsTo>()
        .filterIsInstance<KSClassDeclaration>()
        .onEach {
          check(it.classKind == CLASS) { "@BindsTo is only allowed on classes." }
        }
        .map { it.toAssociationBinding() }

      val factories = resolver.getSymbolsWithAnnotation<Inject>()
        .filterIsInstance<KSFunctionDeclaration>()
        .filter { it.isConstructor() }
        .filterNot {
          // Avoid redundant factory generation. BindsTo takes priority above; AssociationBinding handling
          // will prefer any constructors annotated with @Inject.
          (it.parentDeclaration as? KSClassDeclaration)
            ?.isAnnotationPresent<BindsTo>() == true
        }
        .map { it.toFactoryBindingModel() }

      val providers = resolver.getSymbolsWithAnnotation<Provides>()
        .filterIsInstance<KSFunctionDeclaration>()
        .map { it.toProviderBindingModel() }

      val allBindings = bindings + factories + providers

      dependencyGraph.buildGraph(
        bindings = allBindings,
      )

      factoryGenerator.generate(
        bindings = allBindings.filterNot { it is ProviderBinding },
      )

      componentGenerator.generate(
        models = resolver.getSymbolsWithAnnotation<Component>()
          .filterIsInstance<KSClassDeclaration>()
          .map { it.toComponentModel() },
      )
    } catch (e: IllegalStateException) {
      e.message?.let(logger::error)
        ?: logger.exception(e)
    }

    return emptyList()
  }

  override fun finish() = cleanup()

  override fun onError() = cleanup()

  private fun cleanup() {
    dependencyGraph.clear()
  }
}
