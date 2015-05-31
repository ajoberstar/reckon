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

class SemverExtension {
    Supplier<Versioner> versionerSupplier

    Vcs vcs

    boolean enforcePrecedence = true

    Scope defaultScope = Scope.MINOR

    SortedSet<Stage> stages = [] as SortedSet

    Stage defaultStage

    void setStages(Iterable<Stage> stages) {
        this.stages = stages as SortedSet
    }

    SemverExtension stages(Stage... stages) {
        return this.stages(stages as List)
    }

    SemverExtension stages(Iterable<Stage> stages) {
        stages.each { stage ->
            this.stages << stage
        }
        return this
    }

    Stage getDefaultStage() {
        return defaultStage ?: stages.find()
    }

    Stage getStage(String name) {
        return stages.find { it.name == name }
    }

    Stage finalStage() {
        return Stage.finalStage()
    }

    List<Stage> fixedStages(String... names) {
        return names.collect { Stage.fixedStage(it) }
    }

    List<Stage> floatingStages(String... names) {
        return names.collect { Stage.floatingStage(it) }
    }

    Stage snapshotStage() {
        return Stage.snapshotStage()
    }
}
