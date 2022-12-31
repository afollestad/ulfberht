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

import com.afollestad.ulfberht.graph.BindingModel.Key
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName

/**
 * TODO
 *
 * @author Aidan Follestad (afollestad)
 */
internal data class ComponentModel(
  val scope: ClassName?,
  val className: ClassName,
  val members: List<ComponentMember>,
  val containingFile: KSFile,
) {

  sealed class ComponentMember {
    abstract val providedKey: Key

    /**
     * TODO
     */
    data class Injector(
      override val providedKey: Key,
      val targetType: ClassName,
      val targetParamName: String,
      val targetProperties: Map<String, Key>,
    ) : ComponentMember()

    /**
     * TODO
     */
    data class Getter(
      override val providedKey: Key,
      val functionName: String,
      val qualifier: ClassName?,
    ) : ComponentMember()
  }
}
