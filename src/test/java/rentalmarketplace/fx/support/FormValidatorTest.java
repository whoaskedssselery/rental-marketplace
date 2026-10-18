package rentalmarketplace.fx.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FormValidatorTest {

  @Test
  void requiredTextTrimsAndAccepts() {
    FormValidator validator = new FormValidator();

    assertEquals("Иван", validator.requiredText("ФИО", "  Иван ", 150));
    assertTrue(validator.result().isValid());
  }

  @Test
  void requiredTextRejectsBlankAndNull() {
    FormValidator validator = new FormValidator();

    validator.requiredText("ФИО", "   ", 150);
    validator.requiredText("Название", null, 200);

    assertEquals(2, validator.result().errors().size());
    assertEquals("ФИО: заполните поле", validator.result().errors().get(0).text());
  }

  @Test
  void requiredTextChecksMaxLengthBoundary() {
    FormValidator validator = new FormValidator();

    validator.requiredText("Название", "a".repeat(200), 200);
    assertTrue(validator.result().isValid());

    validator.requiredText("Название", "a".repeat(201), 200);
    assertEquals("Название: не длиннее 200 символов", validator.result().summary());
  }

  @Test
  void optionalTextAllowsEmptyButNotTooLong() {
    FormValidator validator = new FormValidator();

    assertEquals("", validator.optionalText("Телефон", null, 30));
    assertTrue(validator.result().isValid());

    validator.optionalText("Телефон", "1".repeat(31), 30);
    assertFalse(validator.result().isValid());
  }

  @Test
  void emailAcceptsValidAddress() {
    FormValidator validator = new FormValidator();

    validator.email("Email", "ivan.petrov+test@example.com", 150);

    assertTrue(validator.result().isValid());
  }

  @Test
  void emailRejectsBrokenAddresses() {
    for (String broken :
        new String[] {"ivan", "ivan@", "@example.com", "ivan@example", "a b@c.ru"}) {
      FormValidator validator = new FormValidator();

      validator.email("Email", broken, 150);

      assertFalse(validator.result().isValid(), broken);
    }
  }

  @Test
  void emptyEmailGivesSingleError() {
    FormValidator validator = new FormValidator();

    validator.email("Email", "", 150);

    assertEquals(1, validator.result().errors().size());
  }

  @Test
  void positiveAmountParsesDotAndComma() {
    FormValidator validator = new FormValidator();

    assertEquals(new BigDecimal("500.50"), validator.positiveAmount("Цена", "500,50"));
    assertEquals(new BigDecimal("500"), validator.positiveAmount("Цена", " 500 "));
    assertTrue(validator.result().isValid());
  }

  @Test
  void positiveAmountRejectsZeroNegativeEmptyAndText() {
    for (String bad : new String[] {"0", "-5", "", "  ", "abc", "1e", "12.345"}) {
      FormValidator validator = new FormValidator();

      assertNull(validator.positiveAmount("Цена", bad), bad);
      assertFalse(validator.result().isValid(), bad);
    }
  }

  @Test
  void positiveAmountChecksUpperBoundary() {
    FormValidator validator = new FormValidator();

    assertEquals(new BigDecimal("99999999.99"), validator.positiveAmount("Цена", "99999999.99"));
    assertNull(validator.positiveAmount("Цена", "100000000"));
    assertEquals("Цена: не больше 99999999.99", validator.result().summary());
  }

  @Test
  void integerInRangeChecksBoundaries() {
    FormValidator validator = new FormValidator();

    assertEquals(1, validator.integerInRange("Оценка", "1", 1, 5));
    assertEquals(5, validator.integerInRange("Оценка", "5", 1, 5));
    assertTrue(validator.result().isValid());

    assertNull(validator.integerInRange("Оценка", "0", 1, 5));
    assertNull(validator.integerInRange("Оценка", "6", 1, 5));
    assertNull(validator.integerInRange("Оценка", "x", 1, 5));
    assertEquals(3, validator.result().errors().size());
  }

  @Test
  void requiredDateRejectsNull() {
    FormValidator validator = new FormValidator();

    assertNull(validator.requiredDate("Начало", null));
    assertEquals("Начало: выберите дату", validator.result().summary());
  }

  @Test
  void endAfterStartRequiresStrictOrder() {
    LocalDate start = LocalDate.of(2026, 9, 10);

    FormValidator same = new FormValidator();
    same.endAfterStart("Конец", start, start);
    assertFalse(same.result().isValid());

    FormValidator earlier = new FormValidator();
    earlier.endAfterStart("Конец", start, start.minusDays(1));
    assertFalse(earlier.result().isValid());

    FormValidator later = new FormValidator();
    later.endAfterStart("Конец", start, start.plusDays(1));
    assertTrue(later.result().isValid());
  }

  @Test
  void endAfterStartIgnoresMissingDates() {
    FormValidator validator = new FormValidator();

    validator.endAfterStart("Конец", null, LocalDate.of(2026, 9, 10));
    validator.endAfterStart("Конец", LocalDate.of(2026, 9, 10), null);

    assertTrue(validator.result().isValid());
  }

  @Test
  void selectedRejectsNull() {
    FormValidator validator = new FormValidator();

    assertEquals("x", validator.selected("Объект", "x"));
    assertNull(validator.selected("Арендатор", null));
    assertEquals("Арендатор: выберите значение из списка", validator.result().summary());
  }

  @Test
  void summaryListsEveryErrorOnItsOwnLine() {
    FormValidator validator = new FormValidator();

    validator.requiredText("ФИО", "", 150);
    validator.requiredDate("Начало", null);

    assertEquals("ФИО: заполните поле\nНачало: выберите дату", validator.result().summary());
  }
}
