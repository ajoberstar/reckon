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

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Versions {
  private static final Logger logger = LoggerFactory.getLogger(Versions.class);

  public static final Version VERSION_0 = Version.forIntegers(0, 0, 0);

  private Versions() {
    // do not instantiate
  }

  public static Optional<Version> valueOf(String version) {
    try {
      return Optional.of(Version.valueOf(version));
    } catch (IllegalArgumentException | ParseException e) {
      logger.debug("Cannot parse {} as version.", version, e);
      return Optional.empty();
    }
  }

  public static boolean isNormal(Version version) {
    return version.getPreReleaseVersion().isEmpty();
  }

  public static Version getNormal(Version version) {
    return Version.forIntegers(
        version.getMajorVersion(), version.getMinorVersion(), version.getPatchVersion());
  }

  public static Version incrementNormal(Version version, Scope scope) {
    switch (scope) {
      case MAJOR:
        return version.incrementMajorVersion();
      case MINOR:
        return version.incrementMinorVersion();
      case PATCH:
        return version.incrementPatchVersion();
      default:
        throw new AssertionError("Invalid scope: " + scope);
    }
  }
}
