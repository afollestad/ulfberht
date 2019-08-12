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

import com.afollestad.ulfberht.annotation.Component
import com.afollestad.ulfberht.annotation.Module
import com.afollestad.ulfberht.components.ComponentBuilder
import com.afollestad.ulfberht.modules.ModuleBuilder
import com.afollestad.ulfberht.util.DependencyGraph
import com.afollestad.ulfberht.util.Names.MODULE_NAME_SUFFIX
import com.afollestad.ulfberht.util.ProcessorUtil.filterClassesAndInterfaces
import com.afollestad.ulfberht.util.ProcessorUtil.getAnnotationMirror
import com.afollestad.ulfberht.util.ProcessorUtil.getModulesTypes
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/**
 * Ulfberht's annotation processor, registered in /resources/META-INF.services.
 *
 * @author Aidan Follestad (@afollestad)
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class UlfberhtProcessor : AbstractProcessor() {
  private val dependencyGraph: DependencyGraph by lazy {
    DependencyGraph(environment = processingEnv)
  }
  private val componentBuilder: ComponentBuilder by lazy {
    ComponentBuilder(environment = processingEnv)
  }
  private val moduleBuilder: ModuleBuilder by lazy {
    ModuleBuilder(environment = processingEnv, dependencyGraph = dependencyGraph)
  }
  private val extensionsBuilder: ExtensionsBuilder by lazy {
    ExtensionsBuilder(environment = processingEnv)
  }
  private val modulesWithViewModels: MutableList<String> = mutableListOf()

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(
        Component::class.java.canonicalName,
        Module::class.java.canonicalName
    )
  }

  override fun process(
    annotations: MutableSet<out TypeElement>,
    roundEnv: RoundEnvironment
  ): Boolean {
    if (annotations.isEmpty()) {
      dependencyGraph.clear()
      modulesWithViewModels.clear()
      return false
    }

    roundEnv.getElementsAnnotatedWith(Module::class.java)
        .filterClassesAndInterfaces()
        .forEach {
          moduleBuilder.generate(it)
          if (moduleBuilder.haveViewModels) {
            modulesWithViewModels.add(it.toString())
          }
        }
    roundEnv.getElementsAnnotatedWith(Component::class.java)
        .filterClassesAndInterfaces()
        .forEach {
          val modulesInComponent =
            it.getAnnotationMirror<Component>()!!.getModulesTypes(processingEnv)
          val haveViewModels = modulesInComponent.any { moduleInComponent ->
            modulesWithViewModels.any { moduleWithViewModel ->
              "$moduleInComponent" == "$moduleWithViewModel$MODULE_NAME_SUFFIX"
            }
          }
          componentBuilder.generate(it, haveViewModels)
        }

    extensionsBuilder.generate()
    return true
  }
}
