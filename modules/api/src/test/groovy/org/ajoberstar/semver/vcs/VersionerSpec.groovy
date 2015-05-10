package org.ajoberstar.semver.vcs

import spock.lang.Unroll

import static ChangeScope.*

import com.github.zafarkhaja.semver.Version
import spock.lang.Specification

class VersionerSpec extends Specification {
    private VcsProvider provider = Mock()
    private Versioner versioner = new Versioner(provider)

    def 'rebuild fails if no current version'() {
        given:
        provider.currentVersion >> Optional.empty()
        when:
        versioner.rebuild()
        then:
        thrown(IllegalStateException)
    }

    def 'rebuild works if current versions present in unmodified repo'() {
        given:
        provider.currentVersion >> Optional.of(Version.forIntegers(1, 2, 3))
        expect:
        versioner.rebuild() == Version.forIntegers(1, 2, 3)
    }

    @Unroll
    def 'infer fails if null arguments'(ChangeScope scope, String stage) {
        when:
        versioner.infer(scope, stage, true)
        then:
        thrown(NullPointerException)
        where:
        scope   | stage
        null    | 'something'
        MAJOR   | null
    }

    @Unroll
    def 'infer determines correct version'() {
        given:
        provider.previousVersion >> Optional.of(Version.valueOf('1.3.0-rc.3'))
        provider.previousRelease >> Optional.of(Version.valueOf('1.2.3'))
        expect:
        versioner.infer(scope, stage, fixed) == Version.valueOf(expected)
        where:
        scope | stage   | fixed | expected
        MAJOR | 'final' | true  | '2.0.0'
        MINOR | 'final' | true  | '1.3.0'
        MAJOR | 'other' | true  | '2.0.0-other.1'
        MINOR | 'rc'    | true  | '1.3.0-rc.4'
        MINOR | 'other' | false | '1.3.0-rc.3.other.1'
    }

    @Unroll
    def 'infer determines correct version when no previous version'() {
        given:
        provider.previousVersion >> Optional.empty()
        provider.previousRelease >> Optional.empty()
        expect:
        versioner.infer(scope, stage, fixed) == Version.valueOf(expected)
        where:
        scope | stage   | fixed | expected
        MAJOR | 'final' | true  | '1.0.0'
        MINOR | 'final' | true  | '0.1.0'
        PATCH | 'final' | true  | '0.0.1'
        MAJOR | 'other' | true  | '1.0.0-other.1'
        MINOR | 'other' | true  | '0.1.0-other.1'
        PATCH | 'other' | true  | '0.0.1-other.1'
    }

    @Unroll
    def 'infer determines correct version when no previous release'() {
        given:
        provider.previousVersion >> Optional.of(Version.valueOf('0.1.0-rc.3'))
        provider.previousRelease >> Optional.empty()
        expect:
        versioner.infer(scope, stage, fixed) == Version.valueOf(expected)
        where:
        scope | stage   | fixed | expected
        MAJOR | 'final' | true  | '1.0.0'
        MINOR | 'final' | true  | '0.1.0'
        MAJOR | 'other' | true  | '1.0.0-other.1'
        MINOR | 'rc'    | true  | '0.1.0-rc.4'
        MINOR | 'other' | false | '0.1.0-rc.3.other.1'
        MINOR | 'super' | false | '0.1.0-super.1'
    }

    @Unroll
    def 'infer determines correct version when previous version was release'() {
        given:
        provider.previousVersion >> Optional.of(Version.valueOf('1.0.0'))
        provider.previousRelease >> Optional.of(Version.valueOf('1.0.0'))
        expect:
        versioner.infer(scope, stage, fixed) == Version.valueOf(expected)
        where:
        scope | stage   | fixed | expected
        MAJOR | 'final' | true  | '2.0.0'
        MINOR | 'final' | true  | '1.1.0'
        PATCH | 'final' | true  | '1.0.1'
        MAJOR | 'other' | true  | '2.0.0-other.1'
        MINOR | 'other' | true  | '1.1.0-other.1'
        PATCH | 'other' | true  | '1.0.1-other.1'
    }

    @Unroll
    def 'infer enforces version precedence'() {
        given:
        provider.previousVersion >> Optional.of(Version.valueOf('1.3.0-rc.3'))
        provider.previousRelease >> Optional.of(Version.valueOf('1.2.3'))
        when:
        versioner.infer(scope, stage, fixed)
        then:
        thrown(IllegalArgumentException)
        where:
        scope | stage   | fixed
        PATCH | 'final' | true
        PATCH | 'other' | true
        MINOR | 'other' | true
    }
}
