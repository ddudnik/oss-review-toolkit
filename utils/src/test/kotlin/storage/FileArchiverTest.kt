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

import com.here.ort.utils.safeMkdirs

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class FileArchiverTest : StringSpec({
    "All files matching any of the patterns are archived" {
        val dir = createTempDir("ort")

        fun createFile(path: String) {
            val file = dir.resolve(path)
            file.parentFile.safeMkdirs()
            file.writeText(path)
        }

        createFile("a")
        createFile("b")
        createFile("c")
        createFile("d/a")
        createFile("d/b")
        createFile("d/c")
        createFile("d/d/a")
        createFile("d/d/b")
        createFile("d/d/c")

        val storageDir = createTempDir("ort")
        val storage = LocalFileStorage(storageDir)
        val archiver = FileArchiver(listOf("a", "**/a", "**/b"), storage)

        archiver.archive(dir, "archive")

        fun assertFileArchived(path: String) {
            val file = storageDir.resolve("archive/$path")
            file.isFile shouldBe true
            file.readText() shouldBe path
        }

        assertFileArchived("a")
        assertFileArchived("d/a")
        assertFileArchived("d/b")
        assertFileArchived("d/d/a")
        assertFileArchived("d/d/b")

        fun assertFileNotArchived(path: String) {
            val file = storageDir.resolve("archive/$path")
            file.exists() shouldBe false
        }

        assertFileNotArchived("b")
        assertFileNotArchived("c")
        assertFileNotArchived("d/c")
        assertFileNotArchived("d/d/c")
    }
})
