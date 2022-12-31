@file:Suppress("RedundantSuppression", "ClassName", "RedundantVisibilityModifier")

package com.afollestad.ulfberht

import com.afollestad.ulfberht.test.Snippets
import com.afollestad.ulfberht.test.TestKotlinCompiler
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests specific to @Inject should go in [UlfberhtProcessorFactoryInjectTest].
 * Tests specific to @BindsTo should go in [UlfberhtProcessorFactoryBindsToTest].
 * Tests specific to @Component should go in [UlfberhtProcessorComponentTest].
 */
class UlfberhtProcessorFactoryTest {

  @Rule
  @JvmField
  var temporaryFolder: TemporaryFolder = TemporaryFolder()

  private val compiler = TestKotlinCompiler(temporaryFolder)

  @Test
  fun `compilation fails if there are duplicate non-qualified bindings`() {
    val compilationResult = compiler.compile(
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.annotation.Provides

        class AuthTokens @Inject constructor() {
          val currentToken: String = "fake-auth-token"
        }
        
        @Provides fun provideAuthTokens(): AuthTokens = AuthTokens()
        """,
      ),
    )
    assertEquals(COMPILATION_ERROR, compilationResult.exitCode)
    val expectedMessage = "e: [ksp] Duplicate bindings for Key(type=com.tests.ulfberht.AuthTokens, qualifier=null): " +
      "FactoryBinding(scope=null, factoryKey=Key(type=com.tests.ulfberht.AuthTokens_Factory, qualifier=null), " +
      "providedKey=Key(type=com.tests.ulfberht.AuthTokens, qualifier=null), parameters={}, isSingleton=false, " +
      "containingFile=File: file1.kt, factoryParameterName=authTokensFactory), ProviderBinding(functionName=" +
      "com.tests.ulfberht.provideAuthTokens, scope=null, factoryKey=null, providedKey=Key(" +
      "type=com.tests.ulfberht.AuthTokens, qualifier=null), parameters={}, isSingleton=false, " +
      "containingFile=File: file1.kt, factoryParameterName=authTokensProvider)"
    assertTrue(
      "Expected message containing text \"$expectedMessage\" but got: ${compilationResult.messages}",
    ) {
      compilationResult.messages.contains(expectedMessage)
    }
  }

  @Test
  fun `compilation fails if there are duplicate qualified bindings`() {
    val compilationResult = compiler.compile(
      Snippets.TestQualifier,
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.annotation.Provides
        
        @TestQualifier
        class AuthTokens @Inject constructor() {
          val currentToken: String = "fake-auth-token"
        }
        
        @Provides @TestQualifier 
        fun provideAuthTokens(): AuthTokens = AuthTokens()
        """,
      ),
    )
    assertEquals(COMPILATION_ERROR, compilationResult.exitCode)
    val expectedMessage = "e: [ksp] Duplicate bindings for Key(type=com.tests.ulfberht.AuthTokens, " +
      "qualifier=com.tests.ulfberht.TestQualifier): FactoryBinding(scope=null, factoryKey=Key(type=" +
      "com.tests.ulfberht.AuthTokens_TestQualifier_Factory, qualifier=com.tests.ulfberht.TestQualifier), " +
      "providedKey=Key(type=com.tests.ulfberht.AuthTokens, qualifier=com.tests.ulfberht.TestQualifier), " +
      "parameters={}, isSingleton=false, containingFile=File: file1.kt, " +
      "factoryParameterName=authTokensTestQualifierFactory), ProviderBinding(functionName=" +
      "com.tests.ulfberht.provideAuthTokens, scope=null, factoryKey=null, providedKey=" +
      "Key(type=com.tests.ulfberht.AuthTokens, qualifier=com.tests.ulfberht.TestQualifier), " +
      "parameters={}, isSingleton=false, containingFile=File: file1.kt, " +
      "factoryParameterName=authTokensTestQualifierProvider)"
    assertTrue(
      "Expected message containing text \"$expectedMessage\" but got: ${compilationResult.messages}",
    ) {
      compilationResult.messages.contains(expectedMessage)
    }
  }

  @Test
  fun `@Provides and @BindsTo don't conflict if they use different qualifiers`() {
    val compilationResult = compiler.compile(
      Snippets.TestQualifier,
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.annotation.Provides

        class AuthTokens @Inject constructor() {
          val currentToken: String = "fake-auth-token"
        }
        
        @Provides @TestQualifier 
        fun provideAuthTokens(): AuthTokens = AuthTokens()
        """,
      ),
    )
    assertEquals(OK, compilationResult.exitCode)
  }
}
