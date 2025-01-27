package com.example.messmanagement.service;

import com.example.messmanagement.entity.Menu;
import com.example.messmanagement.repository.MenuRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.time.LocalDate;

@Service
public class ExcelMenuUpdater {

    @Autowired
    private MenuRepository menuRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void processExcelFile(MultipartFile file) {
        try (InputStream fis = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Ensure the `menu` table exists in the database
            createMenuTableIfNotExists();

            // Clear existing rows in the `menu` table
            menuRepository.deleteAll();

            // Process the uploaded Excel file
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("No sheet found in the Excel file.");
            }

            Row dateRow1 = sheet.getRow(1); // First row with dates
            Row dateRow2 = sheet.getRow(2); // Second row with dates

            if (dateRow1 == null || dateRow2 == null) {
                throw new IllegalArgumentException("Date rows are missing in the Excel file.");
            }

            int totalColumns = dateRow1.getLastCellNum(); // Number of columns (days)

            for (int col = 0; col < totalColumns; col++) {
                String dayOfWeek = sheet.getRow(0).getCell(col).getStringCellValue();
                LocalDate firstDate = parseDate(dateRow1.getCell(col));
                LocalDate secondDate = parseDate(dateRow2.getCell(col));

                if (firstDate == null || secondDate == null) {
                    throw new IllegalArgumentException("Invalid date format at column: " + col);
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
        } catch (Exception e) {
            throw new RuntimeException("Error processing Excel file: " + e.getMessage(), e);
        }
    }

    private LocalDate parseDate(Cell cell) {
        try {
            if (cell == null) return null;
            return cell.getLocalDateTimeCellValue().toLocalDate();
        } catch (Exception e) {
            throw new RuntimeException("Date parsing failed for cell: " + cell);
        }
    }

    private boolean isMealType(String cellValue) {
        return cellValue.equalsIgnoreCase("BREAKFAST") ||
               cellValue.equalsIgnoreCase("LUNCH") ||
               cellValue.equalsIgnoreCase("SNACKS") ||
               cellValue.equalsIgnoreCase("DINNER");
    }

    private void saveMenu(String day, LocalDate date, String mealType, String menuItems) {
        Menu menu = new Menu();
        menu.setDayOfWeek(day);
        menu.setDate(date);
        menu.setMealType(mealType);
        menu.setMenuItems(menuItems);
        menuRepository.save(menu);
    }

    private void createMenuTableIfNotExists() {
        String createTableQuery = """
            CREATE TABLE IF NOT EXISTS menu (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                day_of_week VARCHAR(255) NOT NULL,
                meal_type VARCHAR(255) NOT NULL,
                menu_items VARCHAR(2000),
                date DATE NOT NULL
            )
        """;
        entityManager.createNativeQuery(createTableQuery).executeUpdate();
    }
}