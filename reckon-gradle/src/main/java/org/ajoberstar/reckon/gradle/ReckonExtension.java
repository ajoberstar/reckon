package org.ajoberstar.reckon.gradle;

import java.util.Optional;

import javax.inject.Inject;

import org.ajoberstar.grgit.gradle.GrgitService;
import org.ajoberstar.reckon.core.Reckoner;
import org.ajoberstar.reckon.core.Version;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

public class ReckonExtension {
  private static Logger logger = Logging.getLogger(ReckonExtension.class);

  private final Reckoner.Builder reckoner;
  private final Property<GrgitService> grgitService;
  private final Property<String> scope;
  private final Property<String> stage;
  private final Property<Version> version;

  @Inject
  public ReckonExtension(ObjectFactory objectFactory, ProviderFactory providerFactory) {
    this.reckoner = Reckoner.builder();
    this.grgitService = objectFactory.property(GrgitService.class);
    this.scope = objectFactory.property(String.class);
    this.stage = objectFactory.property(String.class);
    this.version = objectFactory.property(Version.class);

    var versionProvider = providerFactory.provider(this::reckonVersion);
    this.version.set(versionProvider);
    this.version.disallowChanges();
    this.version.finalizeValueOnRead();
  }

  public ReckonExtension scopeFromProp() {
    this.reckoner.scopeCalc(inventory -> Optional.ofNullable(scope.getOrNull()));
    return this;
  }

  public ReckonExtension stageFromProp(String... stages) {
    this.reckoner.stages(stages);
    this.reckoner.stageCalc((inventory, targetNormal) -> Optional.ofNullable(stage.getOrNull()));
    return this;
  }

  public ReckonExtension snapshotFromProp() {
    this.reckoner.snapshots();
    this.reckoner.stageCalc((inventory, targetNormal) -> Optional.ofNullable(stage.getOrNull()));
    return this;
  }

  public Provider<Version> getVersion() {
    return version;
  }

  Property<GrgitService> getGrgitService() {
    return grgitService;
  }

  Property<String> getScope() {
    return scope;
  }

  Property<String> getStage() {
    return stage;
  }

  private Version reckonVersion() {
    try {
      var git = grgitService.get().getGrgit();
      var repo = git.getRepository().getJgit().getRepository();
      reckoner.git(repo);
    } catch (Exception e) {
      // no git repo found
      reckoner.git(null);
    }

    var version = reckoner.build().reckon();
    logger.warn("Reckoned version: {}", version);
    return version;
  }
}
