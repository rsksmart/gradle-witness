package org.whispersystems.witness

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.kotlin.dsl.create
import java.io.File
import java.security.MessageDigest

open class WitnessPluginExtension {
    var verify: List<String> = listOf()
}

class WitnessPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<WitnessPluginExtension>("dependencyVerification")
        project.afterEvaluate {
            extension.verify.forEach { assertion ->
                val (group, name, hash) = assertion.split(":")

                val dependency: ResolvedArtifact? =
                    project.configurations.getByName("testRuntime").resolvedConfiguration.resolvedArtifacts.find {
                        it.name == name && it.moduleVersion.id.group == group
                    }

                println("Verifying $group:$name")

                if (dependency == null) {
                    throw InvalidUserDataException("No dependency for integrity assertion found: $group:$name")
                }

                if (hash != calculateSha256(dependency.file)) {
                    throw InvalidUserDataException("Checksum failed for $assertion")
                }
            }
        }

        project.task("calculateChecksums").doLast {
            println("dependencyVerification {")
            println("    verify = [")

            project.configurations.getByName("testRuntime").resolvedConfiguration.resolvedArtifacts.forEach { dep ->
                println("        '" + dep.moduleVersion.id.group + ":" + dep.name + ":" + calculateSha256(dep.file) + "',")
            }

            println("    ]")
            println("}")
        }
    }

    private fun calculateSha256(file: File): String {
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        file.forEachBlock(4096) { bytes, size ->
            md.update(bytes, 0, size)
        }

        return md.digest().joinToString(separator = "") { String.format("%02x", it) }
    }
}
