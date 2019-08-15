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

import com.afollestad.ulfberht.Provider

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

/** Creates a provider via [FactoryProvider]. */
fun <T : Any> factory(block: () -> T): Provider<T> = FactoryProvider(block)

/** Creates a provider via [SingletonProvider]. */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> BaseModule.singleton(noinline block: () -> T): Provider<T> {
  val key: String = T::class.qualifiedName!!
  if (cachedProviders.containsKey(key)) {
    return cachedProviders[key] as Provider<T>
  }
  return SingletonProvider(block).also { cachedProviders[key] = it }
}
