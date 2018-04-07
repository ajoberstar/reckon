package org.ajoberstar.reckon.gradle;

import com.github.zafarkhaja.semver.Version;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ajoberstar.grgit.Grgit;
import org.ajoberstar.reckon.core.NormalStrategy;
import org.ajoberstar.reckon.core.PreReleaseStrategy;
import org.ajoberstar.reckon.core.Reckoner;
import org.ajoberstar.reckon.core.VcsInventory;
import org.ajoberstar.reckon.core.VcsInventorySupplier;
import org.ajoberstar.reckon.core.git.GitInventorySupplier;
import org.ajoberstar.reckon.core.strategy.ScopeNormalStrategy;
import org.ajoberstar.reckon.core.strategy.SnapshotPreReleaseStrategy;
import org.ajoberstar.reckon.core.strategy.StagePreReleaseStrategy;
import org.gradle.api.Project;

public class ReckonExtension {
  private static final String SCOPE_PROP = "reckon.scope";
  private static final String STAGE_PROP = "reckon.stage";
  private static final String SNAPSHOT_PROP = "reckon.snapshot";

  private Project project;
  private Grgit grgit;
  private VcsInventorySupplier vcsInventory;
  private NormalStrategy normal;
  private PreReleaseStrategy preRelease;

  public ReckonExtension(Project project) {
    this.project = project;
  }

  public void setVcsInventory(VcsInventorySupplier vcsInventory) {
    this.vcsInventory = vcsInventory;
  }

  public void setNormal(NormalStrategy normal) {
    this.normal = normal;
  }

  public void setPreRelease(PreReleaseStrategy preRelease) {
    this.preRelease = preRelease;
  }

  public VcsInventorySupplier git(Grgit grgit) {
    this.grgit = grgit;
    return new GitInventorySupplier(grgit.getRepository().getJgit().getRepository());
  }

  public NormalStrategy scopeFromProp() {
    Function<VcsInventory, Optional<String>> supplier = ignored -> Optional.ofNullable(project.findProperty(SCOPE_PROP)).map(Object::toString);
    return new ScopeNormalStrategy(supplier);
  }

  public PreReleaseStrategy stageFromProp(String... stages) {
    Set<String> stageSet = Arrays.stream(stages).collect(Collectors.toSet());
    BiFunction<VcsInventory, Version, Optional<String>> supplier = (inventory, targetNormal) -> Optional.ofNullable(project.findProperty(STAGE_PROP)).map(Object::toString);
    return new StagePreReleaseStrategy(stageSet, supplier);
  }

  public PreReleaseStrategy snapshotFromProp() {
    BiFunction<VcsInventory, Version, Boolean> supplier = (inventory, targetNormal) -> Optional.ofNullable(project.findProperty(SNAPSHOT_PROP))
        .map(Object::toString)
        .map(Boolean::parseBoolean)
        .orElse(true);
    return new SnapshotPreReleaseStrategy(supplier);
  }

  Grgit getGrgit() {
    return grgit;
  }

  String reckonVersion() {
    if (vcsInventory == null) {
      project.getLogger().warn("No VCS found/configured. Version will be 'unspecified'.");
      return "unspecified";
    } else if (normal == null || preRelease == null) {
      throw new IllegalStateException("Must provide strategies for normal and preRelease on the reckon extension.");
    } else {
      String version = Reckoner.reckon(vcsInventory, normal, preRelease);
      project.getLogger().warn("Reckoned version: {}", version);
      return version;
    }
  }
}
