package rentalmarketplace.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.exception.InputClosedException;
import rentalmarketplace.model.UserRole;

class ConsoleInputTest {

  private ConsoleInput inputWith(String... lines) {
    String text = String.join("\n", lines) + "\n";
    return new ConsoleInput(
        new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
  }

  @Test
  void readInt_skipsNonNumericInputUntilNumberEntered() {
    assertEquals(42, inputWith("abc", "4.5", "42").readInt("n: "));
  }

  @Test
  void readBigDecimal_skipsInvalidInput() {
    assertEquals(new BigDecimal("500.50"), inputWith("много", "500.50").readBigDecimal("p: "));
  }

  @Test
  void readDate_skipsInvalidFormat() {
    assertEquals(LocalDate.of(2026, 9, 15), inputWith("15.09.2026", "2026-09-15").readDate("d: "));
  }

  @Test
  void readYesNo_acceptsOnlyYesOrNo() {
    assertTrue(inputWith("может", "да").readYesNo("ok"));
    assertFalse(inputWith("НЕТ").readYesNo("ok"));
  }

  @Test
  void readEnum_isCaseInsensitiveAndRepeatsOnUnknownValue() {
    assertEquals(UserRole.OWNER, inputWith("boss", "owner").readEnum("role", UserRole.class));
  }

  @Test
  void readExistingId_repeatsUntilEntityFound() {
    int id =
        inputWith("1", "2")
            .readExistingId(
                "id: ",
                candidate -> {
                  if (candidate != 2) {
                    throw new EntityNotFoundException("нет " + candidate);
                  }
                  return candidate;
                });

    assertEquals(2, id);
  }

  @Test
  void readString_throwsInputClosedWhenInputEnds() {
    ConsoleInput input = inputWith("only line");
    input.readString("a: ");

    assertThrows(InputClosedException.class, () -> input.readString("b: "));
  }
}
