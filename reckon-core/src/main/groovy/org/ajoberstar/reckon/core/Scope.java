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
package org.ajoberstar.reckon.core;

import com.github.zafarkhaja.semver.Version;

import java.util.function.UnaryOperator;

/**
 * The scope of changes represented in the VCS currently.
 */
public enum Scope {
    MAJOR(Version::incrementMajorVersion),
    MINOR(Version::incrementMinorVersion),
    PATCH(Version::incrementPatchVersion);

    private final Versioner versioner;

    Scope(UnaryOperator<Version> incrementer) {
        this.versioner = (base, vcs) -> {
            Version prevVersion = vcs.getPreviousVersion().orElse(base);
            Version prevRelease = vcs.getPreviousRelease().orElse(base);
            Version incremented = incrementer.apply(prevRelease);
            if (incremented.getNormalVersion().equals(prevVersion.getNormalVersion())) {
                return prevVersion;
            } else {
                return incremented;
            }
        };
    }

    /**
     * A versioner that will increment the component of the previous release
     * corresponding with the name of the enum value. (e.g. MAJOR would increment
     * 1.2.3 to 2.0.0). If the previous version has the same normal component
     * as the currently incremented one, it's pre-release information will be
     * preserved.
     * @return the versioner for this scope
     */
    public Versioner getVersioner() {
        return versioner;
    }
}
