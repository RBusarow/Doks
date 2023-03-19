/*
 * Copyright (C) 2023 Rick Busarow
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

import builds.VERSION_NAME
import builds.isRealRootProject

plugins {
  id("module")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
}

val pluginId = "com.rickbusarow.docusync"
val pluginArtifactId = "docusync-gradle-plugin"
val moduleDescription = "the Docusync Gradle plugin"

@Suppress("UnstableApiUsage")
val pluginDeclaration: NamedDomainObjectProvider<PluginDeclaration> =
  gradlePlugin.plugins
    .register(pluginArtifactId) {
      id = pluginId
      displayName = "Docusync"
      implementationClass = "com.rickbusarow.docusync.gradle.DocusyncPlugin"
      version = VERSION_NAME
      description = moduleDescription
      tags.set(listOf("markdown", "documentation"))
    }

val shade by configurations.register("shadowCompileOnly")

module {
  autoService()
  serialization()
  shadow(shade)

  published(
    artifactId = pluginArtifactId,
    pomDescription = moduleDescription
  )

  publishedPlugin(pluginDeclaration = pluginDeclaration)
}

dependencies {

  compileOnly(gradleApi())

  implementation(libs.kotlin.compiler)

  val mainConfig = if (rootProject.isRealRootProject()) {
    shade.name
  } else {
    "implementation"
  }

  mainConfig(libs.jetbrains.markdown)
  mainConfig(libs.kotlinx.coroutines.core)
  mainConfig(libs.kotlinx.serialization.core)
  mainConfig(libs.kotlinx.serialization.json)

  testImplementation(libs.jetbrains.markdown)
  testImplementation(libs.junit.engine)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.params)
  testImplementation(libs.kotest.assertions.api)
  testImplementation(libs.kotest.assertions.core.jvm)
  testImplementation(libs.kotest.assertions.shared)
  testImplementation(libs.kotest.common)
  testImplementation(libs.kotest.extensions)
  testImplementation(libs.kotest.property.jvm)
  testImplementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.kotlinx.serialization.core)
  testImplementation(libs.kotlinx.serialization.json)
}
