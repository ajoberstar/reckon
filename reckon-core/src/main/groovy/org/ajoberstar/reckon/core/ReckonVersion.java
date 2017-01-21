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
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReckonVersion implements Comparable<ReckonVersion> {
  private static final Pattern STAGE_REGEX =
      Pattern.compile("^(?<name>\\w+)(?:\\.(?<num>\\d+)(?:\\.(?<commits>\\d+))?)?");
  public static final ReckonVersion VERSION_0 = new ReckonVersion(Version.forIntegers(0, 0, 0));

  private static final Logger logger = LoggerFactory.getLogger(ReckonVersion.class);

  private final Version version;
  private final String stageName;
  private final int stageNum;
  private final int stageCommits;

  private ReckonVersion(Version version) {
    this.version = version;

    Matcher matcher = STAGE_REGEX.matcher(version.getPreReleaseVersion());
    if (matcher.find()) {
      this.stageName = matcher.group("name");
      this.stageNum = Optional.ofNullable(matcher.group("num")).map(Integer::parseInt).orElse(-1);
      this.stageCommits =
          Optional.ofNullable(matcher.group("commits")).map(Integer::parseInt).orElse(-1);
    } else {
      this.stageName = null;
      this.stageNum = -1;
      this.stageCommits = -1;
    }
  }

  public ReckonVersion getNormal() {
    return new ReckonVersion(
        Version.forIntegers(
            version.getMajorVersion(), version.getMinorVersion(), version.getPatchVersion()));
  }

  public boolean isNormal() {
    return version.getPreReleaseVersion().isEmpty();
  }

  public ReckonVersion incrementNormal(Scope scope) {
    switch (scope) {
      case MAJOR:
        return new ReckonVersion(version.incrementMajorVersion());
      case MINOR:
        return new ReckonVersion(version.incrementMinorVersion());
      case PATCH:
        return new ReckonVersion(version.incrementPatchVersion());
      default:
        throw new AssertionError("Invalid scope: " + scope);
    }
  }

  public ReckonVersion incrementTaggedStage(String stage) {
    Objects.requireNonNull(stage, "Stage must be non-null.");
    if (stage.equals(stageName)) {
      int num = stageNum > 0 ? stageNum + 1 : 1;
      return new ReckonVersion(version.setPreReleaseVersion(stage + "." + num));
    } else {
      return new ReckonVersion(version.setPreReleaseVersion(stage + ".1"));
    }
  }

  public ReckonVersion incrementUntaggedStage(String defaultStage, int commits, String commitId) {
    Objects.requireNonNull(defaultStage, "Default stage must be non-null.");
    Objects.requireNonNull(commitId, "Commit ID must be non-null.");
    if (commits < 1) {
      throw new IllegalArgumentException("Commits must be 1 or greater.");
    }
    if (stageName == null) {
      return new ReckonVersion(
          version.setPreReleaseVersion(defaultStage + ".0." + commits).setBuildMetadata(commitId));
    } else {
      return new ReckonVersion(
          version
              .setPreReleaseVersion(stageName + "." + stageNum + "." + commits)
              .setBuildMetadata(commitId));
    }
  }

  public boolean shouldBeTagged() {
    return stageCommits > 0;
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
    } catch (IllegalArgumentException | ParseException e) {
      logger.debug("Cannot parse {} as version.", version, e);
      return Optional.empty();
    }
  }
}
