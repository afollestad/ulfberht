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
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class ComponentsTest {

  @Test fun get() {
    assertThat(Components.cache).isEmpty()

    val component1 = component<ComponentParent>() as BaseComponent
    assertThat(component1.modules.single())
        .isInstanceOf<Module1_Module>()

    val cacheEntry = Components.cache.entries.single()
    assertThat(cacheEntry.key).isEqualTo(ComponentParent::class.qualifiedName)
    assertThat(cacheEntry.value).isInstanceOf<ComponentParent>()

    val component2 = component<ComponentParent>() as BaseComponent
    assertThat(component2).isSameInstanceAs(component1)
    assertThat(component2.parent).isNull()
    assertThat(component2.children).isEmpty()
    assertThat(component2.modules.single())
        .isInstanceOf<Module1_Module>()
  }

  @Test fun `get also gets parent chain`() {
    val component = component<ComponentChild2>() as BaseComponent
    assertThat(component.modules.single())
        .isInstanceOf<Module3_Module>()

    val parent = component.parent
    assertThat(parent).isInstanceOf<ComponentChild1>()
    assertThat(parent!!.modules.single())
        .isInstanceOf<Module2_Module>()

    val parentOfParent = parent.parent
    assertThat(parentOfParent).isInstanceOf<ComponentParent>()
    assertThat(parentOfParent!!.modules.single())
        .isInstanceOf<Module1_Module>()
  }

  @Test fun remove() {
    component<ComponentParent>()
    assertThat(Components.cache).isNotEmpty()

    Components.remove(ComponentParent::class)
    assertThat(Components.cache).isEmpty()
  }

  @Test fun `exit scope destroys children`() {
    val componentChild2 = component<ComponentChild2>() as TestBaseComponent
    val module3 = componentChild2.modules.single() as TestBaseModule
    val componentChild1 = component<ComponentChild1>() as TestBaseComponent
    val module2 = componentChild1.modules.single() as TestBaseModule
    val componentParent = component<ComponentParent>() as TestBaseComponent
    val module1 = componentParent.modules.single() as TestBaseModule

    val scope = getScope(SCOPE_CHILD_1)
    assertThat(componentChild2.isDestroyed).isFalse()
    assertThat(module3.isDestroyed).isFalse()
    assertThat(componentChild1.isDestroyed).isFalse()
    assertThat(module2.isDestroyed).isFalse()
    assertThat(componentParent.isDestroyed).isFalse()
    assertThat(module1.isDestroyed).isFalse()

    scope.exit()
    // Expected to destroy both children 1 and 2...
    assertThat(componentChild2.isDestroyed).isTrue()
    assertThat(module3.isDestroyed).isTrue()
    assertThat(componentChild1.isDestroyed).isTrue()
    assertThat(module2.isDestroyed).isTrue()
    // ..but not the main parent
    assertThat(componentParent.isDestroyed).isFalse()
    assertThat(module1.isDestroyed).isFalse()
  }

  @After fun tearDown() {
    Components.resetForTests()
    Scopes.resetForTests()
  }
}
