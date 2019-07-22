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

import com.afollestad.ulfberht.scopes.RealScope
import com.afollestad.ulfberht.scopes.Scope

/**
 * Handles caching and creating scopes that are attached to components.
 *
 * @author Aidan Follestad (@afollestad)
 */
@PublishedApi
internal object Scopes {
  private val scopes = hashMapOf<String, Scope>()

  fun get(name: String): Scope? {
    if (name.isEmpty()) return null
    return scopes[name] ?: RealScope(name).also {
      scopes[name] = it
    }
  }

  fun resetForTests() {
    scopes.clear()
  }
}

/** Retrieves a [Scope] by [name].*/
fun getScope(name: String): Scope {
  require(name.isNotEmpty()) { "Invalid scope name: $name" }
  return Scopes.get(name)!!
}
