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
package com.afollestad.ulfberhtsample.modules

import com.afollestad.ulfberht.annotation.Binds
import com.afollestad.ulfberht.annotation.Module
import com.afollestad.ulfberht.annotation.Provides
import com.afollestad.ulfberht.annotation.Singleton
import com.afollestad.ulfberhtsample.Qualifiers.MAIN
import com.afollestad.ulfberhtsample.api.Adder
import com.afollestad.ulfberhtsample.api.Calculator
import com.afollestad.ulfberhtsample.api.Multiplier
import com.afollestad.ulfberhtsample.api.RealAdder
import com.afollestad.ulfberhtsample.api.RealCalculator
import com.afollestad.ulfberhtsample.api.RealMultiplier

@Module
interface ModuleOne {
  @Binds
  @Singleton
  fun calculator(calculator: RealCalculator): Calculator
}

@Module
interface ModuleTwo {
  @Binds
  fun adder(adder: RealAdder): Adder

  @Binds
  fun multiplier(multiplier: RealMultiplier): Multiplier
}

@Module
abstract class ModuleThree {
  @Provides(MAIN)
  @Singleton
  fun testString(adder: RealAdder): String {
    return "Hello, World! $adder"
  }
}