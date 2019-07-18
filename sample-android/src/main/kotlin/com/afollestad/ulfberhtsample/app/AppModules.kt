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
package com.afollestad.ulfberhtsample.app

import com.afollestad.ulfberht.annotation.Binds
import com.afollestad.ulfberht.annotation.Module
import com.afollestad.ulfberht.annotation.Provides
import com.afollestad.ulfberht.annotation.Singleton
import com.afollestad.ulfberhtsample.Qualifiers.API_KEY
import com.afollestad.ulfberhtsample.api.RealSession
import com.afollestad.ulfberhtsample.api.Session

@Module
abstract class AppProvideModules {
  @Provides(API_KEY) fun apiKey(): String {
    return "o3riuhfoij30p9i2ug34igub"
  }
}

@Module
interface AppBindModules {
  @Binds @Singleton
  fun session(impl: RealSession): Session
}
