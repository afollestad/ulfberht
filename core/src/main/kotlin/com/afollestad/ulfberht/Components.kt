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

import com.afollestad.ulfberht.common.BaseComponent
import com.afollestad.ulfberht.common.Logger
import com.afollestad.ulfberht.util.asKClass
import com.afollestad.ulfberht.util.getParentType
import com.afollestad.ulfberht.util.newInstance
import kotlin.reflect.KClass

/**
 * Manages caching and constructing components and their hierarchy.
 *
 * @author Aidan Follestad (@afollestad)
 */
@PublishedApi
internal object Components {
  val cache = mutableMapOf<String, BaseComponent>()
  val parentTypeCache = mutableMapOf<String, KClass<*>>()

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> get(type: KClass<T>): T {
    // First see if we can retrieve the component from the cache
    val key = type.qualifiedName!!
    if (cache.containsKey(key)) {
      Logger.log("Got from cache: $key")
      return cache[key] as T
    }

    // If we cannot, we need to create it. First, get its parent component, if there is one.
    val componentParent: BaseComponent? = getParent(type)

    // Now instantiate the component, filling in the parent and scope.
    val newComponent = "${key}_Component".asKClass()
        .newInstance<BaseComponent>(componentParent)
        .also { cache[key] = it }
    // Add the newly instantiated component to its parent's child list.
    componentParent?.children?.add(newComponent)

    // Get the scope, if any, and attach the component.
    val scope = Scopes.get(newComponent.scope)
    scope?.let { scope.addObserver(newComponent) }

    Logger.log("Instantiated fresh: $key, parent: ${parentTypeCache[key]}, scope: $scope")
    return newComponent as T
  }

  fun remove(type: KClass<*>) {
    val key = type.qualifiedName!!
    cache.remove(key)
  }

  fun resetForTests() {
    cache.clear()
    parentTypeCache.clear()
  }

  private fun getParent(type: KClass<*>): BaseComponent? {
    val key = type.qualifiedName!!
    return (parentTypeCache[key] ?: type.getParentType())
        ?.let { parentType ->
          parentTypeCache[key] = parentType
          get(parentType) as BaseComponent
        }
  }
}

inline fun <reified T : Any> component(): T {
  return Components.get(T::class)
}
