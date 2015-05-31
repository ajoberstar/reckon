package org.ajoberstar.semver.vcs.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class OpinionSemverVcsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply('org.ajoberstar.semver-vcs-base')
        SemverExtension extension = project.extensions.getByType(SemverExtension)
        extension.with {
            finalStage()
            fixedStages('rc', 'milestone')
            floatingStages('dev')
        }
    }
}
