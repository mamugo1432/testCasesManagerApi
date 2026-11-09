# Tasks — Gestión de proyectos

Tareas derivadas de `plan.md`, ordenadas por dependencia, cada una de menos
de 30 minutos, con sus RF y un criterio «Hecho cuando:» verificable.
Convención: `[ ]` pendiente → `[x]` hecho y verificado.

## Fase 1 — Dominio

- [x] **T1. Modelo de dominio `Project`**
  - Crea `domain/model/Project` con `id`, `userId`, `title` y `createdAt`
    (`LocalDateTime`), POJO con Lombok y sin anotaciones JPA.
  - RF: RF-005, RF-009
  - Hecho cuando: la clase compila sin imports de `infraestructure/` ni de
    JPA y conserva el título tal cual se le pasa.

- [x] **T2. Excepciones de dominio de proyectos**
  - Crea `ProjectNotFoundException` (proyecto inexistente o ajeno) y
    `ProjectLimitExceededException` (límite alcanzado), ambas con mensaje en
    español.
  - RF: RF-004, RF-012, RF-021, RF-025
  - Hecho cuando: ambas existen en `domain/exception` y sus mensajes están en
    español; el mensaje del límite incluye el número de proyectos.

- [x] **T3. Puertos de entrada de proyectos**
  - Crea `CreateProjectUseCase`, `ListProjectsUseCase`, `GetProjectUseCase` y
    `DeleteProjectUseCase` con DTOs de aplicación como parámetros/retorno.
  - RF: RF-005, RF-016, RF-020, RF-022
  - Hecho cuando: las cuatro interfaces existen y solo referencian DTOs de
    `application/dto` o modelos de dominio.

- [x] **T4. Puertos de salida de proyectos**
  - Crea `ProjectRepository` (guardar, buscar por `id + userId`, listar por
    usuario ordenado, contar por usuario y borrar) y `ProjectLimitProvider`
    (devolver el máximo por usuario).
  - RF: RF-003, RF-012, RF-013, RF-016, RF-017, RF-022
  - Hecho cuando: `ProjectRepository` permite filtrar siempre por
    `id + userId` y `ProjectLimitProvider` expone el límite sin depender de
    infraestructura.

## Fase 2 — Aplicación

- [x] **T5. DTOs de proyecto con validaciones**
  - Crea `CreateProjectRequest` (`title` con `@NotBlank` y
    `@Size(max = 255)`) y `ProjectResponse` (`id`, `title`, `createdAt`
    como `String`); no incluir `userId` ni permitir fijar `createdAt`.
  - RF: RF-005, RF-006, RF-007, RF-008, RF-009, RF-011, RF-016, RF-020
  - Hecho cuando: `CreateProjectRequestValidationTest` en verde: rechaza
    nulo, vacío, solo espacios y 256 unidades UTF-16; acepta 255 unidades y
    127 emojis; conserva espacios exteriores.

- [x] **T6. `ProjectService` — creación**
  - Implementa `CreateProjectUseCase`: toma el `userId` del principal, no
    recorta el título, asigna `createdAt` en el servidor, permite títulos
    repetidos y construye `ProjectResponse` con la fecha en `dd/MM/yyyy`.
  - RF: RF-002, RF-005, RF-006, RF-009, RF-010
  - Hecho cuando: `ProjectServiceTest` en verde: el repositorio recibe el
    título exacto, el `userId` sale del principal, dos títulos iguales se
    aceptan y `createdAt` se devuelve como `dd/MM/yyyy`.

- [x] **T7. `ProjectService` — límite de proyectos**
  - Antes de guardar, cuenta los proyectos del usuario y compara con
    `ProjectLimitProvider`; si ya alcanzó el límite, lanza
    `ProjectLimitExceededException` sin llamar a `save`. Documenta el
    comportamiento best-effort.
  - RF: RF-012, RF-013, RF-014, RF-015
  - Hecho cuando: `ProjectServiceTest` en verde: con límite 10 y 10
    proyectos mockeados se lanza la excepción y no se guarda; con 9 se
    guarda el décimo; un usuario por encima del límite no puede crear.

