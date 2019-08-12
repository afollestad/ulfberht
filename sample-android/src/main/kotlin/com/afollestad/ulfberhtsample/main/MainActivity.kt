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
package com.afollestad.ulfberhtsample.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.ulfberht.annotation.Inject
import com.afollestad.ulfberht.annotation.ScopeOwner
import com.afollestad.ulfberht.component
import com.afollestad.ulfberhtsample.ScopeNames.MAIN
import com.afollestad.ulfberhtsample.api.Client
import com.afollestad.ulfberhtsample.api.Session
import com.afollestad.ulfberhtsample.login.LoginActivity

@ScopeOwner(MAIN)
class MainActivity : AppCompatActivity() {
  @Inject lateinit var session: Session
  @Inject lateinit var client: Client
  @Inject lateinit var mainViewModel: MainViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    component<MainComponent>().inject(this)
    // TODO use client
  }

  override fun onResume() {
    super.onResume()

    if (session.isLoggedIn()) {
      Toast.makeText(this, "Logged in with token: ${session.getAuthToken()}", Toast.LENGTH_LONG)
          .show()
    } else {
      startActivity(Intent(this, LoginActivity::class.java))
    }
  }
}
