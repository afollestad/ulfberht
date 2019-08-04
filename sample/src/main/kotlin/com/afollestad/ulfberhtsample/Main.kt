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
import com.afollestad.ulfberht.annotation.Param
import com.afollestad.ulfberht.annotation.Provides
import com.afollestad.ulfberht.common.Logger
import com.afollestad.ulfberht.component

data class SomeClass<A, B>(
  val left: A,
  val right: B
)

@Component(modules = [MyModule1::class, MyModule2::class])
interface MyComponent {
  fun inject(main: Main)
}

@Module
interface MyModule1 {
  @Binds fun one(one: OneImpl): One

  @Binds fun two(two: TwoImpl): Two
}

@Module
abstract class MyModule2 {
  @Provides fun one(): SomeClass<String, Boolean> = SomeClass("test", true)

  @Provides fun two(): SomeClass<Int, Long> = SomeClass(6, 10L)
}

interface One {
  fun doSomething()
}

class OneImpl(
  private val two: Two,
  private val idk: SomeClass<String, Boolean>
) : One {
  override fun doSomething() = two.doSomething()
}

interface Two {
  fun doSomething()
}

class TwoImpl(
  @Param("message") private val message: String
) : Two {
  override fun doSomething() = println(message)
}

class Main {
  @Inject lateinit var one: One
  @Inject lateinit var someClass1: SomeClass<String, Boolean>
  @Inject lateinit var someClass2: SomeClass<Int, Long>

  fun doSomething() {
    one.doSomething()
    println(someClass1.toString())
    println(someClass2.toString())
  }

  init {
    component<MyComponent>(
        "message" to "Hello, World!"
    ).inject(this)
  }
}

fun main() {
  Logger.install { println("[LOG] $it") }
  Main().doSomething()
}
