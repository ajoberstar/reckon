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
        Versioners.builder().finalRelease().build()
        then:
        thrown(NullPointerException)
    }

    def 'build fails if a release method is not called'() {
        when:
        Versioners.builder().changeScope(MAJOR).build()
        then:
        thrown(NullPointerException)
    }

    def 'buildMetadata fails for null argument'() {
        when:
        Versioners.builder().buildMetadata(null)
        then:
        thrown(NullPointerException)
    }

    def 'persistentPreRelease fails for null argument'() {
        when:
        Versioners.builder().fixedStagePreRelease(null)
        then:
        thrown(NullPointerException)
    }

    def 'transientPreRelease fails for null argument'() {
        when:
        Versioners.builder().floatingStagePreRelease(null)
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
        '2.0.0'              | Versioners.builder().changeScope(MAJOR).finalRelease()
        '1.3.0'              | Versioners.builder().changeScope(MINOR).finalRelease()
        '2.0.0-other.1'      | Versioners.builder().changeScope(MAJOR).fixedStagePreRelease('other')
        '1.3.0-rc.4'         | Versioners.builder().changeScope(MINOR).fixedStagePreRelease('rc')
        '1.3.0-rc.3.other.1' | Versioners.builder().changeScope(MINOR).floatingStagePreRelease('other')
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
        '1.0.0'          | Versioners.builder().changeScope(MAJOR).finalRelease()
        '0.1.0'          | Versioners.builder().changeScope(MINOR).finalRelease()
        '0.0.1'          | Versioners.builder().changeScope(PATCH).finalRelease()
        '1.0.0-other.1'  | Versioners.builder().changeScope(MAJOR).fixedStagePreRelease('other')
        '0.1.0-other.1'  | Versioners.builder().changeScope(MINOR).fixedStagePreRelease('other')
        '0.0.1-other.1'  | Versioners.builder().changeScope(PATCH).fixedStagePreRelease('other')
        '0.0.1-SNAPSHOT' | Versioners.builder().changeScope(PATCH).snapshotPreRelease()
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
        '1.0.0'              | Versioners.builder().changeScope(MAJOR).finalRelease()
        '0.1.0'              | Versioners.builder().changeScope(MINOR).finalRelease()
        '1.0.0-other.1'      | Versioners.builder().changeScope(MAJOR).fixedStagePreRelease('other')
        '0.1.0-rc.4'         | Versioners.builder().changeScope(MINOR).fixedStagePreRelease('rc')
        '0.1.0-rc.3.other.1' | Versioners.builder().changeScope(MINOR).floatingStagePreRelease('other')
        '0.1.0-super.1'      | Versioners.builder().changeScope(MINOR).floatingStagePreRelease('super')
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
        '2.0.0'  | Versioners.builder().changeScope(MAJOR).finalRelease()
        '1.1.0'  | Versioners.builder().changeScope(MINOR).finalRelease()
        '1.0.1'  | Versioners.builder().changeScope(PATCH).finalRelease()
        '2.0.0-other.1' | Versioners.builder().changeScope(MAJOR).fixedStagePreRelease('other')
        '1.1.0-other.1' | Versioners.builder().changeScope(MINOR).fixedStagePreRelease('other')
        '1.0.1-other.1' | Versioners.builder().changeScope(PATCH).fixedStagePreRelease('other')
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
                Versioners.builder().changeScope(PATCH).finalRelease(),
                Versioners.builder().changeScope(PATCH).fixedStagePreRelease('other'),
                Versioners.builder().changeScope(MINOR).fixedStagePreRelease('other')
        ]
    }
}
