package org.ajoberstar.reckon.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.lib.Repository;

public final class Reckoner {
  public static final String FINAL_STAGE = "final";
  public static final String SNAPSHOT_STAGE = "snapshot";

  private final VcsInventorySupplier inventorySupplier;
  private final Function<VcsInventory, Optional<String>> scopeCalc;
  private final BiFunction<VcsInventory, Version, Optional<String>> stageCalc;
  private final Set<String> stages;
  private final String defaultStage;

  private Reckoner(VcsInventorySupplier inventorySupplier, Function<VcsInventory, Optional<String>> scopeCalc, BiFunction<VcsInventory, Version, Optional<String>> stageCalc, Set<String> stages, String defaultStage) {
    this.inventorySupplier = inventorySupplier;
    this.scopeCalc = scopeCalc;
    this.stageCalc = stageCalc;
    this.stages = stages;
    this.defaultStage = defaultStage;
  }

  public Version reckon() {
    VcsInventory inventory = inventorySupplier.getInventory();
    Version targetNormal = reckonNormal(inventory);
    Version reckoned = reckonTargetVersion(inventory, targetNormal);

    if (inventory.getClaimedVersions().contains(reckoned) && !inventory.getCurrentVersion().map(reckoned::equals).orElse(false)) {
      throw new IllegalStateException("Reckoned version " + reckoned + " has already been released.");
    }

    if (inventory.getBaseVersion().compareTo(reckoned) > 0) {
      throw new IllegalStateException("Reckoned version " + reckoned + " is (and cannot be) less than base version " + inventory.getBaseVersion());
    }

    inventory.getCurrentVersion().ifPresent(current -> {
      if (inventory.isClean() && current.isFinal() && !reckoned.isFinal()) {
        throw new IllegalStateException("Cannot re-release a final version " + current + " as a pre-release: " + reckoned);
      }
    });

    return reckoned;
  }

  private Version reckonNormal(VcsInventory inventory) {
    Optional<Scope> providedScope = scopeCalc.apply(inventory).filter(value -> !value.isEmpty()).map(Scope::from);

    Scope scope;
    if (providedScope.isPresent()) {
      scope = providedScope.get();
    } else {
      Optional<Scope> inferredScope = Scope.infer(inventory.getBaseNormal(), inventory.getBaseVersion());
      scope = inferredScope.orElse(Scope.MINOR);
    }

    Version targetNormal = inventory.getBaseNormal().incrementNormal(scope);

    // if a version's already being developed on a parallel branch we'll skip it
    if (inventory.getParallelNormals().contains(targetNormal)) {
      targetNormal = targetNormal.incrementNormal(scope);
    }

    if (inventory.getClaimedVersions().contains(targetNormal) && !inventory.getCurrentVersion().filter(targetNormal::equals).isPresent()) {
      throw new IllegalStateException("Reckoned target normal version " + targetNormal + " has already been released.");
    }

    return targetNormal;
  }

  private Version reckonTargetVersion(VcsInventory inventory, Version targetNormal) {
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

    if (SNAPSHOT_STAGE.equals(defaultStage)) {
      return Version.valueOf(String.format("%s-%s", targetBase.getNormal(), "SNAPSHOT"));
    } else if (stage == null) {
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

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private VcsInventorySupplier inventorySupplier;
    private Function<VcsInventory, Optional<String>> scopeCalc;
    private BiFunction<VcsInventory, Version, Optional<String>> stageCalc;
    private Set<String> stages;
    private String defaultStage;

    Builder vcs(VcsInventorySupplier inventorySupplier) {
      this.inventorySupplier = inventorySupplier;
      return this;
    }

    public Builder git(Repository repo) {
      if (repo == null) {
        this.inventorySupplier = () -> new VcsInventory(null, false, null, null, null, 0, Collections.emptySet(), Collections.emptySet());
      } else {
        this.inventorySupplier = new GitInventorySupplier(repo);
      }
      return this;
    }

    public Builder scopeCalc(Function<VcsInventory, Optional<String>> scopeCalc) {
      this.scopeCalc = scopeCalc;
      return this;
    }

    public Builder stages(String... stages) {
      this.stages = Arrays.stream(stages).collect(Collectors.toSet());
      this.defaultStage = this.stages.stream()
          .filter(name -> !FINAL_STAGE.equals(name))
          .sorted()
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("No non-final stages provided."));
      return this;
    }

    public Builder snapshots() {
      this.stages = Stream.of(Reckoner.FINAL_STAGE, Reckoner.SNAPSHOT_STAGE).collect(Collectors.toSet());
      this.defaultStage = Reckoner.SNAPSHOT_STAGE;
      return this;
    }

    public Builder stageCalc(BiFunction<VcsInventory, Version, Optional<String>> stageCalc) {
      this.stageCalc = stageCalc;
      return this;
    }

    public Reckoner build() {
      Objects.requireNonNull(inventorySupplier, "Must provide a vcs.");
      Objects.requireNonNull(scopeCalc, "Must provide a scope supplier.");
      Objects.requireNonNull(stages, "Must provide set of stages.");
      Objects.requireNonNull(stageCalc, "Must provide a stage supplier.");
      return new Reckoner(inventorySupplier, scopeCalc, stageCalc, stages, defaultStage);
    }
  }
}
