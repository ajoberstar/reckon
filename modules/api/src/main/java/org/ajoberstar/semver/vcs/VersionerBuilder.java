package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Objects;

public class VersionerBuilder {
    private Versioner normal;
    private Versioner preRelease;
    private Versioner buildMetadata;

    VersionerBuilder() {
        this.normal = null;
        this.preRelease = null;
        this.buildMetadata = Versioner.IDENTITY;
    }

    public VersionerBuilder useScope(Scope scope) {
        this.normal = (identity, vcs) -> {
            Version previous = vcs.getPreviousVersion().orElse(identity);
            Version base = vcs.getPreviousRelease().orElse(identity);
            Version incremented = applyScope(base, scope);
            if (incremented.getNormalVersion().equals(previous.getNormalVersion())) {
                return previous;
            } else {
                return incremented;
            }
        };
        return this;
    }

    public VersionerBuilder useFinal() {
        this.preRelease = (base, vcs) -> Version.valueOf(base.getNormalVersion());
        this.buildMetadata = Versioner.IDENTITY;
        return this;
    }

    public VersionerBuilder useFixedStage(String stage) {
        Objects.requireNonNull(stage, "Stage cannot be null.");
        this.preRelease = (base, vcs) -> {
            if (stage.equals(parseStage(base))) {
                return base.incrementPreReleaseVersion();
            } else {
                return base.setPreReleaseVersion(stage).incrementPreReleaseVersion();
            }
        };
        return this;
    }

    public VersionerBuilder useFloatingStage(String stage) {
        Objects.requireNonNull(stage, "Stage cannot be null.");
        this.preRelease = (base, vcs) -> {
            String previousStage = parseStage(base);
            if (stage.equals(previousStage)) {
                return base.incrementPreReleaseVersion();
            } else if (stage.compareTo(previousStage) > 0) {
                return base.setPreReleaseVersion(stage).incrementPreReleaseVersion();
            } else {
                return base.setPreReleaseVersion(base.getPreReleaseVersion() + "." + stage).incrementPreReleaseVersion();
            }
        };
        return this;
    }

    public VersionerBuilder useSnapshotStage() {
        this.preRelease = (base, vcs) -> base.setPreReleaseVersion("SNAPSHOT");
        return this;
    }

    public VersionerBuilder useBuildMetadata(Versioner buildMetadata) {
        Objects.requireNonNull(buildMetadata, "Build metadata versioner cannot be null.");
        this.buildMetadata = buildMetadata;
        return this;
    }

    public Versioner build() {
        Objects.requireNonNull(normal, "Normal versioner cannot be null.");
        Objects.requireNonNull(preRelease, "PreRelease versioner cannot be null.");
        Objects.requireNonNull(buildMetadata, "Build metadata versioner cannot be null.");
        Versioner versioner = buildMetadata.compose(preRelease).compose(normal);
        return (base, vcs) -> {
            Version inferred = versioner.infer(base, vcs);
            Version previous = vcs.getPreviousVersion().orElse(base);
            if (inferred.greaterThanOrEqualTo(previous)) {
                return inferred;
            } else {
                throw new IllegalArgumentException("Inferred version (" + inferred + ") must have higher precedence than previous (" + previous + ")");
            }
        };
    }

    private static Version applyScope(Version base, Scope scope) {
        switch (scope) {
            case MAJOR:
                return base.incrementMajorVersion();
            case MINOR:
                return base.incrementMinorVersion();
            case PATCH:
                return base.incrementPatchVersion();
            default:
                throw new IllegalArgumentException("Invalid scope: " + scope);
        }
    }

    private static String parseStage(Version inferred) {
        String[] preRelease = inferred.getPreReleaseVersion().split("\\.");
        return preRelease[0];
    }
}
