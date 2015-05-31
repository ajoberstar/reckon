package org.ajoberstar.semver.vcs.gradle

import org.ajoberstar.semver.vcs.Stage
import org.ajoberstar.semver.vcs.Vcs
import org.ajoberstar.semver.vcs.Versioner

class SemverExtension {
    Vcs vcs
    Versioner versioner

    boolean enforcePrecedence = true

    Map<String, Stage> stages = [:]

    void stages(Stage... stages) {
        stages.each { stage ->
            stages[stage.name] == stage
        }
    }
}
