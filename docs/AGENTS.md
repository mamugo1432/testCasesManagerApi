# AGENTS.md — TestCase Manager (Backend)

## Proyecto

Backend de TestCase Manager, una web para gestionar proyectos y casos de prueba
manuales. API REST con Java 21, Maven y Spring Boot 4.1.1, arquitectura hexagonal
proporcionada (dominio / aplicación / adaptadores). Persistencia en MySQL 8 con
Spring Data JPA (`ddl-auto=none`; el esquema lo define el script SQL, fuente de
verdad). Autenticación JWT stateless (access token + endpoint de validación
`GET /api/auth/me`, sin refresh token). Documentación de la API con
springdoc-openapi (Swagger UI).

## Comandos

- Ejecutar: `mvn spring-boot:run`
- Tests: `mvn test`
- Lint/formato: `mvn spotless:apply` (Spotless se configura en el setup inicial)

## Estilo y convenciones

- Java 21 (records para DTOs, text blocks, switch expresivo donde aporte).
- Identificadores, clases y paquetes en inglés; specs (`docs/`) y mensajes de
  error/validación en español.
- Arquitectura hexagonal: `domain/` (entidades JPA, enums, excepciones),
  `application/` (servicios/casos de uso y puertos), `infrastructure/`
  (adaptadores persistence/web/config). Sin dependencias de infraestructura
  hacia atrás.
- DTOs separados de las entidades en la capa web: `*Request` / `*Response`.
- Enumeras de dominio: `Priority` (HIGH, MEDIUM, LOW) e
  `Status` (IN_PROGRESS, COMPLETED).
- Errores de API con respuesta estructurada (mensaje + campos), códigos HTTP
  coherentes (400 validación, 401 auth, 404 no encontrado o no propio).
- Propiedades configurables (expiración JWT, orígenes CORS) en
  `application.yml`, sin valores mágicos en el código.

## Reglas

- Lee la spec activa en `docs/` (00 a 05) antes de tocar código de esa
  funcionalidad; la implementación debe cumplir sus criterios de aceptación.
- No modificar el esquema de la base de datos (script SQL) sin preguntar antes.
- No añadir dependencias, endpoints ni funcionalidades fuera del alcance sin
  preguntar primero (ver `alcance_proyecto.md`, sección 8).
- Toda operación sobre proyectos o test cases debe verificar la propiedad del
  usuario autenticado (404 si no es suyo).
- La normalización de etiquetas se hace solo en el backend (trim, minúsculas,
  sin diacríticas), reutilizando la etiqueta existente.

## Al terminar cualquier tarea

- Ejecutar `mvn test` y asegurar que el proyecto compila y pasa todos los
  tests antes de dar la tarea por terminada.
