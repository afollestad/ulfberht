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

import com.afollestad.ulfberht.annotation.Binds
import com.afollestad.ulfberht.annotation.IntoSet
import com.afollestad.ulfberht.annotation.Provides
import com.afollestad.ulfberht.annotation.Singleton
import com.afollestad.ulfberht.util.BindOrProvide.BIND
import com.afollestad.ulfberht.util.BindOrProvide.PROVIDE
import com.afollestad.ulfberht.util.BinderOrProvider.Companion.PROVIDE_FUNCTION_PREFIX
import com.afollestad.ulfberht.util.ProcessorUtil.asTypeElement
import com.afollestad.ulfberht.util.ProcessorUtil.error
import com.afollestad.ulfberht.util.ProcessorUtil.filterMethods
import com.afollestad.ulfberht.util.ProcessorUtil.getConstructorParamsTypesAndArgs
import com.afollestad.ulfberht.util.ProcessorUtil.getMethodParamsTypeAndArgs
import com.afollestad.ulfberht.util.ProcessorUtil.hasAnnotationMirror
import com.afollestad.ulfberht.util.ProcessorUtil.isAbstractClass
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.INTERFACE
import javax.lang.model.element.ExecutableElement

/** @author Aidan Follestad (@afollestad) */
internal enum class BindOrProvide {
  BIND,
  PROVIDE
}

/** @author Aidan Follestad (@afollestad) */
internal data class BinderOrProviderKey(
  val mode: BindOrProvide,
  val providedType: TypeAndArgs,
  val qualifier: String?,
  val intoSet: Boolean
)

/** @author Aidan Follestad (@afollestad) */
internal data class BinderOrProvider(
  val mode: BindOrProvide,
  val methodName: String,
  val getterName: String = methodName,
  val providedType: TypeAndArgs,
  val concreteType: TypeAndArgs,
  val isSingleton: Boolean,
  val fillArgumentTypes: Sequence<TypeAndArgs>,
  val intoSet: Boolean
) {
  /** Used for grouping. */
  val key: BinderOrProviderKey
    get() = BinderOrProviderKey(
        mode = mode,
        providedType = providedType,
        qualifier = qualifier,
        intoSet = intoSet
    )
  val qualifier: String? get() = providedType.qualifier

  companion object {
    const val PROVIDE_FUNCTION_PREFIX = "_provides"
  }
}

internal typealias KeyedBinderOrProvider = Map.Entry<BinderOrProviderKey, List<BinderOrProvider>>

/** @author Aidan Follestad (@afollestad) */
internal fun Element.getBindsAndProvidesMethods(
  env: ProcessingEnvironment,
  dependencyGraph: DependencyGraph
): Sequence<KeyedBinderOrProvider> {
  return enclosedElements
      .filterMethods()
      .filter {
        it.hasAnnotationMirror<Binds>() ||
            it.hasAnnotationMirror<Provides>()
      }
      .map { method ->
        if (method.hasAnnotationMirror<Binds>()) {
          if (kind != INTERFACE) {
            env.error("$method: @Binds methods can only be used in an interface.")
            return@map null
          }
          method.asBindsMethod(env, dependencyGraph)
        } else {
          if (!isAbstractClass()) {
            env.error("$method: @Provides methods can only be used in an abstract class.")
            return@map null
          }
          method.asProvidesMethod(env, dependencyGraph)
        }
      }
      .filterNotNull()
      .groupBy { it.key }
      .asSequence()
}

private fun ExecutableElement.asBindsMethod(
  env: ProcessingEnvironment,
  dependencyGraph: DependencyGraph
): BinderOrProvider? {
  if (parameters.size != 1) {
    env.error("$this: @Binds methods must have a single parameter.")
    return null
  }

  val parameterType = parameters.single()
      .asType()
  if (!env.typeUtils.isSubtype(parameterType, returnType)) {
    env.error(
        "@Binds method $simpleName() parameter of type " +
            "$parameterType must be a subclass of $returnType"
    )
    return null
  }

  val returnTypeAndArgs = returnTypeAsTypeAndArgs(env)
  val qualifier = returnTypeAndArgs.qualifier
  val concreteType = parameterType.asTypeAndArgs(env, qualifier = qualifier)
  dependencyGraph.bind(
      concrete = concreteType,
      to = returnTypeAndArgs
  )

  val fillArgumentTypes = parameterType
      .asTypeElement()
      .getConstructorParamsTypesAndArgs(env)

  return BinderOrProvider(
      mode = BIND,
      methodName = simpleName.toString(),
      providedType = returnTypeAndArgs,
      concreteType = concreteType,
      isSingleton = hasAnnotationMirror<Singleton>(),
      fillArgumentTypes = fillArgumentTypes,
      intoSet = hasAnnotationMirror<IntoSet>()
  )
}

private fun ExecutableElement.asProvidesMethod(
  env: ProcessingEnvironment,
  dependencyGraph: DependencyGraph
): BinderOrProvider? {
  val returnTypeAndArgs = returnTypeAsTypeAndArgs(env)
  val methodName = simpleName.toString()
  val fillArgumentTypes = getMethodParamsTypeAndArgs(env)

  fillArgumentTypes.forEach {
    dependencyGraph.bind(
        concrete = returnTypeAndArgs,
        to = it
    )
  }

  return BinderOrProvider(
      mode = PROVIDE,
      methodName = methodName,
      getterName = "$PROVIDE_FUNCTION_PREFIX${methodName.capitalize()}",
      providedType = returnTypeAndArgs,
      concreteType = returnTypeAndArgs,
      isSingleton = hasAnnotationMirror<Singleton>(),
      fillArgumentTypes = fillArgumentTypes,
      intoSet = hasAnnotationMirror<IntoSet>()
  )
}
