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

import com.afollestad.ulfberht.annotation.Inject
import com.afollestad.ulfberht.graph.BindingModel.Key
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ksp.toClassName

internal val KSDeclaration.simpleNameString: String
  get() = simpleName.getShortName()

internal val KSDeclaration.qualifiedNameString: String
  get() = (qualifiedName ?: simpleName).asString()

internal val KSClassDeclaration.injectConstructor: KSFunctionDeclaration
  get() = getConstructors()
    .firstOrNull { it.isAnnotationPresent<Inject>() }
    ?: primaryConstructor
    ?: error("No @Inject or primary constructor on $qualifiedNameString.")

internal val KSClassDeclaration.injectProperties: List<KSPropertyDeclaration>
  get() = getAllProperties().filter { it.isAnnotationPresent<Inject>() }
    .toList()

internal val KSClassDeclaration.bindingKey: Key
  get() = Key(
    type = toClassName(),
    qualifier = qualifier,
  )
