package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.function.UnaryOperator;

public enum Scope {
    MAJOR(Version::incrementMajorVersion),
    MINOR(Version::incrementMinorVersion),
    PATCH(Version::incrementPatchVersion);

    private final Versioner versioner;

    Scope(UnaryOperator<Version> incrementer) {
        this.versioner = (base, vcs) -> {
            Version prevVersion = vcs.getPreviousVersion().orElse(base);
            Version prevRelease = vcs.getPreviousRelease().orElse(base);
            Version incremented = incrementer.apply(prevRelease);
            if (incremented.getNormalVersion().equals(prevVersion.getNormalVersion())) {
                return prevVersion;
            } else {
                return incremented;
            }
        };
    }

    public Versioner getVersioner() {
        return versioner;
    }
}
