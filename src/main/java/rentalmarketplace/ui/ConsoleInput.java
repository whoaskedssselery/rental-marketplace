package rentalmarketplace.ui;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.IntFunction;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.exception.InputClosedException;

public class ConsoleInput {
  private final Scanner scanner;

  public ConsoleInput() {
    this(System.in, Charset.defaultCharset());
  }

  public ConsoleInput(InputStream source, Charset charset) {
    this.scanner = new Scanner(source, charset);
  }

  public String readString(String prompt) {
    System.out.print(prompt);
    try {
      return scanner.nextLine().trim();
    } catch (NoSuchElementException e) {
      throw new InputClosedException();
    }
  }

  public int readInt(String prompt) {
    while (true) {
      try {
        return Integer.parseInt(readString(prompt));
      } catch (NumberFormatException e) {
        System.out.println("Ошибка: нужно ввести целое число");
      }
    }
  }

  public BigDecimal readBigDecimal(String prompt) {
    while (true) {
      try {
        return new BigDecimal(readString(prompt));
      } catch (NumberFormatException e) {
        System.out.println("Ошибка: нужно ввести число (например, 500.00)");
      }
    }
  }

  public LocalDate readDate(String prompt) {
    while (true) {
      try {
        return LocalDate.parse(readString(prompt));
      } catch (DateTimeParseException e) {
        System.out.println("Ошибка: формат даты гггг-мм-дд, например 2026-09-15");
      }
    }
  }

  public boolean readYesNo(String prompt) {
    while (true) {
      String answer = readString(prompt + " (да/нет): ").toLowerCase();
      if (answer.equals("да")) {
        return true;
      }
      if (answer.equals("нет")) {
        return false;
      }
      System.out.println("Ошибка: нужно ответить да или нет");
    }
  }

  public <E extends Enum<E>> E readEnum(String prompt, Class<E> enumType) {
    String options =
        String.join("/", Arrays.stream(enumType.getEnumConstants()).map(Enum::name).toList());
    while (true) {
      String input = readString(prompt + " (" + options + "): ").toUpperCase();
      try {
        return Enum.valueOf(enumType, input);
      } catch (IllegalArgumentException e) {
        System.out.println("Ошибка: допустимые значения - " + options);
      }
    }
  }

  public int readExistingId(String prompt, IntFunction<?> findById) {
    while (true) {
      int id = readInt(prompt);
      try {
        findById.apply(id);
        return id;
      } catch (EntityNotFoundException e) {
        System.out.println("Ошибка: " + e.getMessage());
      }
    }
  }
}
