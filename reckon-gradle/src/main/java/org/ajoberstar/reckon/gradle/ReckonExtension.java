package org.ajoberstar.reckon.gradle;

import java.util.Optional;

import org.ajoberstar.grgit.Grgit;
import org.ajoberstar.reckon.core.Reckoner;
import org.ajoberstar.reckon.core.VcsInventorySupplier;
import org.gradle.api.Project;

public class ReckonExtension {
  private static final String SCOPE_PROP = "reckon.scope";
  private static final String STAGE_PROP = "reckon.stage";
  private static final String SNAPSHOT_PROP = "reckon.snapshot";

  private Project project;
  private Grgit grgit;
  private Reckoner.Builder reckoner;

  public ReckonExtension(Project project) {
    this.project = project;
    this.reckoner = Reckoner.builder();
  }

  @Deprecated
  public void setVcsInventory(VcsInventorySupplier inventorySupplier) {
    project.getLogger().warn("reckon.vcsInventory = <vcs> is deprecated and will be removed in 1.0.0. Call reckon.git instead.");
    this.reckoner.vcs(inventorySupplier);
  }

  @Deprecated
  public void setNormal(ReckonExtension ext) {
    project.getLogger().warn("reckon.normal = scopeFromProp() is deprecated and will be removed in 1.0.0. Call reckon.scopeFromProp() instead.");
    // no op
  }

  @Deprecated
  public void setPreRelease(ReckonExtension ext) {
    project.getLogger().warn("reckon.preRelease = stageFromProp() or snapshotFromProp() is deprecated and will be removed in 1.0.0. Call reckon.stageFromProp() or reckon.snapshotFromProp() instead.");
    // no op
  }

  public ReckonExtension git(Grgit grgit) {
    this.grgit = grgit;
    this.reckoner.git(grgit.getRepository().getJgit().getRepository());
    return this;
  }

  public ReckonExtension scopeFromProp() {
    this.reckoner.scopeCalc(inventory -> findProperty(SCOPE_PROP));
    return this;
  }

  public ReckonExtension stageFromProp(String... stages) {
    this.reckoner.stages(stages);
    this.reckoner.stageCalc((inventory, targetNormal) -> findProperty(STAGE_PROP));
    return this;
  }

  public ReckonExtension snapshotFromProp() {
    this.reckoner.snapshots();
    this.reckoner.stageCalc((inventory, targetNormal) -> {
      Optional<String> stageProp = findProperty(STAGE_PROP);
      Optional<String> snapshotProp = findProperty(SNAPSHOT_PROP)
          .map(Boolean::parseBoolean)
          .map(isSnapshot -> isSnapshot ? "snapshot" : "final");

      snapshotProp.ifPresent(val -> {
        project.getLogger().warn("Property {} is deprecated and will be removed in 1.0.0. Use {} set to one of [snapshot, final].", SNAPSHOT_PROP, STAGE_PROP);
      });

      return stageProp.isPresent() ? stageProp : snapshotProp;
    });
    return this;
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
    if (grgit == null) {
      project.getLogger().warn("No VCS found/configured. Version will be 'unspecified'.");
      return "unspecified";
    } else {
      String version = reckoner.build().reckon().toString();
      project.getLogger().warn("Reckoned version: {}", version);
      return version;
    }
  }
}
