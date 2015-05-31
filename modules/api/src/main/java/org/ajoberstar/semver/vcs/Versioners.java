package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Objects;

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

    public static Versioner forScopeAndStage(Scope scope, Stage stage, boolean enforcePrecedence) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(stage);
        return identity()
                .compose(enforcePrecedence ? enforcePrecedence() : identity())
                .compose(stage.getVersioner())
                .compose(scope.getVersioner());
    }
}
