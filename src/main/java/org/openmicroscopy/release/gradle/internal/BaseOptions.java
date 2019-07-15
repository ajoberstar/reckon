package org.openmicroscopy.release.gradle.internal;

import org.gradle.api.Project;

import java.util.Optional;

abstract class BaseOptions {

    final Project project;

    BaseOptions(Project project) {
        this.project = project;
    }

    Optional<String> findProperty(String value, Object fallback) {
        Object result = Optional.ofNullable(project.findProperty(value)).orElse(fallback);
        return Optional.ofNullable(result).map(Object::toString);
    }

}
