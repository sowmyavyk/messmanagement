package com.example.messmanagement.service;

import com.example.messmanagement.entity.Menu;
import com.example.messmanagement.repository.MenuRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;

@Service
public class ExcelMenuUpdater {

    @Autowired
    private MenuRepository menuRepository;

    @Scheduled(fixedRate = 86400000) // Runs daily
    public void updateMenuFromExcel() {
        try (InputStream fis = new ClassPathResource("resources/20-Jan_to_2-Feb_menu.xlsx").getInputStream();
            Workbook workbook = new XSSFWorkbook(fis)) {
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
        } catch (Exception e) {
            System.err.println("Error while processing Excel file: " + e.getMessage());
            e.printStackTrace();
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