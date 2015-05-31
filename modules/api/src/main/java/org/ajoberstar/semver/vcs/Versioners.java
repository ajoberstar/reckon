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
package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Objects;

public final class Versioners {
    private Versioners() {
        throw new AssertionError("Cannot instantiate this class.");
    }

    public static final Version VERSION_0 = Version.forIntegers(0, 0, 0);

    public static Versioner identity() {
        return (base, vcs) -> base;
    }

    public static Versioner force(String version) {
        Version forced = Version.valueOf(version);
        return (base, vcs) -> forced;
    }

    public static Versioner rebuild() {
        return (base, vcs) -> vcs.getCurrentVersion()
                .orElseThrow(() -> new IllegalStateException("Cannot rebuild since current revision doesn't have a version."));
    }

    public static Versioner enforcePrecedence() {
        return (base, vcs) -> {
            Version previous = vcs.getPreviousVersion().orElse(base);
            if (base.greaterThanOrEqualTo(previous)) {
                return base;
            } else {
                throw new IllegalArgumentException("Inferred version (" + base + ") must have higher precedence than previous (" + previous + ")");
            }
        };
    }

    public static Versioner forScopeAndStage(Scope scope, Stage stage, boolean enforcePrecedence) {
        Objects.requireNonNull(scope, "Scope cannot be null.");
        Objects.requireNonNull(stage, "Stage cannot be null.");
        return identity()
                .compose(enforcePrecedence ? enforcePrecedence() : identity())
                .compose(stage.getVersioner())
                .compose(scope.getVersioner());
    }
}
