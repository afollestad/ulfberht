package com.afollestad.ulfberht.sample.api.providers

import com.afollestad.ulfberht.annotation.Provides
import com.afollestad.ulfberht.annotation.Singleton
import com.afollestad.ulfberht.sample.api.ApiCredentials
import com.afollestad.ulfberht.sample.api.AuthTokens
import com.afollestad.ulfberht.sample.api.qualifiers.AuthToken

object TestProvider {

  @Provides @Singleton
  fun provideApiCredentials(): ApiCredentials =
    ApiCredentials(
      key = "fake-api-key",
      secret = "fake-api-secret",
    )

  @Provides @AuthToken
  fun provideAuthToken(authTokens: AuthTokens): String = authTokens.currentToken
}
