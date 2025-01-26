package com.example.messmanagement.service;

import com.example.messmanagement.entity.Menu;
import com.example.messmanagement.repository.MenuRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;

@Service
public class ExcelMenuUpdater {

    @Autowired
    private MenuRepository menuRepository;

    // Google Drive or external file URL
    private static final String EXCEL_FILE_URL = System.getenv("EXCEL_FILE_URL");

    @Scheduled(fixedRate = 86400000) // Runs daily
    public void updateMenuFromExcel() {
        try {
            if (EXCEL_FILE_URL == null || EXCEL_FILE_URL.isEmpty()) {
                System.err.println("Excel file URL is not set in environment variables.");
                return;
            }

            // Download the file from the URL
            File excelFile = downloadFile(EXCEL_FILE_URL);

            if (excelFile == null || !excelFile.exists()) {
                System.err.println("Failed to download the Excel file.");
                return;
            }

            // Process the downloaded Excel file
            try (Workbook workbook = new XSSFWorkbook(excelFile)) {
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    System.err.println("Sheet not found in the Excel file.");
                    return;
                }

                Row dateRow1 = sheet.getRow(1); // First row with dates
                Row dateRow2 = sheet.getRow(2); // Second row with dates

                if (dateRow1 == null || dateRow2 == null) {
                    System.err.println("Date rows are missing.");
                    return;
                }

                int totalColumns = dateRow1.getLastCellNum(); // Number of columns (days)
                System.out.println("Total Columns (Days): " + totalColumns);

                for (int col = 0; col < totalColumns; col++) {
                    String dayOfWeek = sheet.getRow(0).getCell(col).getStringCellValue();
                    LocalDate firstDate = parseDate(dateRow1.getCell(col));
                    LocalDate secondDate = parseDate(dateRow2.getCell(col));

                    if (firstDate == null || secondDate == null) {
                        System.err.println("Invalid date at column: " + col);
                        continue;
                    }

                    int rowIndex = 3; // Start processing menu items from Row 3
                    while (rowIndex <= sheet.getLastRowNum()) {
                        Row row = sheet.getRow(rowIndex);
                        if (row == null || row.getCell(0) == null) {
                            rowIndex++;
                            continue;
                        }

                        String cellValue = row.getCell(0).getStringCellValue().trim();
                        if (isMealType(cellValue)) {
                            String mealType = cellValue;
                            rowIndex++;

                            StringBuilder menuItems = new StringBuilder();
                            while (rowIndex <= sheet.getLastRowNum()) {
                                Row itemRow = sheet.getRow(rowIndex);
                                if (itemRow == null || itemRow.getCell(0) == null || isMealType(itemRow.getCell(0).getStringCellValue().trim())) {
                                    break; // Stop if we reach another meal type or an empty row
                                }

                                Cell menuCell = itemRow.getCell(col);
                                if (menuCell != null) {
                                    menuItems.append(menuCell.getStringCellValue()).append(", ");
                                }

                                rowIndex++;
                            }

                            saveMenu(dayOfWeek, firstDate, mealType, menuItems.toString());
                            saveMenu(dayOfWeek, secondDate, mealType, menuItems.toString());
                        } else {
                            rowIndex++;
                        }
                    }
                }
            }

            System.out.println("Menu successfully updated from Excel file.");
        } catch (Exception e) {
            System.err.println("Error while processing Excel file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File downloadFile(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to download file. HTTP response code: " + connection.getResponseCode());
                return null;
            }

            File tempFile = File.createTempFile("menu", ".xlsx");
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile;
        } catch (Exception e) {
            System.err.println("Error downloading file: " + e.getMessage());
            return null;
        }
    }

    private LocalDate parseDate(Cell cell) {
        try {
            if (cell == null) return null;
            return cell.getLocalDateTimeCellValue().toLocalDate();
        } catch (Exception e) {
            System.err.println("Date parsing failed for cell: " + cell);
            return null;
        }
    }

    private boolean isMealType(String cellValue) {
        return cellValue.equalsIgnoreCase("BREAKFAST") ||
               cellValue.equalsIgnoreCase("LUNCH") ||
               cellValue.equalsIgnoreCase("SNACKS") ||
               cellValue.equalsIgnoreCase("DINNER");
    }

    private void saveMenu(String day, LocalDate date, String mealType, String menuItems) {
        try {
            Menu menu = new Menu();
            menu.setDayOfWeek(day);
            menu.setDate(date);
            menu.setMealType(mealType);
            menu.setMenuItems(menuItems);
            menuRepository.save(menu);
            System.out.println("Saved menu: " + menu);
        } catch (Exception e) {
            System.err.println("Failed to save menu: " + e.getMessage());
        }
    }
}