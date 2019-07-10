package org.whispersystems.witness

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File


class WitnessPluginTest {
    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    private lateinit var buildFile: File

    private val correctHash = "b5f1d6071f1548d05be82f59f9039c7d37a1787bd8e3c677e31ee275af4a4621"
    private val incorrectHash = "b5f1d6071f1548d05be82f59f9039c7d37a1787bd8e3c677e31ee275af4a462f"

    @Before
    fun setup() {
        buildFile = testProjectDir.newFile("build.gradle")
        buildFile.writeText("""
            plugins {
              id 'witness'
            }
            repositories {
                mavenCentral()
            }
            apply plugin:'java'
            """)
    }

    @Test
    fun `can verify dependency and assemble project`() {
        addDependency()
        addVerification(correctHash)

        val result = runnerForAssemble().build()

        assertThat(result.output, containsString("Verifying com.typesafe:config"))
        assertThat(result.task(":assemble")!!.outcome, equalTo(SUCCESS))
    }

    @Test
    fun `can assemble project with dependency without validation`() {
        addDependency()

        val result = runnerForAssemble().build()

        assertThat(result.task(":assemble")!!.outcome, equalTo(SUCCESS))
    }

    @Test
    fun `stops assembling project when dependency can't be verified`() {
        addDependency()
        addVerification(incorrectHash)

        val result = runnerForAssemble().buildAndFail()

        assertThat(result.output, containsString("Checksum failed for com.typesafe:config"))
    }

    private fun addDependency() {
        buildFile.appendText("""
            dependencies {
                compile "com.typesafe:config:1.3.3"
            }
            """)
    }

    private fun addVerification(hash: String) {
        buildFile.appendText("""
            dependencyVerification {
                verify = [
                    'com.typesafe:config:$hash'
                ]
            }
            """)
    }

    private fun runnerForAssemble() = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments("assemble")
        .withPluginClasspath()
}