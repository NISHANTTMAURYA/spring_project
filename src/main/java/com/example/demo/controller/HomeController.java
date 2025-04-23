package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.TodoService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private TodoService todoService;

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        // Add attributes for authenticated users
        if (principal != null) {
            Optional<User> userOpt = userService.findByUsername(principal.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                model.addAttribute("pendingTodoCount", todoService.findByUserAndCompleted(user, false).size());
                model.addAttribute("todayTodoCount", todoService.findTodayTasks(user).size());
            }
        }
        
        return "home";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        if (principal != null) {
            Optional<User> userOpt = userService.findByUsername(principal.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                model.addAttribute("pendingTodos", todoService.findByUserAndCompleted(user, false));
                model.addAttribute("todayTodos", todoService.findTodayTasks(user));
                model.addAttribute("completedTodos", todoService.findByUserAndCompleted(user, true));
            }
        }
        
        return "dashboard";
    }
} 