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

import com.rickbusarow.docusync.psi.DocusyncPsiFileFactory
import com.rickbusarow.docusync.psi.NamedSamples
import com.rickbusarow.docusync.psi.SampleRequest
import com.rickbusarow.docusync.psi.SampleResult
import kotlinx.serialization.encodeToString
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

/**
 * Parses source code in [sampleCode] in order to find all requested samples. The results are written
 * to [samplesMapping].
 *
 * @since 0.1.0
 */
abstract class DocusyncParseTask : DocusyncTask("Parses source files for requested samples") {

  /**
   * The requests to be parsed by this task. The content of [samplesMapping] will hold these requests
   * and their results.
   *
   * @since 0.1.0
   */
  @get:Input
  internal abstract val sampleRequests: ListProperty<SampleRequest>

  /**
   * The sample code sources for this source set. This is a [ConfigurableFileCollection], meaning that
   * it can be dynamically configured.
   *
   * @since 0.1.0
   */
  @get:InputFiles
  @get:SkipWhenEmpty
  abstract val sampleCode: ConfigurableFileCollection

  /**
   * This JSON file is generated by the [DocusyncDocsTask] tasks associated with this source set. It is
   * a map of fully-qualified names of code samples to their corresponding code snippets.
   *
   * @since 0.1.0
   */
  @get:OutputFile
  abstract val samplesMapping: RegularFileProperty

  /** @since 0.1.0 */
  @TaskAction
  fun execute() {

    val namedSamples = NamedSamples(DocusyncPsiFileFactory())

    val requests = sampleRequests.get()
      .map { SampleRequest(it.fqName, it.bodyOnly) }

    val results = namedSamples.findAll(sampleCode.filter { it.isFile }.files, requests)
      .map { SampleResult(request = it.request, content = it.content) }

    val jsonString = json.encodeToString(results.associateBy { it.request })

    with(samplesMapping.get().asFile) {
      parentFile?.mkdirs()
      writeText(jsonString)
    }
  }
}
