package com.example.messmanagement.controller;

import com.example.messmanagement.entity.Menu;
import com.example.messmanagement.service.ExcelMenuUpdater;
import com.example.messmanagement.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private ExcelMenuUpdater excelMenuUpdater;

    @Autowired
    private MenuService menuService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            excelMenuUpdater.processExcelFile(file);
            return ResponseEntity.ok("Excel file processed and database updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to process Excel file: " + e.getMessage());
        }
    }

    @GetMapping("/today")
    public ResponseEntity<List<Menu>> getTodayMenu() {
        try {
            LocalDate today = LocalDate.now(); // Get today's date
            System.out.println("Today's Date: " + today); // Debugging log
            List<Menu> todayMenu = menuService.getMenuByDate(today); // Fetch menu for today
            if (todayMenu.isEmpty()) {
                return ResponseEntity.ok().body(List.of()); // Return empty if no results found
            }
            return ResponseEntity.ok(todayMenu); // Return the list of today's menu
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null); // Handle exceptions gracefully
        }
    }
}