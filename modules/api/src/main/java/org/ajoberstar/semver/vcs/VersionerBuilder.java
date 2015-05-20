package org.ajoberstar.semver.vcs;

import java.util.Objects;

public final class VersionerBuilder {
    private Versioner normal;
    private Versioner preRelease;
    private Versioner buildMetadata;
    private Versioner enforcement;

    public VersionerBuilder() {
        this.normal = Versioners.identity();
        this.preRelease = Versioners.identity();
        this.buildMetadata = Versioners.identity();
    }

    public VersionerBuilder normal(Versioner normal) {
        Objects.requireNonNull(normal);
        this.normal = normal;
        return this;
    }

    public VersionerBuilder preRelease(Versioner preRelease) {
        Objects.requireNonNull(preRelease);
        this.preRelease = preRelease;
        return this;
    }

    public VersionerBuilder buildMetadata(Versioner buildMetadata) {
        Objects.requireNonNull(buildMetadata);
        this.buildMetadata = buildMetadata;
        return this;
    }

    public VersionerBuilder enforcePrecedence(boolean enforcePrecedence) {
        if (enforcePrecedence) {
            this.enforcement = Versioners.enforcePrecedence();
        } else {
            this.enforcement = Versioners.identity();
        }
        return this;
    }

    public Versioner build() {
        return enforcement
                .compose(buildMetadata)
                .compose(preRelease)
                .compose(normal);
    }
}
