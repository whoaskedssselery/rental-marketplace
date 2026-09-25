package rentalmarketplace.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;

class ExcelExporterTest {
  @TempDir Path tempDir;

  @Test
  void export_writesThreeSheetsWithHeaderAndDataRows() throws Exception {
    LocalDateTime now = LocalDateTime.of(2026, 9, 1, 10, 0);
    User user = new User(1, "Иван Петров", "ivan@example.com", null, UserRole.OWNER, now);
    Listing listing =
        new Listing(1, 1, "Дрель", "мощная", new BigDecimal("500.00"), "Инструменты", true, now);
    RentalRequest request =
        new RentalRequest(
            1,
            1,
            2,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 3),
            RentalRequestStatus.NEW,
            new BigDecimal("1000.00"),
            now);
    Path file = tempDir.resolve("export.xlsx");

    ExcelExporter.export(List.of(user), List.of(listing), List.of(request), file);

    try (InputStream in = Files.newInputStream(file);
        Workbook workbook = new XSSFWorkbook(in)) {
      assertEquals(3, workbook.getNumberOfSheets());
      Sheet users = workbook.getSheet("Пользователи");
      assertEquals("id", users.getRow(0).getCell(0).getStringCellValue());
      assertEquals("Иван Петров", users.getRow(1).getCell(1).getStringCellValue());
      Sheet listings = workbook.getSheet("Объекты аренды");
      assertEquals(500.0, listings.getRow(1).getCell(4).getNumericCellValue());
      assertEquals("да", listings.getRow(1).getCell(6).getStringCellValue());
      Sheet requests = workbook.getSheet("Заявки на аренду");
      assertEquals("NEW", requests.getRow(1).getCell(5).getStringCellValue());
      assertEquals(1000.0, requests.getRow(1).getCell(6).getNumericCellValue());
    }
  }

  @Test
  void export_withEmptyLists_writesOnlyHeaders() throws Exception {
    Path file = tempDir.resolve("empty.xlsx");

    ExcelExporter.export(List.of(), List.of(), List.of(), file);

    try (InputStream in = Files.newInputStream(file);
        Workbook workbook = new XSSFWorkbook(in)) {
      assertEquals(0, workbook.getSheet("Пользователи").getLastRowNum());
    }
  }
}
