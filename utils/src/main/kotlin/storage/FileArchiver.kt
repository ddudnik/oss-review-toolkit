/*
 * Copyright (C) 2019 HERE Europe B.V.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.ort.utils.storage

import com.here.ort.utils.log

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths

/**
 * A class to archive files matched by provided [patterns] in a [FileStorage][storage].
 */
class FileArchiver(
    /**
     * A list of globs to match the paths of files that shall be archived. For details about the glob pattern see
     * [java.nio.file.FileSystem.getPathMatcher].
     */
    val patterns: List<String>,

    /**
     * The [FileStorage] to use for archiving files.
     */
    val storage: FileStorage
) {
    private val pathMatchers by lazy { patterns.map { FileSystems.getDefault().getPathMatcher("glob:$it") } }

    /**
     * Archive all files in [directory] matching any of the configured [patterns] in the [storage]. The path of the
     * archived file relative to [directory] is appended to [storagePath].
     */
    fun archive(directory: File, storagePath: String) {
        directory.walkTopDown().map { file ->
            val relativePath = file.relativeTo(directory).path
            Pair(file, relativePath)
        }.filter { (_, relativePath) ->
            pathMatchers.any { it.matches(Paths.get(relativePath)) }
        }.forEach { (file, relativePath) ->
            log.debug { "Archiving file $relativePath." }
            storage.write("$storagePath/$relativePath", file.inputStream())
        }
    }
}
