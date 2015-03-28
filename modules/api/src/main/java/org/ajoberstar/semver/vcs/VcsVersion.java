package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Optional;

public interface VcsVersion {
    Version getVersion();
    String getRevisionHint();
    Optional<String> getRevisionId();
}
