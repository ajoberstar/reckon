/*
 * Copyright 2015-2017 the original author or authors.
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

/** Companion functions for creating common versioners. */
public final class Versioners {
  /** A Version 0.0.0 */
  public static final Version VERSION_0 = Version.forIntegers(0, 0, 0);

  private Versioners() {
    throw new AssertionError("Cannot instantiate this class.");
  }

  /** @return a versioner that will return the base. */
  public static Versioner identity() {
    return (base, vcs) -> base;
  }

  /**
   * Parses the given version and creates a versioner that will always return it.
   *
   * @param version the version to return
   * @return a versioner that will always return the given version
   */
  public static Versioner force(String version) {
    Version forced = Version.valueOf(version);
    return (base, vcs) -> forced;
  }

  /**
   * @return a versioner that will return the current version from the VCS or throw an exception if
   *     one is not available
   */
  public static Versioner rebuild() {
    return (base, vcs) ->
        vcs.getCurrentVersion()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Cannot rebuild since current revision doesn't have a version."));
  }

  /**
   * @return a versioner that will validate that the base is greater than the VCS's previous version
   *     (if available). If it's not greater than or equal to an exception will be thrown.
   */
  public static Versioner enforcePrecedence() {
    return (base, vcs) -> {
      Version previous = vcs.getPreviousVersion().orElse(base);
      if (base.greaterThanOrEqualTo(previous)) {
        return base;
      } else {
        throw new IllegalArgumentException(
            "Inferred version ("
                + base
                + ") must have higher precedence than previous ("
                + previous
                + ")");
      }
    };
  }

  /**
   * Creates a versioner that will use the given scope and stage to increment the version.
   *
   * @param scope the scope of change made since the last final release
   * @param stage the stage of changes made since the last release
   * @param enforcePrecedence whether or not to enforce that the inferred version is greater than or
   *     equal to the previous
   * @return a versioner using the scope and stage
   */
  public static Versioner forScopeAndStage(Scope scope, Stage stage, boolean enforcePrecedence) {
    Objects.requireNonNull(scope, "Scope cannot be null.");
    Objects.requireNonNull(stage, "Stage cannot be null.");
    return identity()
        .compose(enforcePrecedence ? enforcePrecedence() : identity())
        .compose(stage.getVersioner())
        .compose(scope.getVersioner());
  }
}
