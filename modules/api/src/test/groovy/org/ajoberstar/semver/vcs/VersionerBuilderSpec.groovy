package org.ajoberstar.semver.vcs

import com.github.zafarkhaja.semver.Version
import spock.lang.Specification
import spock.lang.Unroll

import static org.ajoberstar.semver.vcs.Scope.*

class VersionerBuilderSpec extends Specification {
    private Vcs vcs = Mock()

    private Version doInfer(Versioner versioner) {
        return versioner.infer(Versioners.VERSION_0, vcs)
    }

    def 'build fails if changeScope not called'() {
        when:
        Versioners.builder().useFinal().build()
        then:
        thrown(NullPointerException)
    }

    def 'build fails if a release method is not called'() {
        when:
        Versioners.builder().useScope(MAJOR).build()
        then:
        thrown(NullPointerException)
    }

    def 'buildMetadata fails for null argument'() {
        when:
        Versioners.builder().useBuildMetadata(null)
        then:
        thrown(NullPointerException)
    }

    def 'persistentPreRelease fails for null argument'() {
        when:
        Versioners.builder().useFixedStage(null)
        then:
        thrown(NullPointerException)
    }

    def 'transientPreRelease fails for null argument'() {
        when:
        Versioners.builder().useFloatingStage(null)
        then:
        thrown(NullPointerException)
    }

    @Unroll
    def 'infer determines correct version'() {
        given:
        vcs.previousVersion >> Optional.of(Version.valueOf('1.3.0-rc.3'))
        vcs.previousRelease >> Optional.of(Version.valueOf('1.2.3'))
        expect:
        doInfer(builder.build()) == Version.valueOf(expected)
        where:
        expected             | builder
        '2.0.0'              | Versioners.builder().useScope(MAJOR).useFinal()
        '1.3.0'              | Versioners.builder().useScope(MINOR).useFinal()
        '2.0.0-other.1'      | Versioners.builder().useScope(MAJOR).useFixedStage('other')
        '1.3.0-rc.4'         | Versioners.builder().useScope(MINOR).useFixedStage('rc')
        '1.3.0-rc.3.other.1' | Versioners.builder().useScope(MINOR).useFloatingStage('other')
    }

    @Unroll
    def 'infer determines correct version when no previous version'() {
        given:
        vcs.previousVersion >> Optional.empty()
        vcs.previousRelease >> Optional.empty()
        expect:
        doInfer(builder.build()) == Version.valueOf(expected)
        where:
        expected         | builder
        '1.0.0'          | Versioners.builder().useScope(MAJOR).useFinal()
        '0.1.0'          | Versioners.builder().useScope(MINOR).useFinal()
        '0.0.1'          | Versioners.builder().useScope(PATCH).useFinal()
        '1.0.0-other.1'  | Versioners.builder().useScope(MAJOR).useFixedStage('other')
        '0.1.0-other.1'  | Versioners.builder().useScope(MINOR).useFixedStage('other')
        '0.0.1-other.1'  | Versioners.builder().useScope(PATCH).useFixedStage('other')
        '0.0.1-SNAPSHOT' | Versioners.builder().useScope(PATCH).useSnapshotStage()
    }

    @Unroll
    def 'infer determines correct version when no previous release'() {
        given:
        vcs.previousVersion >> Optional.of(Version.valueOf('0.1.0-rc.3'))
        vcs.previousRelease >> Optional.empty()
        expect:
        doInfer(builder.build()) == Version.valueOf(expected)
        where:
        expected             | builder
        '1.0.0'              | Versioners.builder().useScope(MAJOR).useFinal()
        '0.1.0'              | Versioners.builder().useScope(MINOR).useFinal()
        '1.0.0-other.1'      | Versioners.builder().useScope(MAJOR).useFixedStage('other')
        '0.1.0-rc.4'         | Versioners.builder().useScope(MINOR).useFixedStage('rc')
        '0.1.0-rc.3.other.1' | Versioners.builder().useScope(MINOR).useFloatingStage('other')
        '0.1.0-super.1'      | Versioners.builder().useScope(MINOR).useFloatingStage('super')
    }

    @Unroll
    def 'infer determines correct version when previous version was release'() {
        given:
        vcs.previousVersion >> Optional.of(Version.valueOf('1.0.0'))
        vcs.previousRelease >> Optional.of(Version.valueOf('1.0.0'))
        expect:
        doInfer(builder.build()) == Version.valueOf(expected)
        where:
        expected | builder
        '2.0.0'  | Versioners.builder().useScope(MAJOR).useFinal()
        '1.1.0'  | Versioners.builder().useScope(MINOR).useFinal()
        '1.0.1'  | Versioners.builder().useScope(PATCH).useFinal()
        '2.0.0-other.1' | Versioners.builder().useScope(MAJOR).useFixedStage('other')
        '1.1.0-other.1' | Versioners.builder().useScope(MINOR).useFixedStage('other')
        '1.0.1-other.1' | Versioners.builder().useScope(PATCH).useFixedStage('other')
    }

    @Unroll
    def 'infer enforces version precedence'() {
        given:
        vcs.previousVersion >> Optional.of(Version.valueOf('1.3.0-rc.3'))
        vcs.previousRelease >> Optional.of(Version.valueOf('1.2.3'))
        when:
        doInfer(builder.build())
        then:
        thrown(IllegalArgumentException)
        where:
        builder << [
                Versioners.builder().useScope(PATCH).useFinal(),
                Versioners.builder().useScope(PATCH).useFixedStage('other'),
                Versioners.builder().useScope(MINOR).useFixedStage('other')
        ]
    }
}
