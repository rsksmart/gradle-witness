package org.whispersystems.witness

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.kotlin.dsl.create
import java.io.File
import java.security.MessageDigest


open class WitnessPluginExtension {
    var verify: List<String> = listOf()
    var exclude: List<String> = listOf()
}

class WitnessPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<WitnessPluginExtension>("dependencyVerification")
        project.afterEvaluate {
            validateResolvedDependencies(project, extension)
        }

        project.task("calculateChecksums").doLast {
            println("dependencyVerification {")
            println("    verify = [")

            project.incomingDependencies.forEach { dep ->
                println("        '" + dep.group + ":" + dep.module + ":" + calculateSha256(project.jarForDependency(dep)) + "',")
            }

            println("    ]")
            println("}")
        }
    }

    private fun validateResolvedDependencies(project: Project, extension: WitnessPluginExtension) {
        val incomingDependencies = project.incomingDependencies.toMutableSet()
        extension.exclude.forEach { assertion ->
            val (group, name) = assertion.split(":")

            val dependency = incomingDependencies.find { it.group == group && it.module == name }
                ?: throw InvalidUserDataException("No dependency for integrity exclusion found: $group:$name")

            println("Skipping verification for $dependency")

            incomingDependencies.remove(dependency)
        }

        extension.verify.forEach { assertion ->
            val (group, name, hash) = assertion.split(":")

            val dependency = incomingDependencies.find { it.group == group && it.module == name }
                ?: throw InvalidUserDataException("No dependency for integrity assertion found: $group:$name")

            println("Verifying $dependency")

            if (hash != calculateSha256(project.jarForDependency(dependency))) {
                throw InvalidUserDataException("Checksum failed for $assertion")
            }

            incomingDependencies.remove(dependency)
        }

        if (incomingDependencies.isNotEmpty()) {
            val errorMessage = incomingDependencies.joinToString("\n", "No dependency for integrity assertion found for: \n") { "- ${it.displayName}" }
            throw InvalidUserDataException(errorMessage)
        }
    }

    private fun Project.jarForDependency(resolved: ModuleComponentIdentifier) =
        configurations.getAt("testRuntime").resolvedConfiguration.resolvedArtifacts
            .single { it.moduleVersion.id.module == resolved.moduleIdentifier }
            .file

    private val Project.incomingDependencies
        get() = configurations.getAt("testRuntime").incoming.resolutionResult.allDependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .map { it.selected.id }
            .filterIsInstance<ModuleComponentIdentifier>()

    private fun calculateSha256(file: File): String {
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        file.forEachBlock(4096) { bytes, size ->
            md.update(bytes, 0, size)
        }

        return md.digest().joinToString(separator = "") { String.format("%02x", it) }
    }
}
