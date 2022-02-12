package org.ajoberstar.reckon.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

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
    var pattern = Pattern.compile("^(major|minor|patch): .+");
    var calc = ScopeCalculator.ofCommitMessage(msg -> {
      var matcher = pattern.matcher(msg);
      if (matcher.find()) {
        return Optional.of(Scope.from(matcher.group(1)));
      } else {
        return Optional.empty();
      }
    });
    assertEquals(Optional.empty(), calc.calculate(VcsInventory.empty(false)), "Should not find a scope in an empty inventory");

    var inventoryNoMatches = getInventoryWithMessages("some message", "other message\n\nminor: something", "major");
    assertEquals(Optional.empty(), calc.calculate(inventoryNoMatches), "Should not find a scope when no messages match");

    var inventoryOneMatch = getInventoryWithMessages("some message", "patch: some fix");
    assertEquals(Optional.of(Scope.PATCH), calc.calculate(inventoryOneMatch), "Should find the one matching scope");

    var inventoryMultiMatch = getInventoryWithMessages("some message", "patch: some fix", "major: breaking change");
    assertEquals(Optional.of(Scope.MAJOR), calc.calculate(inventoryMultiMatch), "Should find the more significant matching scope");
  }

  private VcsInventory getInventoryWithMessages(String... messages) {
    return new VcsInventory(
      null,
      false,
      null,
      null,
      null,
      0,
      null,
      null,
      Arrays.asList(messages)
    );
  }
}
