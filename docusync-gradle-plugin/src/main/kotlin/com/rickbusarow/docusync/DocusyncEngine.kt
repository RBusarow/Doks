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

package com.rickbusarow.docusync

import com.rickbusarow.docusync.markdown.markdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File

/** */
class DocusyncEngine(
  private val ruleCache: Rules,
  private val autoCorrect: Boolean
) : java.io.Serializable {

  /** */
  fun run(files: List<File>): Boolean = runBlocking {

    files.map { file ->
      async(Dispatchers.Default) {

        file.markdown(
          rules = ruleCache,
          autoCorrect = autoCorrect
        )
      }
    }
      .awaitAll()
      .all { it }
  }

  /** */
  fun run(file: File): Boolean {
    return file.markdown(
      rules = ruleCache,
      autoCorrect = autoCorrect
    )
  }
}
