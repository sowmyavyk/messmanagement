package com.example.messmanagement.repository;

import com.example.messmanagement.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByDayOfWeekAndDate(String dayOfWeek, LocalDate date);
}