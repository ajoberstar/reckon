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
        Version result = doInfer(scope, stage, fixed);
        boolean valid = provider.getPreviousVersion()
                .map(prev -> result.greaterThanOrEqualTo(prev))
                .orElse(true);

        if (valid) {
            return result;
        } else {
            throw new IllegalArgumentException("Inferred version " + result + " has lower precendence than previous " + provider.getPreviousVersion().orElse(Version.forIntegers(0)));
        }
    }

    private Version doInfer(ChangeScope scope, String stage, boolean fixed) {
        Version normal = applyScope(scope);
        if ("final".equals(stage)) {
            return normal;
        } else {
            return applyStage(normal, stage, fixed).incrementPreReleaseVersion();
        }
    }

    private Version applyScope(ChangeScope scope) {
        Version version = provider.getPreviousRelease().orElse(Version.forIntegers(0, 0, 0));
        switch (scope) {
            case MAJOR:
                return version.incrementMajorVersion();
            case MINOR:
                return version.incrementMinorVersion();
            case PATCH:
                return version.incrementPatchVersion();
            default:
                throw new IllegalArgumentException("Invalid scope: " + scope);
        }
    }

    private Version applyStage(Version version, String stage, boolean fixed) {
        Version previous = provider.getPreviousVersion().orElse(Version.forIntegers(0, 0, 0));
        if (version.getNormalVersion().equals(previous.getNormalVersion())) {
            if (stage.equals(parseStage(previous))) {
                return version.setPreReleaseVersion(previous.getPreReleaseVersion());
            } else if (fixed) {
                return version.setPreReleaseVersion(stage);
            } else {
                return version.setPreReleaseVersion(previous.getPreReleaseVersion() + "." + stage);
            }
        } else {
            return version.setPreReleaseVersion(stage);
        }
    }

    private String parseStage(Version version) {
        String[] preRelease = version.getPreReleaseVersion().split("\\.", 2);
        return preRelease[0];
    }
}
