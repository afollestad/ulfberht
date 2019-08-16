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

import com.afollestad.ulfberht.util.Names.QUALIFIER
import com.afollestad.ulfberht.util.ProcessorUtil.applyIf
import com.afollestad.ulfberht.util.ProcessorUtil.getQualifier
import com.afollestad.ulfberht.util.ProcessorUtil.hasSuperClass
import com.afollestad.ulfberht.util.ProcessorUtil.warn
import com.afollestad.ulfberht.util.Types.FRAGMENT
import com.afollestad.ulfberht.util.Types.FRAGMENT_ACTIVITY
import com.afollestad.ulfberht.util.Types.VIEW_MODEL_PROVIDERS
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MUTABLE_SET
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/** @author Aidan Follestad (@afollestad) */
internal data class InjectingField(
  val fieldName: String,
  val fieldType: TypeAndArgs,
  val qualifier: String?
) {
  fun setFieldInTarget(
    environment: ProcessingEnvironment,
    targetParamName: String,
    targetClassElement: TypeElement,
    code: CodeBlock.Builder
  ): Boolean {
    code.add("$targetParamName.%N = ", fieldName)
    return if (fieldType.isViewModel) {
      setViewModelFieldInTarget(environment, targetParamName, targetClassElement, code)
    } else {
      setNormalFieldInTarget(code)
    }.also {
      code.add("\n")
    }
  }

  private fun setViewModelFieldInTarget(
    environment: ProcessingEnvironment,
    targetParamName: String,
    targetClassElement: TypeElement,
    code: CodeBlock.Builder
  ): Boolean {
    if (!targetClassElement.hasSuperClass(environment, FRAGMENT, FRAGMENT_ACTIVITY)) {
      environment.warn(
          "$targetClassElement cannot inject ViewModels. Must be a Fragment or Activity."
      )
      return false
    }
    code.add(
        "%T.of($targetParamName, this)[%T::class.java]",
        VIEW_MODEL_PROVIDERS, fieldType.erasedType
    )
    return true
  }

  private fun setNormalFieldInTarget(code: CodeBlock.Builder): Boolean {
    val getterName = fieldType.getterName
    val doubleBang = if (fieldType.isProvider && !fieldType.isSet) "!!" else ""

    code.add("$getterName(%T::class", fieldType.erasedType)
    code.applyIf(fieldType.hasGenericArgs) {
      add(", setOf(")
      for ((index, typeArg) in fieldType.genericArgs.withIndex()) {
        if (index > 0) add(", ")
        add("%T::class", typeArg)
      }
      add(")")
    }
    code.applyIf(qualifier != null) {
      add(", $QUALIFIER = %S", qualifier)
    }
    code.add(")$doubleBang")
    code.applyIf(!fieldType.isProvider && fieldType.hasGenericArgs) {
      if (fieldType.isSet) {
        add(" as %T<%T>", MUTABLE_SET, fieldType.fullType)
      } else {
        add(" as %T", fieldType.fullType)
      }
    }

    return true
  }
}

/** @author Aidan Follestad (@afollestad) */
internal fun VariableElement.asInjectField(
  env: ProcessingEnvironment
): InjectingField {
  return InjectingField(
      fieldName = simpleName.toString(),
      fieldType = asTypeAndArgs(env),
      qualifier = getQualifier()
  )
}
