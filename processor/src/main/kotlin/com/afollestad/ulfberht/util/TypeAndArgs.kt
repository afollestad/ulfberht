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

import com.afollestad.ulfberht.Provider
import com.afollestad.ulfberht.util.Names.GET_NAME
import com.afollestad.ulfberht.util.Names.GET_PROVIDER_NAME
import com.afollestad.ulfberht.util.Names.GET_SET_NAME
import com.afollestad.ulfberht.util.Names.GET_SET_PROVIDER_NAME
import com.afollestad.ulfberht.util.ProcessorUtil.correctTypeName
import com.afollestad.ulfberht.util.ProcessorUtil.getQualifier
import com.afollestad.ulfberht.util.ProcessorUtil.isNullable
import com.afollestad.ulfberht.util.ProcessorUtil.isViewModel
import com.afollestad.ulfberht.util.ProcessorUtil.warn
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

/** @author Aidan Follestad (@afollestad) */
internal data class TypeAndArgs(
  val fullType: TypeName,
  val erasedType: TypeName,
  val genericArgs: Array<TypeName>,
  val qualifier: String?,
  val isProvider: Boolean,
  val isViewModel: Boolean,
  val isSet: Boolean
) {
  val hasGenericArgs: Boolean = genericArgs.isNotEmpty()
  val getterName: String = when {
    isSet && isProvider -> GET_SET_PROVIDER_NAME
    isSet -> GET_SET_NAME
    isProvider -> GET_PROVIDER_NAME
    else -> GET_NAME
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
    if (isSet != other.isSet) return false
    return true
  }

  override fun hashCode(): Int {
    var result = fullType.hashCode()
    result = 31 * result + erasedType.hashCode()
    result = 31 * result + (qualifier?.hashCode() ?: 0)
    result = 31 * result + isProvider.hashCode()
    result = 31 * result + isViewModel.hashCode()
    result = 31 * result + isSet.hashCode()
    return result
  }
}

/** @author Aidan Follestad (@afollestad) */
internal fun TypeMirror.asTypeAndArgs(
  env: ProcessingEnvironment,
  nullable: Boolean = false,
  qualifier: String?,
  isProvider: Boolean = false,
  isSet: Boolean = false
): TypeAndArgs {
  val baseType = env.typeUtils.erasure(this)
      .correctTypeName(env)
  val typeArgs = if (this is DeclaredType) {
    typeArguments
  } else {
    emptyList<TypeMirror>()
  }

  if (baseType.toString() == Provider::class.qualifiedName) {
    check(typeArgs.size == 1)
    return typeArgs.single()
        .asTypeAndArgs(env, nullable, qualifier, isProvider = true, isSet = isSet)
  } else if (baseType == SET || baseType == MUTABLE_SET) {
    check(typeArgs.size == 1)
    return typeArgs.single()
        .asTypeAndArgs(env, nullable, qualifier, isSet = true, isProvider = isProvider)
  }

  val isViewModel = isViewModel(env)
  val typeArgsNames: Array<TypeName> = if (isViewModel) {
    if (typeArgs.isNotEmpty()) {
      env.warn("$this: Generic args on view models are ignored.")
    }
    emptyArray()
  } else {
    typeArgs
        .map { it.correctTypeName(env) }
        .toTypedArray()
  }
  val actualQualifier = if (isViewModel) {
    if (!qualifier.isNullOrEmpty()) {
      env.warn("$this: Qualifiers on view models are ignored.")
    }
    null
  } else {
    qualifier
  }

  return TypeAndArgs(
      fullType = correctTypeName(env).copy(nullable = nullable),
      erasedType = baseType.copy(nullable = nullable),
      genericArgs = typeArgsNames,
      qualifier = actualQualifier,
      isProvider = isProvider,
      isViewModel = isViewModel,
      isSet = isSet
  )
}

/** @author Aidan Follestad (@afollestad) */
internal fun VariableElement.asTypeAndArgs(
  env: ProcessingEnvironment
): TypeAndArgs {
  return asType().asTypeAndArgs(
      env = env,
      nullable = isNullable(),
      qualifier = getQualifier()
  )
}

/** @author Aidan Follestad (@afollestad) */
internal fun ExecutableElement.returnTypeAsTypeAndArgs(
  env: ProcessingEnvironment
): TypeAndArgs {
  return returnType.asTypeAndArgs(
      env = env,
      nullable = isNullable(),
      qualifier = getQualifier()
  )
}
