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

import com.afollestad.ulfberht.annotation.Component
import com.afollestad.ulfberht.graph.ComponentModel.ComponentMember
import com.afollestad.ulfberht.graph.ComponentModel.ComponentMember.Getter
import com.afollestad.ulfberht.graph.ComponentModel.ComponentMember.Injector
import com.afollestad.ulfberht.utilities.asBindingKey
import com.afollestad.ulfberht.utilities.bindingKey
import com.afollestad.ulfberht.utilities.getAnnotationByType
import com.afollestad.ulfberht.utilities.getKSTypeArgument
import com.afollestad.ulfberht.utilities.injectProperties
import com.afollestad.ulfberht.utilities.isUnit
import com.afollestad.ulfberht.utilities.qualifiedNameString
import com.afollestad.ulfberht.utilities.qualifier
import com.afollestad.ulfberht.utilities.shortNameString
import com.afollestad.ulfberht.utilities.simpleNameString
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * TODO
 */
@Throws(IllegalStateException::class)
internal fun KSClassDeclaration.toComponentModel(): ComponentModel {
  val component = getAnnotationByType<Component>()
  val scope = component.getKSTypeArgument("scope")
    .takeUnless { it.isUnit }

  return ComponentModel(
    scope = scope?.toClassName(),
    className = toClassName(),
    members = getAllFunctions()
      .mapNotNull { it.toComponentMember() }
      .toList(),
    containingFile = containingFile!!,
  )
}

@Throws(IllegalStateException::class)
private fun KSFunctionDeclaration.toComponentMember(): ComponentMember? {
  return when (simpleNameString) {
    "inject" -> {
      val functionReturnType: KSType? = returnType?.resolve()
      check(functionReturnType?.isUnit == true) {
        "Function $qualifiedNameString must not have a return type: $functionReturnType"
      }
      val singleParam = parameters.singleOrNull()
      val targetType = parameters.singleOrNull()?.type?.resolve()
        ?.let { it.declaration as? KSClassDeclaration }
        ?: error("Function $qualifiedNameString must have a single parameter with a class type.")

      Injector(
        providedKey = targetType.toClassName()
          .asBindingKey(qualifier),
        targetType = targetType.toClassName(),
        targetParamName = singleParam!!.shortNameString,
        targetProperties = targetType.injectProperties.associate { it.simpleNameString to it.bindingKey },
      )
    }

    "equals", "hashCode", "toString" -> null

    else -> {
      check(parameters.isEmpty()) {
        "Function $qualifiedNameString must have no parameters."
      }
      Getter(
        providedKey = bindingKey,
        functionName = simpleNameString,
        qualifier = qualifier,
      )
    }
  }
}
