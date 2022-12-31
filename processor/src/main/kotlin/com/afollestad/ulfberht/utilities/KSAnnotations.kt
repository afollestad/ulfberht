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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType

/**
 * We are unable to use KClass directly from KSP as it runs before classes are able to be resolved.
 * This method allows us to read KClass properties from annotation classes as [KSType].
 */
@Throws(IllegalStateException::class)
internal fun KSAnnotation.getKSTypeArgument(name: String): KSType {
  val argument = arguments.firstOrNull { it.name?.asString() == name }
    ?: error("No annotation argument found with name: $name")
  return argument.value as? KSType
    ?: error("Unable to cast argument value to KSType: ${argument.value}")
}

/**
 * We are unable to use KClass directly from KSP as it runs before classes are able to be resolved.
 * This method allows us to read KClass properties from annotation classes as [KSType].
 */
@Suppress("UNCHECKED_CAST")
@Throws(IllegalStateException::class)
internal fun KSAnnotation.getKSTypeArgumentList(name: String): List<KSType> {
  val argument = arguments.firstOrNull { it.name?.asString() == name }
    ?: error("No annotation argument found with name: $name")
  return argument.value as? List<KSType>
    ?: error("Unable to cast argument value to List<KSType>: ${argument.value}")
}
