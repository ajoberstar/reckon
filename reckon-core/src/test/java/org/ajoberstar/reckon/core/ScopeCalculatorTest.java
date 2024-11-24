package org.ajoberstar.reckon.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ScopeCalculatorTest {
  @Test
  @DisplayName("ofUserString filters out empty scope strings")
  public void ofUserStringFiltersEmptyStrings() {
    var calc = ScopeCalculator.ofUserString(i -> Optional.of(""));
    assertEquals(Optional.empty(), calc.calculate(VcsInventory.empty(false)));
  }

  @Test
  @DisplayName("ofUserString handles mixed case")
  public void ofUserStringMixedCase() {
    var calc = ScopeCalculator.ofUserString(i -> Optional.of("MiNor"));
    assertEquals(Optional.of(Scope.MINOR), calc.calculate(VcsInventory.empty(false)));
  }

  @Test
  @DisplayName("ofCommitMessage behaves as expected")
  public void ofCommitMessageNoMatch() {
    var calc = ScopeCalculator.ofCommitMessages();

    assertEquals(Optional.empty(), calc.calculate(VcsInventory.empty(false)), "Should not find a scope in an empty inventory");

    var inventoryNoMatches = getInventoryWithMessages(Version.valueOf("1.2.3"), "other message\n\nminor: something", "major");
    assertEquals(Optional.empty(), calc.calculate(inventoryNoMatches), "Should not find a scope when no messages match");

    var inventoryOneMatch = getInventoryWithMessages(Version.valueOf("1.2.3"), "some message", "patch: some fix");
    assertEquals(Optional.of(Scope.PATCH), calc.calculate(inventoryOneMatch), "Should find the one matching scope");

    var inventoryMultiMatch = getInventoryWithMessages(Version.valueOf("1.2.3"), "some message", "patch: some fix", "major(api): breaking change");
    assertEquals(Optional.of(Scope.MAJOR), calc.calculate(inventoryMultiMatch), "Should find the more significant matching scope");

    var inventoryMultiMatchPre1 = getInventoryWithMessages(Version.valueOf("0.7.5"), "some message", "patch: some fix", "major: breaking change");
    assertEquals(Optional.of(Scope.MINOR), calc.calculate(inventoryMultiMatchPre1), "Before 1.0 should find the more significant matching scope, but cap at minor");

    var inventoryMultiMatchPre1Force = getInventoryWithMessages(Version.valueOf("0.7.5"), "some message", "major!: force to 1.0", "patch: some fix", "major: breaking change");
    assertEquals(Optional.of(Scope.MAJOR), calc.calculate(inventoryMultiMatchPre1Force), "Before 1.0, can force 1.0 using major! as a prefix");
  }

  private VcsInventory getInventoryWithMessages(Version baseNormal, String... messages) {
    return new VcsInventory(
        null,
        false,
        null,
        null,
        baseNormal,
        0,
        null,
        null,
        Arrays.asList(messages));
  }
}
