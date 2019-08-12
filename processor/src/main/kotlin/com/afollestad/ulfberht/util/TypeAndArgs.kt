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

import com.afollestad.ulfberht.util.Names.GET_NAME
import com.afollestad.ulfberht.util.Names.GET_PROVIDER_NAME
import com.squareup.kotlinpoet.TypeName

/** @author Aidan Follestad (@afollestad) */
internal data class TypeAndArgs(
  val fullType: TypeName,
  val erasedType: TypeName,
  val genericArgs: Array<TypeName>,
  val qualifier: String?,
  val isProvider: Boolean,
  val isViewModel: Boolean
) {
  val hasGenericArgs: Boolean = genericArgs.isNotEmpty()
  val getterName: String = if (isProvider) {
    GET_PROVIDER_NAME
  } else {
    GET_NAME
  }

  override fun toString(): String = if (qualifier != null) {
    "@\"$qualifier\" $fullType"
  } else {
    fullType.toString()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as TypeAndArgs
    if (fullType != other.fullType) return false
    if (erasedType != other.erasedType) return false
    if (!genericArgs.contentEquals(other.genericArgs)) return false
    if (qualifier != other.qualifier) return false
    if (isProvider != other.isProvider) return false
    if (isViewModel != other.isViewModel) return false
    return true
  }

  override fun hashCode(): Int {
    var result = fullType.hashCode()
    result = 31 * result + erasedType.hashCode()
    result = 31 * result + (qualifier?.hashCode() ?: 0)
    result = 31 * result + isProvider.hashCode()
    result = 31 * result + isViewModel.hashCode()
    return result
  }
}
