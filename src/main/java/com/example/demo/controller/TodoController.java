package com.example.demo.controller;

import com.example.demo.dto.TodoExtractResponse;
import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
import com.example.demo.service.GeminiService;
import com.example.demo.service.TodoService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import jakarta.servlet.http.HttpSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Controller
@RequestMapping("/todos")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private GeminiService geminiService;
    
    @Value("${google.ai.enabled:false}")
    private boolean geminiEnabled;
    
    @Value("${google.ai.gemini-url:}")
    private String geminiUrl;
    
    @Value("${google.ai.api-key:}")
    private String apiKey;

    // Helper method to get current user
    private User getCurrentUser(Principal principal) {
        return userService.findByUsername(principal.getName()).orElseThrow(() -> 
            new IllegalStateException("User not found: " + principal.getName()));
    }

    @GetMapping
    public String listTodos(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        List<Todo> todos = todoService.findAllByUserOrderByDueDate(user);
        model.addAttribute("todos", todos);
        return "todo/list";
    }
    
    @GetMapping("/create")
    public String createTodoForm(Model model) {
        model.addAttribute("todo", new Todo());
        model.addAttribute("priorities", Todo.Priority.values());
        return "todo/form";
    }
    
    @PostMapping("/create")
    public String createTodo(@ModelAttribute Todo todo, Principal principal, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        todo.setUser(user);
        todoService.save(todo);
        redirectAttributes.addFlashAttribute("success", "Todo created successfully!");
        return "redirect:/todos";
    }
    
    @GetMapping("/edit/{id}")
    public String editTodoForm(@PathVariable Long id, Model model, Principal principal) {
        User user = getCurrentUser(principal);
        Optional<Todo> todoOpt = todoService.findById(id);
        
        if (todoOpt.isPresent()) {
            Todo todo = todoOpt.get();
            
            // Security check - ensure the todo belongs to the current user
            if (!todo.getUser().getId().equals(user.getId())) {
                return "redirect:/todos";
            }
            
            model.addAttribute("todo", todo);
            model.addAttribute("priorities", Todo.Priority.values());
            return "todo/form";
        }
        
        return "redirect:/todos";
    }
    
    @PostMapping("/update/{id}")
    public String updateTodo(@PathVariable Long id, @ModelAttribute Todo todo, Principal principal, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        Optional<Todo> existingTodoOpt = todoService.findById(id);
        
        if (existingTodoOpt.isPresent()) {
            Todo existingTodo = existingTodoOpt.get();
            
            // Security check - ensure the todo belongs to the current user
            if (!existingTodo.getUser().getId().equals(user.getId())) {
                return "redirect:/todos";
            }
            
            // Update fields
            existingTodo.setTitle(todo.getTitle());
            existingTodo.setDescription(todo.getDescription());
            existingTodo.setDueDate(todo.getDueDate());
            existingTodo.setPriority(todo.getPriority());
            
            todoService.save(existingTodo);
            redirectAttributes.addFlashAttribute("success", "Todo updated successfully!");
        }
        
        return "redirect:/todos";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteTodo(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        Optional<Todo> todoOpt = todoService.findById(id);
        
        if (todoOpt.isPresent()) {
            Todo todo = todoOpt.get();
            
            // Security check - ensure the todo belongs to the current user
            if (todo.getUser().getId().equals(user.getId())) {
                todoService.delete(todo);
                redirectAttributes.addFlashAttribute("success", "Todo deleted successfully!");
            }
        }
        
        return "redirect:/todos";
    }
    
    @PostMapping(value = "/toggle/{id}", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Boolean> toggleTodoStatus(@PathVariable Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);
        boolean success = todoService.toggleComplete(id, user);
        return ResponseEntity.ok(success);
    }
    
    // Filter endpoints
    @GetMapping("/filter/completed")
    public String filterCompleted(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        List<Todo> todos = todoService.findByUserAndCompleted(user, true);
        model.addAttribute("todos", todos);
        model.addAttribute("filterTitle", "Completed Tasks");
        return "todo/list";
    }
    
    @GetMapping("/filter/pending")
    public String filterPending(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        List<Todo> todos = todoService.findByUserAndCompleted(user, false);
        model.addAttribute("todos", todos);
        model.addAttribute("filterTitle", "Pending Tasks");
        return "todo/list";
    }
    
    @GetMapping("/filter/today")
    public String filterToday(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        List<Todo> todos = todoService.findTodayTasks(user);
        model.addAttribute("todos", todos);
        model.addAttribute("filterTitle", "Today's Tasks");
        return "todo/list";
    }
    
    @GetMapping("/search")
    public String searchTodos(@RequestParam String keyword, Model model, Principal principal) {
        User user = getCurrentUser(principal);
        List<Todo> todos = todoService.searchUserTasks(user, keyword);
        model.addAttribute("todos", todos);
        model.addAttribute("filterTitle", "Search Results for: " + keyword);
        return "todo/list";
    }
    
    // Gemini API integration for image processing
    @GetMapping("/extract-from-image")
    public String showImageUploadForm(Model model) {
        // Add Gemini API status to the model
        model.addAttribute("geminiEnabled", geminiEnabled);
        model.addAttribute("geminiUrl", geminiUrl);
        model.addAttribute("apiKeyConfigured", apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_API_KEY"));
        
        return "todo/image-upload";
    }
    
    @PostMapping("/extract-from-image")
    public String processImageTodos(@RequestParam("image") MultipartFile imageFile, 
                                  Principal principal, 
                                  RedirectAttributes redirectAttributes,
                                  HttpSession session) {
        User user = getCurrentUser(principal);
        
        try {
            TodoExtractResponse response = geminiService.extractTodosFromImage(imageFile);
            
            if (response.getTasks() != null && !response.getTasks().isEmpty()) {
                // Check if any tasks are missing dates
                boolean needsDateConfirmation = false;
                for (TodoExtractResponse.TodoTaskDto taskDto : response.getTasks()) {
                    if (taskDto.getDueDate() == null || taskDto.getDueDate().isEmpty()) {
                        needsDateConfirmation = true;
                        break;
                    }
                }
                
                // Store tasks in session for date confirmation if needed
                if (needsDateConfirmation) {
                    session.setAttribute("extractedTasks", response.getTasks());
                    return "redirect:/todos/confirm-dates";
                }
                
                // If all tasks have dates, save directly
                for (TodoExtractResponse.TodoTaskDto taskDto : response.getTasks()) {
                    Todo todo = taskDto.toTodoEntity();
                    todo.setUser(user);
                    todoService.save(todo);
                }
                
                redirectAttributes.addFlashAttribute("success", "Successfully extracted " + response.getTasks().size() + " todos from the image!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Could not extract any todos from the image. Please try again with a clearer image.");
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to process the image: " + e.getMessage());
        }
        
        return "redirect:/todos";
    }
    
    @GetMapping("/confirm-dates")
    public String confirmDates(Model model, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<TodoExtractResponse.TodoTaskDto> tasks = 
            (List<TodoExtractResponse.TodoTaskDto>) session.getAttribute("extractedTasks");
        
        if (tasks == null || tasks.isEmpty()) {
            return "redirect:/todos/extract-from-image";
        }
        
        model.addAttribute("tasks", tasks);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("tomorrow", LocalDate.now().plusDays(1));
        
        return "todo/confirm-dates";
    }
    
    @PostMapping("/save-with-dates")
    public String saveWithDates(@RequestParam("taskDates") String taskDatesJson,
                              Principal principal,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        
        @SuppressWarnings("unchecked")
        List<TodoExtractResponse.TodoTaskDto> tasks = 
            (List<TodoExtractResponse.TodoTaskDto>) session.getAttribute("extractedTasks");
        
        if (tasks == null || tasks.isEmpty()) {
            return "redirect:/todos/extract-from-image";
        }
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> taskDates = objectMapper.readValue(taskDatesJson, 
                new TypeReference<List<Map<String, Object>>>() {});
            
            int savedCount = 0;
            for (Map<String, Object> taskDate : taskDates) {
                int index = (Integer) taskDate.get("index");
                String date = (String) taskDate.get("date");
                
                if (index < tasks.size()) {
                    TodoExtractResponse.TodoTaskDto taskDto = tasks.get(index);
                    Todo todo = taskDto.toTodoEntity();
                    todo.setDueDate(LocalDate.parse(date));
                    todo.setUser(user);
                    todoService.save(todo);
                    savedCount++;
                }
            }
            
            // Clear the session
            session.removeAttribute("extractedTasks");
            
            redirectAttributes.addFlashAttribute("success", 
                "Successfully added " + savedCount + " todos to your list!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Error saving todos: " + e.getMessage());
        }
        
        return "redirect:/todos";
    }
    
    // API Debugging endpoint
    @GetMapping("/api-debug")
    @ResponseBody
    public Map<String, Object> debugGeminiApi() {
        Map<String, Object> debugInfo = new HashMap<>();
        
        // Collect configuration information
        debugInfo.put("geminiEnabled", geminiEnabled);
        debugInfo.put("geminiUrl", geminiUrl);
        debugInfo.put("apiKeyConfigured", apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_API_KEY"));
        debugInfo.put("apiKeyFirstChars", apiKey != null && apiKey.length() > 4 ? 
                apiKey.substring(0, 4) + "..." : null);
        debugInfo.put("modelName", geminiUrl.substring(geminiUrl.lastIndexOf("/") + 1, geminiUrl.lastIndexOf(":")));
        
        return debugInfo;
    }
} 