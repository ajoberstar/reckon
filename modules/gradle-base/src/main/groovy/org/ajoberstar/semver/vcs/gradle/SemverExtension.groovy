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

    SemverExtension stages(Stage... stages) {
        return stages(stages as List)
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
