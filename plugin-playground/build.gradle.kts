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

plugins {
  base
  id("com.rickbusarow.docusync")
}

docusync {

  docSet("main") {

    docs(
      fileTree(projectDir) {
        include("**/*.md", "**/*.mdx")
      }
    )

    sampleCodeSource.from(fileTree(projectDir.resolve("src/main/kotlin")))

    rule("code") {

      replacement = sourceCode(
        fqName = "com.example.foo.MyPlugin",
        bodyOnly = true,
        codeBlockLanguage = "kotlin",
        remark = "title=\"build.gradle.kts\""
      )
    }
  }
}

val createDocs by tasks.registering {

  val root = project.file("mds")

  doLast {

    root.deleteRecursively()

    root.mkdirs()

    repeat(5) {

      val file = root.resolve("markdown_$it.md")

      file.writeText(
        """
        ## My File $it

        <!--docusync cats-to-dogs,dogs-to-cats-->

        com.example.foo:foo-utils:1.2.3-SNAPSHOT

        cats

        category

        api 'com.rickbusarow.modulecheck:modulecheck-core:0.12.5'

        dog

        <!--/docusync-->

        <!--docusync code-->

        <!--/docusync-->

        <!--docusync code2-->
        <!--/docusync-->
        """.trimIndent()
      )
    }
  }
}
