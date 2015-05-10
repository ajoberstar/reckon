package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Optional;

public interface Vcs {
    Optional<Version> getCurrentVersion();
    Optional<Version> getPreviousRelease();
    Optional<Version> getPreviousVersion();
}
