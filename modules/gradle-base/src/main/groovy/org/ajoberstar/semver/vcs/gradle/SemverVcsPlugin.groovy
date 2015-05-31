package org.ajoberstar.semver.vcs.gradle

import com.github.zafarkhaja.semver.Version
import org.ajoberstar.semver.vcs.Stage
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
    static final String BASE_PROP = 'semver.base'

    @Override
    void apply(Project project) {
        def extension = project.extensions.create('semver', SemverExtension)
        extension.versionerSupplier = {
            return projectProp(project, FORCE_PROP).map { version ->
                Versioners.force(version)
            }.orElseGet {
                Scope scope = projectProp(project, SCOPE_PROP).map { value ->
                    Scope.valueOf(value.toUpperCase())
                }.orElse(extension.defaultScope)

                Stage stage = projectProp(project, STAGE_PROP).map { name ->
                    Optional.ofNullable(extension.stages[name]).orElseThrow {
                        new IllegalArgumentException("Stage name ${name} is not valid: ${extension.stages.keySet()}")
                    }
                }.orElse(extension.defaultStage)

                Versioners.forScopeAndStage(scope, stage, extension.enforcePrecedence)
            }
        }

        def lazyVersion = new LazyVersion(project, extension)
        project.allprojects {
            version = lazyVersion
        }
    }

    private static Optional<String> projectProp(Project project, String name) {
        if (project.hasProperty(name)) {
            return Optional.of(project.getProperty(name))
        } else {
            Optional.empty()
        }
    }

    private class LazyVersion {
        private final AtomicReference<String> version = new AtomicReference<>()
        private final Project project
        private final SemverExtension extension

        LazyVersion(Project project, SemverExtension extension) {
            this.project = project
            this.extension = extension
        }

        private String infer() {
            Version base = projectProp(project, BASE_PROP).map {
                Version.valueOf(it)
            }.orElse(Versioners.VERSION_0)

            Version inferred = extension.versionerSupplier.get()
                .infer(base, extension.vcs)

            project.logger.warn('Inferred version: {}', inferred)
            return inferred.toString()
        }

        @Override
        String toString() {
            return version.updateAndGet { current ->
                current ?: infer()
            }
        }
    }
}
