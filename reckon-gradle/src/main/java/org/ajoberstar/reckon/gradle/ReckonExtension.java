package org.ajoberstar.reckon.gradle;

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.ajoberstar.reckon.core.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

public class ReckonExtension {
  private static final Logger logger = Logging.getLogger(ReckonExtension.class);

  private final DirectoryProperty repoDirectory;
  private final Reckoner.Builder reckonerBuilder;
  private final Property<String> scope;
  private final Property<String> stage;
  private final Property<Version> version;

  private final Property<String> remote;

  private VersionTagParser tagParser;
  private VersionTagWriter tagWriter;
  private final Provider<String> tagName;

  private final Property<String> tagMessage;

  @Inject
  public ReckonExtension(ObjectFactory objectFactory, ProviderFactory providerFactory) {
    this.repoDirectory = objectFactory.directoryProperty();
    this.reckonerBuilder = Reckoner.builder();
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

    this.tagName = version.map(v -> {
      if (tagWriter == null || !v.isSignificant()) {
        return null;
      } else {
        return tagWriter.write(v);
      }
    });

    this.tagMessage = objectFactory.property(String.class);
  }

  public DirectoryProperty getRepoDirectory() {
    return repoDirectory;
  }

  public void setDefaultInferredScope(String scope) {
    setDefaultInferredScope(Scope.from(scope));
  }

  public void setDefaultInferredScope(Scope scope) {
    this.reckonerBuilder.defaultInferredScope(scope);
  }

  public void setParallelBranchScope(String scope) {
    setParallelBranchScope(Scope.from(scope));
  }

  public void setParallelBranchScope(Scope scope) {
    this.reckonerBuilder.parallelBranchScope(scope);
  }

  public void stages(String... stages) {
    this.reckonerBuilder.stages(stages);
  }

  public void snapshots() {
    this.reckonerBuilder.snapshots();
  }

  public void setScopeCalc(ScopeCalculator scopeCalc) {
    this.reckonerBuilder.scopeCalc(scopeCalc);
  }

  public void setStageCalc(StageCalculator stageCalc) {
    this.reckonerBuilder.stageCalc(stageCalc);
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

  public ScopeCalculator calcScopeFromCommitMessageParser(CommitMessageScopeParser messageParser) {
    return ScopeCalculator.ofCommitMessageParser(messageParser);
  }

  public StageCalculator calcStageFromProp() {
    return StageCalculator.ofUserString((inventory, targetNormal) -> Optional.ofNullable(stage.getOrNull()));
  }

  @Deprecated
  public ReckonExtension scopeFromProp() {
    this.reckonerBuilder.scopeCalc(calcScopeFromProp());
    return this;
  }

  @Deprecated
  public ReckonExtension stageFromProp(String... stages) {
    this.reckonerBuilder.stages(stages);
    this.reckonerBuilder.stageCalc(calcStageFromProp());
    return this;
  }

  @Deprecated
  public ReckonExtension snapshotFromProp() {
    this.reckonerBuilder.snapshots();
    this.reckonerBuilder.stageCalc(calcStageFromProp());
    return this;
  }

  public Property<String> getRemote() {
    return remote;
  }

  public void setTagParser(VersionTagParser tagParser) {
    this.tagParser = tagParser;
  }

  public Provider<String> getTagName() {
    return tagName;
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

  Property<String> getScope() {
    return scope;
  }

  Property<String> getStage() {
    return stage;
  }

  private Version reckonVersion() {
    try (var repo = openRepo()) {
      reckonerBuilder.git(repo, tagParser);

      Reckoner reckoner;
      try {
        reckoner = reckonerBuilder.build();
      } catch (Exception e) {
        throw new ReckonConfigurationException("Failed to configure Reckon: " + e.getMessage(), e);
      }

      var version = reckoner.reckon();
      logger.warn("Reckoned version: {}", version);
      return version;
    }
  }

  private Repository openRepo() {
    try {
      var builder = new FileRepositoryBuilder();
      builder.readEnvironment();
      builder.findGitDir(getRepoDirectory().getAsFile().get());
      if (builder.getGitDir() == null) {
        throw new IllegalStateException("No .git directory found!");
      }
      return builder.build();
    } catch (Exception e) {
      // no git repo found
      return null;
    }
  }
}
