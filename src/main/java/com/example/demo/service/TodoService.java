package com.example.demo.service;

import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
import com.example.demo.repository.TodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TodoService {

    @Autowired
    private TodoRepository todoRepository;

    @Transactional(readOnly = true)
    public List<Todo> findAllByUser(User user) {
        return todoRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<Todo> findAllByUserOrderByDueDate(User user) {
        return todoRepository.findByUserOrderByDueDateAsc(user);
    }

    @Transactional(readOnly = true)
    public List<Todo> findAllByUserOrderByCreatedAt(User user) {
        return todoRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Todo> findByUserAndCompleted(User user, boolean completed) {
        return todoRepository.findByUserAndCompleted(user, completed);
    }

    @Transactional(readOnly = true)
    public List<Todo> findByUserAndDueDateBetween(User user, LocalDate startDate, LocalDate endDate) {
        return todoRepository.findByUserAndDueDateBetween(user, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Todo> findTodayTasks(User user) {
        return todoRepository.findByUserAndDueDateLessThanEqual(user, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Todo> searchUserTasks(User user, String keyword) {
        return todoRepository.findByUserAndTitleContainingIgnoreCase(user, keyword);
    }

    @Transactional(readOnly = true)
    public Optional<Todo> findById(Long id) {
        return todoRepository.findById(id);
    }

    @Transactional
    public Todo save(Todo todo) {
        return todoRepository.save(todo);
    }

    @Transactional
    public void delete(Todo todo) {
        todoRepository.delete(todo);
    }
    
    @Transactional
    public boolean toggleComplete(Long id, User user) {
        Optional<Todo> todoOpt = todoRepository.findById(id);
        if (todoOpt.isPresent()) {
            Todo todo = todoOpt.get();
            // Security check - make sure the todo belongs to the user
            if (todo.getUser().getId().equals(user.getId())) {
                todo.setCompleted(!todo.isCompleted());
                todoRepository.save(todo);
                return true;
            }
        }
        return false;
    }
} 