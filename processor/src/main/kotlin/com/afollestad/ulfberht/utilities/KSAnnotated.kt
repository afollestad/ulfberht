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
@file:OptIn(KspExperimental::class)

package com.afollestad.ulfberht.utilities

import com.afollestad.ulfberht.annotation.Qualifier
import com.afollestad.ulfberht.annotation.Scope
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

internal inline fun <reified A : Annotation> KSAnnotated.getAnnotationByTypeOrNull(): KSAnnotation? {
  val targetQualifiedName = A::class.qualifiedName ?: A::class.simpleName
  return annotations.firstOrNull {
    val declaration = it.annotationType.resolve().declaration
    declaration.qualifiedNameString == targetQualifiedName
  }
}

internal inline fun <reified A : Annotation> KSAnnotated.getAnnotationByType(): KSAnnotation =
  getAnnotationByTypeOrNull<A>() ?: error("No ${A::class.qualifiedName} annotation present on $this")

internal inline fun <reified A : Annotation> KSAnnotated.isAnnotationPresent(): Boolean =
  getAnnotationByTypeOrNull<A>() != null

internal fun KSDeclaration.hasSuperType(type: ClassName): Boolean =
  (this as? KSClassDeclaration)?.superTypes
    ?.map { it.toTypeName() }
    ?.any { it == type }
    ?: error("Could not get super types for: $this")

internal val KSAnnotated.qualifier: ClassName?
  get() = annotations
    .firstOrNull { it.annotationType.resolve().declaration.isAnnotationPresent<Qualifier>() }
    ?.annotationType
    ?.resolve()
    ?.declaration
    ?.let { it as? KSClassDeclaration }
    ?.toClassName()

internal val KSAnnotated.scope: ClassName?
  get() = annotations
    .firstOrNull { it.annotationType.resolve().declaration.isAnnotationPresent<Scope>() }
    ?.annotationType
    ?.resolve()
    ?.declaration
    ?.let { it as? KSClassDeclaration }
    ?.toClassName()
