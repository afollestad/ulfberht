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
@file:Suppress("unused")

package com.afollestad.ulfberht.android

import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.afollestad.ulfberht.getScope
import com.afollestad.ulfberht.scopes.Scope

/**
 * Attaches a [Scope] to a [LifecycleOwner], automatically exiting the scope when the given
 * lifecycle is destroyed.
 */
fun LifecycleOwner.attachScope(name: String): Scope {
  val scope = getScope(name)
  this.lifecycle.addObserver(object : LifecycleObserver {
    @OnLifecycleEvent(ON_DESTROY)
    fun onDestroy() = scope.exit()
  })
  return scope
}
