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
package com.afollestad.ulfberht.utilities

import com.afollestad.ulfberht.graph.BindingModel.Key
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ksp.toClassName

internal val KSValueParameter.nameString: String
  get() = name?.asString() ?: error("Parameter doesn't have name: $this")

internal val KSValueParameter.shortNameString: String
  get() = name?.getShortName() ?: error("Parameter doesn't have name: $this")

internal val KSValueParameter.bindingKey: Key
  get() = Key(
    type = type.resolve().toClassName(),
    qualifier = qualifier,
  )