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
package com.afollestad.ulfberhtsample

import com.afollestad.ulfberht.annotation.Binds
import com.afollestad.ulfberht.annotation.Component
import com.afollestad.ulfberht.annotation.Inject
import com.afollestad.ulfberht.annotation.Module
import com.afollestad.ulfberht.common.Logger
import com.afollestad.ulfberht.component

@Component(modules = [MyModule::class])
interface MyComponent {
  fun inject(main: Main)
}

@Module
interface MyModule {
  @Binds fun one(one: OneImpl): One

  @Binds fun two(two: TwoImpl): Two
}

interface One {
  fun doSomething()
}

class OneImpl(private val two: Two) : One {
  override fun doSomething() = two.doSomething()
}

interface Two {
  fun doSomething()
}

class TwoImpl : Two {
  override fun doSomething() {
    println("hello, from Two!")
  }
}

class Main {
  @Inject lateinit var one: One

  fun doSomething() = one.doSomething()

  init {
    component<MyComponent>().inject(this)
  }
}

fun main() {
  Logger.install { println("[LOG] $it") }
  Main().doSomething()
}
