-- ============================================================
-- TestCase Manager
-- Esquema inicial para MySQL 8.0.16+
-- ============================================================
-- Tablas:
--   users          Usuarios registrados
--   projects       Proyectos propiedad de un usuario
--   test_cases     Casos de prueba de cada proyecto
--   test_steps     Pasos ordenados de cada caso de prueba
--   tags           Etiquetas globales reutilizables
--   test_case_tags Relación N:M entre casos de prueba y etiquetas
--
-- Nota:
-- La normalización de etiquetas (trim, minúsculas y eliminación
-- de tildes) debe realizarse en el backend. normalized_name tiene
-- una restricción UNIQUE para evitar etiquetas normalizadas duplicadas.
--
-- La regla que impide pasar a COMPLETED sin pasos y resultado
-- esperado debe validarse en la lógica de negocio de Spring Boot,
-- ya que depende de datos de otras tablas.
-- ============================================================

CREATE DATABASE IF NOT EXISTS testcase_manager
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE testcase_manager;

CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE projects (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_projects_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_projects_user_id (user_id)
) ENGINE = InnoDB;

CREATE TABLE test_cases (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    project_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    expected_result VARCHAR(255) NULL,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_test_cases PRIMARY KEY (id),
    CONSTRAINT fk_test_cases_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_test_cases_priority
        CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_test_cases_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    INDEX idx_test_cases_project_id (project_id),
    INDEX idx_test_cases_project_status (project_id, status),
    INDEX idx_test_cases_project_priority (project_id, priority)
) ENGINE = InnoDB;

CREATE TABLE test_steps (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    test_case_id BIGINT UNSIGNED NOT NULL,
    instruction VARCHAR(255) NOT NULL,
    position INT UNSIGNED NOT NULL,

    CONSTRAINT pk_test_steps PRIMARY KEY (id),
    CONSTRAINT fk_test_steps_test_case
        FOREIGN KEY (test_case_id) REFERENCES test_cases (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT uq_test_steps_position UNIQUE (test_case_id, position),
    INDEX idx_test_steps_test_case_id (test_case_id)
) ENGINE = InnoDB;

CREATE TABLE tags (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    normalized_name VARCHAR(50)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin
        NOT NULL,

    CONSTRAINT pk_tags PRIMARY KEY (id),
    CONSTRAINT uq_tags_normalized_name UNIQUE (normalized_name)
) ENGINE = InnoDB;

CREATE TABLE test_case_tags (
    test_case_id BIGINT UNSIGNED NOT NULL,
    tag_id BIGINT UNSIGNED NOT NULL,

    CONSTRAINT pk_test_case_tags PRIMARY KEY (test_case_id, tag_id),
    CONSTRAINT fk_test_case_tags_test_case
        FOREIGN KEY (test_case_id) REFERENCES test_cases (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_test_case_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_test_case_tags_tag_id (tag_id)
) ENGINE = InnoDB;
