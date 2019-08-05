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

import com.afollestad.ulfberht.util.ProcessorUtil.error
import javax.annotation.processing.ProcessingEnvironment

/**
 * Helps keep track of the dependency graph for features like circular
 * dependency detection.
 *
 * @author Aidan Follestad (@afollestad)
 */
internal class DependencyGraph(
  private val environment: ProcessingEnvironment
) {
  private val bindings = mutableMapOf<TypeAndArgs, TypeAndArgs>()
  private val graph = mutableMapOf<TypeAndArgs, MutableSet<TypeAndArgs>>()

  fun bind(
    concrete: TypeAndArgs,
    to: TypeAndArgs
  ) {
    bindings[concrete] = to
  }

  fun put(
    owner: TypeAndArgs,
    dependency: TypeAndArgs
  ): Boolean {
    if (get(dependency).contains(owner)) {
      environment.error(
          "Circular dependency detected between $dependency and $owner!"
      )
      return false
    }
    get(owner).add(dependency)

    bindings[dependency]?.let { depBinding ->
      put(owner, depBinding)
    }

    return true
  }

  operator fun get(owner: TypeAndArgs): MutableSet<TypeAndArgs> {
    return graph[owner] ?: mutableSetOf<TypeAndArgs>().also { graph[owner] = it }
  }

  fun clear() {
    bindings.clear()
    graph.clear()
  }
}
