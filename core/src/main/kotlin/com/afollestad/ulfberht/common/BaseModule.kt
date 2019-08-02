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
    wantedType: KClass<T>,
    genericArgs: Set<KClass<*>> = emptySet(),
    qualifier: String? = null
  ): T {
    return getProvider(
        wantedType = wantedType,
        qualifier = qualifier,
        calledBy = null
    )?.get() ?: error(
        "Didn't find provider for type ${wantedType.qualifiedName} (qualifier=\"$qualifier)\""
    )
  }

  /** Retrieves a [Provider] for a provided class. */
  fun <T : Any> getProvider(
    wantedType: KClass<T>,
    genericArgs: Set<KClass<*>> = emptySet(),
    qualifier: String? = null,
    calledBy: BaseComponent? = null
  ): Provider<T>?

  /** Destroys the module, releasing held singletons and other resources. */
  fun destroy() {
    cachedProviders.forEach { it.value.destroy() }
    cachedProviders.clear()
  }

  /** A [Provider] that returns a new instance for every call to [get]. */
  class FactoryProvider<T>(private val creator: () -> T) : Provider<T> {
    override fun get(): T = creator()

    override fun destroy() = Unit
  }

  /** A [Provider] that always returns the same instance with every call to [get]. */
  class SingletonProvider<T>(private val creator: () -> T) : Provider<T> {
    private var value: T? = null

    override fun get(): T {
      return value ?: creator().also { value = it }
    }

    override fun destroy() {
      value = null
    }
  }
}
