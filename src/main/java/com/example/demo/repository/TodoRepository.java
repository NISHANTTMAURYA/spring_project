package com.example.demo.repository;

import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUser(User user);
    List<Todo> findByUserOrderByDueDateAsc(User user);
    List<Todo> findByUserOrderByCreatedAtDesc(User user);
    List<Todo> findByUserAndCompleted(User user, boolean completed);
    List<Todo> findByUserAndDueDateBetween(User user, LocalDate startDate, LocalDate endDate);
    List<Todo> findByUserAndDueDateLessThanEqual(User user, LocalDate date);
    List<Todo> findByUserAndTitleContainingIgnoreCase(User user, String keyword);
} 