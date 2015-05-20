package org.ajoberstar.semver.vcs.gradle

import com.github.zafarkhaja.semver.Version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.ajoberstar.semver.vcs.Scope
import org.ajoberstar.semver.vcs.Versioners
import org.ajoberstar.semver.vcs.VersionerBuilder

import java.util.concurrent.atomic.AtomicReference

class SemverVcsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def extension = project.extensions.create('semver', SemverExtension)

        VersionerBuilder builder = Versioners.builder()

        Scope scope = Scope.MINOR
        if (project.hasProperty('semver.scope')) {
            scope = Scope.valueOf(project.getProperty('semver.scope').toUpperCase())
        }
        builder.useScope(scope)

        if (project.hasProperty('semver.stage')) {
            String stage = project.getProperty('semver.stage')
            if (stage == 'final') {
                builder.useFinal()
            } else {
                builder.useFixedStage(stage)
            }
        } else {
            builder.useFloatingStage('dev')
        }

        extension.versioner = builder.build()

        def delayedVersion = new DelayedVersion(extension)
        project.allprojects {
            version = delayedVersion
        }
    }

    private class DelayedVersion {
        private final AtomicReference<String> version = new AtomicReference<>()
        private final SemverExtension extension

        DelayedVersion(SemverExtension extension) {
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
