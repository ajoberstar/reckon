package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Objects;
import java.util.Arrays;

public final class Versioners {
    private Versioners() {
        throw new AssertionError("Cannot instantiate this class.");
    }

    public static final Version VERSION_0 = Version.forIntegers(0, 0, 0);

    public static Versioner identity() {
        return (base, vcs) -> base;
    }

    public static Versioner force(String version) {
        Version forced = Version.valueOf(version);
        return (base, vcs) -> forced;
    }

    public static Versioner rebuild() {
        return (base, vcs) -> vcs.getCurrentVersion()
                .orElseThrow(() -> new IllegalStateException("Cannot rebuild since current revision doesn't have a version."));
    }

    public static Versioner useScope(Scope scope) {
        return (identity, vcs) -> {
            Version previous = vcs.getPreviousVersion().orElse(identity);
            Version base = vcs.getPreviousRelease().orElse(identity);
            Version incremented = applyScope(base, scope);
            if (incremented.getNormalVersion().equals(previous.getNormalVersion())) {
                return previous;
            } else {
                return incremented;
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

    public static Versioner useFinalStage() {
        return (base, vcs) -> Version.valueOf(base.getNormalVersion());
    }

    public static Versioner useFixedStage(String stage) {
        Objects.requireNonNull(stage, "Stage cannot be null.");
        return (base, vcs) -> {
            String[] previousStage = parseStage(base);
            if (stage.equals(previousStage[0])) {
                String stageBase = String.join(".", previousStage);
                return base.setPreReleaseVersion(stageBase).incrementPreReleaseVersion();
            } else {
                return base.setPreReleaseVersion(stage).incrementPreReleaseVersion();
            }
        };
    }

    public static Versioner useFloatingStage(String stage) {
        Objects.requireNonNull(stage, "Stage cannot be null.");
        return (base, vcs) -> {
            String[] previousStage = parseStage(base);
            if (stage.equals(previousStage[0])) {
                return base.incrementPreReleaseVersion();
            } else if (stage.compareTo(previousStage[0]) > 0) {
                return base.setPreReleaseVersion(stage).incrementPreReleaseVersion();
            } else {
                return base.setPreReleaseVersion(base.getPreReleaseVersion() + "." + stage).incrementPreReleaseVersion();
            }
        };
    }

    public static Versioner useSnapshotStage() {
        return (base, vcs) -> base.setPreReleaseVersion("SNAPSHOT");
    }

    private static String[] parseStage(Version inferred) {
        String[] preRelease = inferred.getPreReleaseVersion().split("\\.");
        boolean hasCount = preRelease.length == 1 ? false : preRelease[1].chars()
            .allMatch(Character::isDigit);
        if (hasCount) {
            return Arrays.copyOfRange(preRelease, 0, 2);
        } else {
            return Arrays.copyOfRange(preRelease, 0, 1);
        }
    }

    public static Versioner enforcePrecedence() {
        return (base, vcs) -> {
            Version previous = vcs.getPreviousVersion().orElse(base);
            if (base.greaterThanOrEqualTo(previous)) {
                return base;
            } else {
                throw new IllegalArgumentException("Inferred version (" + base + ") must have higher precedence than previous (" + previous + ")");
            }
        };
    }
}
