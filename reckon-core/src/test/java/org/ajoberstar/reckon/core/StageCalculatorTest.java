package org.ajoberstar.reckon.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class StageCalculatorTest {
  @Test
  @DisplayName("ofUserString filters out empty scope strings")
  public void ofUserStringFiltersEmptyStrings() {
    var calc = StageCalculator.ofUserString((i, v) -> Optional.of(""));
    assertEquals(Optional.empty(), calc.calculate(VcsInventory.empty(false), Version.IDENTITY));
  }

  @Test
  @DisplayName("ofUserString handles mixed case")
  public void ofUserStringMixedCase() {
    var calc = StageCalculator.ofUserString((i, v) -> Optional.of("BeTa"));
    assertEquals(Optional.of("beta"), calc.calculate(VcsInventory.empty(false), Version.IDENTITY));
  }
}
