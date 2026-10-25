package rentalmarketplace.fx.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

public class FormValidator {
  private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
  private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
  private static final DateTimeFormatter DATE_INPUT =
      DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);

  private final ValidationResult result = new ValidationResult();

  public ValidationResult result() {
    return result;
  }

  public String requiredText(String field, String value, int maxLength) {
    String text = value == null ? "" : value.trim();
    if (text.isEmpty()) {
      result.add(field, "заполните поле");
    } else if (text.length() > maxLength) {
      result.add(field, "не длиннее " + maxLength + " символов");
    }
    return text;
  }

  public String optionalText(String field, String value, int maxLength) {
    String text = value == null ? "" : value.trim();
    if (text.length() > maxLength) {
      result.add(field, "не длиннее " + maxLength + " символов");
    }
    return text;
  }

  public String email(String field, String value, int maxLength) {
    String text = requiredText(field, value, maxLength);
    if (!text.isEmpty() && text.length() <= maxLength && !EMAIL.matcher(text).matches()) {
      result.add(field, "введите адрес в формате name@example.com");
    }
    return text;
  }

  public BigDecimal positiveAmount(String field, String value) {
    String text = value == null ? "" : value.trim().replace(',', '.');
    if (text.isEmpty()) {
      result.add(field, "введите число больше 0");
      return null;
    }
    BigDecimal amount;
    try {
      amount = new BigDecimal(text);
    } catch (NumberFormatException e) {
      result.add(field, "введите число, например 500 или 500.50");
      return null;
    }
    if (amount.signum() <= 0) {
      result.add(field, "введите число больше 0");
      return null;
    }
    if (amount.scale() > 2) {
      result.add(field, "не больше двух знаков после запятой");
      return null;
    }
    if (amount.compareTo(MAX_AMOUNT) > 0) {
      result.add(field, "не больше " + MAX_AMOUNT.toPlainString());
      return null;
    }
    return amount;
  }

  public LocalDate requiredDate(String field, LocalDate value) {
    if (value == null) {
      result.add(field, "выберите дату");
    }
    return value;
  }

  public static LocalDate parseDateText(String value) {
    return value == null || value.isBlank() ? null : LocalDate.parse(value.trim(), DATE_INPUT);
  }

  public LocalDate requiredDateText(String field, String value) {
    try {
      return requiredDate(field, parseDateText(value));
    } catch (DateTimeParseException error) {
      result.add(field, "введите существующую дату в формате ДД.ММ.ГГГГ");
      return null;
    }
  }

  public void endAfterStart(String field, LocalDate start, LocalDate end) {
    if (start != null && end != null && !end.isAfter(start)) {
      result.add(field, "дата окончания должна быть позже даты начала");
    }
  }

  public <T> T selected(String field, T value) {
    if (value == null) {
      result.add(field, "выберите значение из списка");
    }
    return value;
  }
}
