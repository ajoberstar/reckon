package org.ajoberstar.reckon.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.ajoberstar.reckon.core.Reckoner;
import org.ajoberstar.reckon.core.Version;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "infer",
    description = "Infer a version from the project's .git directory.")
public class InferCommand implements Callable<Integer> {
  @Option(names = "--git-dir", paramLabel = "PATH", description = "Path to .git directory of the repo to infer from.")
  Path gitDir;

  @Option(names = "--allow-stages", paramLabel = "STAGES", split = ",", description = "The allowed stage names for pre-release builds. Cannot be used with --allow-snapshots.")
  String[] allowStages;

  @Option(names = "--allow-snapshots", description = "Enables the use of SNAPSHOT pre-releases. Cannot be used with --allow-stages.")
  boolean allowSnapshots;

  @Option(names = "--scope", paramLabel = "{major|minor|patch}", description = "The scope of change since the last final version.")
  String scope;

  @Option(names = "--stage", paramLabel = "STAGE", description = "The stage of development for this version.")
  String stage;

  @Override
  public Integer call() throws Exception {
    Reckoner.Builder builder = Reckoner.builder();

    builder.git(openRepo(gitDir));

    if (allowSnapshots) {
      builder.snapshots();
    } else {
      builder.stages(allowStages);
    }

    builder.scopeCalc(inventory -> Optional.ofNullable(scope));
    builder.stageCalc((inventory, targetNormal) -> Optional.ofNullable(stage)
        .filter(s -> !s.trim().isEmpty())
        .filter(s -> !"null".equals(s)));

    Version version = builder.build().reckon();

    System.out.println(version);

    return 0;
  }

  private Repository openRepo(Path dir) throws Exception {
    File dirFile = Optional.ofNullable(dir)
        .map(Path::toFile)
        .orElse(null);

    return new FileRepositoryBuilder()
        .setGitDir(dirFile)
        .readEnvironment()
        .findGitDir()
        .build();
  }
}
