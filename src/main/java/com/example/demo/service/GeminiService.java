package com.example.demo.service;

import com.example.demo.dto.TodoExtractResponse;
import com.example.demo.entity.Todo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${google.ai.api-key}")
    private String apiKey;
    
    @Value("${google.ai.gemini-url}")
    private String geminiUrl;
    
    @Value("${google.ai.enabled:false}")
    private boolean enabled;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Extract todos from an image using Gemini API
     */
    public TodoExtractResponse extractTodosFromImage(MultipartFile imageFile) throws IOException {
        // Check if Gemini API is enabled
        if (!enabled || apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY")) {
            logger.warn("Gemini API is disabled or API key is not configured. Using fallback method.");
            logger.debug("API Enabled: {}, API Key configured properly: {}", 
                    enabled, 
                    !(apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY")));
            return createFallbackResponse(imageFile);
        }
        
        try {
            logger.info("Starting Gemini API call for image processing: {}", imageFile.getOriginalFilename());
            logger.debug("Image details - Size: {} bytes, Content Type: {}", 
                    imageFile.getSize(), imageFile.getContentType());
            
            // Convert image to base64
            byte[] imageBytes = imageFile.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            logger.debug("Successfully converted image to base64 (length: {} chars)", base64Image.length());
            
            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);
            
            // Create prompt
            String prompt = "Extract todo tasks from this image. Your primary objective is to accurately identify all tasks and their details.\n\n"
                    + "For each task, extract the following information:\n"
                    + "1. Title (required): The main task description\n"
                    + "2. Description (optional): Any additional details about the task\n"
                    + "3. Priority (optional): The importance level of the task\n"
                    + "4. Due Date (important): When the task should be completed\n\n"
                    + "IMPORTANT INSTRUCTIONS FOR DATES:\n"
                    + "- Look carefully for any dates mentioned in the image\n"
                    + "- Dates might appear as 'due on May 5', 'by tomorrow', 'next Monday', etc.\n"
                    + "- Convert all date references to ISO format (YYYY-MM-DD)\n"
                    + "- If a date is mentioned without a year, assume current year\n"
                    + "- If a relative date is mentioned (tomorrow, next week), calculate the actual date\n"
                    + "- Today's date is " + LocalDate.now().toString() + "\n\n"
                    + "Format the response as a JSON array of tasks with fields: title, description, priority, dueDate. "
                    + "For priority, use values: LOW, MEDIUM, HIGH. "
                    + "For dueDate, use ISO format (YYYY-MM-DD) or leave empty if no date is specified.";
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> part1 = new HashMap<>();
            Map<String, Object> part2 = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            
            part1.put("text", prompt);
            
            inlineData.put("mimeType", imageFile.getContentType() != null ? imageFile.getContentType() : "image/jpeg");
            inlineData.put("data", base64Image);
            part2.put("inlineData", inlineData);
            
            List<Object> parts = new ArrayList<>();
            parts.add(part1);
            parts.add(part2);
            
            contents.put("parts", parts);
            requestBody.put("contents", contents);
            
            // Set model parameters for Gemini Pro Vision model
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.2);
            generationConfig.put("maxOutputTokens", 1024);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            requestBody.put("generationConfig", generationConfig);
            
            // Log request details
            try {
                String requestJson = objectMapper.writeValueAsString(requestBody);
                logger.debug("Gemini API Request URL: {}", geminiUrl);
                logger.debug("Gemini API Request body structure (without base64 content): {}", 
                        requestJson.replaceAll("\"data\":\"[^\"]+\"", "\"data\":\"[BASE64_DATA]\""));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to log request body: {}", e.getMessage());
            }
            
            // Make API request
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            logger.info("Sending request to Gemini API...");
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<JsonNode> apiResponse;
            
            try {
                apiResponse = restTemplate.postForEntity(geminiUrl, entity, JsonNode.class);
                long endTime = System.currentTimeMillis();
                logger.info("Received response from Gemini API in {} ms", (endTime - startTime));
            } catch (RestClientException e) {
                logger.error("Error calling Gemini API: {}", e.getMessage());
                if (e.getMessage().contains("404")) {
                    logger.error("404 Not Found: This typically means the model name is incorrect. " + 
                                "Check that '{}' is a valid model name for generateContent method.", 
                                geminiUrl.substring(geminiUrl.lastIndexOf("/") + 1));
                }
                return createFallbackResponse(imageFile);
            }
            
            // Process response
            if (apiResponse.getBody() != null) {
                JsonNode responseBody = apiResponse.getBody();
                
                // Check for errors in the response
                JsonNode errorNode = responseBody.path("error");
                if (!errorNode.isMissingNode()) {
                    String errorCode = errorNode.path("code").asText();
                    String errorMessage = errorNode.path("message").asText();
                    logger.error("Gemini API returned an error - Code: {}, Message: {}", errorCode, errorMessage);
                    return createFallbackResponse(imageFile);
                }
                
                try {
                    String responseJson = objectMapper.writeValueAsString(responseBody);
                    logger.debug("Raw API Response: {}", responseJson);
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to log response body: {}", e.getMessage());
                }
                
                // Extract text content from the response - works for gemini-pro-vision
                String textContent;
                try {
                    textContent = responseBody
                            .path("candidates")
                            .path(0)
                            .path("content")
                            .path("parts")
                            .path(0)
                            .path("text")
                            .asText();
                    
                    logger.info("Successfully extracted text content from Gemini response");
                    logger.debug("Extracted text content: {}", textContent);
                } catch (Exception e) {
                    logger.error("Error parsing Gemini response format: {}", e.getMessage());
                    logger.error("Expected response structure not found. Response might be in a different format.");
                    return createFallbackResponse(imageFile);
                }
                
                // Extract JSON from response text
                int jsonStart = textContent.indexOf('[');
                int jsonEnd = textContent.lastIndexOf(']') + 1;
                
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = textContent.substring(jsonStart, jsonEnd);
                    logger.info("Found JSON data in the response");
                    logger.debug("Extracted JSON: {}", jsonStr);
                    
                    try {
                        TodoExtractResponse.TodoTaskDto[] tasks = objectMapper.readValue(jsonStr, TodoExtractResponse.TodoTaskDto[].class);
                        
                        TodoExtractResponse result = new TodoExtractResponse();
                        result.setTasks(Arrays.asList(tasks));
                        logger.info("Successfully extracted {} tasks from image", tasks.length);
                        
                        // Log the extracted tasks
                        for (int i = 0; i < tasks.length; i++) {
                            TodoExtractResponse.TodoTaskDto task = tasks[i];
                            logger.debug("Task {}: Title='{}', Priority={}, DueDate={}", 
                                    i+1, task.getTitle(), task.getPriority(), task.getDueDate());
                        }
                        
                        return result;
                    } catch (Exception e) {
                        logger.error("Error parsing JSON from Gemini response: {}", e.getMessage());
                        return createFallbackResponse(imageFile);
                    }
                } else {
                    logger.warn("No JSON data found in Gemini response");
                    logger.debug("Response does not contain JSON array. Text content: {}", textContent);
                    return createFallbackResponse(imageFile);
                }
            }
            
            logger.warn("Received empty response from Gemini API");
            return new TodoExtractResponse();
        } catch (RestClientException e) {
            logger.error("Error calling Gemini API: {}", e.getMessage());
            return createFallbackResponse(imageFile);
        } catch (Exception e) {
            logger.error("Unexpected error during Gemini API processing: {}", e.getMessage());
            logger.debug("Exception details:", e);
            return createFallbackResponse(imageFile);
        }
    }
    
    /**
     * Creates a fallback response when the Gemini API is not available
     */
    private TodoExtractResponse createFallbackResponse(MultipartFile imageFile) {
        logger.info("Creating fallback response for image: {}", imageFile.getOriginalFilename());
        
        TodoExtractResponse response = new TodoExtractResponse();
        List<TodoExtractResponse.TodoTaskDto> tasks = new ArrayList<>();
        
        // Create a simple task based on image name
        TodoExtractResponse.TodoTaskDto task = new TodoExtractResponse.TodoTaskDto();
        task.setTitle("Task from image: " + imageFile.getOriginalFilename());
        task.setDescription("This task was created without Gemini API. To enable Gemini features, configure a valid API key in application.properties.");
        task.setPriority("MEDIUM");
        task.setDueDate(LocalDate.now().plusDays(7).toString());
        
        tasks.add(task);
        response.setTasks(tasks);
        
        logger.debug("Created fallback task with title: {}", task.getTitle());
        return response;
    }
} 