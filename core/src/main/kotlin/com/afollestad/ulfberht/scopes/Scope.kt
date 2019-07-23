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
package com.afollestad.ulfberht.scopes

import com.afollestad.ulfberht.annotation.Component
import com.afollestad.ulfberht.common.Logger

/**
 * A [Scope] groups a set of components so that they can be destroyed
 * at will.
 *
 * @author Aidan Follestad (@afollestad)
 */
interface Scope {
  /** The name of the scope, taken from a [Component] interface. */
  val name: String

  /** Adds a [ScopeObserver] that is notified when the scope exists.  */
  fun addObserver(observer: ScopeObserver)

  /** Removes an added [ScopeObserver]. */
  fun removeObserver(observer: ScopeObserver)

  /** Exits the scope, notifying attached [ScopeObserver]'s. */
  fun exit()
}

/** A ScopeObserver receives exit notifications of a [Scope]. */
interface ScopeObserver {
  /** Invoked when the scope is exited. */
  fun onExit()
}

/** @author Aidan Follestad (@afollestad) */
internal class RealScope(
  override val name: String
) : Scope {
  private val observers = mutableSetOf<ScopeObserver>()

  override fun addObserver(observer: ScopeObserver) {
    observers.add(observer)
  }

  override fun removeObserver(observer: ScopeObserver) {
    observers.remove(observer)
  }

  override fun exit() {
    Logger.log("Exiting scope: $this")
    observers.forEach {
      it.onExit()
      removeObserver(it)
    }
  }

  override fun toString(): String = "${name}_${hashCode()}"
}
