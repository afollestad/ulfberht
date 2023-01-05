package com.afollestad.ulfberht.sample.api.components

import com.afollestad.ulfberht.annotation.Component
import com.afollestad.ulfberht.sample.Main
import com.afollestad.ulfberht.sample.api.qualifiers.ApiKey
import com.afollestad.ulfberht.sample.api.scopes.AppScope

@Component(scope = AppScope::class)
interface AppComponent {

  @ApiKey fun apiToken(): String

  fun inject(main: Main)
}
