package org.ajoberstar.reckon.gradle;

import org.ajoberstar.reckon.core.Version;

/*
 * Allow Kotlin build scripts to gain access to the Version object without exposing implementation
 * details/classes as well as other plugins to extend/implement their own version provider
 */
public interface VersionProvider {
  Version getVersion();
}
