package org.ajoberstar.reckon.gradle;

import java.io.File;

import org.ajoberstar.grgit.Grgit;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GrgitService implements BuildService<GrgitService.Params>, AutoCloseable {
  interface Params extends BuildServiceParameters {
    DirectoryProperty getProjectDirectory();
  }

  private final Logger logger = LoggerFactory.getLogger(GrgitService.class);

  private Grgit grgit;

  public Grgit getGrgit() {
    if (grgit == null) {
      File rootDirectory = getParameters().getProjectDirectory().getAsFile().get();
      try {
        grgit = Grgit.open(arg -> {
          arg.setCurrentDir(rootDirectory);
        });
      } catch (Exception e) {
        grgit = null;
      }
    }
    return grgit;
  }

  @Override
  public void close() {
    if (grgit != null) {
      logger.info("Closing Git repo: {}", grgit.getRepository().getRootDir());
      grgit.close();
    }
  }
}
