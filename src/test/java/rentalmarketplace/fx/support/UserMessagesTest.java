package rentalmarketplace.fx.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.exception.ExportException;

class UserMessagesTest {

  @Test
  void businessErrorsShowTheirOwnText() {
    assertEquals(
        "Цена за день должна быть положительной",
        UserMessages.of(new BusinessRuleException("Цена за день должна быть положительной")));
    assertEquals(
        "Заявка с id=5 не найдена",
        UserMessages.of(new EntityNotFoundException("Заявка с id=5 не найдена")));
  }

  @Test
  void connectionProblemMentionsDocker() {
    String message =
        UserMessages.of(
            new DatabaseAccessException(
                "Не удалось подключиться к базе данных: HikariPool-1 timeout",
                new SQLException("timeout")));

    assertTrue(message.contains("Docker"));
    assertFalse(message.contains("Hikari"));
  }

  @Test
  void otherDatabaseErrorsDoNotLeakSql() {
    String message =
        UserMessages.of(
            new DatabaseAccessException(
                "Ошибка SQL: relation users does not exist", new SQLException("boom")));

    assertEquals("Не удалось выполнить операцию с базой данных. Данные не изменены.", message);
  }

  @Test
  void exportErrorDoesNotShowPath() {
    String message =
        UserMessages.of(
            new ExportException("Не удалось сохранить Excel-файл: C:/secret/a.xlsx", null));

    assertFalse(message.contains("secret"));
    assertTrue(message.startsWith("Не удалось сохранить файл"));
  }

  @Test
  void unknownErrorGetsGenericText() {
    String message = UserMessages.of(new NullPointerException("detail"));

    assertEquals("Произошла непредвиденная ошибка. Подробности записаны в журнал.", message);
  }
}
