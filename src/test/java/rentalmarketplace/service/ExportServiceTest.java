package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.ExportException;

class ExportServiceTest {
  private final ExportService service = new ExportService();

  @TempDir Path dir;

  private Workbook read(Path file) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return new XSSFWorkbook(in);
    }
  }

  @Test
  void writesHeadersAndTypedCells() throws IOException {
    Path file = dir.resolve("out.xlsx");
    List<Object> row =
        Arrays.asList(
            1,
            "Дрель",
            new BigDecimal("500.50"),
            LocalDate.of(2026, 9, 1),
            LocalDateTime.of(2026, 9, 1, 12, 30),
            true,
            null);
    SheetData data =
        new SheetData(
            "Заявки",
            List.of("Номер", "Объект", "Сумма", "Начало", "Создана", "Доступен", "Комментарий"),
            List.of(row));

    service.export(data, file);

    try (Workbook workbook = read(file)) {
      Sheet sheet = workbook.getSheet("Заявки");
      assertEquals("Номер", sheet.getRow(0).getCell(0).getStringCellValue());
      assertEquals(2, sheet.getPhysicalNumberOfRows());
      assertEquals(CellType.NUMERIC, sheet.getRow(1).getCell(0).getCellType());
      assertEquals("Дрель", sheet.getRow(1).getCell(1).getStringCellValue());
      assertEquals(500.5, sheet.getRow(1).getCell(2).getNumericCellValue());
      assertTrue(DateUtil.isCellDateFormatted(sheet.getRow(1).getCell(3)));
      assertTrue(DateUtil.isCellDateFormatted(sheet.getRow(1).getCell(4)));
      assertTrue(sheet.getRow(1).getCell(5).getBooleanCellValue());
    }
  }

  @Test
  void writesEmptyDatasetWithHeadersOnly() throws IOException {
    Path file = dir.resolve("empty.xlsx");

    service.export(new SheetData("Пусто", List.of("A", "B"), List.of()), file);

    try (Workbook workbook = read(file)) {
      assertEquals(1, workbook.getSheet("Пусто").getPhysicalNumberOfRows());
    }
  }

  @Test
  void writesSeveralSheetsWithSafeUniqueNames() throws IOException {
    Path file = dir.resolve("many.xlsx");
    SheetData first = new SheetData("Заявки/2026: [а]", List.of("A"), List.of());
    SheetData second = new SheetData("заявки/2026: [а]", List.of("A"), List.of());
    SheetData third = new SheetData("x".repeat(60), List.of("A"), List.of());

    service.export(List.of(first, second, third), file);

    try (Workbook workbook = read(file)) {
      assertEquals(3, workbook.getNumberOfSheets());
      assertEquals("Заявки 2026   а", workbook.getSheetName(0));
      assertTrue(workbook.getSheetName(1).endsWith("(2)"));
      assertEquals(31, workbook.getSheetName(2).length());
    }
  }

  @Test
  void blankSheetNameGetsDefault() throws IOException {
    Path file = dir.resolve("blank.xlsx");

    service.export(new SheetData("  ", List.of("A"), List.of()), file);

    try (Workbook workbook = read(file)) {
      assertEquals("Лист", workbook.getSheetName(0));
    }
  }

  @Test
  void rejectsEmptyListOfSheets() {
    assertThrows(
        BusinessRuleException.class, () -> service.export(List.of(), dir.resolve("none.xlsx")));
  }

  @Test
  void wrapsIoProblemsIntoExportException() {
    assertThrows(
        ExportException.class,
        () -> service.export(new SheetData("A", List.of("A"), List.of()), dir));
  }
}
