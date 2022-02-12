package org.ajoberstar.reckon.core;

@FunctionalInterface
public interface VersionTagWriter {
  String write(Version version);

  static VersionTagWriter getDefault() {
    return Version::toString;
  }
}
