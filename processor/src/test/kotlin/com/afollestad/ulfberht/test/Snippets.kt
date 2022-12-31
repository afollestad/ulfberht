package com.afollestad.ulfberht.test

import com.tschuchort.compiletesting.SourceFile

object Snippets {

  val AppScope = SourceFile.kotlin(
    name = "AppScope.kt",
    contents = """
      package com.tests.ulfberht
      
      import com.afollestad.ulfberht.annotation.Scope
      
      @Scope
      interface AppScope
    """.trimIndent(),
  )

  val TestQualifier = SourceFile.kotlin(
    name = "TestQualifier.kt",
    contents = """
      package com.tests.ulfberht
      
      import com.afollestad.ulfberht.annotation.Qualifier
      
      @Qualifier
      interface TestQualifier
    """.trimIndent(),
  )
}
