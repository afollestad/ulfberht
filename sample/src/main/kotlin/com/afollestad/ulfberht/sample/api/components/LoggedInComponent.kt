package com.afollestad.ulfberht.sample.api.components

import com.afollestad.ulfberht.annotation.Component
import com.afollestad.ulfberht.sample.Main
import com.afollestad.ulfberht.sample.api.Client
import com.afollestad.ulfberht.sample.api.qualifiers.AuthToken
import com.afollestad.ulfberht.sample.api.scopes.LoggedInScope

@Component(
  scope = LoggedInScope::class,
  parent = AppComponent::class,
)
interface LoggedInComponent {

  @AuthToken fun authToken(): String

  fun oneType(): Client

  fun inject(main: Main)
}