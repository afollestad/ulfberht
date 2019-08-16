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
@file:Suppress("unused")

package com.afollestad.ulfberht.common

import com.afollestad.ulfberht.Components
import com.afollestad.ulfberht.Provider
import com.afollestad.ulfberht.scopes.ScopeObserver
import kotlin.reflect.KClass

/**
 * The base class for all generated components.
 *
 * @author Aidan Follestad (@afollestad)
 */
interface BaseComponent : ScopeObserver {
  val scope: String
  val originalType: KClass<*>
  var parent: BaseComponent?
  val parentType: KClass<*>?
  val children: MutableSet<BaseComponent>
  val modules: Set<BaseModule>
  var runtimeDependencies: Map<String, Any>?

  @Suppress("UNCHECKED_CAST")
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
    )?.get() ?: error(
        "Didn't find provider for type ${wantedType.qualifiedName} (qualifier=\"$qualifier\")"
    )
  }

  fun <T : Any> getProvider(
    wantedType: KClass<*>,
    genericArgs: Set<KClass<*>> = emptySet(),
    qualifier: String? = null,
    calledBy: BaseComponent? = null
  ): Provider<T>? {
    if (calledBy === this) return null
    modules.forEach { module ->
      module.getProvider<T>(
          wantedType = wantedType,
          genericArgs = genericArgs,
          qualifier = qualifier,
          calledBy = calledBy ?: this
      )
          ?.let { return it }
    }

    if (parent != null && calledBy === parent) return null
    val runtimeProvider = getRuntimeDependency<T>(qualifier)
        ?.run { factory { this } }
    return runtimeProvider ?: parent?.getProvider(
        wantedType = wantedType,
        genericArgs = genericArgs,
        qualifier = qualifier,
        calledBy = calledBy ?: this
    )
  }

  fun <T : Any> getSetProvider(
    setOfType: KClass<T>,
    genericArgsOfType: Set<KClass<*>> = emptySet(),
    qualifier: String? = null
  ): Provider<Set<T>> = factory {
    getSet(setOfType, genericArgsOfType, qualifier)
  }

  fun <T : Any> getSet(
    setOfType: KClass<T>,
    genericArgsOfType: Set<KClass<*>> = emptySet(),
    qualifier: String? = null
  ): Set<T> {
    val newSet = mutableSetOf<T>()
    populateSet(
        set = newSet,
        setOfType = setOfType,
        genericArgsOfType = genericArgsOfType,
        qualifier = qualifier
    )
    return newSet
  }

  fun <T : Any> populateSet(
    set: MutableSet<T>,
    setOfType: KClass<T>,
    genericArgsOfType: Set<KClass<*>> = emptySet(),
    qualifier: String? = null,
    calledBy: BaseComponent? = null
  ) {
    if (calledBy === this) return
    modules.forEach { module ->
      module.populateSet(
          set = set,
          setOfType = setOfType,
          genericArgsOfType = genericArgsOfType,
          qualifier = qualifier,
          calledBy = calledBy ?: this
      )
    }

    if (parent != null && calledBy === parent) return
    parent?.populateSet(
        set = set,
        setOfType = setOfType,
        genericArgsOfType = genericArgsOfType,
        qualifier = qualifier,
        calledBy = calledBy ?: this
    )
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> getRuntimeDependency(qualifier: String?): T? {
    return runtimeDependencies?.get(qualifier) as? T
  }

  fun destroy() {
    children.forEach { it.destroy() }
    modules.forEach { it.destroy() }
    runtimeDependencies = null
    Components.remove(originalType)
    Logger.log("Destroyed component ${originalType.qualifiedName}")
  }

  override fun onExit() = destroy()
}
