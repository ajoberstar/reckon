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

    SortedSet<Stage> stages = []

    Stage defaultStage

    Stage getDefaultStage() {
        return defaultStage ?: stages.find()
    }

    Stage getStage(String name) {
        return stages.find { it.name == name }
    }

    SemverExtension clearStages() {
        stages = []
        return this
    }

    SemverExtension stages(Stage... stages) {
        stages.each { stage ->
            this.stages << stage
        }
        return this
    }

    SemverExtension finalStage() {
        stages(Stage.finalStage())
        return this
    }

    SemverExtension fixedStages(String... names) {
        stages(names.collect { Stage.fixedStage(it) } as Stage[])
        return this
    }

    SemverExtension floatingStages(String... names) {
        stages(names.collect { Stage.floatingStage(it) } as Stage[])
        return this
    }

    SemverExtension snapshotStage() {
        stages(Stage.snapshotStage())
        return this
    }
}
