package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import com.example.demo.entity.Item;
import com.example.demo.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demo.config.DataInitializer;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private DataInitializer dataInitializer;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    @GetMapping
    public String adminHome(Model model) {
        logger.info("Admin home accessed with active profile: {}", activeProfile);
        model.addAttribute("activeProfile", activeProfile);
        model.addAttribute("items", itemRepository.findAll());
        // Get list of tables
        List<String> tables = jdbcTemplate.queryForList(getTablesQuery(), String.class);
        model.addAttribute("tables", tables);
        return "admin/index";
    }
    
    @GetMapping("/table/{tableName}")
    public String viewTable(@PathVariable String tableName, Model model) {
        logger.info("Viewing table: {} with active profile: {}", tableName, activeProfile);
        
        // Validate table name to prevent SQL injection
        if (!isValidTableName(tableName)) {
            logger.warn("Invalid table name attempted: {}", tableName);
            return "redirect:/admin";
        }
        
        try {
            // Get table data count
            String countSql = "SELECT COUNT(*) FROM " + tableName;
            Integer rowCount = jdbcTemplate.queryForObject(countSql, Integer.class);
            logger.info("Table {} has {} rows", tableName, rowCount);
            
            // Get a sample row first to extract exact column names with proper case
            String sampleSql = "SELECT * FROM " + tableName + " LIMIT 1";
            List<Map<String, Object>> sampleData = jdbcTemplate.queryForList(sampleSql);
            
            List<String> columns;
            if (!sampleData.isEmpty()) {
                // Get column names directly from the result metadata, preserving case
                columns = new java.util.ArrayList<>(sampleData.get(0).keySet());
                logger.info("Column names from sample data: {}", columns);
            } else {
                // Fallback to schema information
                columns = jdbcTemplate.queryForList(getColumnsQuery(tableName), String.class);
                logger.info("Column names from schema: {}", columns);
            }
            
            // Get table data
            String sql = "SELECT * FROM " + tableName + " LIMIT 100";
            List<Map<String, Object>> tableData = jdbcTemplate.queryForList(sql);
            
            // Log details for debugging
            logger.info("Retrieved {} rows from table {}", tableData.size(), tableName);
            if (!tableData.isEmpty()) {
                Map<String, Object> firstRow = tableData.get(0);
                logger.info("First row data for table {}:", tableName);
                for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
                    logger.info("  {} = {}", entry.getKey(), entry.getValue());
                }
            }
            
            model.addAttribute("tableName", tableName);
            model.addAttribute("columns", columns);
            model.addAttribute("tableData", tableData);
            model.addAttribute("totalRows", rowCount);
            model.addAttribute("showingRows", tableData.size());
            
            return "admin/table";
        } catch (Exception e) {
            logger.error("Error retrieving data from table {}: {}", tableName, e.getMessage(), e);
            model.addAttribute("error", "Error retrieving data: " + e.getMessage());
            return "redirect:/admin";
        }
    }
    
    @GetMapping("/query")
    public String executeQuery(@RequestParam(required = false) String sql, Model model) {
        model.addAttribute("sql", sql);
        
        if (sql != null && !sql.trim().isEmpty()) {
            try {
                // For safety, limit to SELECT queries only
                if (sql.trim().toLowerCase().startsWith("select")) {
                    Query query = entityManager.createNativeQuery(sql);
                    List<?> result = query.getResultList();
                    model.addAttribute("queryResult", result);
                    model.addAttribute("success", true);
                } else {
                    model.addAttribute("error", "Only SELECT queries are allowed for security reasons");
                }
            } catch (Exception e) {
                model.addAttribute("error", "Error executing query: " + e.getMessage());
            }
        }
        
        return "admin/query";
    }
    
    @GetMapping("/db-info")
    public String redirectToDbInfo() {
        // Redirect to the db-info page
        return "redirect:/db-info";
    }
    
    @GetMapping("/reinitialize-data")
    public String reinitializeData(RedirectAttributes redirectAttributes) {
        try {
            logger.info("Manual database initialization triggered");
            // Call data initializer manually
            String[] args = {};
            dataInitializer.run(args);
            redirectAttributes.addFlashAttribute("success", "Database initialization completed successfully");
        } catch (Exception e) {
            logger.error("Error during manual database initialization: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to initialize database: " + e.getMessage());
        }
        return "redirect:/admin";
    }
    
    @PostMapping("/table/{tableName}/delete")
    public String deleteTableData(@PathVariable String tableName, RedirectAttributes redirectAttributes) {
        logger.info("Deleting all data from table: {} with active profile: {}", tableName, activeProfile);
        
        // Validate table name to prevent SQL injection
        if (!isValidTableName(tableName)) {
            logger.warn("Invalid table name attempted in delete operation: {}", tableName);
            redirectAttributes.addFlashAttribute("error", "Invalid table name");
            return "redirect:/admin";
        }
        
        try {
            // Get current row count
            String countSql = "SELECT COUNT(*) FROM " + tableName;
            Integer rowCount = jdbcTemplate.queryForObject(countSql, Integer.class);
            
            // Execute delete query
            String deleteSql = "DELETE FROM " + tableName;
            int deletedRows = jdbcTemplate.update(deleteSql);
            
            logger.info("Successfully deleted {} rows from table {}", deletedRows, tableName);
            redirectAttributes.addFlashAttribute("success", "Successfully deleted " + deletedRows + " rows from table " + tableName);
            
            return "redirect:/admin/table/" + tableName;
        } catch (Exception e) {
            logger.error("Error deleting data from table {}: {}", tableName, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error deleting data: " + e.getMessage());
            return "redirect:/admin/table/" + tableName;
        }
    }
    
    @PostMapping("/table/{tableName}/delete/{id}")
    public String deleteRowById(@PathVariable String tableName, @PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Deleting row with ID {} from table: {}", id, tableName);
        
        // Validate table name to prevent SQL injection
        if (!isValidTableName(tableName)) {
            logger.warn("Invalid table name attempted in delete row operation: {}", tableName);
            redirectAttributes.addFlashAttribute("error", "Invalid table name");
            return "redirect:/admin";
        }
        
        try {
            // Execute delete query for the specific ID
            String deleteSql = "DELETE FROM " + tableName + " WHERE id = ?";
            int deletedRows = jdbcTemplate.update(deleteSql, id);
            
            if (deletedRows > 0) {
                logger.info("Successfully deleted row with ID {} from table {}", id, tableName);
                redirectAttributes.addFlashAttribute("success", "Successfully deleted row with ID " + id);
            } else {
                logger.warn("No rows found with ID {} in table {}", id, tableName);
                redirectAttributes.addFlashAttribute("warning", "No rows found with ID " + id);
            }
            
            return "redirect:/admin/table/" + tableName;
        } catch (Exception e) {
            logger.error("Error deleting row with ID {} from table {}: {}", id, tableName, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error deleting row: " + e.getMessage());
            return "redirect:/admin/table/" + tableName;
        }
    }
    
    @PostMapping("/table/{tableName}/delete-by-condition")
    public String deleteRowsByCondition(
            @PathVariable String tableName, 
            @RequestParam String column, 
            @RequestParam String operator, 
            @RequestParam String value, 
            RedirectAttributes redirectAttributes) {
        
        logger.info("Deleting rows from table: {} where {} {} {}", tableName, column, operator, value);
        
        // Validate table name and column to prevent SQL injection
        if (!isValidTableName(tableName) || !isValidColumnName(column)) {
            logger.warn("Invalid table or column name attempted in conditional delete: {}.{}", tableName, column);
            redirectAttributes.addFlashAttribute("error", "Invalid table or column name");
            return "redirect:/admin/table/" + tableName;
        }
        
        // Validate operator
        if (!isValidOperator(operator)) {
            logger.warn("Invalid operator attempted in conditional delete: {}", operator);
            redirectAttributes.addFlashAttribute("error", "Invalid operator");
            return "redirect:/admin/table/" + tableName;
        }
        
        try {
            // Build condition
            String condition = column + " " + operator + " ?";
            
            // Special case for NULL values
            if ("IS NULL".equals(operator) || "IS NOT NULL".equals(operator)) {
                condition = column + " " + operator;
                // Execute delete query for the condition without parameter
                String deleteSql = "DELETE FROM " + tableName + " WHERE " + condition;
                int deletedRows = jdbcTemplate.update(deleteSql);
                
                logger.info("Successfully deleted {} rows from table {} where {}", deletedRows, tableName, condition);
                redirectAttributes.addFlashAttribute("success", "Successfully deleted " + deletedRows + " rows where " + condition);
            } else {
                // Execute delete query for the condition with parameter
                String deleteSql = "DELETE FROM " + tableName + " WHERE " + condition;
                int deletedRows = jdbcTemplate.update(deleteSql, value);
                
                logger.info("Successfully deleted {} rows from table {} where {} {} {}", deletedRows, tableName, column, operator, value);
                redirectAttributes.addFlashAttribute("success", "Successfully deleted " + deletedRows + " rows where " + 
                                                   column + " " + operator + " '" + value + "'");
            }
            
            return "redirect:/admin/table/" + tableName;
        } catch (Exception e) {
            logger.error("Error deleting rows by condition from table {}: {}", tableName, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error deleting rows: " + e.getMessage());
            return "redirect:/admin/table/" + tableName;
        }
    }
    
    private String getTablesQuery() {
        if ("prod".equals(activeProfile)) {
            // MySQL query to get tables
            return "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = database()";
        } else {
            // H2 query to get tables
            return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'";
        }
    }
    
    private String getColumnsQuery(String tableName) {
        if ("prod".equals(activeProfile)) {
            // MySQL query to get columns
            return "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = database() AND TABLE_NAME = '" + tableName + "'";
        } else {
            // H2 query to get columns
            return "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = '" + tableName + "'";
        }
    }
    
    private boolean isValidTableName(String tableName) {
        // Simple validation to prevent SQL injection
        return tableName.matches("[a-zA-Z0-9_]+");
    }
    
    private boolean isValidColumnName(String columnName) {
        // Simple validation to prevent SQL injection
        return columnName.matches("[a-zA-Z0-9_]+");
    }
    
    private boolean isValidOperator(String operator) {
        // Whitelist of allowed operators
        final String[] VALID_OPERATORS = {
            "=", "<>", "!=", ">", "<", ">=", "<=", "LIKE", "NOT LIKE", "IS NULL", "IS NOT NULL"
        };
        
        for (String validOp : VALID_OPERATORS) {
            if (validOp.equalsIgnoreCase(operator)) {
                return true;
            }
        }
        return false;
    }
} 