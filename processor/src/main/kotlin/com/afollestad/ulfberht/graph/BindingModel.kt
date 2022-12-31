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

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

/**
 * TODO
 *
 * @author Aidan Follestad (afollestad)
 */
internal sealed class BindingModel {
  abstract val scope: ClassName?
  abstract val factoryKey: Key?
  abstract val providedKey: Key
  abstract val parameters: Map<String, Key>
  abstract val isSingleton: Boolean
  abstract val containingFile: KSFile
  abstract val factoryParameterName: String

  /**
   * TODO
   */
  data class Key(
    val type: ClassName,
    val qualifier: ClassName? = null,
  )

  /**
   * TODO
   */
  data class ProviderBinding(
    val functionName: MemberName,
    override val scope: ClassName?,
    override val factoryKey: Key?,
    override val providedKey: Key,
    override val parameters: Map<String, Key>,
    override val isSingleton: Boolean,
    override val containingFile: KSFile,
    override val factoryParameterName: String,
  ) : BindingModel()

  /**
   * TODO
   */
  data class FactoryBinding(
    override val scope: ClassName?,
    override val factoryKey: Key,
    override val providedKey: Key,
    override val parameters: Map<String, Key>,
    override val isSingleton: Boolean,
    override val containingFile: KSFile,
    override val factoryParameterName: String,
  ) : BindingModel()

  /**
   * TODO
   */
  data class AssociationBinding(
    val implementationKey: Key,
    override val scope: ClassName?,
    override val factoryKey: Key,
    override val providedKey: Key,
    override val parameters: Map<String, Key>,
    override val isSingleton: Boolean,
    override val containingFile: KSFile,
    override val factoryParameterName: String,
  ) : BindingModel()
}
