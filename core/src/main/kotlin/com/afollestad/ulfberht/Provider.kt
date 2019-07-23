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
package com.afollestad.ulfberht

/**
 * A [Provider] is in charge of creating an instance of [T]. A Provider
 * can choose its value's retention policy, and should remove references when
 * [destroy] is invoked.
 *
 * @author Aidan Follestad (@afollestad)
 */
interface Provider<T> {
  /** Gets an instance of [T]. */
  fun get(): T

  /** Destroys any stored references. */
  fun destroy()
}
