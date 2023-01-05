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
package com.afollestad.ulfberht.sample

import com.afollestad.ulfberht.annotation.Inject
import com.afollestad.ulfberht.component
import com.afollestad.ulfberht.sample.api.Client
import com.afollestad.ulfberht.sample.api.components.AppComponent

class Main {
  @Inject
  lateinit var client: Client

  init {
    component<AppComponent>().inject(this)
  }

  fun run() {
    client.doSomething()
  }
}

fun main() {
  Main().run()
}
