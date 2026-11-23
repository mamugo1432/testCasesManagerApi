# Plan — Gestión de proyectos

Plan técnico de implementación para `specs/01-projects/spec.md`. Define
módulos, modelo de datos, decisiones justificadas y estrategia de tests.
Cada parte indica los RF que cubre. Sin código: las firmas concretas se
resolverán durante la implementación.

## Decisiones técnicas

| # | Decisión | Alternativa descartada | Por qué | RF |
|---|---|---|---|---|
| D1 | Arquitectura hexagonal con puertos de entrada y salida: `ProjectController` → casos de uso → `ProjectService` → `ProjectRepository` → adaptador JPA | Controlador usando directamente el repositorio JPA | Constitución 2 y 5: las reglas viven en dominio/aplicación y la infraestructura es sustituible; además permite test unitario con mocks | Todos |
| D2 | El propietario sale del `AuthenticatedUser` del JWT y las consultas filtran por `id + userId` | Buscar por `id` y comparar el `userId` después, devolviendo 403 si no coincide | La constitución 4 exige 404, nunca 403; así no se revela si un proyecto ajeno existe | RF-002, RF-003, RF-004, RF-021, RF-025 |
| D3 | Límite comprobado en el servicio con `countByUserId` y la propiedad configurable; es **best-effort**, sin bloqueo ni restricción de BD | Restricción `CHECK`/trigger en BD o `SELECT ... FOR UPDATE` con aislamiento serializable | No se puede tocar el esquema (constitución 3); el producto acepta que dos creaciones simultáneas puedan superar el límite y se documenta como caso conocido | RF-012, RF-013, RF-014, RF-015 |
| D4 | El título se guarda **tal cual llega**, sin `trim` ni normalización; se valida con `@NotBlank` y `@Size(max = 255)` | Recortar espacios exteriores antes de validar y guardar | Decisión de la spec: lo que el usuario escribe es lo que se guarda; el recorte se descartó para no alterar el valor | RF-006, RF-007, RF-008 |
| D5 | Los 255 caracteres se miden en **unidades UTF-16** (`String.length()` / `@Size`) | Contar puntos de código Unicode o caracteres de MySQL | Es el comportamiento nativo de Bean Validation; un emoji cuenta 2 y el límite de Java es más estricto que `VARCHAR(255)`, así nunca se desborda la columna | RF-008 |
| D6 | `createdAt` se asigna en el servidor como `LocalDateTime` y la **capa de aplicación** lo formatea como `dd/MM/yyyy` al construir `ProjectResponse` | `@JsonFormat` o configuración global de Jackson; devolver ISO-8601; usar `Instant`/UTC; formatear en el mapper de infraestructura | La spec fija `dd/MM/yyyy` y la zona horaria del servidor; formatear en aplicación evita depender de la configuración de Jackson 3 y no obliga a que `application/` importe el mapper de infraestructura | RF-009, RF-016, RF-017, RF-020 |
| D7 | La cascada de borrado la ejecuta la **base de datos** con `ON DELETE CASCADE`; el caso de uso solo elimina la fila del proyecto | Borrar manualmente test cases, pasos y asociaciones desde el servicio | El esquema ya define las FK; es la fuente de verdad (constitución 3), evita duplicar lógica y garantiza atomicidad. Las etiquetas globales no se tocan | RF-023, RF-024, RF-026 |
| D8 | El listado se ordena en la consulta por `created_at DESC, id DESC` | Ordenar en memoria en el servicio | El resultado es determinista y la BD hace el trabajo; con el límite por defecto de 10 proyectos el coste es despreciable y el índice `idx_projects_user_id` cubre el filtro | RF-016, RF-017, RF-019 |
| D9 | Proyecto inexistente o ajeno → `ProjectNotFoundException` → 404 con mensaje genérico idéntico; límite → `ProjectLimitExceededException` → 409 con el mensaje exacto | 403 para proyectos ajenos; mensajes distintos para inexistente y ajeno; 400 o 422 para el límite | Evita enumeración de proyectos y unifica el contrato de error; el 409 es el código acordado para el límite | RF-004, RF-012, RF-021, RF-025, RF-028 |
| D10 | `GlobalExceptionHandler` se amplía para cubrir tipo de `id` inválido, JSON malformado, método no soportado, `Content-Type` no soportado y error inesperado | Dejar que Spring devuelva sus cuerpos de error por defecto | Constitución 7: todas las respuestas de error deben usar `ApiError`, sin stack traces ni detalles internos | RF-028, RF-029 |
| D11 | DTO de entrada solo con `title`; los campos desconocidos se ignoran y `@Valid` se aplica en el controlador | Rechazar campos desconocidos; aceptar `userId`, `id` o `createdAt` en el cuerpo | La spec dice que solo `title` viene del cuerpo y que el resto se ignora; el propietario y la fecha los pone el servidor | RF-005, RF-006, RF-007, RF-008, RF-011 |
| D12 | `GET /api/projects` devuelve la lista completa de proyectos propios, sin paginación | Paginar el listado o devolver un envoltorio con metadatos | El límite por defecto de 10 hace innecesaria la paginación y la spec la deja fuera de alcance; un array plano mantiene el contrato simple | RF-016, RF-018, RF-019 |
| D13 | Creación y borrado son transaccionales; el recuento y la inserción comparten transacción, y el borrado con la cascada también | Sin transacción o con varias llamadas independientes | La creación debe ser consistente con el límite y el borrado no puede dejar datos a medias; la cascada de BD participa en la misma transacción | RF-012, RF-014, RF-026, RF-027 |
| D14 | El límite se configura en `app.projects.max-per-user` (10 por defecto) mediante `ProjectLimitProperties` en infraestructura, expuesto a la aplicación por el puerto `ProjectLimitProvider` | Constante en código, valor leído ad hoc en el servicio o inyectar la clase de configuración de infraestructura directamente en `ProjectService` | RF-013 exige ajustarlo sin cambiar código; el puerto mantiene la regla hexagonal de que `application/` no importe `infraestructure/` y permite mockear el límite en los tests | RF-013, RF-015 |
| D15 | Tests unitarios con JUnit 5 + Mockito y tests de controlador con MockMvc; la cascada real se verifica manualmente contra MySQL | Tests de integración con Testcontainers o H2 | Decisión del proyecto: no se añaden dependencias ni se levanta BD en los tests; el esquema y las FK se validan con el script y una prueba manual | Todos; RF-023, RF-024, RF-026 manuales |
| D16 | Se reutiliza la seguridad JWT existente: `SecurityConfig` protege todo salvo los endpoints públicos y las consultas se filtran por el usuario del JWT | Crear una configuración de seguridad específica para proyectos | La cadena actual ya deja `/api/projects/**` autenticado y el principal contiene `id`, `username`, `email` y `role`; la aplicación lo obtiene con el puerto `CurrentUserProvider`, sin `@AuthenticationPrincipal` | RF-001, RF-002, RF-003 |
| D17 | Documentación con springdoc en el controlador y el esquema bearer ya existente | Omitir Swagger o documentar solo con anotaciones sueltas | Convención del proyecto y criterio de finalización de la spec | RF-001 (soporte) |

