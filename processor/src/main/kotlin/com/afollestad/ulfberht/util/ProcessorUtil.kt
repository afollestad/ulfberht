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
import com.afollestad.ulfberht.annotation.Qualifier
import com.afollestad.ulfberht.util.Names.MODULES_LIST_NAME
import com.afollestad.ulfberht.util.Types.VIEW_MODEL
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
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
import javax.lang.model.type.TypeKind.VOID
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.WARNING
import kotlin.reflect.KClass

/** @author Aidan Follestad (@afollestad) */
internal object ProcessorUtil {
  inline fun <reified T : Any> Element.getAnnotationMirror(): AnnotationMirror? {
    return annotationMirrors.firstOrNull { ann ->
      ann.annotationType.toString() == T::class.java.name
    }
  }

  inline fun <reified T : Any> Element.hasAnnotationMirror(): Boolean {
    return getAnnotationMirror<T>() != null
  }

  fun Element.getQualifier(): String? {
    return annotationMirrors.singleOrNull { ann ->
      ann.annotationType.asElement()
          .hasAnnotationMirror<Qualifier>()
    }
        ?.toString()
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

  fun Collection<Element>.injectingFields(
    env: ProcessingEnvironment
  ): Sequence<InjectingField> {
    return filterFields()
        .filter { it.hasAnnotationMirror<Inject>() }
        .map { it.asInjectField(env) }
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

  fun TypeElement.getConstructorParamsTypesAndArgs(
    env: ProcessingEnvironment
  ): Sequence<TypeAndArgs> {
    return getPrimaryConstructor()
        .parameters
        .asSequence()
        .map { it.asTypeAndArgs(env) }
  }

  fun ExecutableElement.getMethodParamsTypeAndArgs(
    env: ProcessingEnvironment
  ): Sequence<TypeAndArgs> {
    return parameters
        .asSequence()
        .map { it.asTypeAndArgs(env) }
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> AnnotationMirror.getParameter(name: String): T? {
    return elementValues.entries
        .singleOrNull { it.key.simpleName.toString() == name }
        ?.value?.value as? T
  }

  @Suppress("UNCHECKED_CAST")
  inline fun <reified T : Any> AnnotationMirror.getListParameter(
    name: String
  ): Sequence<T> {
    val annotationValues = elementValues.entries
        .singleOrNull { it.key.simpleName.toString() == name }
        ?.value?.value as? List<AnnotationValue> ?: return emptySequence()
    return annotationValues
        .asSequence()
        .map { it.value as T }
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
    return getListParameter<TypeMirror>(MODULES_LIST_NAME)
        .map {
          val element = it.asTypeElement()
          val pkg = element.getPackage(env)
          ClassName(pkg, "${element.simpleName}${Names.MODULE_NAME_SUFFIX}")
        }
  }

  fun TypeElement.hasInterface(
    env: ProcessingEnvironment,
    vararg interfaceTypes: TypeName
  ): Boolean {
    require(interfaceTypes.isNotEmpty())
    if (asType().isObject()) {
      return false
    }
    return interfaces.any { existingInterface ->
      interfaceTypes.any {
        it.toString() == existingInterface.toString()
      }
    } || superclass.asTypeElement().hasInterface(env, *interfaceTypes)
  }

  fun TypeElement.hasSuperClass(
    env: ProcessingEnvironment,
    vararg classTypes: TypeName
  ): Boolean {
    require(classTypes.isNotEmpty())
    if (asType().isObject()) {
      return false
    }
    val superClass = getSuperClass(env) ?: return false
    return classTypes.any { existingClass ->
      superclass.toString() == existingClass.toString()
    } || superClass.asTypeElement().hasSuperClass(env, *classTypes)
  }

  fun ProcessingEnvironment.error(message: String) {
    messager.printMessage(ERROR, message)
  }

  fun ProcessingEnvironment.warn(message: String) {
    messager.printMessage(WARNING, message)
  }

  fun <T : Any> T.applyIf(
    condition: Boolean,
    block: T.() -> Unit
  ): T {
    if (condition) {
      this.block()
    }
    return this
  }

  fun TypeMirror?.isVoid(): Boolean {
    if (this == null) {
      return false
    }
    return kind == VOID
  }

  val AnnotationMirror?.name: String?
    get() {
      val qualifier: String = this?.getParameter("name") ?: return null
      return if (qualifier.isEmpty()) null else qualifier
    }

  fun TypeMirror?.isViewModel(env: ProcessingEnvironment): Boolean {
    if (this == null) {
      return false
    }
    return toString() == VIEW_MODEL.toString() ||
        asTypeElement().getSuperClass(env).isViewModel(env)
  }

  private fun Element.getSuperClass(env: ProcessingEnvironment): TypeMirror? {
    return env.typeUtils.directSupertypes(this.asType())
        .asSequence()
        .filter { it.kind == DECLARED }
        .filterNot { it.isObject() }
        .filter { it.asTypeElement().kind == CLASS }
        .singleOrNull()
  }

  fun TypeMirror.correctTypeName(
    env: ProcessingEnvironment
  ): TypeName {
    var baseType: TypeMirror = this
    var genericTypes: Array<TypeName> = emptyArray()
    if (this is DeclaredType) {
      baseType = env.typeUtils.erasure(this)
      genericTypes = typeArguments
          .map { arg -> arg.correctTypeName(env) }
          .toTypedArray()
    }
    return when (baseType.toString()) {
      "java.lang.String" -> STRING
      "java.lang.Short" -> SHORT
      "java.lang.Integer" -> INT
      "java.lang.Long" -> LONG
      "java.lang.Float" -> FLOAT
      "java.lang.Double" -> DOUBLE
      "java.lang.Boolean" -> BOOLEAN
      "java.util.Set",
      "java.util.AbstractSet",
      "java.util.HashSet" -> SET.maybeParameterizedBy(genericTypes)
      "java.util.Map",
      "java.util.AbstractMap",
      "java.util.HashMap" -> MAP.maybeParameterizedBy(genericTypes)
      "java.util.List",
      "java.util.AbstractList",
      "java.util.ArrayList" -> LIST.maybeParameterizedBy(genericTypes)
      else -> if (genericTypes.isNotEmpty()) {
        baseType.asTypeElement()
            .asClassName()
            .parameterizedBy(*genericTypes)
      } else {
        asTypeName()
      }
    }
  }

  private fun ClassName.maybeParameterizedBy(args: Array<out TypeName>): TypeName {
    return if (args.isEmpty()) this else parameterizedBy(*args)
  }

  fun Element.isNullable(): Boolean {
    return getAnnotationMirror<Nullable>() != null &&
        getAnnotationMirror<NotNull>() == null
  }

  private fun String.lastComponent(): String = substring(lastIndexOf('.') + 1)

  private fun TypeMirror?.isObject(): Boolean {
    if (this == null) {
      return false
    }
    return toString() == "java.lang.Object"
  }
}
