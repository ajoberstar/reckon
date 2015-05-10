package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

public final class Versioners {
    private Versioners() {
        throw new AssertionError("Cannot instantiate this class.");
    }

    public static VersionerBuilder builder() {
        return new VersionerBuilder();
    }

    public static Versioner rebuild() {
        return (base, vcs) -> vcs.getCurrentVersion()
                .orElseThrow(() -> new IllegalStateException("Cannot rebuild since current revision doesn't have a version."));
    }

    public static Versioner force(String version) {
        Version forced = Version.valueOf(version);
        return (base, vcs) -> forced;
    }

    public static final Version VERSION_0 = Version.forIntegers(0, 0, 0);
}