## Modelo de datos

### Base de datos (script existente, sin cambios — constitución 3)

Tabla `projects` ya definida en `esquema_creacion_base_datos.sql`:

| Columna | Tipo | Observaciones |
|---|---|---|
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Clave primaria |
| `user_id` | `BIGINT UNSIGNED NOT NULL` | FK a `users.id` con `ON DELETE CASCADE` y `ON UPDATE CASCADE` |
| `title` | `VARCHAR(255) NOT NULL` | Título visible; sin restricción de unicidad |
| `created_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` | Fecha de creación asignada por el servidor |

Índices y restricciones existentes:

- `pk_projects` sobre `id`.
- `fk_projects_user` sobre `user_id`.
- `idx_projects_user_id` sobre `user_id`.

No se crean tablas, columnas, índices ni restricciones nuevas. El límite de
10 no se representa en el esquema: se calcula por recuento. La cascada de
borrado ya está en las FK de `test_cases`, `test_steps` y
`test_case_tags`.

### Modelo de dominio

- **`Project`** (`domain/model`): `id`, `userId`, `title`, `createdAt`.
  POJO con Lombok, sin anotaciones JPA. `createdAt` es `LocalDateTime` y lo
  asigna el servidor.

### Configuración

- **`ProjectLimitProperties`** (`infraestructure/config`): propiedad
  `maxPerUser` con valor por defecto 10 y validación de que sea mayor que 0.
  Se expone a la aplicación mediante el puerto `ProjectLimitProvider`.

