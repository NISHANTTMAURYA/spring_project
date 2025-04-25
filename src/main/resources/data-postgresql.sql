-- PostgreSQL sample data

-- Sample user (password is 'password')
INSERT INTO users (username, password, email, full_name) 
SELECT 'admin', '$2a$10$rYlzymM7HxwB1IcrJZeW0.oAYnL8kDagGGtzFNK0CRnpvAVUu3jdu', 'admin@example.com', 'Admin User'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- User roles
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN' FROM users WHERE username = 'admin'
AND NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'admin') AND role = 'ROLE_ADMIN');

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER' FROM users WHERE username = 'admin'
AND NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'admin') AND role = 'ROLE_USER');

-- Sample todos for admin user
INSERT INTO todos (title, description, due_date, completed, priority, user_id, created_at, updated_at)
SELECT 'Complete project', 'Finish the Spring Boot Todo project with Gemini Vision integration', CURRENT_DATE + INTERVAL '7 days', false, 'HIGH', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users WHERE username = 'admin'
AND NOT EXISTS (SELECT 1 FROM todos WHERE title = 'Complete project' AND user_id = (SELECT id FROM users WHERE username = 'admin'));

INSERT INTO todos (title, description, due_date, completed, priority, user_id, created_at, updated_at)
SELECT 'Learn more about AI', 'Study Gemini Vision API capabilities', CURRENT_DATE + INTERVAL '14 days', false, 'MEDIUM', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users WHERE username = 'admin'
AND NOT EXISTS (SELECT 1 FROM todos WHERE title = 'Learn more about AI' AND user_id = (SELECT id FROM users WHERE username = 'admin')); 