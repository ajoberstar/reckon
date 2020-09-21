package org.ajoberstar.reckon.cli;

import java.io.InputStream;
import java.util.Properties;

import picocli.CommandLine.IVersionProvider;

class ReckonVersionProvider implements IVersionProvider {

  @Override
  public String[] getVersion() throws Exception {
    try (InputStream stream = this.getClass().getResourceAsStream("/META-INF/reckon/org.ajoberstar.reckon/reckon-cli/version.properties")) {
      Properties versionProps = new Properties();
      versionProps.load(stream);
      return new String[] {"reckon v" + versionProps.getProperty("version", "???")};
    }
  }
}
