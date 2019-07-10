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

    private val dependency = "com.typesafe:config"
    private val dependencyVersion = "1.3.3"
    private val correctHash = "b5f1d6071f1548d05be82f59f9039c7d37a1787bd8e3c677e31ee275af4a4621"
    private val incorrectHash = "b5f1d6071f1548d05be82f59f9039c7d37a1787bd8e3c677e31ee275af4a462f"

    @Before
    fun setup() {
        buildFile = testProjectDir.newFile("build.gradle")
    }

    @Test
    fun `can verify dependency and assemble project`() {
        GradleRunnerBuilder().run {
            addDependency("$dependency:$dependencyVersion")
            addVerification("$dependency:$correctHash")
            build(buildFile)
        }

        val result = runnerForAssembleTask().build()

        assertThat(result.output, containsString("Verifying $dependency"))
        assertThat(result.task(":assemble")!!.outcome, equalTo(SUCCESS))
    }

    @Test
    fun `can exclude dependency from verification and assemble project`() {
        GradleRunnerBuilder().run {
            addDependency("$dependency:$dependencyVersion")
            addExclude(dependency)
            build(buildFile)
        }

        val result = runnerForAssembleTask().build()

        assertThat(result.output, containsString("Skipping verification for $dependency"))
        assertThat(result.task(":assemble")!!.outcome, equalTo(SUCCESS))
    }

    @Test
    fun `stops assembling project with missing dependency validation`() {
        GradleRunnerBuilder().run {
            addDependency("$dependency:$dependencyVersion")
            build(buildFile)
        }

        val result = runnerForAssembleTask().buildAndFail()

        assertThat(result.output, containsString("No dependency for integrity assertion found for:"))
        assertThat(result.output, containsString("- $dependency"))
    }

    @Test
    fun `stops assembling project when dependency can't be verified`() {
        GradleRunnerBuilder().run {
            addDependency("$dependency:$dependencyVersion")
            addVerification("$dependency:$incorrectHash")
            build(buildFile)
        }

        val result = runnerForAssembleTask().buildAndFail()

        assertThat(result.output, containsString("Checksum failed for $dependency"))
    }

    @Test
    fun `stops assembling with invalid verification`() {
        GradleRunnerBuilder().run {
            addVerification("$dependency:$correctHash")
            build(buildFile)
        }

        val result = runnerForAssembleTask().buildAndFail()

        assertThat(result.output, containsString("No dependency for integrity assertion found: $dependency"))
    }

    @Test
    fun `stops assembling with invalid exclude`() {
        GradleRunnerBuilder().run {
            addExclude(dependency)
            build(buildFile)
        }

        val result = runnerForAssembleTask().buildAndFail()

        assertThat(result.output, containsString("No dependency for integrity exclusion found: $dependency"))
    }

    @Test
    fun `stops assembling with both exclude and verification`() {
        GradleRunnerBuilder().run {
            addDependency("$dependency:$dependencyVersion")
            addVerification("$dependency:$correctHash")
            addExclude(dependency)
            build(buildFile)
        }

        val result = runnerForAssembleTask().buildAndFail()

        assertThat(result.output, containsString("No dependency for integrity assertion found: $dependency"))
    }

    private fun runnerForAssembleTask() = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments("assemble")
        .withPluginClasspath()
}