### Relaciones de cascada (sin cambios)

| FK | Elimina al borrar el proyecto | RF |
|---|---|---|
| `test_cases.project_id` → `projects.id` | Los test cases del proyecto | RF-023 |
| `test_steps.test_case_id` → `test_cases.id` | Los pasos de esos test cases | RF-023 |
| `test_case_tags.test_case_id` → `test_cases.id` | Las asociaciones de esos test cases con etiquetas | RF-023 |
| `test_case_tags.tag_id` → `tags.id` | No se activa al borrar un proyecto; las etiquetas globales permanecen | RF-024 |

### DTOs (`application/dto/project`)

| DTO | Contenido | Reglas / formato | RF |
|---|---|---|---|
| `CreateProjectRequest` | `title` | `@NotBlank`, `@Size(max = 255)`; sin recorte; longitud en UTF-16 | RF-005, RF-006, RF-007, RF-008 |
| `ProjectResponse` | `id`, `title`, `createdAt` | `createdAt` como `String` en `dd/MM/yyyy`; no expone `userId` | RF-009, RF-016, RF-020 |

## Módulos y estructura

### Dominio (`domain/`)

| Módulo | Responsabilidad | RF |
|---|---|---|
| `model/Project` | Modelo de proyecto del negocio | RF-005, RF-009 |
| `port/in/CreateProjectUseCase` | Contrato: crear un proyecto | RF-005, RF-012 |
| `port/in/ListProjectsUseCase` | Contrato: listar los proyectos del usuario | RF-016, RF-017, RF-018 |
| `port/in/GetProjectUseCase` | Contrato: obtener un proyecto propio | RF-020, RF-021 |
| `port/in/DeleteProjectUseCase` | Contrato: borrar un proyecto propio | RF-022, RF-025, RF-027 |
| `port/out/ProjectRepository` | Contrato: guardar, buscar por `id + userId`, listar por usuario, contar y borrar | RF-003, RF-012, RF-016, RF-017, RF-022 |
| `port/out/ProjectLimitProvider` | Contrato: devolver el máximo de proyectos por usuario; lo implementa la configuración de infraestructura | RF-013 |
| `exception/ProjectNotFoundException` | Proyecto inexistente o ajeno | RF-004, RF-021, RF-025 |
| `exception/ProjectLimitExceededException` | Límite de proyectos alcanzado | RF-012 |

### Aplicación (`application/`)

| Módulo | Responsabilidad | RF |
|---|---|---|
| `dto/project/CreateProjectRequest`, `ProjectResponse` | Contrato de entrada y salida | RF-005 a RF-009, RF-016, RF-020 |
| `service/ProjectService` | Implementa los cuatro casos de uso: resuelve el propietario, comprueba el límite con `ProjectLimitProvider`, asigna `createdAt`, construye `ProjectResponse` con la fecha formateada y delega el orden y el borrado | RF-002 a RF-027 |

### Infraestructura (`infraestructure/`)

