package rentalmarketplace.util;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import rentalmarketplace.exception.ExportException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.User;

public final class ExcelExporter {
  private ExcelExporter() {}

  public static void export(
      List<User> users, List<Listing> listings, List<RentalRequest> requests, Path file) {
    try (Workbook workbook = new XSSFWorkbook();
        OutputStream out = Files.newOutputStream(file)) {
      CellStyle headerStyle = createHeaderStyle(workbook);
      writeUsers(workbook.createSheet("Пользователи"), headerStyle, users);
      writeListings(workbook.createSheet("Объекты аренды"), headerStyle, listings);
      writeRequests(workbook.createSheet("Заявки на аренду"), headerStyle, requests);
      workbook.write(out);
    } catch (IOException e) {
      throw new ExportException("Не удалось сохранить Excel-файл: " + e.getMessage(), e);
    }
  }

  private static void writeUsers(Sheet sheet, CellStyle headerStyle, List<User> users) {
    writeHeader(sheet, headerStyle, "id", "ФИО", "Email", "Телефон", "Роль", "Создан");
    int rowIndex = 1;
    for (User user : users) {
      Row row = sheet.createRow(rowIndex++);
      writeCells(
          row,
          user.getId(),
          user.getFullName(),
          user.getEmail(),
          user.getPhone(),
          user.getRole(),
          user.getCreatedAt());
    }
    autoSize(sheet, 6);
  }

  private static void writeListings(Sheet sheet, CellStyle headerStyle, List<Listing> listings) {
    writeHeader(
        sheet,
        headerStyle,
        "id",
        "id владельца",
        "Название",
        "Описание",
        "Цена за день",
        "Категория",
        "Доступен",
        "Создан");
    int rowIndex = 1;
    for (Listing listing : listings) {
      Row row = sheet.createRow(rowIndex++);
      writeCells(
          row,
          listing.getId(),
          listing.getOwnerId(),
          listing.getTitle(),
          listing.getDescription(),
          listing.getPricePerDay(),
          listing.getCategory(),
          listing.isAvailable() ? "да" : "нет",
          listing.getCreatedAt());
    }
    autoSize(sheet, 8);
  }

  private static void writeRequests(Sheet sheet, CellStyle headerStyle, List<RentalRequest> list) {
    writeHeader(
        sheet,
        headerStyle,
        "id",
        "id объекта",
        "id арендатора",
        "Начало",
        "Конец",
        "Статус",
        "Сумма",
        "Создана");
    int rowIndex = 1;
    for (RentalRequest request : list) {
      Row row = sheet.createRow(rowIndex++);
      writeCells(
          row,
          request.getId(),
          request.getListingId(),
          request.getRenterId(),
          request.getStartDate(),
          request.getEndDate(),
          request.getStatus(),
          request.getTotalPrice(),
          request.getCreatedAt());
    }
    autoSize(sheet, 8);
  }

  private static CellStyle createHeaderStyle(Workbook workbook) {
    Font font = workbook.createFont();
    font.setBold(true);
    CellStyle style = workbook.createCellStyle();
    style.setFont(font);
    return style;
  }

  private static void writeHeader(Sheet sheet, CellStyle style, String... titles) {
    Row row = sheet.createRow(0);
    for (int i = 0; i < titles.length; i++) {
      row.createCell(i).setCellValue(titles[i]);
      row.getCell(i).setCellStyle(style);
    }
  }

  private static void writeCells(Row row, Object... values) {
    for (int i = 0; i < values.length; i++) {
      Object value = values[i];
      if (value instanceof Integer number) {
        row.createCell(i).setCellValue(number);
      } else if (value instanceof BigDecimal number) {
        row.createCell(i).setCellValue(number.doubleValue());
      } else {
        row.createCell(i).setCellValue(value == null ? "" : value.toString());
      }
    }
  }

  private static void autoSize(Sheet sheet, int columns) {
    for (int i = 0; i < columns; i++) {
      sheet.autoSizeColumn(i);
    }
  }
}
