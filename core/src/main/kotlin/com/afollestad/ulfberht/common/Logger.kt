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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.afollestad.ulfberht.common

/**
 * Call [install] with a handler to enable Ulberht logging. Android's implementation has a
 * an extension which enables logging to the Logcat for you.
 */
object Logger {
  private var handler: ((message: String) -> Unit)? = null

  fun install(handler: (message: String) -> Unit) {
    this.handler = handler
  }

  fun log(message: String) {
    handler?.invoke(message)
  }
}
