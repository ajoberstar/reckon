package org.ajoberstar.reckon.gradle;

import java.util.Optional;
import java.util.function.Function;

import org.ajoberstar.grgit.Grgit;
import org.ajoberstar.grgit.Repository;
import org.ajoberstar.reckon.core.Reckoner;
import org.ajoberstar.reckon.core.Version;
import org.eclipse.jgit.api.Git;
import org.gradle.api.Project;

public class ReckonExtension {
  private static final String SCOPE_PROP = "reckon.scope";
  private static final String STAGE_PROP = "reckon.stage";
  private static final String SNAPSHOT_PROP = "reckon.snapshot";

  private Project project;
  private Grgit grgit;
  private Reckoner.Builder reckoner;

  public ReckonExtension(Project project, Grgit grgit) {
    this.project = project;
    this.grgit = grgit;
    this.reckoner = Reckoner.builder();
    this.reckoner.git(gitRepo());
  }

  private org.eclipse.jgit.lib.Repository gitRepo() {
    return Optional.ofNullable(grgit)
        .map(Grgit::getRepository)
        .map(Repository::getJgit)
        .map(Git::getRepository)
        .orElse(null);
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

  public ReckonExtension tagSelector(Function<String, Optional<String>> tagSelector) {
    this.reckoner.git(gitRepo(), tagSelector);
    return this;
  }

  private Optional<String> findProperty(String name) {
    return Optional.ofNullable(project.findProperty(name))
        // composite builds have a parent Gradle build and can't trust the values of these properties
        .filter(value -> project.getGradle().getParent() == null)
        .map(Object::toString);
  }

  Version reckonVersion() {
    Version version = reckoner.build().reckon();
    project.getLogger().warn("Reckoned version: {}", version);
    return version;
  }
}
