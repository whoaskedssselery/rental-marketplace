package rentalmarketplace.fx.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.service.RequestSort;

public final class Labels {
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
  private static final Locale RU = Locale.forLanguageTag("ru-RU");

  private Labels() {}

  public static String status(RentalRequestStatus status) {
    if (status == null) {
      return "";
    }
    return switch (status) {
      case NEW -> "Новая";
      case CONFIRMED -> "Подтверждена";
      case ACTIVE -> "В аренде";
      case COMPLETED -> "Завершена";
      case CANCELLED -> "Отменена";
      case REJECTED -> "Отклонена";
    };
  }

  public static String sort(RequestSort sort) {
    if (sort == null) {
      return "";
    }
    return switch (sort) {
      case CREATED_AT -> "Дата создания";
      case TOTAL_PRICE -> "Сумма";
      case START_DATE -> "Дата начала";
      case LISTING_TITLE -> "Название объекта";
    };
  }

  public static String role(UserRole role) {
    if (role == null) {
      return "";
    }
    return switch (role) {
      case RENTER -> "Арендатор";
      case OWNER -> "Владелец";
      case ADMIN -> "Администратор";
    };
  }

  public static String date(LocalDate date) {
    return date == null ? "" : DATE.format(date);
  }

  public static String dateTime(LocalDateTime dateTime) {
    return dateTime == null ? "" : DATE_TIME.format(dateTime);
  }

  public static String money(BigDecimal amount) {
    return amount == null ? "" : String.format(RU, "%,.2f руб.", amount);
  }

  public static String yesNo(boolean value) {
    return value ? "Да" : "Нет";
  }
}
