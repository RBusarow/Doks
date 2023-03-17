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

package com.rickbusarow.docusync.gradle

import com.rickbusarow.docusync.gradle.internal.registerOnce
import org.gradle.api.Plugin
import org.gradle.api.Project

/**  */
@Suppress("UnnecessaryAbstractClass")
abstract class DocusyncPlugin : Plugin<Project> {

  override fun apply(target: Project) {

    target.extensions.create("docusync", DocusyncExtension::class.java)

    target.tasks.registerOnce<DocusyncDocsTask>("docusyncCheck") { it.autoCorrect = false }
    target.tasks.registerOnce<DocusyncDocsTask>("docusyncFix") { it.autoCorrect = true }
    target.tasks.registerOnce<DocusyncParseTask>("docusyncParse")
  }
}
