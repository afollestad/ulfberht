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

import com.afollestad.ulfberht.util.Names.CLASS_HEADER
import com.afollestad.ulfberht.util.Names.IS_SUBCLASS_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.LIBRARY_PACKAGE
import com.afollestad.ulfberht.util.Types.KCLASS_OF_ANY
import com.afollestad.ulfberht.util.Types.TYPE_VARIABLE_T
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INTERNAL
import javax.annotation.processing.ProcessingEnvironment

/**
 * Generates extension methods that are used by generated components
 * and modules.
 *
 * @author Aidan Follestad (@afollestad)
 */
internal class ExtensionsBuilder(
  private val environment: ProcessingEnvironment
) {
  fun generate() {
    val fileSpec = FileSpec.builder(LIBRARY_PACKAGE, "_ProcessorExtensions")
        .addFunction(isSubClassFunction())
        .build()
    fileSpec.writeTo(environment.filer)
  }

  private fun isSubClassFunction(): FunSpec {
    return FunSpec.builder(IS_SUBCLASS_EXTENSION_NAME)
        .addKdoc(CLASS_HEADER)
        .receiver(KCLASS_OF_ANY)
        .addParameter("ofClass", KCLASS_OF_ANY)
        .addModifiers(INTERNAL)
        .addCode("return ofClass.java.isAssignableFrom(this.java)\n", TYPE_VARIABLE_T)
        .returns(BOOLEAN)
        .build()
  }
}
