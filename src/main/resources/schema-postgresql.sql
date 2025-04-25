-- PostgreSQL database schema

-- Create a dedicated schema for our application
CREATE SCHEMA IF NOT EXISTS todoapp;

-- Set the search path to use our schema
SET search_path TO todoapp;

-- Users table
CREATE TABLE IF NOT EXISTS todoapp.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255)
);

-- User roles table
CREATE TABLE IF NOT EXISTS todoapp.user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES todoapp.users(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role)
);

-- Todos table
CREATE TABLE IF NOT EXISTS todoapp.todos (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE,
    completed BOOLEAN DEFAULT FALSE,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES todoapp.users(id) ON DELETE CASCADE
); 