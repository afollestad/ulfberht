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
import com.afollestad.ulfberht.common.BaseComponent
import com.afollestad.ulfberht.common.BaseModule
import com.afollestad.ulfberht.common.BaseModule.SingletonProvider
import com.afollestad.ulfberht.common.BaseModule.FactoryProvider
import com.afollestad.ulfberht.common.Logger
import com.afollestad.ulfberht.scopes.Scope
import com.afollestad.ulfberht.util.ProcessorUtil.asNullableTypeName
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import kotlin.reflect.KClass

/** @author Aidan Follestad (@afollestad) */
internal object Names {
  const val WANTED_TYPE = "wantedType"
  const val QUALIFIER = "qualifier"
  const val CALLED_BY = "calledBy"

  const val GET_PROVIDER_NAME = "getProvider"

  const val CACHED_PROVIDERS_NAME = "cachedProviders"
  const val FACTORY_EXTENSION_NAME = "factory"
  const val SINGLETON_PROVIDER_EXTENSION_NAME = "singleton"
  const val IS_SUBCLASS_OF_EXTENSION_NAME = "isSubClassOf"

  const val LIBRARY_PACKAGE = "com.afollestad.ulfberht"

  const val MODULE_NAME_SUFFIX = "_Module"
  const val MODULES_LIST_NAME = "modules"
  const val RUNTIME_DEPS_NAME = "runtimeDependencies"
  const val GET_RUNTIME_DEP_NAME = "getRuntimeDependency"
  const val COMPONENT_PARAM_NAME = "component"
}

/** @author Aidan Follestad (@afollestad) */
internal object Types {
  val NULLABLE_KOTLIN_STRING = STRING.copy(nullable = true)

  val TYPE_VARIABLE_T = TypeVariableName("T", ANY)
  val REIFIED_TYPE_VARIABLE_T = TYPE_VARIABLE_T.copy(reified = true)

  private val KCLASS = KClass::class.asTypeName()
  val KCLASS_OF_T = KCLASS.parameterizedBy(TYPE_VARIABLE_T)
  val KCLASS_OF_ANY = KCLASS.parameterizedBy(STAR)

  val PROVIDER = Provider::class.asTypeName()
  val PROVIDER_OF_T = PROVIDER.parameterizedBy(TYPE_VARIABLE_T)
  val PROVIDER_OF_T_NULLABLE = PROVIDER_OF_T.copy(nullable = true)
  val PROVIDER_OF_ANY = PROVIDER.parameterizedBy(STAR)

  val BASE_COMPONENT = BaseComponent::class.asTypeName()
  val NULLABLE_BASE_COMPONENT = BaseComponent::class.asNullableTypeName()

  val BASE_MODULE = BaseModule::class.asTypeName()

  val UNSCOPED_PROVIDER = FactoryProvider::class.asTypeName()
  val SINGLETON_PROVIDER = SingletonProvider::class.asTypeName()

  val LOGGER = Logger::class.asTypeName()
  val LIFECYCLE_OWNER = ClassName("androidx.lifecycle", "LifecycleOwner")
  val LIFECYCLE_OBSERVER = ClassName("androidx.lifecycle", "LifecycleObserver")
  val SCOPE = Scope::class.asTypeName()
  val GET_SCOPE_METHOD = ClassName("com.afollestad.ulfberht", "getScope")
  val ON_LIFECYCLE_EVENT = ClassName("androidx.lifecycle", "OnLifecycleEvent")
  val LIFECYCLE_EVENT_ON_DESTROY = ClassName("androidx.lifecycle.Lifecycle.Event", "ON_DESTROY")
}

/** @author Aidan Follestad (@afollestad) */
internal object Annotations {
  val SUPPRESS_UNCHECKED_CAST: AnnotationSpec
    get() = AnnotationSpec.builder(Suppress::class)
        .addMember("%S", "UNCHECKED_CAST")
        .build()
}
