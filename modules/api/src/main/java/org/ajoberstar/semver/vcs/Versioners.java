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
            Version sanitized = sanitizeStage(base, stage, true);
            String previousStage = sanitized.getPreReleaseVersion().split("\\.")[0];
            if (stage.equals(previousStage)) {
                return sanitized.incrementPreReleaseVersion();
            } else {
                return base.setPreReleaseVersion(stage).incrementPreReleaseVersion();
            }
        };
    }

    public static Versioner useFloatingStage(String stage) {
        Objects.requireNonNull(stage, "Stage cannot be null.");
        return (base, vcs) -> {
            Version sanitized = sanitizeStage(base, stage, false);
            String[] prevStages = sanitized.getPreReleaseVersion().split("\\.");
            if (Arrays.binarySearch(prevStages, stage) >= 0) {
                return sanitized.incrementPreReleaseVersion();
            } else if (stage.compareTo(prevStages[0]) > 0) {
                return sanitized.setPreReleaseVersion(stage).incrementPreReleaseVersion();
            } else {
                return sanitized.setPreReleaseVersion(sanitized.getPreReleaseVersion() + "." + stage).incrementPreReleaseVersion();
            }
        };
    }

    public static Versioner useSnapshotStage() {
        return (base, vcs) -> base.setPreReleaseVersion("SNAPSHOT");
    }

    private static Version sanitizeStage(Version inferred, String stage, boolean fixed) {
        String[] rawIdents = inferred.getPreReleaseVersion().split("\\.");
        int rawEndIndex = getEndIndex(rawIdents, 0);
        int limit;
        if (stage.equals(rawIdents[0])) {
           limit = 2; 
        } else if (!fixed) {
            if (rawIdents.length > 2 && stage.equals(rawIdents[2])) {
                limit = 4;
            } else {
                limit = 2;
            }
        } else {
            return Version.valueOf(inferred.getNormalVersion());
        }
        int endIndex = Math.min(rawEndIndex, limit);
        String[] validIdents = Arrays.copyOfRange(rawIdents, 0, endIndex);
        String preRelease = String.join(".", validIdents);
        return inferred.setPreReleaseVersion(preRelease);
    }

    private static int getEndIndex(String[] array, int index) {
        if (array.length > index) {
           boolean isValid = index % 2 == 0 || array[index].chars().allMatch(Character::isDigit);
           return isValid ? getEndIndex(array, index + 1) : index;
        } else {
            return index;
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
