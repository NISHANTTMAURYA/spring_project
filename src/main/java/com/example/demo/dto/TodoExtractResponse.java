package com.example.demo.dto;

import com.example.demo.entity.Todo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodoExtractResponse {
    private List<TodoTaskDto> tasks;
    
    // Explicitly add getter and setter for tasks
    public List<TodoTaskDto> getTasks() {
        return tasks;
    }
    
    public void setTasks(List<TodoTaskDto> tasks) {
        this.tasks = tasks;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoTaskDto {
        private String title;
        private String description;
        private String priority;
        private String dueDate;
        
        // Explicitly add getters and setters
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getPriority() {
            return priority;
        }
        
        public void setPriority(String priority) {
            this.priority = priority;
        }
        
        public String getDueDate() {
            return dueDate;
        }
        
        public void setDueDate(String dueDate) {
            this.dueDate = dueDate;
        }
        
        public Todo toTodoEntity() {
            Todo todo = new Todo();
            todo.setTitle(title);
            todo.setDescription(description);
            
            // Parse priority if provided
            if (priority != null && !priority.isEmpty()) {
                try {
                    todo.setPriority(Todo.Priority.valueOf(priority.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Default to MEDIUM if invalid priority
                    todo.setPriority(Todo.Priority.MEDIUM);
                }
            }
            
            // Parse due date if provided
            if (dueDate != null && !dueDate.isEmpty()) {
                try {
                    todo.setDueDate(LocalDate.parse(dueDate));
                } catch (Exception e) {
                    // Default to today if invalid date
                    todo.setDueDate(LocalDate.now());
                }
            }
            
            return todo;
        }
    }
} 