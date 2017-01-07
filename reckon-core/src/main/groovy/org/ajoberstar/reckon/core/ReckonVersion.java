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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ReckonVersion implements Comparable<ReckonVersion> {
  public static final ReckonVersion VERSION_0 = new ReckonVersion(Version.forIntegers(0, 0, 0));

  private final Version version;

  private ReckonVersion(Version version) {
    this.version = version;
  }

  public ReckonVersion getNormal() {
    return new ReckonVersion(
        Version.forIntegers(
            version.getMajorVersion(), version.getMinorVersion(), version.getPatchVersion()));
  }

  public boolean isNormal() {
    return version.getPreReleaseVersion().isEmpty();
  }

  @Override
  public boolean equals(Object other) {
    return EqualsBuilder.reflectionEquals(this, other);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public int compareTo(ReckonVersion other) {
    return version.compareTo(other.version);
  }

  @Override
  public String toString() {
    return version.toString();
  }

  public static Optional<ReckonVersion> valueOf(String version) {
    try {
      return Optional.of(new ReckonVersion(Version.valueOf(version)));
    } catch (IllegalArgumentException | ParseException ignored) {
      // explicitly ignoring the exception, since it just indicates this isn't a version
      return Optional.empty();
    }
  }
}
