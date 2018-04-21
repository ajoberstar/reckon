package org.ajoberstar.reckon.core;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public final class VcsInventory {
  private final String commitId;
  private final boolean clean;
  private final Version currentVersion;
  private final int commitsSinceBase;
  private final Version baseVersion;
  private final Version baseNormal;
  private final Set<Version> parallelNormals;
  private final Set<Version> claimedVersions;

  public VcsInventory(
      String commitId,
      boolean clean,
      Version currentVersion,
      Version baseVersion,
      Version baseNormal,
      int commitsSinceBase,
      Set<Version> parallelNormals,
      Set<Version> claimedVersions) {
    if (commitsSinceBase < 0) {
      throw new IllegalArgumentException("Commits since base must be 0 or greater: " + commitsSinceBase);
    }

    this.commitId = commitId;
    this.clean = clean;
    this.currentVersion = currentVersion;
    this.baseVersion = Optional.ofNullable(baseVersion).orElse(Versions.VERSION_0);
    this.baseNormal = Optional.ofNullable(baseNormal).orElse(Versions.VERSION_0);
    this.commitsSinceBase = commitsSinceBase;
    this.parallelNormals = Optional.ofNullable(parallelNormals)
        .map(Collections::unmodifiableSet)
        .orElse(Collections.emptySet());
    this.claimedVersions = Optional.ofNullable(claimedVersions)
        .map(Collections::unmodifiableSet)
        .orElse(Collections.emptySet());
  }

  public Optional<String> getCommitId() {
    return Optional.ofNullable(commitId);
  }

  public boolean isClean() {
    return clean;
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
