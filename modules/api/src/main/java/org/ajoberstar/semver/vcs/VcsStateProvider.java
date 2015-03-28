package org.ajoberstar.semver.vcs;

import java.util.Optional;

public interface VcsStateProvider {
    Optional<String> getCurrentRevisionId();
    Optional<String> getCurrentBranchName();
    boolean isUnmodified();
}
