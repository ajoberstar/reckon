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

import org.ajoberstar.semver.vcs.Scope
import org.ajoberstar.semver.vcs.Stage
import org.ajoberstar.semver.vcs.Vcs
import org.ajoberstar.semver.vcs.Versioner

import java.util.function.Supplier

/**
 * Configuration for the behavior of version inference.
 */
class SemverExtension {
    /**
     * Lambda that will return the versioner to use when
     * inferring the project's version.
     */
    Supplier<Versioner> versionerSupplier

    /**
     * Lambda that will return the Vcs to pass into the
     * Versioner.
     */
    Supplier<Vcs> vcsSupplier

    /**
     * Whether or not version precedence should be enforced when
     * the next version is inferred. Default is {@code true}.
     */
    boolean enforcePrecedence = true

    /**
     * The scope to use if none is provided. Default is {@code MINOR}.
     */
    Scope defaultScope = Scope.MINOR

    /**
     * The stages to allow. Default is: {@code final}, {@code rc} (fixed),
     * {@code milestone} (fixed), {@code dev} (floating).
     */
    SortedSet<Stage> stages = [] as SortedSet

    /**
     * The stage to use if none is provided. Default is {@code null}.
     */
    Stage defaultStage

    /**
     * Overrides the currently configured stages with those provided.
     * @param stages the stages to use (existing ones are removed)
     */
    void setStages(Iterable<Stage> stages) {
        this.stages = stages as SortedSet
    }

    /**
     * Adds stages to those already configured.
     * @param stages the stages to add
     * @return this extension
     */
    SemverExtension stages(Stage... stages) {
        return this.stages(stages as List)
    }

    /**
     * Adds stages to those already configured.
     * @param stages the stages to add
     * @return this extension
     */
    SemverExtension stages(Iterable<Stage> stages) {
        stages.each { stage ->
            this.stages << stage
        }
        return this
    }

    /**
     * Gets the stage to use if none is provided. If not explicitly set,
     * the stage with the lowest precedence is used.
     * @return the default stage
     */
    Stage getDefaultStage() {
        return defaultStage ?: stages.find()
    }

    /**
     * Gets a stage by name.
     * @param name the name of the stage to return
     * @return the stage (or {@code null} if none with that name is configured)
     */
    Stage getStage(String name) {
        return stages.find { it.name == name }
    }

    /**
     * Helper to create a final stage.
     * @return a final stage
     * @see org.ajoberstar.semver.vcs.Stage#finalStage()
     */
    Stage finalStage() {
        return Stage.finalStage()
    }

    /**
     * Helper to create one or more fixed stages.
     * @param names the names of the stages
     * @return a list of fixed stages
     * @see org.ajoberstar.semver.vcs.Stage#fixedStage(String)
     */
    List<Stage> fixedStages(String... names) {
        return names.collect { Stage.fixedStage(it) }
    }

    /**
     * Helper to create one or more floating stages.
     * @param names the names of the stages
     * @return a list of floating stages
     * @see org.ajoberstar.semver.vcs.Stage#floatingStage(String)
     */
    List<Stage> floatingStages(String... names) {
        return names.collect { Stage.floatingStage(it) }
    }

    /**
     * Helper to create a snapshot stage.
     * @return a snapshot stage
     * @see org.ajoberstar.semver.vcs.Stage#snapshotStage()
     */
    Stage snapshotStage() {
        return Stage.snapshotStage()
    }
}
