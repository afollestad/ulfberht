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
import com.afollestad.ulfberht.util.ProcessorUtil.filterClassesAndInterfaces
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@SupportedSourceVersion(SourceVersion.RELEASE_8)
class UlfberhtProcessor : AbstractProcessor() {
  private val componentBuilder: ComponentBuilder by lazy {
    ComponentBuilder(environment = processingEnv)
  }
  private val moduleBuilder: ModuleBuilder by lazy {
    ModuleBuilder(environment = processingEnv)
  }
  private val extensionsBuilder: ExtensionsBuilder by lazy {
    ExtensionsBuilder(environment = processingEnv)
  }

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
      return false
    }
    extensionsBuilder.generate()
    roundEnv.getElementsAnnotatedWith(Component::class.java)
        .filterClassesAndInterfaces()
        .forEach(componentBuilder::generate)
    roundEnv.getElementsAnnotatedWith(Module::class.java)
        .filterClassesAndInterfaces()
        .forEach(moduleBuilder::generate)
    return true
  }
}
