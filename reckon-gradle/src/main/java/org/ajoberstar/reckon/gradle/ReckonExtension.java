package org.ajoberstar.reckon.gradle;

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
import org.ajoberstar.reckon.core.Version;
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
    Function<VcsInventory, Optional<String>> supplier = inventory -> findProperty(SCOPE_PROP);
    return new ScopeNormalStrategy(supplier);
  }

  public PreReleaseStrategy stageFromProp(String... stages) {
    Set<String> stageSet = Arrays.stream(stages).collect(Collectors.toSet());
    BiFunction<VcsInventory, Version, Optional<String>> supplier = (inventory, targetNormal) -> findProperty(STAGE_PROP);
    return new StagePreReleaseStrategy(stageSet, supplier);
  }

  public PreReleaseStrategy snapshotFromProp() {
    BiFunction<VcsInventory, Version, Optional<String>> supplier = (inventory, targetNormal) -> {
      Optional<String> stageProp = findProperty(STAGE_PROP);
      Optional<String> snapshotProp = findProperty(SNAPSHOT_PROP)
          .map(Boolean::parseBoolean)
          .map(isSnapshot -> isSnapshot ? "snapshot" : "final");

      snapshotProp.ifPresent(val -> {
        project.getLogger().warn("Property {} is deprecated and will be removed in 1.0.0. Use {} set to one of [snapshot, final].", SNAPSHOT_PROP, STAGE_PROP);
      });

      return stageProp.isPresent() ? stageProp : snapshotProp;
    };
    return new SnapshotPreReleaseStrategy(supplier);
  }

  private Optional<String> findProperty(String name) {
    return Optional.ofNullable(project.findProperty(name))
        // composite builds have a parent Gradle build and can't trust the values of these properties
        .filter(value -> project.getGradle().getParent() == null)
        .map(Object::toString);
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
