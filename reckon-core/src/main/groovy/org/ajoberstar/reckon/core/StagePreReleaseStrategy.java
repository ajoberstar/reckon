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
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StagePreReleaseStrategy implements PreReleaseStrategy {
  public static final String FINAL_STAGE = "final";
  private static final Pattern STAGE_REGEX = Pattern.compile("^(?<name>\\w+)(?:\\.(?<num>\\d+))?");

  private final Set<String> stages;
  private final String defaultStage;
  private final Supplier<String> stageSupplier;

  public StagePreReleaseStrategy(Set<String> stages, Supplier<String> stageSupplier) {
    this.stages = stages;
    this.defaultStage =
        stages
            .stream()
            .sorted()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No stages provided."));
    this.stageSupplier = stageSupplier;
  }

  @Override
  public Version reckonTargetVersion(VcsInventory inventory, Version targetNormal) {
    String stage = stageSupplier.get();

    if (!stages.contains(stage)) {
      String message = String.format("Stage \"%s\" is not one of: %s", stage, stages);
      throw new IllegalArgumentException(message);
    }

    if (FINAL_STAGE.equals(stage)) {
      return targetNormal;
    }

    Version targetBase =
        targetNormal.equals(Versions.getNormal(inventory.getBaseVersion()))
            ? inventory.getBaseVersion()
            : targetNormal;

    String stageName;
    int stageNum;
    Matcher matcher = STAGE_REGEX.matcher(targetBase.getPreReleaseVersion());
    if (matcher.find()) {
      stageName = matcher.group("name");
      stageNum = Optional.ofNullable(matcher.group("num")).map(Integer::parseInt).orElse(-1);
      Optional.ofNullable(matcher.group("commits")).map(Integer::parseInt).orElse(-1);
    } else {
      stageName = null;
      stageNum = -1;
    }

    if (stage == null) {
      if (stageName == null) {
        return targetBase
            .setPreReleaseVersion(defaultStage + ".0." + inventory.getCommitsSinceBase())
            .setBuildMetadata(inventory.getCommitId());
      } else {
        return targetBase
            .setPreReleaseVersion(
                stageName + "." + stageNum + "." + inventory.getCommitsSinceBase())
            .setBuildMetadata(inventory.getCommitId());
      }
    } else {
      if (stage.equals(stageName)) {
        int num = stageNum > 0 ? stageNum + 1 : 1;
        return targetBase.setPreReleaseVersion(stage + "." + num);
      } else {
        return targetBase.setPreReleaseVersion(stage + ".1");
      }
    }
  }
}
