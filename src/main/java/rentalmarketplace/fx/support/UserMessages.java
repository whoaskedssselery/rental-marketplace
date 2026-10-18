package rentalmarketplace.fx.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.DatabaseAccessException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.exception.ExportException;

public final class UserMessages {
  private static final Logger LOG = LoggerFactory.getLogger(UserMessages.class);
  private static final String CONNECTION_PREFIX = "Не удалось подключиться";

  private UserMessages() {}

  public static String of(Throwable error) {
    if (error instanceof BusinessRuleException || error instanceof EntityNotFoundException) {
      return error.getMessage();
    }
    LOG.error("Ошибка при выполнении операции", error);
    if (error instanceof DatabaseAccessException) {
      return isConnectionProblem(error)
          ? "Нет соединения с базой данных. Проверьте, что Docker и контейнер PostgreSQL запущены."
          : "Не удалось выполнить операцию с базой данных. Данные не изменены.";
    }
    if (error instanceof ExportException) {
      return "Не удалось сохранить файл. Проверьте, что файл не открыт в другой программе "
          + "и что в выбранную папку можно записывать.";
    }
    return "Произошла непредвиденная ошибка. Подробности записаны в журнал.";
  }

  private static boolean isConnectionProblem(Throwable error) {
    String message = error.getMessage();
    return message != null && message.startsWith(CONNECTION_PREFIX);
  }
}
