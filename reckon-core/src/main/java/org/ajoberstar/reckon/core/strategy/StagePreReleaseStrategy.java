package org.ajoberstar.reckon.core.strategy;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.ajoberstar.reckon.core.PreReleaseStrategy;
import org.ajoberstar.reckon.core.VcsInventory;
import org.ajoberstar.reckon.core.Version;

public final class StagePreReleaseStrategy implements PreReleaseStrategy {
  public static final String FINAL_STAGE = "final";
  private static final Pattern STAGE_REGEX = Pattern.compile("^(?<name>\\w+)(?:\\.(?<num>\\d+))?");

  private final Set<String> stages;
  private final String defaultStage;
  private final BiFunction<VcsInventory, Version, Optional<String>> stageCalc;

  public StagePreReleaseStrategy(Set<String> stages, BiFunction<VcsInventory, Version, Optional<String>> stageCalc) {
    this.stages = stages;
    this.defaultStage = stages.stream()
        .filter(name -> !FINAL_STAGE.equals(name))
        .sorted()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No non-final stages provided."));
    this.stageCalc = stageCalc;
  }

  @Override
  public Version reckonTargetVersion(VcsInventory inventory, Version targetNormal) {
    String stage = stageCalc.apply(inventory, targetNormal)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .orElse(null);

    if (stage != null && !stages.contains(stage)) {
      String message = String.format("Stage \"%s\" is not one of: %s", stage, stages);
      throw new IllegalArgumentException(message);
    }

    if (stage != null && !inventory.isClean()) {
      throw new IllegalStateException("Cannot release a final or significant stage without a clean repo.");
    }

    if (FINAL_STAGE.equals(stage)) {
      return targetNormal;
    }

    // rebuild behavior
    if (inventory.isClean() && inventory.getCurrentVersion().isPresent() && stage == null) {
      Version current = inventory.getCurrentVersion().get();
      return current;
    }

    Version targetBase = targetNormal.equals(inventory.getBaseVersion().getNormal()) ? inventory.getBaseVersion() : targetNormal;
    String baseStageName = targetBase.getStage().map(Version.Stage::getName).orElse(defaultStage);
    int baseStageNum = targetBase.getStage().map(Version.Stage::getNum).orElse(0);

    if (stage == null) {
      String buildMetadata = inventory.getCommitId()
          .map(sha -> inventory.isClean() ? sha : sha + ".uncommitted")
          .orElse("uncommitted");

      return Version.valueOf(String.format("%s-%s.%d.%d+%s", targetBase.getNormal(), baseStageName, baseStageNum, inventory.getCommitsSinceBase(), buildMetadata));
    } else if (stage.equals(baseStageName)) {
      return Version.valueOf(String.format("%s-%s.%d", targetBase.getNormal(), baseStageName, baseStageNum + 1));
    } else {
      return Version.valueOf(String.format("%s-%s.%d", targetBase.getNormal(), stage, 1));
    }
  }
}
