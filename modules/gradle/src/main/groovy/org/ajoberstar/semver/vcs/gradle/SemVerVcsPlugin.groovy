package org.ajoberstar.semver.vcs.gradle

import com.github.zafarkhaja.semver.Version
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.concurrent.atomic.AtomicReference

class SemVerVcsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def extension = project.extensions.create('semver', SemVerExtension)
        def delayedVersion = new DelayedVersion(extension)
        project.allprojects {
            version = delayedVersion
        }
    }

    private class DelayedVersion {
        private final AtomicReference<String> version = new AtomicReference<>()
        private final SemVerExtension extension

        DelayedVersion(SemVerExtension extension) {
            this.extension = extension
        }

        private String infer() {
            Version base = Version.forIntegers(0)
            return extension.versioner.infer(base, extension.vcs)
        }

        @Override
        String toString() {
            return version.updateAndGet { current ->
                current ?: infer()
            }
        }
    }
}
