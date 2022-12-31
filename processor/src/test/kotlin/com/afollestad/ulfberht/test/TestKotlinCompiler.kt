package com.afollestad.ulfberht.test

import com.afollestad.ulfberht.UlfberhtProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.intellij.lang.annotations.Language
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

internal class TestKotlinCompiler(
  private val temporaryFolder: TemporaryFolder,
) {
  fun compile(vararg source: SourceFile): KotlinCompilation.Result =
    KotlinCompilation()
      .apply {
        sources = source.toList()
        symbolProcessorProviders = listOf(UlfberhtProcessorProvider())
        workingDir = temporaryFolder.root
        inheritClassPath = true
        verbose = false
      }
      .compile()
}

internal fun KotlinCompilation.Result.assertGeneratedSourceEquals(
  fileName: String,
  @Language("kotlin") expectedSource: String,
) = apply {
  assertEquals(
    KotlinCompilation.ExitCode.OK,
    exitCode,
    "Expected successful compilation exit code. $messages",
  )
  val actualSource = kspGeneratedSources().find { it.name == fileName }?.readText()
    ?: assertError("Could not find generated file with name: $fileName")
  assertEquals(
    expectedSource.trimIndent(),
    actualSource.trimIndent().replace("\t", "  "),
  )
}

private fun KotlinCompilation.Result.kspGeneratedSources(): List<File> {
  val kspWorkingDir = workingDir.resolve("ksp")
  val kspGeneratedDir = kspWorkingDir.resolve("sources")
  val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
  val javaGeneratedDir = kspGeneratedDir.resolve("java")
  return kotlinGeneratedDir.walk().toList() + javaGeneratedDir.walk().toList()
}

private val KotlinCompilation.Result.workingDir: File
  get() = checkNotNull(outputDirectory.parentFile)

private fun assertError(message: String): Nothing {
  throw AssertionError(message)
}
