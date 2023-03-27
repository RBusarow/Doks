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

import com.rickbusarow.docusync.DocusyncEngine
import com.rickbusarow.docusync.Rule
import com.rickbusarow.docusync.Rules
import com.rickbusarow.docusync.internal.createSafely
import com.rickbusarow.docusync.internal.parents
import com.rickbusarow.docusync.psi.DocusyncPsiFileFactory
import com.rickbusarow.docusync.psi.NamedSamples
import com.rickbusarow.docusync.psi.SampleRequest
import com.rickbusarow.docusync.psi.SampleResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.ChangeType.REMOVED
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.konan.file.File
import javax.inject.Inject

/** The base class for all Docusync tasks. */
@Suppress("UnnecessaryAbstractClass")
abstract class DocusyncTask(description: String) : DefaultTask() {
  init {
    group = "Docusync"
    this.description = description
  }

  @delegate:Transient
  @get:Internal
  protected val json: Json by lazy {
    Json {
      prettyPrint = true
      allowStructuredMapKeys = true
    }
  }
}

/**
 * Parses source code in [sampleCode] in order to find all requested samples. The results are written
 * to [samplesMapping].
 */
abstract class DocusyncParseTask : DocusyncTask("Parses source files for requested samples") {

  /**
   * The requests to be parsed by this task. The content of [samplesMapping] will hold these requests
   * and their results.
   */
  @get:Input
  internal abstract val sampleRequests: ListProperty<SampleRequest>

  /**
   * The sample code sources for this source set. This is a [ConfigurableFileCollection], meaning that
   * it can be dynamically configured.
   */
  @get:InputFiles
  @get:SkipWhenEmpty
  abstract val sampleCode: ConfigurableFileCollection

  /**
   * This JSON file is generated by the [DocusyncDocsTask] tasks associated with this source set. It is
   * a map of fully-qualified names of code samples to their corresponding code snippets.
   */
  @get:OutputFile
  abstract val samplesMapping: RegularFileProperty

  /**  */
  @TaskAction
  fun execute() {

    val namedSamples = NamedSamples(DocusyncPsiFileFactory())

    val requests = sampleRequests.get()
      .map { SampleRequest(it.fqName, it.bodyOnly) }

    val results = namedSamples.findAll(sampleCode.files.toList(), requests)
      .map { SampleResult(request = it.request, content = it.content) }

    val jsonString = json.encodeToString(results.associateBy { it.request })

    with(samplesMapping.get().asFile) {
      parentFile?.mkdirs()
      writeText(jsonString)
    }
  }
}

/** Either checks or fixes/updated documentation files for a given [DocusyncSourceSet]. */
abstract class DocusyncDocsTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
  objects: ObjectFactory
) : DocusyncTask("Updates documentation files") {
  /**
   * This JSON file is generated by the `docusyncParse` task associated with this source set. It is a
   * map of fully-qualified names of code samples to their corresponding code snippets.
   */
  @get:Optional
  @get:InputFile
  abstract val samplesMapping: RegularFileProperty

  /**
   * The directory where the updated documentation files will be written during the execution of this
   * task.
   *
   * This property serves as a workaround for the limitations of incremental tasks, which can't have
   * the same inputs as their outputs. Since this task is essentially a "formatting" task that modifies
   * input files, we can't use the actual input files as outputs without risking that they will be
   * deleted by Gradle in case of a binary change to the plugin or build environment. Instead, we use
   * this property to declare a separate directory as the output of this task.
   *
   * Any time this task writes changes to an input file, it also creates a stub file with the same
   * relative path inside the docsShadow directory. During the next incremental build, the task will
   * only need to update the real input files that have changed since the last build, and the contents
   * of the docsShadow directory will be ignored.
   *
   * Note that the contents of the docsShadow directory are not meant to be used by other tasks or
   * processes, and should not be relied on as a source of truth for the documentation files. Its sole
   * purpose is to allow this task to run incrementally without interfering with other tasks that might
   * need to use the same files.
   */
  @get:OutputDirectory
  internal abstract val docsShadow: DirectoryProperty

  /**
   * A file collection containing all the Markdown files included in this source set. These files are
   * not generated, but rather describe the properties and code in the source set. They are checked to
   * ensure that they are up-to-date with the source code and other configuration files. If the
   * properties or code they're describing have changed, the markdown files are updated to reflect
   * those changes.
   */
  @get:Incremental
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val docs: ConfigurableFileCollection

  @get:Input
  internal abstract val ruleBuilders: NamedDomainObjectContainer<RuleBuilderScope>

  private val autoCorrectProperty: Property<Boolean> = objects.property(Boolean::class.java)
    .convention(false)

  /** If true, Docusync will automatically fix any out-of-date documentation. */
  @set:Option(
    option = "autoCorrect",
    description = "If true, Docusync will automatically fix any out-of-date documentation."
  )
  var autoCorrect: Boolean
    @Input
    get() = autoCorrectProperty.get()
    set(value) = autoCorrectProperty.set(value)

  /**  */
  @TaskAction
  fun execute(inputChanges: InputChanges) {

    val resultsByRequest = samplesMapping.orNull
      ?.asFile
      ?.readText()
      ?.let { jsonString -> json.decodeFromString<Map<SampleRequest, SampleResult>>(jsonString) }
      .orEmpty()

    val resultsByRequestHash = resultsByRequest
      .mapKeys { (request, _) -> request.hashCode() }

    val rules = ruleBuilders.toList()
      .map {
        val withSamples = it.replacement.replace("\u200B(-?\\d+)\u200B".toRegex()) { mr ->
          resultsByRequestHash.getValue(mr.groupValues[1].toInt()).content
        }
        Rule(
          name = it.name,
          regex = it.requireRegex(),
          replacement = withSamples
        )
      }
      .associateBy { it.name }

    val engine = DocusyncEngine(
      ruleCache = Rules(rules),
      autoCorrect = autoCorrect
    )

    val queue = workerExecutor.classLoaderIsolation()

    val changes = inputChanges.getFileChanges(docs)
      .mapNotNull { fileChange ->
        fileChange.file.takeIf {
          fileChange.changeType != REMOVED && fileChange.fileType == FileType.FILE
        }
      }

    if (changes.isNotEmpty()) {
      docsShadow.get().asFile.mkdirs()
    }

    changes.forEach { file ->

      val relative = file.relativeTo(docsShadow.get().asFile)
        .normalize()
        .parents()
        .takeWhile { it.name != ".." && it.name.isNotBlank() }
        .toList()
        .reversed()
        .joinToString(File.separator) { it.name }

      queue.submit(DocsWorkAction::class.java) { params ->
        params.docusyncEngine.set(engine)
        params.file.set(file)
        params.outFile.set(docsShadow.get().file(relative))
      }
    }

    queue.await()
  }

  internal interface DocsParameters : WorkParameters {

    val docusyncEngine: Property<DocusyncEngine>

    /**
     * The real doc file to be parsed/synced.
     *
     * @since 0.0.1
     */
    val file: RegularFileProperty

    /**
     * The shadowed file to be created by this worker. Its name should be a relative path of the
     * associated real file, and the content should be a hash code, though it could really be anything
     * or nothing.
     *
     * @since 0.0.1
     */
    val outFile: RegularFileProperty
  }

  internal abstract class DocsWorkAction : WorkAction<DocsParameters> {
    override fun execute() {

      val engine = parameters.docusyncEngine.get()

      val file = parameters.file.get().asFile

      val result = engine.run(file)

      parameters.outFile.get().asFile
        .createSafely("${result.newText.hashCode()}")
    }
  }
}
