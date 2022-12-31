package com.afollestad.ulfberht.sample.api

import com.afollestad.ulfberht.annotation.BindsTo
import com.afollestad.ulfberht.annotation.Inject
import com.afollestad.ulfberht.annotation.Singleton
import com.afollestad.ulfberht.sample.api.scopes.LoggedInScope

interface AuthTokens {
  val currentToken: String
  fun refreshAuthToken()
}

@Singleton
@BindsTo(LoggedInScope::class)
class AuthTokensImpl @Inject constructor(
  private val apiCredentials: ApiCredentials,
) : AuthTokens {

  private var _currentToken: String = "fake-auth-token"

  override val currentToken: String get() = _currentToken

  override fun refreshAuthToken() {
    _currentToken = "refreshed-auth-token-${apiCredentials.key}"
  }
}
