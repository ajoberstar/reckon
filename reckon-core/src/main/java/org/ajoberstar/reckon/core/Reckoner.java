package org.ajoberstar.reckon.core;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary interface to Reckon. Use {@code builder} to get to an instance of {@code Reckoner}.
 */
public final class Reckoner {
  private static final Logger logger = LoggerFactory.getLogger(Reckoner.class);

  public static final String FINAL_STAGE = "final";
  public static final String SNAPSHOT_STAGE = "snapshot";

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");

  private final Clock clock;
  private final VcsInventorySupplier inventorySupplier;
  private final ScopeCalculator scopeCalc;
  private final StageCalculator stageCalc;
  private final Scope defaultInferredScope;
  private final Scope parallelBranchScope;
  private final Set<String> stages;
  private final String defaultStage;

  private Reckoner(Clock clock, VcsInventorySupplier inventorySupplier, ScopeCalculator scopeCalc, StageCalculator stageCalc, Scope defaultInferredScope, Scope parallelBranchScope, Set<String> stages, String defaultStage) {
    this.clock = clock;
    this.inventorySupplier = inventorySupplier;
    this.scopeCalc = scopeCalc;
    this.stageCalc = stageCalc;
    this.defaultInferredScope = defaultInferredScope;
    this.parallelBranchScope = parallelBranchScope;
    this.stages = stages;
    this.defaultStage = defaultStage;
  }

  /**
   * Infers the version that reflects the current state of your repository.
   *
   * @return the reckoned version
   */
  public Version reckon() {
    var inventory = inventorySupplier.getInventory();
    logger.debug("Retrieved the following VCS inventory: {}", inventory);
    var targetNormal = reckonNormal(inventory);
    var reckoned = reckonTargetVersion(inventory, targetNormal);

    if (reckoned.isSignificant() && !inventory.isClean()) {
      throw new IllegalStateException("Cannot release a final or significant stage without a clean repo.");
    }

    if (inventory.getClaimedVersions().contains(reckoned) && !inventory.getCurrentVersion().map(reckoned::equals).orElse(false)) {
      throw new IllegalStateException("Reckoned version " + reckoned + " has already been released.");
    }

    if (inventory.getClaimedVersions().contains(reckoned.getNormal()) && !inventory.getCurrentVersion().filter(reckoned.getNormal()::equals).isPresent() && reckoned.isSignificant()) {
      throw new IllegalStateException("Reckoned target normal version " + reckoned.getNormal() + " has already been released.");
    }

    if (inventory.getBaseVersion().compareTo(reckoned) > 0) {
      throw new IllegalStateException("Reckoned version " + reckoned + " is (and cannot be) less than base version " + inventory.getBaseVersion());
    }

    return reckoned;
  }

  private Version reckonNormal(VcsInventory inventory) {
    var providedScope = scopeCalc.calculate(inventory);

    Scope scope;
    if (providedScope.isPresent()) {
      scope = providedScope.get();
      logger.debug("Using provided scope value: {}", scope);
    } else {
      var inferredScope = Scope.infer(inventory.getBaseNormal(), inventory.getBaseVersion());
      scope = inferredScope.orElse(defaultInferredScope);
      logger.debug("Inferred scope from base version: {}", scope);
    }

    var targetNormal = inventory.getBaseNormal().incrementNormal(scope);
    var probableStage = stageCalc.calculate(inventory, targetNormal);

    // if a version's already being developed on a parallel branch we'll skip it
    if (inventory.getParallelNormals().contains(targetNormal) && probableStage.isPresent()) {
      if (scope.compareTo(parallelBranchScope) < 0) {
        logger.debug("Skipping {} as it's being developed on a parallel branch. While {} was requested, parallel branches claim a {}, using that instead.", scope, parallelBranchScope);
        targetNormal = targetNormal.incrementNormal(parallelBranchScope);
      } else {
        logger.debug("Skipping {} as it's being developed on a parallel branch. Incrementing again with {}", targetNormal, scope);
        targetNormal = targetNormal.incrementNormal(scope);
      }
    }

    return targetNormal;
  }

