package org.ajoberstar.reckon.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class VersionTest {
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"not a version", "1.0-345"})
  @DisplayName("parse returns empty if argument is not valid semantic version")
  public void parseInvalidSemVer(String version) {
    assertEquals(Optional.empty(), Version.parse(version));
  }

  @Test
  @DisplayName("parse returns version if argument is valid version")
  public void parseValidVersion() {
    assertEquals(Optional.of("1.0.0-rc.1"), Version.parse("1.0.0-rc.1").map(Version::toString));
  }

  @Test
  @DisplayName("isFinal returns true only if the version does not have a pre-release component")
  public void isFinal() {
    assertFalse(Version.valueOf("1.0.0-rc.1").isFinal());
    assertTrue(Version.valueOf("1.0.0").isFinal());
  }

  @Test
  @DisplayName("getNormal returns the normal component of the version only")
  public void getNormal() {
    assertEquals(Version.valueOf("1.2.3"), Version.valueOf("1.2.3").getNormal());
    assertEquals(Version.valueOf("1.2.3"), Version.valueOf("1.2.3-rc.1").getNormal());
    assertEquals(Version.valueOf("1.2.3"), Version.valueOf("1.2.3+other.stuff").getNormal());
  }

  @Test
  @DisplayName("incrementNormal correctly uses the scope")
  public void incrementNormal() {
    var base = Version.valueOf("1.2.3-rc.1");
    assertEquals(Version.valueOf("2.0.0"), base.incrementNormal(Scope.MAJOR));
    assertEquals(Version.valueOf("1.3.0"), base.incrementNormal(Scope.MINOR));
    assertEquals(Version.valueOf("1.2.4"), base.incrementNormal(Scope.PATCH));
  }

  @Test
  @DisplayName("inferScope correctly finds the scope")
  public void inferScope() {
    assertEquals(Optional.of(Scope.PATCH), Scope.infer(Version.valueOf("1.2.3"), Version.valueOf("1.2.4-milestone.1")));
    assertEquals(Optional.of(Scope.MINOR), Scope.infer(Version.valueOf("1.2.3"), Version.valueOf("1.3.0-milestone.1")));
    assertEquals(Optional.of(Scope.MAJOR), Scope.infer(Version.valueOf("1.2.3"), Version.valueOf("2.0.0-milestone.1")));
    assertEquals(Optional.empty(), Scope.infer(Version.valueOf("1.2.3"), Version.valueOf("1.2.3")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.4.0", "2.1.0", "1.2.5"})
  @DisplayName("inferScope fails for invalid increment")
  public void inferScopeFail(String afterStr) {
    var after = Version.valueOf(afterStr);
    assertThrows(IllegalStateException.class, () -> Scope.infer(Version.valueOf("1.2.3"), after));
  }
}
