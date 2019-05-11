package org.ajoberstar.reckon.core

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import java.nio.file.Files
import java.security.SecureRandom
import org.ajoberstar.grgit.Grgit
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Ignore

class ReckonerIntegTest extends Specification {
  private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1530724706), ZoneId.of('UTC'))
  private static final String TIMESTAMP = '20180704T171826Z'

  File repoDir
  Grgit grgit
  SecureRandom random = new SecureRandom()

  def 'rebuild works with parallel branches'() {
    given:
    def feature1 = commit()
    tag('0.1.0')
    branch('release-0.1')
    def feature2 = commit()
    tag('0.2.0-rc.1')
    commit()
    checkout('release-0.1')
    commit()
    tag('0.1.1')

    expect:
    reckonStage(null, null) == '0.1.1'
    checkout('0.1.0')
    reckonStage(null, null) == '0.1.0'
    checkout('master')
    tag('0.2.0')
    reckonStage(null, null) == '0.2.0'
    checkout('0.1.1')
    reckonStage(null, null) == '0.1.1'
    checkout('0.1.0')
    reckonStage(null, null) == '0.1.0'
  }

  @Ignore
  def 'multiple release branches'() {
    given:
    commit()
    branch('release/2.0.x')
    checkout('release/2.0.x')
    commit()
    tag('2.0.0')
    commit()
    tag('2.0.1')
    checkout('2.0.0')
    branch('release/2.1.x')
    checkout('release/2.1.x')
    commit()
    checkout('2.0.0')
    branch('release/3.0.x')
    checkout('release/3.0.x')
    commit()
    tag('3.0.0-dev.0')
    commit()
    tag('3.0.0-rc.1')
    checkout('2.0.0')
    branch('release/3.1.x')
    checkout('release/3.1.x')
    commit()
    tag('3.1.0-dev.0')
    commit()

    expect:
    reckonStage(null, null) == '3.1.0-dev.1.1+20180704T171826Z'
  }

  def setup() {
    repoDir = Files.createTempDirectory('repo').toFile()
    grgit = Grgit.init(dir: repoDir)
  }

  def cleanup() {
    grgit.close()
    assert !repoDir.exists() || repoDir.deleteDir()
  }

  private String reckonStage(scope, stage) {
    return Reckoner.builder()
      .clock(CLOCK)
      .git(grgit.repository.jgit.repository)
      .scopeCalc { i -> Optional.ofNullable(scope) }
      .stages('beta', 'milestone', 'rc', 'final')
      .stageCalc { i, v -> Optional.ofNullable(stage) }
      .build()
      .reckon();
  }

  private String reckonSnapshot(scope, stage) {
    return Reckoner.builder()
      .clock(CLOCK)
      .git(grgit.repository.jgit.repository)
      .scopeCalc { i -> Optional.ofNullable(scope) }
      .snapshots()
      .stageCalc { i, v -> Optional.ofNullable(stage) }
      .build()
      .reckon();
  }

  private void commit() {
    byte[] bytes = new byte[128]
    random.nextBytes(bytes)
    int fileName = random.nextInt();
    new File(grgit.repository.rootDir, "${fileName}.txt") << bytes
    grgit.add(patterns: ["${fileName}.txt"])
    def commit = grgit.commit(message: 'do')
    println "Created commit: ${commit.abbreviatedId}"
  }

  private void branch(String name) {
    def currentHead = grgit.head()
    def currentBranch = grgit.branch.current
    def newBranch = grgit.branch.add(name: name)
    def atCommit = grgit.resolve.toCommit(newBranch.fullName)
    println "Added new branch ${name} at ${atCommit.abbreviatedId}"
    assert currentBranch == grgit.branch.current
    assert currentHead == atCommit
  }

  private void tag(String name, boolean annotate = true) {
    def currentHead = grgit.head()
    def newTag = grgit.tag.add(name: name, annotate: annotate)
    def atCommit = grgit.resolve.toCommit(newTag.fullName)
    println "Added new tag ${name} at ${atCommit.abbreviatedId}"
    assert currentHead == atCommit
  }

  private void checkout(String name) {
    def currentHead = grgit.head()
    grgit.checkout(branch: name)
    def atCommit = grgit.resolve.toCommit(name)
    println "Checkout out ${name}, which is at ${atCommit.abbreviatedId}"
    assert name == grgit.branch.current.name || grgit.branch.current.name == 'HEAD'
  }

  private void merge(String name) {
    def currentBranch = grgit.branch.current.name
    grgit.merge(head: name)
    println "Merged ${name} into ${currentBranch}"
    assert currentBranch == grgit.branch.current.name
  }
}
