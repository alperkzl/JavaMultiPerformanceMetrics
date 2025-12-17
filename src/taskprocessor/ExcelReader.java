package taskprocessor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

/**
 * Excel file reader for parsing experiment result files.
 * Handles different column orders by reading header row first.
 */
public class ExcelReader {

    /**
     * Read an Excel file and return a map of column name to value.
     * This handles files with different column orders.
     *
     * @param filePath Path to the Excel file
     * @return Map of column name to value
     * @throws Exception If file cannot be read
     */
    public Map<String, Double> readExcelFile(String filePath) throws Exception {
        Map<String, Double> result = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Read header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new Exception("No header row found in file: " + filePath);
            }

            // Map column index to column name
            Map<Integer, String> columnNames = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String colName = getCellValueAsString(cell).trim();
                    columnNames.put(i, colName);
                }
            }

            // Read data row (row 1)
            Row dataRow = sheet.getRow(1);
            if (dataRow == null) {
                throw new Exception("No data row found in file: " + filePath);
            }

            // Extract values
            for (int i = 0; i < dataRow.getLastCellNum(); i++) {
                Cell cell = dataRow.getCell(i);
                String colName = columnNames.get(i);
                if (colName != null && cell != null) {
                    Double value = getCellValueAsDouble(cell);
                    if (value != null) {
                        result.put(colName, value);
                    }
                }
            }
        }

        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (IllegalStateException e) {
                    try {
                        return Double.parseDouble(cell.getStringCellValue().trim());
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }
            default:
                return null;
        }
    }
}