| Módulo | Responsabilidad | RF |
|---|---|---|
| `adapter/in/rest/ProjectController` | `POST /api/projects` (201), `GET /api/projects` (200), `GET /api/projects/{id}` (200), `DELETE /api/projects/{id}` (204); `@Valid`; el usuario actual se obtiene con `CurrentUserProvider` | RF-001, RF-002, RF-005, RF-016, RF-020, RF-022 |
| `adapter/out/persistence/ProjectJpaEntity` | Mapea la tabla `projects` (`id`, `user_id`, `title`, `created_at`); sin lógica de negocio | RF-003, RF-009 |
| `adapter/out/persistence/ProjectJpaRepository` | Spring Data: búsqueda por `id + userId`, listado ordenado, recuento y borrado | RF-003, RF-012, RF-016, RF-017, RF-022 |
| `adapter/out/persistence/ProjectRepositoryAdapter` | Implementa `ProjectRepository` y traduce entre entidad y dominio | RF-003, RF-012, RF-016, RF-017, RF-022 |
| `mapper/ProjectMapper` | MapStruct: `Project` ↔ `ProjectJpaEntity`; no mapea DTOs ni formatea fechas, para no acoplar `application/` a infraestructura | RF-003, RF-009 |
| `config/ProjectLimitProperties` | Propiedad `app.projects.max-per-user`, valor por defecto 10 y validación de que sea mayor que 0 | RF-013, RF-015 |
| `config/ProjectLimitProviderAdapter` | Implementa `ProjectLimitProvider` y devuelve el valor de `ProjectLimitProperties` | RF-013 |
| `exception/GlobalExceptionHandler` | Traduce las excepciones de proyectos y los errores de entrada a `ApiError` con el código correcto | RF-004, RF-012, RF-028, RF-029 |
| `config/OpenApiConfig` | Esquema bearer existente; el controlador se anota con springdoc | RF-001 (soporte) |
| `application.properties` | Añade `app.projects.max-per-user=10` | RF-013 |

### Validaciones de entrada

| Regla | Mecanismo previsto | RF |
|---|---|---|
| Título obligatorio y no vacío | `@NotBlank` sobre `title`; se rechaza nulo, vacío o solo espacios | RF-007 |
| Título de hasta 255 unidades UTF-16 | `@Size(max = 255)` sobre el valor tal cual se recibe | RF-008 |
| Sin recorte | No se llama a `trim` ni `strip`; el valor llega al dominio igual que en el JSON | RF-006 |
| Solo `title` del cuerpo | El DTO solo declara `title`; los campos desconocidos se ignoran | RF-011 |
| `id` de ruta entero | `Long` en el controlador; un valor no convertible lo traduce el handler a 400 | RF-029 |

### Errores y códigos HTTP

| Excepción / condición | HTTP | Cuerpo | RF |
|---|---|---|---|
| `ProjectNotFoundException` | 404 | `ApiError` con mensaje genérico idéntico para inexistente y ajeno | RF-004, RF-021, RF-025 |
| `ProjectLimitExceededException` | 409 | `ApiError` con «No puedes crear más de {límite} proyectos. Elimina uno para crear otro.» | RF-012 |
| `MethodArgumentNotValidException` | 400 | `ApiError` con `fields` del título | RF-007, RF-008 |
| `MethodArgumentTypeMismatchException` | 400 | `ApiError` con mensaje de identificador inválido | RF-029 |
| `HttpMessageNotReadableException` | 400 | `ApiError` con mensaje de cuerpo inválido | RF-028 |
| `HttpRequestMethodNotSupportedException` | 405 | `ApiError` con mensaje de método no soportado | RF-028 |
| `HttpMediaTypeNotSupportedException` | 415 | `ApiError` con mensaje de tipo de contenido no soportado | RF-028 |
| `Exception` no controlada | 500 | `ApiError` genérico, sin clase ni mensaje interno | RF-028 |
| Token ausente, inválido o caducado | 401 | `ApiError` «No autenticado» desde `SecurityConfig` | RF-001 |

