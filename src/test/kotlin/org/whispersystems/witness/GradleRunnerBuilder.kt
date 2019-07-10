package org.whispersystems.witness

import java.io.File


class GradleRunnerBuilder {

    private val dependencies = mutableListOf<String>()
    private val verifications = mutableListOf<String>()
    private val excludes = mutableListOf<String>()

    private fun buildProjectDefinition(): String {
        return """
            |plugins {
            |  id 'witness'
            |}
            |
            |repositories {
            |    mavenCentral()
            |}
            |
            |apply plugin:'java'
            |
            |${dependenciesList()}
            |
            |dependencyVerification {
            |    ${excludesList()}
            |    ${verifiesList()}
            |}
            """.trimMargin()
    }

    private fun dependenciesList(): String {
        if (dependencies.isEmpty()) {
            return ""
        }

        return dependencies.joinToString(", ", "dependencies { compile ", " }") { "\"$it\"" }
    }

    private fun verifiesList(): String {
        return verifications.joinToString(", ", "verify = [ ", " ]") { "\"$it\"" }

    }

    private fun excludesList(): String {
        return excludes.joinToString(", ", "exclude = [ ", " ]") { "\"$it\"" }

    }

    fun addDependency(dependency: String) {
        dependencies.add(dependency)
    }

    fun addVerification(verification: String) {
        verifications.add(verification)
    }

    fun addExclude(exclude: String) {
        excludes.add(exclude)
    }

    fun build(buildFile: File) {
        buildFile.writeText(buildProjectDefinition())
    }
}