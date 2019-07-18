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
package com.afollestad.ulfberht.util

import com.afollestad.ulfberht.annotation.Inject
import com.afollestad.ulfberht.annotation.Param
import com.afollestad.ulfberht.util.Names.MODULES_LIST_NAME
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementKind.CLASS
import javax.lang.model.element.ElementKind.CONSTRUCTOR
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind.DECLARED
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

/** @author Aidan Follestad (@afollestad) */
internal object ProcessorUtil {
  private fun String.lastComponent(): String = substring(lastIndexOf('.') + 1)

  inline fun <reified T : Any> Element.getAnnotationMirror(): AnnotationMirror? {
    return annotationMirrors.firstOrNull { ann ->
      ann.annotationType.toString() == T::class.java.name
    }
  }

  inline fun <reified T : Any> Element.hasAnnotationMirror(): Boolean {
    return getAnnotationMirror<T>() != null
  }

  fun VariableElement.getFieldTypeName(): TypeName {
    val nullable = getAnnotationMirror<Nullable>() != null &&
        getAnnotationMirror<NotNull>() == null
    return asType()
        .correct()
        .copy(nullable = nullable)
  }

  fun Element.getFullClassName(
    env: ProcessingEnvironment,
    pkg: String
  ): ClassName {
    val simpleClassName = this.simpleName.toString()
    val superClass = getSuperClass(env)
    val fullClassName = if (superClass != null) {
      "${superClass.asTypeName().toString().lastComponent()}.$simpleClassName"
    } else {
      simpleClassName
    }
    return ClassName(pkg, fullClassName)
  }

  private fun Collection<Element>.filterFields(): Sequence<VariableElement> {
    return asSequence().filter { it.kind == ElementKind.FIELD }
        .map { it as VariableElement }
  }

  fun Collection<Element>.injectedFieldsAndQualifiers(): Sequence<Pair<VariableElement, String?>> {
    return filterFields()
        .filter { it.getAnnotationMirror<Inject>() != null }
        .map { Pair(it, it.getAnnotationMirror<Inject>()!!.getParameter<String>("qualifier")) }
  }

  fun Collection<Element>.filterMethods(): Sequence<ExecutableElement> {
    return asSequence()
        .filter { it.kind == ElementKind.METHOD }
        .map { it as ExecutableElement }
  }

  fun Element.getPackage(env: ProcessingEnvironment): String {
    return env.elementUtils.getPackageOf(this)
        .toString()
  }

  private fun Element.getSuperClass(env: ProcessingEnvironment): TypeMirror? {
    return env.typeUtils.directSupertypes(this.asType())
        .asSequence()
        .filter { it.kind == DECLARED }
        .filterNot { it.asTypeName().toString() == "java.lang.Object" }
        .singleOrNull()
  }

  fun TypeMirror.correct(): TypeName {
    return if (toString() == "java.lang.String") {
      ClassName("kotlin", "String")
    } else {
      asTypeName()
    }
  }

  fun Element.isAbstractClass(): Boolean {
    return kind == CLASS && ABSTRACT in modifiers
  }

  fun TypeMirror.asTypeElement(): TypeElement {
    return (this as DeclaredType).asElement() as TypeElement
  }

  private fun TypeElement.getPrimaryConstructor(): ExecutableElement {
    val primaryConstructor = enclosedElements
        .asSequence()
        .filter { it.kind == CONSTRUCTOR }
        .firstOrNull()
    return primaryConstructor as? ExecutableElement ?: error(
        "Type ${this.simpleName} must have a primary constructor."
    )
  }

  fun TypeElement.getConstructorParamsAndQualifiers(): List<Pair<TypeName, String?>> {
    return getPrimaryConstructor()
        .parameters
        .map {
          val qualifier = it.getAnnotationMirror<Param>()
              ?.qualifier
          Pair(it.getFieldTypeName(), qualifier)
        }
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> AnnotationMirror.getParameter(name: String): T? {
    return elementValues.entries
        .singleOrNull { it.key.simpleName.toString() == name }
        ?.value?.value as? T
  }

  @Suppress("UNCHECKED_CAST")
  fun AnnotationMirror.getListParameter(name: String): List<AnnotationValue> {
    return elementValues.entries
        .single { it.key.simpleName.toString() == name }
        .value.value as List<AnnotationValue>
  }

  fun KClass<*>.asNullableTypeName(): TypeName {
    return asTypeName().copy(nullable = true)
  }

  fun Collection<Element>.filterClassesAndInterfaces(): Sequence<Element> {
    return asSequence().filter { it.kind == CLASS || it.kind == ElementKind.INTERFACE }
  }

  fun ClassName.asFileName(suffix: String): String {
    return "${simpleName.replace(".", "_")}$suffix"
  }

  fun AnnotationMirror.getModulesTypes(env: ProcessingEnvironment): Sequence<TypeName> {
    return getListParameter(MODULES_LIST_NAME)
        .asSequence()
        .map { it.value as TypeMirror }
        .map {
          val element = it.asTypeElement()
          val pkg = element.getPackage(env)
          ClassName(pkg, "${element.simpleName}${Names.MODULE_NAME_SUFFIX}")
        }
  }

  val AnnotationMirror?.qualifier: String?
    get() {
      val qualifier: String = this?.getParameter("qualifier") ?: return null
      return if (qualifier.isEmpty()) null else qualifier
    }
}