Todas las respuestas usan `ApiError` y ninguna incluye stack traces ni
detalles internos.

## Estrategia de tests

Solo tests unitarios y de controlador, según la decisión del proyecto. No
se levanta una base de datos en los tests: la cascada real y el
comportamiento de MySQL se verifican manualmente.

| Test | Qué verifica | RF |
|---|---|---|
| `ProjectServiceTest` — creación dentro del límite | Con 9 proyectos mockeados se guarda el décimo; el `userId` sale del principal, el título se guarda exactamente como se recibió y `ProjectResponse.createdAt` sale en `dd/MM/yyyy` | RF-002, RF-005, RF-006, RF-009 |
| `ProjectServiceTest` — límite | Con un `ProjectLimitProvider` que devuelve 10 y 10 proyectos mockeados, la creación del undécimo lanza `ProjectLimitExceededException` y no se llama a `save` | RF-012, RF-013, RF-014 |
| `ProjectServiceTest` — títulos repetidos | Dos proyectos con el mismo título se aceptan; no hay comprobación de unicidad | RF-010 |
| `ProjectServiceTest` — listado | Devuelve lo que entrega el puerto, respeta el orden y devuelve lista vacía sin proyectos; no mezcla proyectos de otros usuarios | RF-016, RF-017, RF-018, RF-019 |
| `ProjectServiceTest` — detalle | Proyecto propio devuelve el proyecto; inexistente o ajeno lanzan la misma `ProjectNotFoundException` | RF-020, RF-021 |
| `ProjectServiceTest` — borrado | Proyecto propio se borra; inexistente o ajeno lanzan `ProjectNotFoundException` y no se borra nada; tras borrar se puede crear otro | RF-022, RF-025, RF-027 |
| `CreateProjectRequestValidationTest` | Rechaza título nulo, vacío y solo espacios; acepta 255 unidades UTF-16 y rechaza 256; 127 emojis se aceptan y 128 se rechazan; conserva espacios exteriores | RF-006, RF-007, RF-008 |
| `ProjectControllerTest` | 201, 200 listado, 200 detalle y 204 borrado; 401 sin token; 400 con `id` no numérico; 404 para inexistente o ajeno; 409 con el mensaje del límite; `createdAt` en `dd/MM/yyyy` | RF-001, RF-005, RF-009, RF-016, RF-020, RF-022, RF-029 |
| `ProjectMapperTest` | Mapea entidad ↔ dominio; conserva `userId` y `createdAt`; no mapea DTOs ni formatea fechas | RF-003, RF-009 |
| `GlobalExceptionHandlerTest` | Cada excepción produce su código y `ApiError`; el 500 no filtra detalles; el 409 incluye el mensaje del límite | RF-004, RF-012, RF-028, RF-029 |
| `ProjectLimitPropertiesTest` | Valor por defecto 10; valor configurado distinto; valor 0 o negativo inválido | RF-013, RF-015 |
| `ProjectLimitProviderAdapterTest` | El adaptador devuelve el valor de las propiedades y el servicio lo recibe por el puerto | RF-013 |
| Verificación manual — cascada | Borrar en MySQL un proyecto con test cases comprueba que desaparecen el proyecto, sus test cases, pasos y asociaciones, y que las filas de `tags` siguen existiendo | RF-023, RF-024, RF-026 |
| Verificación manual — flujo completo | Crear → listar → detalle → borrar → intentar el undécimo proyecto y recibir 409 con el mensaje | RF-005, RF-012, RF-016, RF-020, RF-022 |

**Cobertura de RF**

