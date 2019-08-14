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
import com.afollestad.ulfberht.common.BaseComponent
import com.afollestad.ulfberht.common.Logger
import com.afollestad.ulfberht.util.asKClass
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

  /** Retrieves a cached component or creates a new instance of it. */
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> get(type: KClass<T>): T {
    // First see if we can retrieve the component from the cache
    val key = type.qualifiedName!!
    if (cache.containsKey(key)) {
      Logger.log("Got from cache: $key")
      return cache[key] as T
    }

    // Validate the given [type] as a supported component.
    require(type.java.isInterface && type.annotations.any { it is Component }) {
      "$type is not a @Component annotated interface."
    }

    // Now instantiate the component, filling in the parent and scope.
    val newComponent = "${key}_Component".asKClass()
        .newInstance<BaseComponent>()
        .also { cache[key] = it }
    Logger.log("Instantiated fresh: $key")

    // Add the newly instantiated component to its parent's child list.
    getParent(newComponent)?.children?.add(newComponent)
    if (newComponent.parent != null) {
      Logger.log("Component $key has parent: ${newComponent.parent!!::class}")
    } else {
      Logger.log("Component $key has no parent.")
    }

    // Get the scope, if any, and attach the component.
    val scope = Scopes.get(newComponent.scope)
    scope?.let { scope.addObserver(newComponent) }
    Logger.log("Component $key scope: $scope")

    return newComponent as T
  }

  /** Removes a cached component by the given [type]. */
  fun remove(type: KClass<*>) {
    val key = type.qualifiedName!!
    cache.remove(key)
  }

  /** Visible for testing - clears static caches for unit tests. */
  fun resetForTests() = cache.clear()

  private fun getParent(forComponent: BaseComponent): BaseComponent? {
    val parentType = forComponent.parentType
    if (forComponent.parent == null && parentType != null) {
      forComponent.parent = get(parentType) as BaseComponent
    }
    return forComponent.parent
  }
}

/**
 * Retrieves a component, [T], which must be an interface annotated with @Component.
 * The component hierarchy is automatically built up, so the parent this component and
 * the parents of further parents will all be created if necessary. Components are automatically
 * cached, so no need to do that in your implementation.
 *
 * @author Aidan Follestad (@afollestad)
 */
inline fun <reified T : Any> component(
  vararg runtimeDependencies: Pair<String?, Any>
): T {
  return Components.get(T::class)
      .withRuntimeDependencies(runtimeDependencies.toMap())
}

@PublishedApi
internal inline fun <reified T : Any> Any.withRuntimeDependencies(
  runtimeDependencies: Map<String?, Any>
): T {
  return if (runtimeDependencies.isEmpty()) {
    this as T
  } else {
    (this as BaseComponent).apply {
      this.runtimeDependencies = runtimeDependencies.toMap()
    } as T
  }
}
