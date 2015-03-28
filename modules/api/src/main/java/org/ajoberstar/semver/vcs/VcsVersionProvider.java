package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Optional;
import java.util.Set;

public interface VcsVersionProvider {
    Optional<VcsVersion> getCurrentVersion();
    Optional<VcsVersion> getPreviousRelease();
    Optional<VcsVersion> getPreviousVersion();
    Set<VcsVersion> lookupPriorVersions();
    VcsVersion markCurrentRevisionAs(Version version);
}
