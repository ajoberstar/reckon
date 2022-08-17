package org.ajoberstar.reckon.gradle;

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.ajoberstar.grgit.gradle.GrgitService;
import org.ajoberstar.reckon.core.*;
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

  private final Property<String> remote;

  private VersionTagParser tagParser;
  private VersionTagWriter tagWriter;
  private final Provider<VersionTagParser> tagParserProvider;
  private final Provider<VersionTagWriter> tagWriterProvider;

  private final Property<String> tagMessage;

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

    this.remote = objectFactory.property(String.class);

    this.tagParser = null;
    this.tagWriter = null;
    this.tagParserProvider = providerFactory.provider(() -> this.tagParser);
    this.tagWriterProvider = providerFactory.provider(() -> this.tagWriter);

    this.tagMessage = objectFactory.property(String.class);
  }

  public void setDefaultInferredScope(String scope) {
    this.reckoner.defaultInferredScope(Scope.from(scope));
  }

  public void setParallelBranchScope(String scope) {
    this.reckoner.parallelBranchScope(Scope.from(scope));
  }

  public void stages(String... stages) {
    this.reckoner.stages(stages);
  }

  public void snapshots() {
    this.reckoner.snapshots();
  }

  public void setScopeCalc(ScopeCalculator scopeCalc) {
    this.reckoner.scopeCalc(scopeCalc);
  }

  public void setStageCalc(StageCalculator stageCalc) {
    this.reckoner.stageCalc(stageCalc);
  }

  public ScopeCalculator calcScopeFromProp() {
    return ScopeCalculator.ofUserString(inventory -> Optional.ofNullable(scope.getOrNull()));
  }

  public ScopeCalculator calcScopeFromCommitMessages() {
    return ScopeCalculator.ofCommitMessages();
  }

  public ScopeCalculator calcScopeFromCommitMessages(Function<String, Optional<Scope>> messageParser) {
    return ScopeCalculator.ofCommitMessage(messageParser);
  }

  public StageCalculator calcStageFromProp() {
    return StageCalculator.ofUserString((inventory, targetNormal) -> Optional.ofNullable(stage.getOrNull()));
  }

  @Deprecated
  public ReckonExtension scopeFromProp() {
    this.reckoner.scopeCalc(calcScopeFromProp());
    return this;
  }

  @Deprecated
  public ReckonExtension stageFromProp(String... stages) {
    this.reckoner.stages(stages);
    this.reckoner.stageCalc(calcStageFromProp());
    return this;
  }

  @Deprecated
  public ReckonExtension snapshotFromProp() {
    this.reckoner.snapshots();
    this.reckoner.stageCalc(calcStageFromProp());
    return this;
  }

  public Property<String> getRemote() {
    return remote;
  }

  public Provider<VersionTagParser> getTagParser() {
    return tagParserProvider;
  }

  public void setTagParser(VersionTagParser tagParser) {
    this.tagParser = tagParser;
  }

  public Provider<VersionTagWriter> getTagWriter() {
    return tagWriterProvider;
  }

  public void setTagWriter(VersionTagWriter tagWriter) {
    this.tagWriter = tagWriter;
  }

  public Property<String> getTagMessage() {
    return tagMessage;
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
      reckoner.git(repo, tagParser);
    } catch (Exception e) {
      // no git repo found
      reckoner.git(null);
    }

    var version = reckoner.build().reckon();
    logger.warn("Reckoned version: {}", version);
    return version;
  }
}
