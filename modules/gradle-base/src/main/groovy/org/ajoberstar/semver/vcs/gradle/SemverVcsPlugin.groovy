package org.ajoberstar.semver.vcs.gradle

import com.github.zafarkhaja.semver.Version
import org.ajoberstar.semver.vcs.Versioner
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.ajoberstar.semver.vcs.Scope
import org.ajoberstar.semver.vcs.Versioners

import java.util.concurrent.atomic.AtomicReference

class SemverVcsPlugin implements Plugin<Project> {
    static final String SCOPE_PROP = 'semver.scope'
    static final String STAGE_PROP = 'semver.stage'
    static final String FORCE_PROP = 'semver.force'

    @Override
    void apply(Project project) {
        def extension = project.extensions.create('semver', SemverExtension)

        extension.versioner = projectProp(project, FORCE_PROP).map { version ->
            Versioners.force(version)
        }.orElseGet {
            Scope scope = projectProp(project, SCOPE_PROP).map { value ->
                Scope.valueOf(value.toUpperCase())
            }.orElse(Scope.MINOR)

            Versioner normal = Versioners.useScope(scope)

            String stage = projectProp(project, STAGE_PROP).orElse('dev')
            Versioner preRelease
            switch (stage) {
                case 'final':
                    preRelease = Versioners.useFinalStage()
                    break
                case 'dev':
                    preRelease = Versioners.useFloatingStage(stage)
                    break
                default:
                    preRelease = Versioners.useFixedStage(stage)
            }

            Versioners.enforcePrecedence()
                .compose(preRelease)
                .compose(normal)
        }

        def delayedVersion = new DelayedVersion(extension)
        project.allprojects {
            version = delayedVersion
        }
    }

    private Optional<String> projectProp(Project project, String name) {
        if (project.hasProperty(name)) {
            return Optional.of(project.getProperty(name))
        } else {
            Optional.empty()
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
