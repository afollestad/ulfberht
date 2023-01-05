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

import com.afollestad.ulfberht.annotation.BindsTo
import com.afollestad.ulfberht.annotation.Singleton
import com.afollestad.ulfberht.graph.BindingModel.AssociationBinding
import com.afollestad.ulfberht.graph.BindingModel.FactoryBinding
import com.afollestad.ulfberht.graph.BindingModel.ProviderBinding
import com.afollestad.ulfberht.utilities.Names.FACTORY_NAME_SUFFIX
import com.afollestad.ulfberht.utilities.Types.FACTORY
import com.afollestad.ulfberht.utilities.Types.PROVIDER
import com.afollestad.ulfberht.utilities.asBindingKey
import com.afollestad.ulfberht.utilities.bindingKey
import com.afollestad.ulfberht.utilities.getAnnotationByType
import com.afollestad.ulfberht.utilities.getKSTypeArgument
import com.afollestad.ulfberht.utilities.injectConstructor
import com.afollestad.ulfberht.utilities.isAnnotationPresent
import com.afollestad.ulfberht.utilities.qualifiedNameString
import com.afollestad.ulfberht.utilities.qualifier
import com.afollestad.ulfberht.utilities.scope
import com.afollestad.ulfberht.utilities.shortNameString
import com.afollestad.ulfberht.utilities.simpleNameString
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * TODO
 */
internal val BindingModel.getterName: String
  get() = when (this) {
    is AssociationBinding, is FactoryBinding -> "create"
    is ProviderBinding -> "get"
  }

/**
 * TODO
 */
internal fun BindingModel.toConstructorParameterSpec(): ParameterSpec =
  ParameterSpec
    .builder(
      name = factoryParameterName,
      type = factoryOrProviderClassName.parameterizedBy(providedKey.type),
    )
    .apply { providedKey.qualifier?.let(::addAnnotation) }
    .build()

/**
 * TODO
 */
internal fun BindingModel.toConstructorPropertySpec(): PropertySpec =
  PropertySpec
    .builder(
      name = factoryParameterName,
      type = factoryOrProviderClassName.parameterizedBy(providedKey.type),
      modifiers = setOf(PRIVATE),
    )
    .initializer(factoryParameterName)
    .build()

/**
 * TODO
 */
@Throws(IllegalStateException::class)
internal fun KSFunctionDeclaration.toProviderBindingModel(): ProviderBinding {
  val returnType = (returnType?.resolve()?.declaration as? KSClassDeclaration)
    ?.toClassName()
    ?.takeUnless { it == UNIT }
    ?: error("Provider function $qualifiedNameString must have a non-Unit return type.")

  val parentClassName = (parentDeclaration as? KSClassDeclaration)?.toClassName()
  val qualifier = qualifier

  return ProviderBinding(
    factoryKey = parentClassName?.asBindingKey(qualifier),
    functionName = parentClassName
      ?.let { MemberName(it, simpleNameString) }
      ?: MemberName(packageName.asString(), simpleNameString),
    scope = scope,
    providedKey = returnType.asBindingKey(qualifier),
    parameters = parameters.associate { it.shortNameString to it.bindingKey },
    isSingleton = isAnnotationPresent<Singleton>(),
    containingFile = containingFile!!,
    factoryParameterName = returnType.simpleName
      .plus(qualifier?.let { "_${it.simpleName}" }.orEmpty())
      .plus("Provider")
      .sanitizeForMemberName(),
  )
}

/**
 * TODO
 */
@Throws(IllegalStateException::class)
internal fun KSFunctionDeclaration.toFactoryBindingModel(): FactoryBinding {
  check(isConstructor()) {
    "@Inject function $qualifiedNameString must be on the constructor of a class."
  }
  val parentClass = parentDeclaration as KSClassDeclaration
  val parentClassName = parentClass.toClassName()
  val qualifier = parentClass.qualifier
  val factoryClassName = parentClassName.asFactoryClassName(qualifier)

  return FactoryBinding(
    factoryKey = factoryClassName.asBindingKey(qualifier),
    providedKey = parentClassName.asBindingKey(qualifier),
    scope = scope,
    parameters = parameters.associate { it.shortNameString to it.bindingKey },
    isSingleton = parentClass.isAnnotationPresent<Singleton>(),
    containingFile = containingFile!!,
    factoryParameterName = factoryClassName.simpleName.sanitizeForMemberName(),
  )
}

/**
 * TODO
 */
@Throws(IllegalStateException::class)
internal fun KSClassDeclaration.toAssociationBinding(): AssociationBinding {
  val bindsTo = getAnnotationByType<BindsTo>()
  val providedType = bindsTo.getKSTypeArgument("boundType")
    .toClassName()
    .takeUnless { it == UNIT }
    ?: superTypes.singleOrNull()?.resolve()?.toClassName()
    ?: error("@BindsTo must specify a boundType since there is more than one supertype: $qualifiedNameString")

  val bindingKey = bindingKey
  val qualifier = qualifier
  val factoryClassName = bindingKey.type.asFactoryClassName(qualifier)

  return AssociationBinding(
    implementationKey = bindingKey,
    scope = scope,
    factoryKey = factoryClassName.asBindingKey(qualifier),
    providedKey = providedType.asBindingKey(qualifier),
    parameters = injectConstructor.parameters.associate { it.shortNameString to it.bindingKey },
    isSingleton = isAnnotationPresent<Singleton>(),
    containingFile = containingFile!!,
    factoryParameterName = factoryClassName.simpleName.sanitizeForMemberName(),
  )
}

private fun ClassName.asFactoryClassName(qualifier: ClassName?): ClassName =
  if (!simpleName.endsWith(FACTORY_NAME_SUFFIX)) {
    ClassName(
      packageName = packageName,
      simpleNames = listOf(
        simpleName +
          qualifier?.let { "_${it.simpleName}" }.orEmpty() +
          FACTORY_NAME_SUFFIX,
      ),
    )
  } else {
    this
  }

private val BindingModel.factoryOrProviderClassName: ClassName
  get() = when (this) {
    is FactoryBinding, is AssociationBinding -> FACTORY
    is ProviderBinding -> PROVIDER
  }

private fun String.sanitizeForMemberName(): String =
  replaceFirstChar(Char::lowercase)
    .replace(Regex("[_0-9]"), "")