- [x] **T8. `ProjectService` — listado**
  - Implementa `ListProjectsUseCase` delegando en el puerto la lista ya
    filtrada por usuario y ordenada por `created_at DESC, id DESC`; devuelve
    lista vacía si no hay proyectos.
  - RF: RF-016, RF-017, RF-018, RF-019
  - Hecho cuando: `ProjectServiceTest` en verde: respeta el orden entregado,
    devuelve `[]` sin proyectos y no mezcla proyectos de otros usuarios.

- [x] **T9. `ProjectService` — detalle**
  - Implementa `GetProjectUseCase` buscando por `id + userId`; si no
    aparece, lanza la misma `ProjectNotFoundException` que para un proyecto
    inexistente.
  - RF: RF-020, RF-021
  - Hecho cuando: `ProjectServiceTest` en verde: un proyecto propio se
    devuelve y uno ajeno o inexistente lanza la misma excepción con el mismo
    mensaje.

- [x] **T10. `ProjectService` — borrado**
  - Implementa `DeleteProjectUseCase` comprobando propiedad por
    `id + userId` y borrando solo la fila del proyecto dentro de una
    transacción; la cascada la ejecuta la base de datos.
  - RF: RF-022, RF-025, RF-026, RF-027
  - Hecho cuando: `ProjectServiceTest` en verde: un proyecto propio se borra,
    uno ajeno o inexistente lanza `ProjectNotFoundException` sin borrar, y
    tras borrar el usuario puede volver a crear hasta el límite.

## Fase 3 — Persistencia

- [x] **T11. Entidad y repositorio JPA de proyectos**
  - Crea `ProjectJpaEntity` mapeando `projects` (`id`, `user_id`, `title`,
    `created_at`) y `ProjectJpaRepository` con búsqueda por `id + userId`,
    listado ordenado, recuento y borrado.
  - RF: RF-003, RF-009, RF-012, RF-016, RF-017, RF-022
  - Hecho cuando: la entidad coincide con el script SQL, no tiene lógica de
    negocio y el repositorio declara las consultas filtradas por usuario y
    ordenadas por `createdAt` e `id` descendente.

- [x] **T12. Mapper de proyectos**
  - Crea `ProjectMapper` con MapStruct para mapear `Project` ↔
    `ProjectJpaEntity`; no mapea DTOs ni formatea fechas.
  - RF: RF-003, RF-009
  - Hecho cuando: `ProjectMapperTest` en verde: conserva `id`, `userId`,
    `title` y `createdAt` en ambos sentidos y compila sin dependencias de
    `application/`.

- [x] **T13. Adaptador de persistencia de proyectos**
  - Implementa `ProjectRepository` sobre `ProjectJpaRepository` y
    `ProjectMapper`, traduciendo entidad ↔ dominio.
  - RF: RF-003, RF-012, RF-016, RF-017, RF-022
  - Hecho cuando: el adaptador compila implementando el puerto y todos sus
    métodos delegan en el repositorio JPA con el filtro por usuario.

## Fase 4 — Configuración

- [x] **T14. Propiedad `app.projects.max-per-user`**
  - Crea `ProjectLimitProperties` en `infraestructure/config` con valor por
    defecto 10, validación de que sea mayor que 0 y la propiedad en
    `application.properties`.
  - RF: RF-013, RF-015
  - Hecho cuando: `ProjectLimitPropertiesTest` en verde: valor por defecto
    10, valor configurado distinto y valores 0 o negativos rechazados.

- [x] **T15. Adaptador del límite de proyectos**
  - Crea `ProjectLimitProviderAdapter`, que implementa `ProjectLimitProvider`
    devolviendo el valor de `ProjectLimitProperties`.
  - RF: RF-013
  - Hecho cuando: `ProjectLimitProviderAdapterTest` en verde: el adaptador
    devuelve el valor configurado y `ProjectService` lo recibe por el puerto.

## Fase 5 — Web

