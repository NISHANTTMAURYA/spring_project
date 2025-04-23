package com.example.demo.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        model.addAttribute("timestamp", new java.util.Date());
        model.addAttribute("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            model.addAttribute("status", statusCode);
            model.addAttribute("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
            
            // Handle specific HTTP status codes
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("message", "The requested resource was not found");
                return "error/404";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("message", "You don't have permission to access this resource");
                return "error/403";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("message", errorMessage != null ? errorMessage : "An unexpected error occurred");
            }
        } else {
            model.addAttribute("status", 500);
            model.addAttribute("error", "Error");
            model.addAttribute("message", "Unknown error occurred");
        }
        
        // Check if it's a Gemini API error
        if (exception != null && exception.toString().contains("Gemini")) {
            model.addAttribute("message", "Failed to process request with Gemini API");
            return "error/gemini-error";
        }
        
        return "error";
    }
    
    // Handle specific Gemini API error
    @GetMapping("/gemini-error")
    public String geminiError(Model model) {
        model.addAttribute("timestamp", new java.util.Date());
        model.addAttribute("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        model.addAttribute("error", "Gemini API Error");
        model.addAttribute("message", "Unable to process request using Gemini AI service");
        model.addAttribute("path", "/gemini-processing");
        return "error/gemini-error";
    }
    
    // Custom 404 error page
    @GetMapping("/404")
    public String notFoundError(Model model) {
        model.addAttribute("timestamp", new java.util.Date());
        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", "The requested resource could not be found");
        model.addAttribute("path", "/unknown");
        return "error/404";
    }
} 