package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Objects;

public class Versioner {
    private final VcsProvider provider;

    public Versioner(VcsProvider provider) {
        this.provider = provider;
    }

    public Version rebuild() {
        return provider.getCurrentVersion()
                .orElseThrow(() -> new IllegalStateException("Cannot rebuild since no version on current revision."));
    }

    public Version infer(ChangeScope scope, String stage, boolean fixed) {
        Objects.requireNonNull(scope, "Scope cannot be null.");
        Objects.requireNonNull(stage, "Stage cannot be null.");

        Version previousRelease = provider.getPreviousRelease().orElse(Version.forIntegers(0, 0, 0));
        Version incremented = applyScope(previousRelease, scope);


        if ("final".equals(stage)) {
            return incremented;
        } else {
            Version previousVersion = provider.getPreviousVersion().orElse(Version.forIntegers(0, 0, 0));
            if (incremented.getNormalVersion().equals(previousVersion.getNormalVersion())) {
                String[] previousPreRelease = previousVersion.getPreReleaseVersion().split("\\.");
                boolean sameStage = stage.equals(previousPreRelease[0]);
                if (sameStage) {
                    return incremented.setPreReleaseVersion(previousVersion.getPreReleaseVersion()).incrementPreReleaseVersion();
                } else if (fixed) {
                    return incremented.setPreReleaseVersion(stage).incrementPreReleaseVersion();
                } else {
                    return incremented.setPreReleaseVersion(previousVersion.getPreReleaseVersion() + "." + stage).incrementPreReleaseVersion();
                }
            } else {
                return incremented.setPreReleaseVersion(stage).incrementPreReleaseVersion();
            }
        }
    }

    private Version applyScope(Version version, ChangeScope scope) {
        switch (scope) {
            case MAJOR:
                return version.incrementMajorVersion();
            case MINOR:
                return version.incrementMinorVersion();
            case PATCH:
                return version.incrementPatchVersion();
            default:
                throw new IllegalArgumentException("Scope cannot be null.");
        }
    }
}
