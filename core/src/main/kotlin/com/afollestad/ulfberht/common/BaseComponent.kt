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

interface BaseComponent : ScopeObserver {
  val scope: String
  val originalType: KClass<*>
  val parent: BaseComponent?
  val children: MutableSet<BaseComponent>
  val modules: Set<BaseModule>

  fun <T : Any> get(
    wantedType: KClass<T>,
    qualifier: String? = null
  ): T {
    return getProvider(
        wantedType = wantedType,
        qualifier = qualifier,
        calledBy = null
    )?.get() ?: error("Didn't find provider for type ${wantedType.qualifiedName}")
  }

  fun <T : Any> getProvider(
    wantedType: KClass<T>,
    qualifier: String? = null,
    calledBy: BaseComponent? = null
  ): Provider<T>?

  fun destroy() {
    children.forEach { it.destroy() }
    modules.forEach { it.destroy() }
    Components.remove(originalType)
    Logger.log("Destroyed component ${originalType.qualifiedName}")
  }

  override fun onExit() = destroy()
}
