package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Optional;

public interface VersionTranslator {
    Optional<Version> parseName(String name);
    Optional<String> emitName(Version version);
}
