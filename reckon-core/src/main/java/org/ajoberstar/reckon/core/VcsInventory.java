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
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public final class VcsInventory {
  private final String commitId;
  private final Version currentVersion;
  private final int commitsSinceBase;
  private final Version baseVersion;
  private final Version baseNormal;
  private final Set<Version> parallelNormals;
  private final Set<Version> claimedVersions;

  public VcsInventory(
      String commitId,
      Version currentVersion,
      Version baseVersion,
      Version baseNormal,
      int commitsSinceBase,
      Set<Version> parallelNormals,
      Set<Version> claimedVersions) {
    if (commitsSinceBase < 0) {
      throw new IllegalArgumentException(
          "Commits since base must be 0 or greater: " + commitsSinceBase);
    }

    this.commitId = commitId;
    this.currentVersion = currentVersion;
    this.baseVersion = Optional.ofNullable(baseVersion).orElse(Versions.VERSION_0);
    this.baseNormal = Optional.ofNullable(baseNormal).orElse(Versions.VERSION_0);
    this.commitsSinceBase = commitsSinceBase;
    this.parallelNormals =
        Optional.ofNullable(parallelNormals)
            .map(Collections::unmodifiableSet)
            .orElse(Collections.emptySet());
    this.claimedVersions =
        Optional.ofNullable(claimedVersions)
            .map(Collections::unmodifiableSet)
            .orElse(Collections.emptySet());
  }

  public Optional<String> getCommitId() {
    return Optional.ofNullable(commitId);
  }

  public Optional<Version> getCurrentVersion() {
    return Optional.ofNullable(currentVersion);
  }

  public int getCommitsSinceBase() {
    return commitsSinceBase;
  }

  public Version getBaseVersion() {
    return baseVersion;
  }

  public Version getBaseNormal() {
    return baseNormal;
  }

  public Set<Version> getParallelNormals() {
    return parallelNormals;
  }

  public Set<Version> getClaimedVersions() {
    return claimedVersions;
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
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
