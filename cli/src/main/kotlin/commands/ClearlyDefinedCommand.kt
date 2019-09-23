/*
 * Copyright (C) 2019 Bosch Software Innovations GmbH
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

package com.here.ort.commands

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

import com.here.ort.CommandWithHelp
import com.here.ort.utils.PARAMETER_ORDER_OPTIONAL

@Parameters(commandNames = ["cd"], commandDescription = "Interact with the ClearlyDefined service.")
object ClearlyDefinedCommand : CommandWithHelp() {
    @Parameters(commandNames = ["curations"], commandDescription = "Interact with curations.")
    object CurationsCommand : CommandWithHelp() {
        @Parameter(
            description = "Get a curation for a component revision.",
            names = ["--get-curation"],
            order = PARAMETER_ORDER_OPTIONAL
        )
        private var getCurationId: String? = null

        override fun runCommand(jc: JCommander): Int {
            println("Yeah!")
            return 0
        }
    }

    override fun runCommand(jc: JCommander): Int {
        // JCommander already validates the command names.
        val command = jc.commands[jc.parsedCommand]!!
        val subCommand = command.commands[command.parsedCommand]!!
        val commandObject = subCommand.objects.first() as CommandWithHelp

        // Delegate running actions to the specified command.
        return commandObject.run(jc)
    }
}
