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

import java.util.Objects;

/**
 * Defines the contract of version inference. This is the core
 * interface by which a semantic version is calculated from a
 * VCS's state.
 */
@FunctionalInterface
public interface Versioner {
    /**
     * Infers the project's version based on the parameters.
     * @param base the version to increment off of, unless
     * @param vcs the version control system holding the
     *            current state of the project
     * @return the version to be used for the project or
     * passed into the next versioner
     */
    Version infer(Version base, Vcs vcs);

    /**
     * Combines the given versioner with this one. The given
     * versioner will be executed first followed by this one.
     * The returned versioner will behave as {@code this.infer(before.infer(base, vcs), vcs)}.
     * @param before the versioner to compose with this one
     * @return a versioner representing the composition of the two
     */
    default Versioner compose(Versioner before) {
        Objects.requireNonNull(before);
        return (base, vcs) -> infer(before.infer(base, vcs), vcs);
    }
}
