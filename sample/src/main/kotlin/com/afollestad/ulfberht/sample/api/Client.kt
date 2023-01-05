package com.afollestad.ulfberht.sample.api

import com.afollestad.ulfberht.annotation.BindsTo
import com.afollestad.ulfberht.annotation.Inject
import com.afollestad.ulfberht.annotation.Singleton
import com.afollestad.ulfberht.sample.api.qualifiers.AuthToken
import com.afollestad.ulfberht.sample.api.scopes.AppScope

interface Client {
  fun doSomething()
}

@Singleton
@BindsTo(scope = AppScope::class)
class ClientImpl @Inject constructor(
  @AuthToken private val authToken: String,
) : Client {
  override fun doSomething() {
    println("Auth token: $authToken")
  }
}
