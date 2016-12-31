/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ajoberstar.semver.vcs.gradle

import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.Version
import org.ajoberstar.semver.vcs.Stage
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.ajoberstar.semver.vcs.Scope
import org.ajoberstar.semver.vcs.Versioners

import java.util.concurrent.atomic.AtomicReference

/**
 * Plugin providing version inference according to Semantic
 * Versioning. In addition to the {@code SemVerExtension} the
 * following project properties are supported to influence the
 * inference.
 *
 * <ul>
 *   <li>semver.scope - one of MAJOR, MINOR, or PATCH</li>
 *   <li>semver.stage - the name of the stage to use</li>
 *   <li>semver.base - a version string to use as the initial input, typically only used if no versions are tagged</li>
 *   <li>semver.force - a version string to use instead of inferring based on the VCS</li>
 * </ul>
 */
class SemverVcsPlugin implements Plugin<Project> {
    static final String SCOPE_PROP = 'semver.scope'
    static final String STAGE_PROP = 'semver.stage'
    static final String FORCE_PROP = 'semver.force'
    static final String BASE_PROP = 'semver.base'

    @Override
    void apply(Project project) {
        def extension = project.extensions.create('semver', SemverExtension)

        extension.with {
            stages finalStage()
            stages fixedStages('rc', 'milestone')
            stages floatingStages('dev')
        }

        extension.versionerSupplier = {
            return projectProp(project, FORCE_PROP).map { version ->
                try {
                    Versioners.force(version)
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Invalid forced version: ${version}", e)
                }
            }.orElseGet {
                Scope scope = projectProp(project, SCOPE_PROP).map { value ->
                    try {
                        Scope.valueOf(value.toUpperCase())
                    } catch(IllegalArgumentException e) {
                        throw new IllegalArgumentException("Scope name ${value} is not valid: ${Scope.values()*.toString()*.toLowerCase()}", e)
                    }
                }.orElse(extension.defaultScope)

                Stage stage = projectProp(project, STAGE_PROP).map { name ->
                    Optional.ofNullable(extension.getStage(name)).orElseThrow {
                        new IllegalArgumentException("Stage name ${name} is not valid: ${extension.stages*.name}")
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
            Version base = projectProp(project, BASE_PROP).map { version ->
                try {
                    Version.valueOf(version)
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Invalid base version: ${version}", e)
                }
            }.orElse(Versioners.VERSION_0)

            Version inferred = extension.versionerSupplier.get()
                .infer(base, extension.vcsSupplier.get())

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
