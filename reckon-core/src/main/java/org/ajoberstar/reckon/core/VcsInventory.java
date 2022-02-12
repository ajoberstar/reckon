package org.ajoberstar.reckon.core;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * An inventory of the state of your VCS and the versions tagged within it.
 */
public final class VcsInventory {
  private final String commitId;
  private final boolean clean;
  private final Version currentVersion;
  private final int commitsSinceBase;
  private final Version baseVersion;
  private final Version baseNormal;
  private final Set<Version> parallelNormals;
  private final Set<Version> claimedVersions;
  private final List<String> commitMessages;

  /**
   * This is intentionally package private.
   */
  VcsInventory(
      String commitId,
      boolean clean,
      Version currentVersion,
      Version baseVersion,
      Version baseNormal,
      int commitsSinceBase,
      Set<Version> parallelNormals,
      Set<Version> claimedVersions,
      List<String> commitMessages) {
    if (commitsSinceBase < 0) {
      throw new IllegalArgumentException("Commits since base must be 0 or greater: " + commitsSinceBase);
    }

    this.commitId = commitId;
    this.clean = clean;
    this.currentVersion = currentVersion;
    this.baseVersion = Optional.ofNullable(baseVersion).orElse(Version.IDENTITY);
    this.baseNormal = Optional.ofNullable(baseNormal).orElse(Version.IDENTITY);
    this.commitsSinceBase = commitsSinceBase;
    this.parallelNormals = Optional.ofNullable(parallelNormals)
        .map(Collections::unmodifiableSet)
        .orElse(Collections.emptySet());
    this.claimedVersions = Optional.ofNullable(claimedVersions)
        .map(Collections::unmodifiableSet)
        .orElse(Collections.emptySet());
    this.commitMessages = Optional.ofNullable(commitMessages)
        .map(Collections::unmodifiableList)
        .orElse(Collections.emptyList());
  }

  /**
   * The ID of the current commit, if any, in the active branch of the repository.
   */
  public Optional<String> getCommitId() {
    return Optional.ofNullable(commitId);
  }

  /**
   * Whether the repository has any uncommitted changes.
   */
  public boolean isClean() {
    return clean;
  }

  /**
   * Gets the version tagged on the current commit of the repository, if any.
   */
  public Optional<Version> getCurrentVersion() {
    return Optional.ofNullable(currentVersion);
  }

  /**
   * Number of commits between the current commmit and the base normal version tag.
   */
  public int getCommitsSinceBase() {
    return commitsSinceBase;
  }

  /**
   * The most recent (based on ancestry, not time) tagged version from the current commit. May be a
   * pre-release version, but could be the same as baseNormal.
   */
  public Version getBaseVersion() {
    return baseVersion;
  }

  /**
   * The most recent (based on ancestry, not time) tagged final version from the current commit.
   */
  public Version getBaseNormal() {
    return baseNormal;
  }

  /**
   * Any normal versions under development in other branches.
   */
  public Set<Version> getParallelNormals() {
    return parallelNormals;
  }

  /**
   * Any versions that have already been released or otherwise claimed.
   */
  public Set<Version> getClaimedVersions() {
    return claimedVersions;
  }

  /**
   * All commit messages between the current HEAD commit and the base verison's commit.
   */
  public List<String> getCommitMessages() {
    return commitMessages;
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
