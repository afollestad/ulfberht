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
@file:Suppress("ClassName", "unused")

package com.afollestad.ulfberht

import com.afollestad.ulfberht.annotation.Module
import com.afollestad.ulfberht.common.BaseComponent
import com.afollestad.ulfberht.common.BaseModule
import kotlin.reflect.KClass

abstract class TestBaseModule : BaseModule {
  var isDestroyed: Boolean = false
  override val cachedProviders: MutableMap<String, Provider<*>> = mutableMapOf()

  override fun <T : Any> getProvider(
    wantedType: KClass<*>,
    genericArgs: Set<KClass<*>>,
    qualifier: String?,
    calledBy: BaseComponent?
  ): Provider<T>? = null

  override fun <T : Any> populateSet(
    set: MutableSet<T>,
    setOfType: KClass<T>,
    genericArgsOfType: Set<KClass<*>>,
    qualifier: String?,
    calledBy: BaseComponent?
  ) = Unit

  override fun destroy() {
    isDestroyed = true
    super.destroy()
  }
}

@Module
class Module1_Module(override val component: BaseComponent) : TestBaseModule()

@Module
class Module2_Module(override val component: BaseComponent) : TestBaseModule()

@Module
class Module3_Module(override val component: BaseComponent) : TestBaseModule()
