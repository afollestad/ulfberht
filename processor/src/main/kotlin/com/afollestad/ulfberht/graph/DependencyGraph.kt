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
package com.afollestad.ulfberht.graph

import com.afollestad.ulfberht.graph.BindingModel.AssociationBinding
import com.afollestad.ulfberht.graph.BindingModel.Key

/**
 * TODO
 *
 * @author Aidan Follestad (afollestad)
 */
internal class DependencyGraph {
  private val associations = mutableMapOf<Key, Key>()
  private val graph = mutableMapOf<Key, MutableSet<Key>>()
  private var bindings = emptyMap<Key, BindingModel>()

  /**
   * TODO
   */
  @Throws(IllegalStateException::class)
  fun buildGraph(bindings: Sequence<BindingModel>) = apply {
    bindings.forEach { binding ->
      if (binding is AssociationBinding) {
        associations[binding.providedKey] = binding.implementationKey
      }
      addDependencies(
        type = when (binding) {
          is AssociationBinding -> binding.implementationKey
          else -> binding.providedKey
        },
        dependsOn = binding.parameters.values,
      )
    }

    bindings
      .distinct()
      .groupBy { it.providedKey }
      .filter { it.value.size > 1 }
      .forEach { (key, bindings) -> error("Duplicate bindings for $key: ${bindings.joinToString()}") }

    this.bindings = bindings.associateBy { it.providedKey }
  }

  /**
   * TODO
   */
  @Throws(IllegalStateException::class)
  fun requireBinding(key: Key): BindingModel =
    bindings[key]
      ?: associations[key]?.let(bindings::get)
      ?: noMatchingBindingFor(key)

  /**
   * TODO
   */
  fun requireDependencies(
    key: Key,
    transitive: Boolean = false,
  ): Set<Key> {
    val dependencies = graph[key].orEmpty() +
      associations[key]?.let { graph[it] }.orEmpty()

    return if (transitive) {
      dependencies + dependencies.flatMap {
        recursiveDependenciesFor(it) +
          recursiveDependenciesFor(it)
      }
    } else {
      dependencies
    }
  }

  /**
   * TODO
   */
  fun clear() {
    graph.clear()
  }

  private fun addDependencies(
    type: Key,
    dependsOn: Collection<Key>,
  ) {
    dependenciesFor(type).addAll(dependsOn)
  }

  private fun dependenciesFor(type: Key): MutableSet<Key> {
    val dependencies = graph[type] ?: mutableSetOf<Key>().also { graph[type] = it }
    associations[type]?.let { associationKey ->
      dependencies.addAll(dependenciesFor(associationKey))
    }
    return dependencies
  }

  private fun recursiveDependenciesFor(type: Key): List<Key> =
    dependenciesFor(type).flatMap { key ->
      setOf(key) + recursiveDependenciesFor(key)
    }

  private fun noMatchingBindingFor(key: Key): Nothing {
    val possibleCandidates = bindings.keys
      .filter {
        it.type == key.type ||
          it.type == associations[key]?.type
      }
    error(
      buildString {
        append("No matching binding for $key.")
        if (possibleCandidates.isNotEmpty()) {
          append(" Possible candidates: $possibleCandidates")
        }
      },
    )
  }
}
