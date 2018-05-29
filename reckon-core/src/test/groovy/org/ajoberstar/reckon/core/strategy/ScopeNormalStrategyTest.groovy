package org.ajoberstar.reckon.core.strategy

import org.ajoberstar.reckon.core.Scope
import org.ajoberstar.reckon.core.VcsInventory
import org.ajoberstar.reckon.core.Version
import spock.lang.Specification

class ScopeNormalStrategyTest extends Specification {
  def 'rebuilding claimed version succeeds, if repo is clean'() {
    given:
    def inventory = new VcsInventory(
      'abcdef',
      true,
      null,
      Version.valueOf('0.0.0'),
      Version.valueOf('0.0.0'),
      1,
      [] as Set,
      [Version.valueOf('0.1.0'), Version.valueOf('0.1.1'), Version.valueOf('0.2.0')] as Set
      )
    expect:
    new ScopeNormalStrategy({ Optional.empty() }).reckonNormal(inventory).toString() == '0.1.0'
  }

  def 'if scope supplier returns null, throw'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        true,
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    when:
    new ScopeNormalStrategy({ null }).reckonNormal(inventory)
    then:
    thrown(NullPointerException)
  }

  def 'if scope supplier returns invalid scope, throw'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        true,
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    when:
    new ScopeNormalStrategy({ Optional.of("general") }).reckonNormal(inventory)
    then:
    def e = thrown(IllegalArgumentException)
    e.getMessage() == 'Scope "general" is not one of: major, minor, patch'
  }

  def 'if supplier returns empty, scope defaults to minor if base version is base normal'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        true,
        null,
        Version.valueOf('1.2.2'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    expect:
    new ScopeNormalStrategy({ Optional.empty() }).reckonNormal(inventory).toString() == '1.3.0'
  }

  def 'if supplier returns empty, scope defaults to scope used by base version'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        true,
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    expect:
    new ScopeNormalStrategy({ Optional.empty() }).reckonNormal(inventory).toString() == '1.2.3'
  }

  def 'if no conflict with parallel or claimed, incremented version is returned'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        true,
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    expect:
    new ScopeNormalStrategy({ Optional.of('major') }).reckonNormal(inventory).toString() == '2.0.0'
  }

  def 'if incremented version is in the parallel normals, increment again'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        true,
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [Version.valueOf('2.0.0')] as Set,
        [] as Set
        )
    expect:
    new ScopeNormalStrategy({ Optional.of('major') }).reckonNormal(inventory).toString() == '3.0.0'
  }

  def 'if target normal is in the claimed versions, throw'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        true,
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [Version.valueOf('2.0.0')] as Set
        )
    when:
    new ScopeNormalStrategy({ Optional.of('major') }).reckonNormal(inventory)
    then:
    thrown(IllegalStateException)
  }

}
