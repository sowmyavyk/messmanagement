package com.example.messmanagement.controller;

import com.example.messmanagement.entity.Menu;
import com.example.messmanagement.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping("/today")
    public List<Menu> getTodayMenu() {
        LocalDate today = LocalDate.now();
        return menuService.getDailyMenu(today.getDayOfWeek().toString(), today);
    }
}