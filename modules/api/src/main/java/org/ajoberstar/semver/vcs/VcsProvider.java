package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Optional;
import java.util.Set;

public interface VcsProvider<R> {
	R getVcs();
    Optional<Version> getCurrentVersion();
    Optional<Version> getPreviousRelease();
    Optional<Version> getPreviousVersion();
    Optional<String> getCurrentRevisionId();
    Optional<String> getCurrentBranchName();
    boolean isUnmodified();
}