- [x] **T16. `ProjectController` — creación y listado**
  - Implementa `POST /api/projects` (201) y `GET /api/projects` (200) con
    `@Valid`, `@AuthenticationPrincipal` y el DTO de respuesta.
  - RF: RF-001, RF-002, RF-005, RF-011, RF-016
  - Hecho cuando: `ProjectControllerTest` en verde: 201 con
    `{id, title, createdAt}`, 200 con la lista, 401 sin token y los campos
    desconocidos del cuerpo se ignoran.

- [x] **T17. `ProjectController` — detalle y borrado**
  - Implementa `GET /api/projects/{id}` (200) y
    `DELETE /api/projects/{id}` (204) con `id` de tipo `Long`.
  - RF: RF-020, RF-022, RF-029
  - Hecho cuando: `ProjectControllerTest` en verde: 200 con el detalle, 204
    sin cuerpo, 404 para inexistente o ajeno y 400 con un `id` no numérico.

- [x] **T18. Documentación OpenAPI de proyectos**
  - Anota el controlador con springdoc y el esquema bearer existente.
  - RF: RF-001 (soporte)
  - Hecho cuando: Swagger UI muestra los cuatro endpoints de proyectos con
    seguridad bearer y sus códigos de respuesta.

## Fase 6 — Errores

- [x] **T19. Handler de excepciones de proyectos**
  - Amplía `GlobalExceptionHandler`: `ProjectNotFoundException` → 404 con
    mensaje genérico y `ProjectLimitExceededException` → 409 con el mensaje
    del límite.
  - RF: RF-004, RF-012, RF-021, RF-025
  - Hecho cuando: `GlobalExceptionHandlerTest` en verde: el 404 es idéntico
    para inexistente y ajeno y el 409 incluye el mensaje «No puedes crear más
    de {límite} proyectos. Elimina uno para crear otro.».

- [x] **T20. Handler de errores de entrada y errores internos**
  - Añade al handler: validación → 400 con `fields`; tipo de `id` inválido →
    400; JSON malformado → 400; método no soportado → 405; `Content-Type` no
    soportado → 415; error no controlado → 500 genérico.
  - RF: RF-028, RF-029
  - Hecho cuando: `GlobalExceptionHandlerTest` en verde: cada excepción
    produce su código con `ApiError` y el 500 no filtra clase ni mensaje
    interno.

## Fase 7 — Verificación

- [x] **T21. Suite de controlador completa**
  - Completa `ProjectControllerTest` con MockMvc para los cuatro endpoints,
    incluyendo 401 sin token, 404 de propiedad, 409 de límite y formato
    `dd/MM/yyyy`.
  - RF: RF-001, RF-005, RF-009, RF-016, RF-020, RF-022, RF-029
  - Hecho cuando: la suite de controlador está en verde y cubre todos los
    códigos del contrato de la spec.

- [x] **T22. `mvn test` y criterios de la spec**
  - Ejecuta la suite completa, aplica Spotless y repasa los criterios de
    finalización de `spec.md`.
  - RF: todos
  - Hecho cuando: `mvn test` y `mvn spotless:apply` terminan sin errores y
    los criterios de finalización de la spec quedan marcados o justificados.

- [ ] **T23. Verificación manual de la cascada en MySQL**
  - Crea un proyecto con test cases, pasos y etiquetas asociadas; bórralo y
    comprueba en MySQL qué filas desaparecen.
  - RF: RF-023, RF-024, RF-026
  - Hecho cuando: tras borrar el proyecto desaparecen sus test cases, pasos y
    asociaciones, y las filas de `tags` siguen existiendo.

- [ ] **T24. Verificación manual del flujo completo**
  - Crea → lista → consulta detalle → borra → intenta crear el undécimo
    proyecto.
  - RF: RF-005, RF-012, RF-016, RF-020, RF-022
  - Hecho cuando: el flujo responde 201, 200, 200, 204 y 409 con el mensaje
    del límite, y tras borrar un proyecto la creación vuelve a funcionar.
