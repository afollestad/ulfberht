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

import com.afollestad.ulfberht.utilities.Types.VIEW_MODEL
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toClassName

internal val KSType.isViewModel: Boolean
  get() = toClassName() == VIEW_MODEL || declaration.hasSuperType(VIEW_MODEL)

internal val KSType.isSet: Boolean
  get() = toClassName() == SET || declaration.hasSuperType(SET)

internal val KSType.isUnit: Boolean
  get() = toClassName() == UNIT
