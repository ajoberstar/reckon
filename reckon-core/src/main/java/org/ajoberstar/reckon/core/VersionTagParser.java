package org.ajoberstar.reckon.core;

import java.util.Optional;

@FunctionalInterface
public interface VersionTagParser {
  Optional<Version> parse(String tagName);

  static VersionTagParser getDefault() {
    return tagName -> Version.parse(tagName.replaceAll("^v", ""));
  }
}
