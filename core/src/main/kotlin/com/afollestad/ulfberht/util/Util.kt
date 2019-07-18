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

import com.afollestad.ulfberht.common.BaseComponent
import com.afollestad.ulfberht.annotation.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

internal fun String.asKClass(): KClass<*> = Class.forName(this).kotlin

internal inline fun <reified T : Any> KClass<*>.newInstance(parent: BaseComponent?): T =
  primaryConstructor!!.call(parent) as T

internal fun KClass<*>.getParentType(): KClass<*>? {
  val parent = (annotations.singleOrNull { it is Component } as? Component)?.parent
  return if (parent == null || parent == Any::class) {
    null
  } else {
    parent
  }
}
