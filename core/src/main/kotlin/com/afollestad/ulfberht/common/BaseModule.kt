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
@file:Suppress("UNCHECKED_CAST", "unused")

package com.afollestad.ulfberht.common

import com.afollestad.ulfberht.Provider
import kotlin.reflect.KClass

/**
 * The base class for all generated modules.
 *
 * @author Aidan Follestad (@afollestad)
 */
interface BaseModule {
  val component: BaseComponent
  val cachedProviders: MutableMap<String, Provider<*>>

  /** Retrieves a provided class via its [Provider]. */
  fun <T : Any> get(
    wantedType: KClass<*>,
    genericArgs: Set<KClass<*>> = emptySet(),
    qualifier: String? = null
  ): T {
    return getProvider<T>(
        wantedType = wantedType,
        genericArgs = genericArgs,
        qualifier = qualifier,
        calledBy = null
    )?.get() ?: missingProviderError(wantedType, genericArgs, qualifier)
  }

  /** Retrieves a [Provider] for a provided class. */
  fun <T : Any> getProvider(
    wantedType: KClass<*>,
    genericArgs: Set<KClass<*>> = emptySet(),
    qualifier: String? = null,
    calledBy: BaseComponent? = null
  ): Provider<T>?

  /** Destroys the module, releasing held singletons and other resources. */
  fun destroy() {
    cachedProviders.forEach { it.value.destroy() }
    cachedProviders.clear()
  }
}

private fun missingProviderError(
  wantedType: KClass<*>,
  genericArgs: Set<KClass<*>>,
  qualifier: String?
): Nothing = error(StringBuilder().apply {
  append("Didn't find provider for type ${wantedType.qualifiedName}")
  if (genericArgs.isNotEmpty()) {
    append(
        genericArgs.joinToString(
            separator = ", ", prefix = "<", postfix = ">"
        ) { it.qualifiedName!! })
  }
  if (qualifier != null) {
    append(" (qualifier=\"$qualifier\")")
  }
})
