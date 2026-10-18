package rentalmarketplace.service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.ExportException;

public class ExportService {
  private static final int SHEET_NAME_MAX = 31;
  private static final String FORBIDDEN_IN_SHEET_NAME = "[]:*?/\\";

  public void export(List<SheetData> sheets, Path file) {
    if (sheets == null || sheets.isEmpty()) {
      throw new BusinessRuleException("Нет данных для выгрузки");
    }
    try (Workbook workbook = new XSSFWorkbook();
        OutputStream out = Files.newOutputStream(file)) {
      CellStyle headerStyle = headerStyle(workbook);
      CellStyle dateStyle = formatStyle(workbook, "dd.mm.yyyy");
      CellStyle dateTimeStyle = formatStyle(workbook, "dd.mm.yyyy hh:mm");
      Set<String> usedNames = new HashSet<>();
      for (SheetData data : sheets) {
        Sheet sheet = workbook.createSheet(uniqueName(data.name(), usedNames));
        writeSheet(sheet, data, headerStyle, dateStyle, dateTimeStyle);
      }
      workbook.write(out);
    } catch (IOException e) {
      throw new ExportException("Не удалось сохранить Excel-файл: " + e.getMessage(), e);
    }
  }

  public void export(SheetData sheet, Path file) {
    export(List.of(sheet), file);
  }

  private void writeSheet(
      Sheet sheet,
      SheetData data,
      CellStyle headerStyle,
      CellStyle dateStyle,
      CellStyle dateTimeStyle) {
    Row header = sheet.createRow(0);
    for (int column = 0; column < data.headers().size(); column++) {
      Cell cell = header.createCell(column);
      cell.setCellValue(data.headers().get(column));
      cell.setCellStyle(headerStyle);
    }
    int rowIndex = 1;
    for (List<Object> values : data.rows()) {
      Row row = sheet.createRow(rowIndex++);
      for (int column = 0; column < values.size(); column++) {
        writeValue(row.createCell(column), values.get(column), dateStyle, dateTimeStyle);
      }
    }
    for (int column = 0; column < data.headers().size(); column++) {
      sheet.autoSizeColumn(column);
    }
  }

  private void writeValue(Cell cell, Object value, CellStyle dateStyle, CellStyle dateTimeStyle) {
    if (value == null) {
      return;
    }
    if (value instanceof Boolean flag) {
      cell.setCellValue(flag);
    } else if (value instanceof BigDecimal decimal) {
      cell.setCellValue(decimal.doubleValue());
    } else if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
    } else if (value instanceof LocalDate date) {
      cell.setCellValue(date);
      cell.setCellStyle(dateStyle);
    } else if (value instanceof LocalDateTime dateTime) {
      cell.setCellValue(dateTime);
      cell.setCellStyle(dateTimeStyle);
    } else {
      cell.setCellValue(String.valueOf(value));
    }
  }

  private String uniqueName(String requested, Set<String> used) {
    StringBuilder cleaned = new StringBuilder();
    for (char symbol : (requested == null ? "" : requested).toCharArray()) {
      cleaned.append(FORBIDDEN_IN_SHEET_NAME.indexOf(symbol) >= 0 ? ' ' : symbol);
    }
    String base = cleaned.toString().trim();
    if (base.isEmpty()) {
      base = "Лист";
    }
    if (base.length() > SHEET_NAME_MAX) {
      base = base.substring(0, SHEET_NAME_MAX);
    }
    String name = base;
    int suffix = 2;
    while (!used.add(name.toLowerCase())) {
      String tail = " (" + suffix++ + ")";
      name = base.substring(0, Math.min(base.length(), SHEET_NAME_MAX - tail.length())) + tail;
    }
    return name;
  }

  private CellStyle headerStyle(Workbook workbook) {
    Font font = workbook.createFont();
    font.setBold(true);
    CellStyle style = workbook.createCellStyle();
    style.setFont(font);
    return style;
  }

  private CellStyle formatStyle(Workbook workbook, String pattern) {
    CreationHelper helper = workbook.getCreationHelper();
    CellStyle style = workbook.createCellStyle();
    style.setDataFormat(helper.createDataFormat().getFormat(pattern));
    return style;
  }
}
