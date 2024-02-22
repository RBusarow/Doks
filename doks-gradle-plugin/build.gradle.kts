/*
 * Copyright (C) 2024 Rick Busarow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.rickbusarow.kgx.isRealRootProject

plugins {
  id("com.rickbusarow.mahout.kotlin-jvm-module")
  // id("com.rickbusarow.mahout.java-gradle-plugin")
  id("com.rickbusarow.mahout.gradle-test")
  id("java-gradle-plugin")
  id("dev.drewhamilton.poko")

  alias(libs.plugins.buildconfig)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.gradle.plugin.publish)

  idea
}

val pluginId = "com.rickbusarow.doks"
val pluginArtifactId = "doks-gradle-plugin"
val moduleDescription = "the Doks Gradle plugin"

@Suppress("UnstableApiUsage")
val pluginDeclaration: NamedDomainObjectProvider<PluginDeclaration> =
  gradlePlugin.plugins
    .register(pluginArtifactId) {
      id = pluginId
      displayName = "Doks"
      implementationClass = "com.rickbusarow.doks.DoksPlugin"
      version = mahoutProperties.versionName.get()
      description = moduleDescription
      this@register.tags.set(listOf("markdown", "documentation"))
    }

val shade by configurations.register("shadowCompileOnly")

mahout {
  // serialization()
  // shadow(shade)

  // publishPlugin(pluginDeclaration)

  publishing {
    publishMaven(
      artifactId = pluginArtifactId,
      pomDescription = moduleDescription
    )

    // publishedPlugin(pluginDeclaration = pluginDeclaration)
  }
}

buildConfig {

  this@buildConfig.sourceSets.named("gradleTest") {

    packageName(mahoutProperties.group.get())
    className("BuildConfig")

    useKotlinOutput {
      internalVisibility = true
    }

    val buildM2 = rootProject.layout.buildDirectory.dir("m2").map { it.asFile }
    buildConfigField("localBuildM2Dir", buildM2)

    buildConfigField("pluginId", pluginId)
    buildConfigField("version", mahoutProperties.versionName.get())
    buildConfigField("doksVersion", mahoutProperties.versionName.get())
    buildConfigField("kotlinVersion", libs.versions.kotlin)
    buildConfigField("gradleVersion", gradle.gradleVersion)
  }
}

val mainConfig: String = if (rootProject.isRealRootProject()) {
  shade.name
} else {
  "implementation"
}

val gradleTestImplementation by configurations
val testImplementation by configurations

dependencies {

  compileOnly(gradleApi())

  for (testImpl in listOf(testImplementation, gradleTestImplementation)) {

    testImpl(libs.junit.jupiter)
    testImpl(libs.junit.jupiter.api)
    testImpl(libs.kotest.assertions.core.jvm)
    testImpl(libs.kotest.assertions.shared)
    testImpl(libs.kotest.common)
    testImpl(libs.kotest.property.jvm)
    testImpl(libs.rickBusarow.kase)
  }

  gradleTestImplementation(libs.kotlin.compiler)
  gradleTestImplementation(libs.rickBusarow.kase.gradle)
  gradleTestImplementation(libs.rickBusarow.kase.gradle.dsl)

  mainConfig(libs.java.diff.utils)
  mainConfig(libs.jetbrains.markdown)
  mainConfig(libs.kotlin.compiler)
  mainConfig(libs.kotlinx.coroutines.core)
  mainConfig(libs.kotlinx.serialization.core)
  mainConfig(libs.kotlinx.serialization.json)

  testImplementation(libs.java.diff.utils)
  testImplementation(libs.jetbrains.markdown)
  testImplementation(libs.kotlin.compiler)
  testImplementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.kotlinx.serialization.core)
  testImplementation(libs.kotlinx.serialization.json)
}
