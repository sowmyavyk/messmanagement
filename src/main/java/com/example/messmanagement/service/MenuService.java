package com.example.messmanagement.service;

import com.example.messmanagement.entity.Menu;
import com.example.messmanagement.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MenuService {
    @Autowired
    private MenuRepository menuRepository;

    public List<Menu> getMenuByDate(LocalDate date) {
        // Log the date being used for the query
        System.out.println("Fetching menu for date: " + date);

        // Query based only on today's date
        return menuRepository.findByDate(date);
    }
}