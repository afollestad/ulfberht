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
package com.afollestad.ulfberht.util

import com.squareup.kotlinpoet.TypeName
import javax.annotation.processing.ProcessingEnvironment
import com.afollestad.ulfberht.util.ProcessorUtil.error

/**
 * Helps keep track of the dependency graph for features like circular
 * dependency detection.
 *
 * @author Aidan Follestad (@afollestad)
 */
internal class DependencyGraph(
  private val environment: ProcessingEnvironment
) {
  private val graph = mutableMapOf<String, MutableSet<String>>()

  private fun put(
    owner: String,
    dependency: String
  ): Boolean {
    if (get(dependency).contains(owner)) {
      environment.error("Circular dependency detected between $dependency and $owner!")
      return false
    }
    get(owner).add(dependency)
    return true
  }

  fun put(
    owner: TypeName,
    dependency: TypeName
  ) = put(owner.toString(), dependency.toString())

  operator fun get(owner: String): MutableSet<String> {
    return graph[owner] ?: mutableSetOf<String>().also { graph[owner] = it }
  }

  operator fun get(owner: TypeName): MutableSet<String> = get(owner.toString())

  fun clear() {
    graph.clear()
  }
}
