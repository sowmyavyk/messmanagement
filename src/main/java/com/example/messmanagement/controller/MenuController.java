package com.example.messmanagement.controller;

import com.example.messmanagement.service.ExcelMenuUpdater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private ExcelMenuUpdater excelMenuUpdater;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            excelMenuUpdater.processExcelFile(file);
            return ResponseEntity.ok("Excel file processed and database updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to process Excel file: " + e.getMessage());
        }
    }
}