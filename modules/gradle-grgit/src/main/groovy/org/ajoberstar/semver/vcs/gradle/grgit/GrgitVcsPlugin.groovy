package org.ajoberstar.semver.vcs.gradle.grgit

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.semver.vcs.grgit.GrgitVcs
import org.gradle.api.Plugin
import org.gradle.api.Project

class GrgitVcsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply('org.ajoberstar.grgit')
        project.pluginManager.apply('org.ajoberstar.semver-vcs-base')
        project.extensions.getByName('semver').with {
            vcs = new GrgitVcs(git)
        }
    }
}
