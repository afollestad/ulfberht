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
import com.afollestad.ulfberht.api.ComponentImpl
import kotlin.reflect.KClass

/**
 * TODO
 *
 * @author Aidan Follestad (@afollestad)
 */
inline fun <reified T : Any> component(): T {
  // TODO: runtime deps?
  return Components.get(T::class)
}

/**
 * TODO
 *
 * @author Aidan Follestad (@afollestad)
 */
@PublishedApi
internal object Components {

  private val cache = mutableMapOf<String, ComponentImpl>()

  /**
   * TODO
   */
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> get(type: KClass<T>): T {
    // First see if we can retrieve the component from the cache
    val key = type.qualifiedName!!
    if (cache.containsKey(key)) {
      return cache[key] as T
    }

    require(
      type.java.isInterface &&
        type.java.annotations.any { it.annotationClass == Component::class },
    ) {
      "$key is not a @Component annotated interface."
    }

    val newComponent = Class.forName("${key}_Component")
    return (newComponent.constructors.first().newInstance() as T)
      .also { cache[key] = it as ComponentImpl }
  }

  /**
   * TODO
   */
  fun destroy(type: KClass<*>) {
    val key = type.qualifiedName!!
    cache[key]?.destroy()
    cache.remove(key)
  }
}
