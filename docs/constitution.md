# Constitución — TestCase Manager Backend

1. Stack fijo: Java 21, Maven, Spring Boot 4.1.1, MySQL 8, JWT (jjwt 0.12.6), MapStruct, Lombok, springdoc-openapi; nada de dependencias nuevas sin preguntar.
2. Arquitectura hexagonal estricta: los servicios implementan port/in, los adaptadores port/out; domain y application nunca importan de infrastructure.
3. El script SQL es la única fuente de verdad del esquema: ddl-auto=none y no se toca el script sin aprobación.
4. La autorización por rol se centraliza en `SecurityFilterChain` con `hasRole`/`hasAnyRole` y los controladores no reciben `@AuthenticationPrincipal`. Además, toda operación sobre recursos verifica la propiedad del usuario autenticado; un recurso ajeno responde 404, nunca 403.
5. Toda regla de negocio (transición a COMPLETED, normalización de etiquetas) vive en dominio/aplicación, jamás en controladores. Las cascadas físicas de borrado se delegan en las FK del esquema (`ON DELETE CASCADE`); la aplicación no las duplica con borrados manuales.
6. El backend valida siempre la entrada (@Valid + DTOs), aunque el frontend también lo haga.
7. Los errores se devuelven con excepciones de dominio + GlobalExceptionHandler y respuesta estructurada; sin stack traces ni detalles internos al cliente.
8. Las estadísticas se calculan al vuello; nunca se guardan valores precalculados en BD.
9. Toda lógica de negocio nueva tiene tests unitarios (JUnit 5 + Mockito) en verde antes de dar la tarea por terminada.
10. Cada tarea termina con `mvn test` en verde y el checklist de la skill hexagonal verificado.
11. Nada fuera del alcance (alcance_proyecto.md §8) sin aprobación explícita, aunque parezca pequeño.
12. Antes de implementar una funcionalidad debe existir su spec en docs/ y la implementación cumple sus criterios de aceptación.
