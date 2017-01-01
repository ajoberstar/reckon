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

import java.util.Optional;

/**
 *
 */
public interface Vcs {
    /**
     * Gets the version of the current revision of the VCS, if it
     * is marked.
     * @return a version representing the current revision or an empty
     * Optional
     */
    Optional<Version> getCurrentVersion();

    /**
     * Gets the previous final version (i.e. no pre-release information),
     * if there is one.
     * @return a version representing the previous release marked in the VCS
     * or an empty Optional
     */
    Optional<Version> getPreviousRelease();

    /**
     * Gets the previous version, if there is one.
     * @return a version representing the previous version marked
     * in the VCS or an empty Optional
     */
    Optional<Version> getPreviousVersion();
}
