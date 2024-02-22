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

import com.rickbusarow.doks.DoksTask
import com.rickbusarow.kgx.mustRunAfter
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.rickbusarow.mahout.root")
  alias(libs.plugins.moduleCheck)
  alias(libs.plugins.github.release)
  alias(libs.plugins.doks)
}

moduleCheck {
  deleteUnused = true
  checks.sortDependencies = true
}

doks {
  dokSet {
    docs("README.md", "CHANGELOG.md")

    val GROUP = mahoutProperties.group.get()

    sampleCodeSource(
      project(":doks-gradle-plugin").kotlinExtension.sourceSets
        .named("gradleTest")
        .map(KotlinSourceSet::kotlin)
    )

    rule("kotlin-dsl-config-simple") {
      replacement = sourceCode(
        fqName = "com.rickbusarow.doks.ConfigTest.`kotlin dsl config simple`.config",
        bodyOnly = true,
        codeBlockLanguage = "kotlin",
        attributes = "title=\"build.gradle.kts\""
      )
    }

    rule("groovy-dsl-config-simple") {
      replacement = sourceCode(
        fqName = "com.rickbusarow.doks.ConfigTest.`groovy dsl config simple`.config",
        bodyOnly = true,
        codeBlockLanguage = "groovy",
        attributes = "title=\"build.gradle\""
      )
    }

    rule("kotlin-dsl-config-code") {
      replacement = sourceCode(
        fqName = "com.rickbusarow.doks.ConfigTest.`kotlin dsl config code`.config",
        bodyOnly = true,
        codeBlockLanguage = "kotlin",
        attributes = "title=\"build.gradle.kts\""
      )
    }

    rule("groovy-dsl-config-code") {
      replacement = sourceCode(
        fqName = "com.rickbusarow.doks.ConfigTest.`groovy dsl config code`.config",
        bodyOnly = true,
        codeBlockLanguage = "groovy",
        attributes = "title=\"build.gradle\""
      )
    }

    rule("dollar-raw-string") {
      regex = "\${'$'}".escapeRegex()
      replacement = "$".escapeReplacement()
    }
    rule("buildConfig-version") {
      regex = "\${BuildConfig.version}".escapeRegex()
      val version = libs.versions.rickBusarow.doks.get()
      replacement = version.escapeReplacement()
    }
    rule("plugin-with-version") {
      val version = libs.versions.rickBusarow.doks.get()
      regex = gradlePluginWithVersion(GROUP)
      replacement = "$1$2$3$4${version.escapeReplacement()}$6"
    }
    rule("doks-group") {
      regex = "com\\.(?:rickbusarow|square|squareup)\\.doks"
      replacement = GROUP
    }
  }
}

subprojects.map {
  it.tasks.withType(KotlinCompile::class.java)
    .mustRunAfter(tasks.withType(DoksTask::class.java))
}
