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

import com.afollestad.ulfberht.annotation.Component
import com.afollestad.ulfberht.common.BaseComponent
import com.afollestad.ulfberht.common.BaseModule
import kotlin.reflect.KClass

const val SCOPE_CHILD_1 = "scope_child1"

interface TestBaseComponent : BaseComponent {
  var isDestroyed: Boolean

  override fun destroy() {
    isDestroyed = true
    super.destroy()
  }
}

@Component(modules = [Module1_Module::class])
interface ComponentParent

class ComponentParent_Component(
  override val parent: BaseComponent? = null
) : ComponentParent, TestBaseComponent {
  override var isDestroyed: Boolean = false
  override val scope: String = ""
  override val originalType: KClass<*> = ComponentParent::class
  override val children: MutableSet<BaseComponent> = mutableSetOf()
  override val modules: Set<BaseModule> = setOf(Module1_Module(this))
  override var runtimeDependencies: Map<String?, Any>? = null

  override fun <T : Any> getProvider(
    wantedType: KClass<T>,
    genericArgs: Set<KClass<*>>,
    qualifier: String?,
    calledBy: BaseComponent?
  ): Provider<T>? = error("Not implemented")
}

@Component(
    scope = SCOPE_CHILD_1,
    parent = ComponentParent::class,
    modules = [Module2_Module::class]
)
interface ComponentChild1

class ComponentChild1_Component(
  override val parent: BaseComponent? = null
) : ComponentChild1, TestBaseComponent {
  override var isDestroyed: Boolean = false
  override val scope: String = SCOPE_CHILD_1
  override val originalType: KClass<*> = ComponentParent::class
  override val children: MutableSet<BaseComponent> = mutableSetOf()
  override val modules: Set<BaseModule> = setOf(Module2_Module(this))
  override var runtimeDependencies: Map<String?, Any>? = null

  override fun <T : Any> getProvider(
    wantedType: KClass<T>,
    genericArgs: Set<KClass<*>>,
    qualifier: String?,
    calledBy: BaseComponent?
  ): Provider<T>? = error("Not implemented")
}

@Component(
    parent = ComponentChild1::class,
    modules = [Module3_Module::class]
)
interface ComponentChild2

class ComponentChild2_Component(
  override val parent: BaseComponent? = null
) : ComponentChild2, TestBaseComponent {
  override var isDestroyed: Boolean = false
  override val scope: String = ""
  override val originalType: KClass<*> = ComponentParent::class
  override val children: MutableSet<BaseComponent> = mutableSetOf()
  override val modules: Set<BaseModule> = setOf(Module3_Module(this))
  override var runtimeDependencies: Map<String?, Any>? = null

  override fun <T : Any> getProvider(
    wantedType: KClass<T>,
    genericArgs: Set<KClass<*>>,
    qualifier: String?,
    calledBy: BaseComponent?
  ): Provider<T>? = error("Not implemented")
}
