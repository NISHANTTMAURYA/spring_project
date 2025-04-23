package com.example.demo.config;

import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
import com.example.demo.service.TodoService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;
    
    @Autowired
    private TodoService todoService;
    
    @Value("${app.init-db:false}")
    private boolean initDb;

    @Override
    @Transactional
    public void run(String... args) {
        if (initDb) {
            initializeData();
        }
    }

    private void initializeData() {
        // Only initialize if no users exist
        if (!userService.existsByUsername("admin") && !userService.existsByUsername("user")) {
            // Create admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123"); // Will be encoded by the userService
            admin.setEmail("admin@example.com");
            admin.setFullName("Admin User");
            admin.setRoles(Collections.singletonList("ADMIN"));
            userService.registerNewUser(admin);
            
            // Create regular user
            User user = new User();
            user.setUsername("user");
            user.setPassword("user123"); // Will be encoded by the userService
            user.setEmail("user@example.com");
            user.setFullName("Regular User");
            userService.registerNewUser(user);
            
            // Create some sample todos for the regular user
            createSampleTodos(user);
        }
    }
    
    private void createSampleTodos(User user) {
        // Today's high priority task
        Todo todo1 = new Todo();
        todo1.setTitle("Complete the Spring Boot project");
        todo1.setDescription("Finish implementing the CRUD operations and test the application");
        todo1.setDueDate(LocalDate.now());
        todo1.setPriority(Todo.Priority.HIGH);
        todo1.setUser(user);
        todoService.save(todo1);
        
        // Future medium priority task
        Todo todo2 = new Todo();
        todo2.setTitle("Learn Gemini API");
        todo2.setDescription("Understand how to integrate Google's Gemini API for image recognition");
        todo2.setDueDate(LocalDate.now().plusDays(3));
        todo2.setPriority(Todo.Priority.MEDIUM);
        todo2.setUser(user);
        todoService.save(todo2);
        
        // Future low priority task
        Todo todo3 = new Todo();
        todo3.setTitle("Update portfolio website");
        todo3.setDescription("Add the new todo app project to my developer portfolio");
        todo3.setDueDate(LocalDate.now().plusDays(7));
        todo3.setPriority(Todo.Priority.LOW);
        todo3.setUser(user);
        todoService.save(todo3);
        
        // Completed task
        Todo todo4 = new Todo();
        todo4.setTitle("Set up Spring Boot project");
        todo4.setDescription("Initialize the project structure and configure the dependencies");
        todo4.setDueDate(LocalDate.now().minusDays(1));
        todo4.setPriority(Todo.Priority.HIGH);
        todo4.setCompleted(true);
        todo4.setUser(user);
        todoService.save(todo4);
    }
}