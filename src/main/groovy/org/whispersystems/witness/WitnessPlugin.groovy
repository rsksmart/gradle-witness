package org.whispersystems.witness

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact

import java.security.MessageDigest

class WitnessPluginExtension {
    List verify
}

class WitnessPlugin implements Plugin<Project> {

    static String calculateSha256(file) {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        file.eachByte 4096, {bytes, size ->
            md.update(bytes, 0, size);
        }
        return md.digest().collect {String.format "%02x", it}.join();
    }

    void apply(Project project) {
        project.extensions.create("dependencyVerification", WitnessPluginExtension)
        project.afterEvaluate {
            List artifacts = new ArrayList()
            project.configurations.each {
                if (it.metaClass.respondsTo(it, 'isCanBeResolved') ? it.isCanBeResolved() : true) {
                    artifacts.addAll(it.resolvedConfiguration.resolvedArtifacts)
                }
            }
            project.dependencyVerification.verify.each {
                assertion ->
                    List parts = assertion.tokenize(":")
                    def (group, name, hash) = parts

                    List dependencies = artifacts.findAll {
                        return it.name.equals(name) && it.moduleVersion.id.group.equals(group)
                    }

                    println "Verifying " + group + ":" + name

                    if (dependencies.isEmpty()) {
                        throw new InvalidUserDataException("No dependency for integrity assertion found: " + group + ":" + name)
                    }

                    Set files = new TreeSet()
                    dependencies.each { files.add(it.file) }
                    files.each {
                        if (!hash.equals(calculateSha256(it))) {
                            throw new InvalidUserDataException("Checksum failed for " + assertion)
                        }
                    }
            }

        }


        project.task('calculateChecksums') << {
            Set dependencies = new TreeSet()
            project.configurations.each {
                if (it.metaClass.respondsTo(it, 'isCanBeResolved') ? it.isCanBeResolved() : true) {
                    it.resolvedConfiguration.resolvedArtifacts.findAll {
                        // Skip internal dependencies
                        it.moduleVersion.id.version != 'unspecified'
                    }.each {
                        dep -> dependencies.add(dep.moduleVersion.id.group + ":" + dep.name + ":" + calculateSha256(dep.file))
                    }
                }
            }

            println "dependencyVerification {"
            println "    verify = ["
            dependencies.each { dep -> println "        '" + dep + "'," }
            println "    ]"
            println "}"
        }
    }
}