  private Version reckonTargetVersion(VcsInventory inventory, Version targetNormal) {
    var stage = stageCalc.calculate(inventory, targetNormal).orElse(null);

    if (stage != null && !stages.contains(stage)) {
      var message = String.format("Stage \"%s\" is not one of: %s", stage, stages);
      throw new IllegalArgumentException(message);
    }

    if (FINAL_STAGE.equals(stage)) {
      logger.debug("Using final stage.");
      return targetNormal;
    }

    // rebuild behavior
    if (inventory.isClean() && inventory.getCurrentVersion().isPresent() && stage == null) {
      var current = inventory.getCurrentVersion().get();
      logger.debug("Clean repo with current version and no provided stage. Treating as a rebuild of {}.", current);
      return current;
    }

    var targetBase = targetNormal.equals(inventory.getBaseVersion().getNormal()) ? inventory.getBaseVersion() : targetNormal;
    var baseStageName = targetBase.getStage().map(Version.Stage::getName).orElse(defaultStage);
    var baseStageNum = targetBase.getStage().map(Version.Stage::getNum).orElse(0);

    if (SNAPSHOT_STAGE.equals(defaultStage)) {
      logger.debug("Using snapshot stage.");
      return Version.valueOf(String.format("%s-%s", targetBase.getNormal(), "SNAPSHOT"));
    } else if (stage == null) {
      logger.debug("No stage provided. Treating as an insignificant version.");
      var buildMetadata = inventory.getCommitId()
          .filter(sha -> inventory.isClean())
          .orElseGet(() -> DATE_FORMAT.format(ZonedDateTime.now(clock)));

      return Version.valueOf(String.format("%s-%s.%d.%d+%s", targetBase.getNormal(), baseStageName, baseStageNum, inventory.getCommitsSinceBase(), buildMetadata));
    } else if (stage.equals(baseStageName)) {
      logger.debug("Provided stage {} is same as in target base {}. Incrementing the stage number.", stage, targetBase);
      return Version.valueOf(String.format("%s-%s.%d", targetBase.getNormal(), baseStageName, baseStageNum + 1));
    } else {
      logger.debug("Provided stage {} is different than in target base {}. Starting stage number at 1.", stage, targetBase);
      return Version.valueOf(String.format("%s-%s.%d", targetBase.getNormal(), stage, 1));
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Clock clock;
    private VcsInventorySupplier inventorySupplier;
    private ScopeCalculator scopeCalc;
    private StageCalculator stageCalc;
    private Scope defaultInferredScope = Scope.MINOR;
    private Scope parallelBranchScope = Scope.PATCH;
    private Set<String> stages;
    private String defaultStage;

    Builder clock(Clock clock) {
      this.clock = clock;
      return this;
    }

    Builder vcs(VcsInventorySupplier inventorySupplier) {
      this.inventorySupplier = inventorySupplier;
      return this;
    }

    /**
     * Use the given JGit repository to infer the state of Git.
     *
     * @param repo repository that the version should be inferred from
     * @return this builder
     */
    public Builder git(Repository repo) {
      return git(repo, null);
    }

    /**
     * Use the given JGit repository to infer the state of Git.
     *
     * @param repo repository that the version should be inferred from
     * @param tagParser a parser used to find versions from tag names
     * @return this builder
     */
    public Builder git(Repository repo, VersionTagParser tagParser) {
      if (repo == null) {
        this.inventorySupplier = () -> VcsInventory.empty(false);
      } else {
        var realParser = Optional.ofNullable(tagParser).orElse(VersionTagParser.getDefault());
        this.inventorySupplier = new GitInventorySupplier(repo, realParser);
      }
      return this;
    }

    /**
     * Use the given function to determine what scope should be used when inferring the version.
     *
     * @param scopeCalc the function that provides the scope
     * @return this builder
     */
    public Builder scopeCalc(ScopeCalculator scopeCalc) {
      this.scopeCalc = scopeCalc;
      return this;
    }

    public Builder defaultInferredScope(Scope defaultInferredScope) {
      this.defaultInferredScope = defaultInferredScope;
      return this;
    }

    public Builder parallelBranchScope(Scope parallelBranchScope) {
      this.parallelBranchScope = parallelBranchScope;
      return this;
    }

    /**
     * Use the given stages as valid options during inference.
     *
     * @param stages the valid stages
     * @return this builder
     */
    public Builder stages(String... stages) {
      this.stages = Arrays.stream(stages)
          .map(String::toLowerCase)
          .collect(Collectors.toSet());
      this.defaultStage = this.stages.stream()
          .filter(name -> !FINAL_STAGE.equals(name))
          .sorted()
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("No non-final stages provided."));

      if (this.stages.contains(SNAPSHOT_STAGE)) {
        throw new IllegalArgumentException("Snapshots are not supported in stage mode.");
      }

      return this;
    }

    /**
     * Use only the {@code snapshot} and {@code final} stages. Alternative to calling {@code stages()}.
     *
     * @return this builder
     */
    public Builder snapshots() {
      this.stages = Stream.of(Reckoner.FINAL_STAGE, Reckoner.SNAPSHOT_STAGE).collect(Collectors.toSet());
      this.defaultStage = Reckoner.SNAPSHOT_STAGE;
      return this;
    }

    /**
     * Use the given function to determine what staged should be used when inferring the version. This
     * must return a version contained in {@code stages()}.
     *
     * @param stageCalc the function that provides the stage
     * @return this builder
     */
    public Builder stageCalc(StageCalculator stageCalc) {
      this.stageCalc = stageCalc;
      return this;
    }

    /**
     * Builds the reckoner.
     *
     * @return the reckoner
     */
    public Reckoner build() {
      var clock = Optional.ofNullable(this.clock).orElseGet(Clock::systemUTC);
      Objects.requireNonNull(inventorySupplier, "Must provide a vcs.");
      Objects.requireNonNull(scopeCalc, "Must provide a scope supplier.");
      Objects.requireNonNull(defaultInferredScope, "Must provide a default inferred scope");
      Objects.requireNonNull(parallelBranchScope, "Must provide a parallel branch scope");
      Objects.requireNonNull(stages, "Must provide set of stages.");
      Objects.requireNonNull(stageCalc, "Must provide a stage supplier.");
      return new Reckoner(clock, inventorySupplier, scopeCalc, stageCalc, defaultInferredScope, parallelBranchScope, stages, defaultStage);
    }
  }
}
