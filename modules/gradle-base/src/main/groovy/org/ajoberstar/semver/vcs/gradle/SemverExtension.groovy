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

    Stage defaultStage

    Stage getDefaultStage() {
        return defaultStage ?: stages.values().min()
    }

    Map<String, Stage> stages = [:]

    SemverExtension stages(Stage... stages) {
        stages.each { stage ->
            this.stages[stage.name] = stage
        }
        return this
    }

    SemverExtension finalStage() {
        stages(Stage.finalStage())
        return this
    }

    SemverExtension fixedStages(String... names) {
        stages(names.collect { Stage.fixedStage(it) })
        return this
    }

    SemverExtension floatingStages(String... names) {
        stages(names.collect { Stage.floatingStage(it) })
        return this
    }

    SemverExtension snapshotStage() {
        stages(Stage.snapshotStage())
        return this
    }
}
