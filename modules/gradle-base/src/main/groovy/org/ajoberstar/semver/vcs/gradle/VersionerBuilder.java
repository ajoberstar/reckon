package org.ajoberstar.semver.vcs.gradle;

import org.ajoberstar.semver.vcs.Versioner;
import org.gradle.api.Project;

@FunctionalInterface
public interface VersionerBuilder {
    Versioner build(Project project);
}
