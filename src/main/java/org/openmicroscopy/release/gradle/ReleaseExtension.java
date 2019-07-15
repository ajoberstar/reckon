package org.openmicroscopy.release.gradle;

import groovy.lang.Closure;
import org.gradle.api.Action;

import java.util.Map;

public interface ReleaseExtension {

    @Deprecated
    void setNormal(ReleaseExtension ext);

    @Deprecated
    void setPreRelease(ReleaseExtension ext);

    /**
     * @param scopeOpts
     */
    void scopeOptions(Map<String, ?> scopeOpts);

    /**
     * Configures scope specific options of release.
     * <p>
     * The supplied action configures an instance of {@link ScopeOptions},
     * which can be used to configure how scope is determined.
     * </p>
     *
     * @param scopeOpts An action used to configure the scope options.
     */
    void scopeOptions(Action<? super ScopeOptions> scopeOpts);

    /**
     * Configures scope specific options of release.
     * <p>
     * The supplied action configures an instance of {@link ScopeOptions},
     * which can be used to configure how scope is determined.
     * </p>
     *
     * @param scopeOpts A closure used to configure the scope options.
     */
    void scopeOptions(Closure scopeOpts);

    /**
     *
     */
    void useStages();

    /**
     * @param stageOpts
     */
    void useStages(Map<String, ?> stageOpts);

    /**
     * @param stageOpts
     */
    void useStages(Action<? super StageOptions> stageOpts);

    /**
     * @param stageOpts
     */
    void useStages(Closure stageOpts);

    /**
     *
     */
    void useSnapshot();

    /**
     * @param snapshotOpts
     */
    void useSnapshot(Map<String, ?> snapshotOpts);

    /**
     * @param snapshotOpts
     */
    void useSnapshot(Action<? super SnapshotOptions> snapshotOpts);

    /**
     * @param stageOpts
     */
    void useSnapshot(Closure stageOpts);

}
