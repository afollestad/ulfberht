@file:Suppress("RedundantSuppression", "ClassName", "RedundantVisibilityModifier")

package com.afollestad.ulfberht

import com.afollestad.ulfberht.test.Snippets
import com.afollestad.ulfberht.test.TestKotlinCompiler
import com.afollestad.ulfberht.test.assertGeneratedSourceEquals
import com.tschuchort.compiletesting.SourceFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UlfberhtProcessorComponentTest {

  @Rule
  @JvmField
  var temporaryFolder: TemporaryFolder = TemporaryFolder()

  private val compiler = TestKotlinCompiler(temporaryFolder)

  @Test
  fun `a interface annotated with @Component generates a Component with an inject method`() {
    val compilationResult = compiler.compile(
      Snippets.AppScope,
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Component
        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.component

        class Client @Inject constructor() {
          fun doSomething() {
            println("Hello, world!")
          }
        }

        class Main {
          @Inject lateinit var client: Client
          
          init {
            component<AppComponent>().inject(this)
          }
          
          fun run() {
            client.doSomething()
          }
        }

        @Component
        interface AppComponent {
          fun inject(main: Main)
        }
        """,
      ),
    )
    compilationResult.assertGeneratedSourceEquals(
      fileName = "AppComponent_Component.kt",
      expectedSource = """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.Factory
        import com.afollestad.ulfberht.api.ComponentImpl
        import kotlin.Unit
        import kotlin.reflect.KClass
        
        /**
         * Generated by [Ulfberht](https://github.com/afollestad/ulfberht)
         */
        public class AppComponent_Component : ComponentImpl, AppComponent {
          public override val scope: KClass<*>? = null
        
          public override val parent: ComponentImpl? = null
        
          private val clientFactory: Factory<Client> by lazy {
            Client_Factory()
          }
        
          public override fun inject(main: Main): Unit {
            main.client = clientFactory.create()
          }
        
          public override fun destroy(): Unit {
            clientFactory.destroy()
          }
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `a interface annotated with @Component generates a Component with a getter method for an unqualified Provider`() {
    val compilationResult = compiler.compile(
      Snippets.AppScope,
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Component
        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.annotation.Provides
        import com.afollestad.ulfberht.component

        data class ApiCredentials(val key: String)
        
        @Provides fun providesApiCredentials(): ApiCredentials = ApiCredentials("fake-api-key")

        @Component
        interface AppComponent {
          fun apiCredentials(): ApiCredentials
        }

        fun main() {
          println(component<AppComponent>().apiCredentials())
        }
        """,
      ),
    )
    compilationResult.assertGeneratedSourceEquals(
      fileName = "AppComponent_Component.kt",
      expectedSource = """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.Provider
        import com.afollestad.ulfberht.api.ComponentImpl
        import com.afollestad.ulfberht.provider
        import kotlin.Unit
        import kotlin.reflect.KClass
        
        /**
         * Generated by [Ulfberht](https://github.com/afollestad/ulfberht)
         */
        public class AppComponent_Component : ComponentImpl, AppComponent {
          public override val scope: KClass<*>? = null
        
          public override val parent: ComponentImpl? = null
        
          private val apiCredentialsProvider: Provider<ApiCredentials> by lazy {
            provider {
              providesApiCredentials()
            }
          }
        
          public override fun apiCredentials(): ApiCredentials = apiCredentialsProvider.get()
        
          public override fun destroy(): Unit {
            apiCredentialsProvider.destroy()
          }
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `a interface annotated with @Component generates a Component with a getter method for a qualified Provider`() {
    val compilationResult = compiler.compile(
      Snippets.AppScope,
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Component
        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.annotation.Provides
        import com.afollestad.ulfberht.annotation.Qualifier
        import com.afollestad.ulfberht.component

        @Qualifier
        interface AuthToken
        
        @Provides @AuthToken
        fun providesAuthToken(): String = "fake-auth-token"

        fun main() {
          println(component<AppComponent>().authToken())
        }

        @Component
        interface AppComponent {
          @AuthToken
          fun authToken(): String
        }
        """,
      ),
    )
    compilationResult.assertGeneratedSourceEquals(
      fileName = "AppComponent_Component.kt",
      expectedSource = """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.Provider
        import com.afollestad.ulfberht.api.ComponentImpl
        import com.afollestad.ulfberht.provider
        import kotlin.String
        import kotlin.Unit
        import kotlin.reflect.KClass
        
        /**
         * Generated by [Ulfberht](https://github.com/afollestad/ulfberht)
         */
        public class AppComponent_Component : ComponentImpl, AppComponent {
          public override val scope: KClass<*>? = null
        
          public override val parent: ComponentImpl? = null
        
          private val stringAuthTokenProvider: Provider<String> by lazy {
            provider {
              providesAuthToken()
            }
          }
        
          @AuthToken
          public override fun authToken(): String = stringAuthTokenProvider.get()
        
          public override fun destroy(): Unit {
            stringAuthTokenProvider.destroy()
          }
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `a interface annotated with @Component generates a Component with a getter method for an unqualified Factory with no parameters`() {
    val compilationResult = compiler.compile(
      Snippets.AppScope,
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Component
        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.component

        class Client @Inject constructor() {
          fun doSomething() = Unit
        }
        
        @Component
        interface AppComponent {
          fun client(): Client
        }

        fun main() {
          println(component<AppComponent>().client())
        }
        """,
      ),
    )
    compilationResult.assertGeneratedSourceEquals(
      fileName = "Client_Factory.kt",
      expectedSource = """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.Factory
        
        /**
         * Generated by [Ulfberht](https://github.com/afollestad/ulfberht)
         */
        public class Client_Factory : Factory<Client> {
          public override fun create(): Client = Client()
        
          public override fun destroy() = Unit
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `a interface annotated with @Component generates a Component with a getter method for a qualified Factory with no parameters`() {
    val compilationResult = compiler.compile(
      Snippets.TestQualifier,
      Snippets.AppScope,
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Component
        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.annotation.Provides
        import com.afollestad.ulfberht.component

        @TestQualifier
        class Client @Inject constructor() {
          fun doSomething() = Unit
        }
        
        @Component
        interface AppComponent {
          @TestQualifier
          fun client(): Client
        }

        fun main() {
          println(component<AppComponent>().client())
        }
        """,
      ),
    )
    compilationResult.assertGeneratedSourceEquals(
      fileName = "AppComponent_Component.kt",
      expectedSource = """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.Factory
        import com.afollestad.ulfberht.api.ComponentImpl
        import kotlin.Unit
        import kotlin.reflect.KClass
        
        /**
         * Generated by [Ulfberht](https://github.com/afollestad/ulfberht)
         */
        public class AppComponent_Component : ComponentImpl, AppComponent {
          public override val scope: KClass<*>? = null
        
          public override val parent: ComponentImpl? = null
        
          private val clientTestQualifierFactory: Factory<Client> by lazy {
            Client_TestQualifier_Factory()
          }
        
          @TestQualifier
          public override fun client(): Client = clientTestQualifierFactory.create()
        
          public override fun destroy(): Unit {
            clientTestQualifierFactory.destroy()
          }
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `a interface annotated with @Component generates a Component with a getter method for an unqualified Factory with parameters`() {
    val compilationResult = compiler.compile(
      Snippets.AppScope,
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Component
        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.annotation.Provides
        import com.afollestad.ulfberht.component

        data class ApiCredentials(val key: String)
        
        @Provides fun provideApiCredentials(): ApiCredentials = ApiCredentials("fake-api-key")

        class Client @Inject constructor(
          val apiCredentials: ApiCredentials,
        ) {
          fun doSomething() {
            println("Credentials: $\{apiCredentials\}")
          }
        }
        
        @Component
        interface AppComponent {
          fun client(): Client
        }

        fun main() {
          println(component<AppComponent>().client())
        }
        """,
      ),
    )
    compilationResult.assertGeneratedSourceEquals(
      fileName = "AppComponent_Component.kt",
      expectedSource = """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.Factory
        import com.afollestad.ulfberht.Provider
        import com.afollestad.ulfberht.api.ComponentImpl
        import com.afollestad.ulfberht.provider
        import kotlin.Unit
        import kotlin.reflect.KClass
        
        /**
         * Generated by [Ulfberht](https://github.com/afollestad/ulfberht)
         */
        public class AppComponent_Component : ComponentImpl, AppComponent {
          public override val scope: KClass<*>? = null
        
          public override val parent: ComponentImpl? = null
        
          private val clientFactory: Factory<Client> by lazy {
            Client_Factory(
              apiCredentialsProvider = apiCredentialsProvider,
            )
          }
        
          private val apiCredentialsProvider: Provider<ApiCredentials> by lazy {
            provider {
              provideApiCredentials()
            }
          }
        
          public override fun client(): Client = clientFactory.create()
        
          public override fun destroy(): Unit {
            clientFactory.destroy()
            apiCredentialsProvider.destroy()
          }
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `a interface annotated with @Component generates a Component with a getter method for a qualified Factory with parameters`() {
    val compilationResult = compiler.compile(
      Snippets.AppScope,
      Snippets.TestQualifier,
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Component
        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.annotation.Provides
        import com.afollestad.ulfberht.component

        data class ApiCredentials(val key: String)
        
        @Provides fun provideApiCredentials(): ApiCredentials = ApiCredentials("fake-api-key")

        @TestQualifier
        class Client @Inject constructor(
          val apiCredentials: ApiCredentials,
        ) {
          fun doSomething() {
            println("Credentials: $\{apiCredentials\}")
          }
        }
        
        @Component
        interface AppComponent {
          @TestQualifier
          fun client(): Client
        }

        fun main() {
          println(component<AppComponent>().client())
        }
        """,
      ),
    )
    compilationResult.assertGeneratedSourceEquals(
      fileName = "AppComponent_Component.kt",
      expectedSource = """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.Factory
        import com.afollestad.ulfberht.Provider
        import com.afollestad.ulfberht.api.ComponentImpl
        import com.afollestad.ulfberht.provider
        import kotlin.Unit
        import kotlin.reflect.KClass
        
        /**
         * Generated by [Ulfberht](https://github.com/afollestad/ulfberht)
         */
        public class AppComponent_Component : ComponentImpl, AppComponent {
          public override val scope: KClass<*>? = null
        
          public override val parent: ComponentImpl? = null
        
          private val clientTestQualifierFactory: Factory<Client> by lazy {
            Client_TestQualifier_Factory(
              apiCredentialsProvider = apiCredentialsProvider,
            )
          }
        
          private val apiCredentialsProvider: Provider<ApiCredentials> by lazy {
            provider {
              provideApiCredentials()
            }
          }
        
          @TestQualifier
          public override fun client(): Client = clientTestQualifierFactory.create()
        
          public override fun destroy(): Unit {
            clientTestQualifierFactory.destroy()
            apiCredentialsProvider.destroy()
          }
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `a interface annotated with @Component generates a Component with a getter method for a singleton Provider`() {
    val compilationResult = compiler.compile(
      Snippets.AppScope,
      SourceFile.kotlin(
        "file1.kt",
        """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.annotation.Component
        import com.afollestad.ulfberht.annotation.Inject
        import com.afollestad.ulfberht.annotation.Provides
        import com.afollestad.ulfberht.annotation.Singleton
        import com.afollestad.ulfberht.component

        data class ApiCredentials(val key: String)
        
        @Provides @Singleton
        fun provideApiCredentials(): ApiCredentials = ApiCredentials("fake-api-key")

        class Client @Inject constructor(
          val apiCredentials: ApiCredentials,
        ) {
          fun doSomething() {
            println("Credentials: $\{apiCredentials\}")
          }
        }
        
        @Component
        interface AppComponent {
          fun client(): Client
        }

        fun main() {
          println(component<AppComponent>().client())
        }
        """,
      ),
    )
    compilationResult.assertGeneratedSourceEquals(
      fileName = "AppComponent_Component.kt",
      expectedSource = """
        package com.tests.ulfberht

        import com.afollestad.ulfberht.Factory
        import com.afollestad.ulfberht.Provider
        import com.afollestad.ulfberht.api.ComponentImpl
        import com.afollestad.ulfberht.singletonProvider
        import kotlin.Unit
        import kotlin.reflect.KClass
        
        /**
         * Generated by [Ulfberht](https://github.com/afollestad/ulfberht)
         */
        public class AppComponent_Component : ComponentImpl, AppComponent {
          public override val scope: KClass<*>? = null
        
          public override val parent: ComponentImpl? = null
        
          private val clientFactory: Factory<Client> by lazy {
            Client_Factory(
              apiCredentialsProvider = apiCredentialsProvider,
            )
          }
        
          private val apiCredentialsProvider: Provider<ApiCredentials> by lazy {
            singletonProvider {
              provideApiCredentials()
            }
          }
        
          public override fun client(): Client = clientFactory.create()
        
          public override fun destroy(): Unit {
            clientFactory.destroy()
            apiCredentialsProvider.destroy()
          }
        }
      """.trimIndent(),
    )
  }
}