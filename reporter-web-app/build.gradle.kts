import org.apache.tools.ant.taskdefs.condition.Os

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsSetupTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnSetupTask

// The Yarn plugin is only applied programmatically in Kotlin projects that target JavaScript. As we do not target
// JavaScript from Kotlin (yet), manually apply the plugin to make its setup tasks available.
YarnPlugin.apply(project).version = "1.17.3"

// The Yarn plugin registers tasks always on the root project.
val kotlinNodeJsSetup by rootProject.tasks.existing(NodeJsSetupTask::class)

val nodeInstallationDir = kotlinNodeJsSetup.get().destination
val nodeExecutable = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    nodeInstallationDir.resolve("node.exe")
} else {
    nodeInstallationDir.resolve("bin/node")
}
logger.quiet("Using Node executable file at '$nodeExecutable'.")

// Work-around for the YarnSetupTask using the wrong action annotation, see
// https://github.com/JetBrains/kotlin/pull/2563.
open class FixedYarnSetupTask : YarnSetupTask() {
    @TaskAction
    fun fixedSetup() = super.setup()
}

val fixedKotlinYarnSetup by tasks.registering(FixedYarnSetupTask::class) {
    dependsOn(kotlinNodeJsSetup)
}

val yarnJsFile = fixedKotlinYarnSetup.get().destination.resolve("bin/yarn.js")
logger.quiet("Using Yarn JavaScript file at '$yarnJsFile'.")

tasks.addRule("Pattern: yarn<Command>") {
    val taskName = this
    if (taskName.startsWith("yarn")) {
        val command = taskName.removePrefix("yarn").decapitalize()

        tasks.register<Exec>(taskName) {
            // Execute the installed Yarn version.
            commandLine = listOf(nodeExecutable.path, yarnJsFile, command)
        }
    }
}

/*
 * Further configure rule tasks, e.g. with inputs and outputs.
 */

tasks {
    "yarnInstall" {
        description = "Use Yarn to install the Node.js dependencies."
        group = "Node"

        dependsOn(fixedKotlinYarnSetup)

        inputs.files(listOf("package.json", "yarn.lock"))
        outputs.dir("node_modules")
    }

    "yarnBuild" {
        description = "Use Yarn to build the Node.js application."
        group = "Node"

        dependsOn("yarnInstall")

        inputs.dir("config")
        inputs.dir("node_modules")
        inputs.dir("public")
        inputs.dir("scripts")
        inputs.dir("src")

        outputs.dir("build")
    }

    "yarnLint" {
        description = "Let Yarn run the linter to check for style issues."
        group = "Node"

        dependsOn("yarnInstall")
    }
}

/*
 * Resemble the Java plugin tasks for convenience.
 */

tasks.register("build") {
    dependsOn(listOf("yarnBuild", "yarnLint"))
}

tasks.register("check") {
    dependsOn("yarnLint")
}

tasks.register<Delete>("clean") {
    delete("build")
    delete("node_modules")
    delete("yarn-error.log")
}