| RF | Dónde se cubre |
|---|---|
| RF-001 | `SecurityConfig` existente + `ProjectControllerTest` (401) |
| RF-002 | `SecurityContextCurrentUserProvider` + `ProjectServiceTest` |
| RF-003 | `ProjectRepository` y adaptador (consultas por `id + userId`) + tests de servicio |
| RF-004 | `ProjectNotFoundException` + tests de servicio/controlador/handler |
| RF-005 | `ProjectService` + `ProjectController` + tests |
| RF-006 | Validación sin recorte + `ProjectServiceTest` + `ProjectMapperTest` |
| RF-007 | `@NotBlank` + `CreateProjectRequestValidationTest` |
| RF-008 | `@Size(max = 255)` en UTF-16 + `CreateProjectRequestValidationTest` |
| RF-009 | `ProjectService` (asigna y formatea `createdAt`) + `ProjectControllerTest` |
| RF-010 | `ProjectServiceTest` (títulos repetidos) |
| RF-011 | DTO solo con `title` + `ProjectControllerTest` (campos desconocidos ignorados) |
| RF-012 | `ProjectService` + `ProjectLimitExceededException` + handler/controller tests |
| RF-013 | `ProjectLimitProperties` + `ProjectLimitProviderAdapter` + sus tests + `ProjectServiceTest` |
| RF-014 | `ProjectServiceTest` (best-effort documentado; sin test de concurrencia real) |
| RF-015 | `ProjectLimitPropertiesTest` + verificación manual |
| RF-016 | `ProjectService` + `ProjectControllerTest` |
| RF-017 | Consulta ordenada del repositorio + `ProjectServiceTest` |
| RF-018 | `ProjectServiceTest` (lista vacía) |
| RF-019 | Consulta por `userId` + DTO sin contadores |
| RF-020 | `ProjectService` + `ProjectControllerTest` |
| RF-021 | `ProjectNotFoundException` + tests |
| RF-022 | `ProjectService` + `ProjectControllerTest` |
| RF-023 | Cascada de BD + verificación manual |
| RF-024 | Cascada de BD + verificación manual (tags intactos) |
| RF-025 | `ProjectService` (comprueba propiedad antes de borrar) + tests |
| RF-026 | `@Transactional` + verificación manual |
| RF-027 | `ProjectServiceTest` (borrar libera el hueco) |
| RF-028 | `GlobalExceptionHandler` + `GlobalExceptionHandlerTest` |
| RF-029 | Handler de tipo inválido + `ProjectControllerTest` |

## Puntos abiertos y supuestos

- **Límite configurable vs. 10 fijo:** los tests usan el valor por defecto
  10. Los casos de la spec que dicen “9 → décimo” y “10 → 409” siguen
  asumiendo 10; si el límite se configura distinto, habría que
  parametrizarlos.
- **Espacios Unicode:** `@NotBlank` usa `String.trim()` y no rechaza un
  título compuesto solo por NBSP (`U+00A0`). Si se quieren rechazar todos
  los espacios Unicode, hará falta un validador específico.
- **Sanitización:** no se escapa HTML ni se eliminan caracteres de control;
  el backend guarda el título tal cual. Si se requiere sanitización, es una
  decisión nueva.
- **Precisión del `id`:** se devuelve como número JSON (`Long`). Si el
  frontend usa JavaScript, un `id` mayor que 2^53-1 perdería precisión; no
  se decide serializarlo como string.
- **Trazabilidad del detalle:** `GET /api/projects/{id}` se implementa
  porque está en la spec, aunque el alcance no lo enumere explícitamente.
- **Formato de fecha:** `dd/MM/yyyy` no es ISO-8601 y no es ordenable
  lexicográficamente; el orden se hace en servidor sobre el timestamp. La
  zona horaria queda fijada a la del servidor.
- **Concurrencia:** el límite es best-effort y no se prueba con hilos
  reales; el caso aceptado queda documentado en la spec.
- **Cascada:** al depender de MySQL, no se cubre con tests unitarios; la
  verificación es manual y el script SQL es la fuente de verdad.
