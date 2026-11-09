package es.test.manager.application.dto.project;

/**
 * Public representation of a project. It never exposes the owner id, and the creation date is
 * formatted by the application layer as {@code dd/MM/yyyy} (RF-009, RF-016, RF-020).
 */
public record ProjectResponse(Long id, String title, String createdAt) {}
