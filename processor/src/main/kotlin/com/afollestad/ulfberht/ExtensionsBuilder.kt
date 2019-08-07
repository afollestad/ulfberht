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

import com.afollestad.ulfberht.util.Annotations.SUPPRESS_UNCHECKED_CAST
import com.afollestad.ulfberht.util.Names.CACHED_PROVIDERS_NAME
import com.afollestad.ulfberht.util.Names.CLASS_HEADER
import com.afollestad.ulfberht.util.Names.IS_SUBCLASS_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.LIBRARY_PACKAGE
import com.afollestad.ulfberht.util.Names.FACTORY_EXTENSION_NAME
import com.afollestad.ulfberht.util.Names.SINGLETON_PROVIDER_EXTENSION_NAME
import com.afollestad.ulfberht.util.Types.BASE_MODULE
import com.afollestad.ulfberht.util.Types.KCLASS_OF_ANY
import com.afollestad.ulfberht.util.Types.PROVIDER_OF_T
import com.afollestad.ulfberht.util.Types.REIFIED_TYPE_VARIABLE_T
import com.afollestad.ulfberht.util.Types.SINGLETON_PROVIDER
import com.afollestad.ulfberht.util.Types.TYPE_VARIABLE_T
import com.afollestad.ulfberht.util.Types.UNSCOPED_PROVIDER
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INLINE
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.NOINLINE
import com.squareup.kotlinpoet.LambdaTypeName
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
        .addFunction(factoryProviderFunction())
        .addFunction(singletonProviderFunction())
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

  private fun factoryProviderFunction(): FunSpec {
    val parameterType = LambdaTypeName.get(returnType = TYPE_VARIABLE_T)
    return FunSpec.builder(FACTORY_EXTENSION_NAME)
        .addKdoc(CLASS_HEADER)
        .addModifiers(INTERNAL)
        .addTypeVariable(TYPE_VARIABLE_T)
        .addParameter("block", parameterType)
        .returns(PROVIDER_OF_T)
        .addCode("return %T(block)\n", UNSCOPED_PROVIDER)
        .build()
  }

  private fun singletonProviderFunction(): FunSpec {
    val parameterType = LambdaTypeName.get(returnType = REIFIED_TYPE_VARIABLE_T)
    return FunSpec.builder(SINGLETON_PROVIDER_EXTENSION_NAME)
        .addKdoc(CLASS_HEADER)
        .receiver(BASE_MODULE)
        .addAnnotation(SUPPRESS_UNCHECKED_CAST)
        .addModifiers(INLINE, INTERNAL)
        .addTypeVariable(REIFIED_TYPE_VARIABLE_T)
        .addParameter("block", parameterType, NOINLINE)
        .returns(PROVIDER_OF_T)
        .addCode(
            CodeBlock.of(
                """
                val key: String = %T::class.qualifiedName!!
                if ($CACHED_PROVIDERS_NAME.containsKey(key)) {
                  return $CACHED_PROVIDERS_NAME[key] as %T
                }
                return %T(block).also { $CACHED_PROVIDERS_NAME[key] = it }
                """.trimIndent() + "\n",
                REIFIED_TYPE_VARIABLE_T,
                PROVIDER_OF_T,
                SINGLETON_PROVIDER
            )
        )
        .build()
  }
}
