# Plan — Gestión de test cases y pasos

Plan técnico de implementación para `specs/02-test-cases/spec.md`. Define
módulos, modelo de datos, decisiones justificadas y estrategia de tests.
Cada parte indica los RF que cubre. Sin código: las firmas concretas se
resolverán durante la implementación.

El plan respeta la constitución: arquitectura hexagonal estricta, esquema SQL
como única fuente de verdad, autorización por rol en `SecurityFilterChain`,
propiedad comprobada en los casos de uso con 404 para recursos ajenos,
cascadas físicas delegadas a MySQL y validación de entrada en backend.

Las dos dudas técnicas que dejó abiertas la spec quedan cerradas aquí:

- **Mapeo JPA:** las relaciones viven solo en las entidades JPA de
  infraestructura. `TestCaseJpaEntity` se conecta con `TestStepJpaEntity` y
  con `TestCaseTagJpaEntity`, y esta última se conecta con `TagJpaEntity`. El
  dominio sigue siendo POJOs sin anotaciones JPA y el adaptador compone el
  modelo.
- **Reemplazo de pasos:** se vacían las colecciones de pasos y asociaciones
  de la entidad gestionada, se hace `flush()` para que Hibernate ejecute los
  `DELETE` y después se añaden las filas nuevas con `position` 1..N, dentro
  de la misma transacción, para respetar `UNIQUE (test_case_id, position)`.

## 1. Decisiones técnicas

| # | Decisión | Alternativa descartada | Por qué | RF |
|---|---|---|---|---|
| D1 | Arquitectura hexagonal: `TestCaseController` → casos de uso → `TestCaseService` → puertos `TestCaseRepository` y `TagRepository` → adaptadores JPA | Controlador usando directamente repositorios Spring Data | Constitución 2 y 5: las reglas viven en dominio/aplicación y la infraestructura es sustituible; permite tests unitarios con mocks | Todos |
| D2 | El usuario sale de `CurrentUserProvider`; el proyecto se busca por `id + userId` y el test case por `id + projectId`. Cualquier recurso ajeno o inexistente responde 404 | Buscar por id y comparar propietario después, devolviendo 403 si no coincide | Constitución 4 y RF-003/RF-004: no se revela la existencia de recursos ajenos; tampoco hay privilegios especiales para `ADMIN` | RF-001 a RF-006, RF-018, RF-027, RF-030, RF-041, RF-047 |
| D3 | Relaciones JPA solo en infraestructura: `TestCaseJpaEntity` tiene `@OneToMany` hacia `TestStepJpaEntity` y hacia `TestCaseTagJpaEntity`; `TestCaseTagJpaEntity` tiene `@ManyToOne` hacia el test case y hacia `TagJpaEntity`. El dominio no tiene anotaciones ni relaciones JPA; sus vínculos son de negocio y el adaptador compone el modelo | Conectar las entidades de dominio directamente, dejar las tablas sin relación en el modelo JPA o usar un `@ManyToMany` directo entre test case y tag | Las relaciones son un detalle de persistencia y deben vivir en infraestructura; así el dominio queda puro y el adaptador controla el *flush*. La tabla de unión explícita permite controlar huérfanos y cargar etiquetas en lote sin `join fetch` con paginación | RF-021 a RF-026, RF-038, RF-039, RF-046, RF-048, RF-050, RF-056 |
| D4 | Reemplazo de pasos y asociaciones vaciando las colecciones de `TestCaseJpaEntity` y haciendo `flush()` antes de añadir las filas nuevas con `position` 1..N | Borrado masivo por consulta, actualizar posiciones una a una o renumerar temporalmente a posiciones negativas | Con relaciones JPA, `clear()` + `flush()` obliga a Hibernate a ejecutar los `DELETE` de huérfanos antes de los `INSERT`; así se respeta `UNIQUE (test_case_id, position)` sin renunciar a las relaciones en infraestructura. Todo ocurre en la misma transacción | RF-038, RF-042, RF-050, RF-052, RF-053 |
| D5 | La normalización de etiquetas vive en un servicio de dominio `TagNormalizer`, puro y sin dependencias de infraestructura: recorte Unicode, minúsculas con `Locale.ROOT`, NFC, NFD sin marcas `Mn` salvo U+0303 tras `n`, y NFC final | Normalizar en el controlador, en el adaptador o en MySQL; usar `toLowerCase()` sin `Locale.ROOT`; eliminar todos los diacríticos | Constitución 5 y RF-058 a RF-066: la regla es negocio, debe ser testeable y conservar la `ñ`. La base de datos solo almacena el resultado | RF-015, RF-016, RF-058 a RF-066 |
| D6 | `TagResolver` en aplicación: deduplica por nombre normalizado, consulta las existentes en lote, crea las nuevas con `saveAllAndFlush` y traduce `DataIntegrityViolationException` a `TagConflictException` (409) | Hacer *upsert* con `INSERT ... ON DUPLICATE KEY`, reintentar y releer, o comprobar y luego insertar sin apoyo de la restricción única | RF-019, RF-039 y RF-066: la concurrencia se resuelve con `UNIQUE(normalized_name)`; una transacción gana y la otra recibe 409 sin duplicar, reintentar ni releer | RF-015, RF-016, RF-019, RF-039, RF-058 a RF-066 |
| D7 | Las etiquetas son globales: se reemplazan solo las filas de `test_case_tags` a través de `TestCaseTagJpaEntity`; `TagJpaEntity` no se borra | Borrar etiquetas huérfanas al editar o borrar un test case, o poner `CascadeType.REMOVE` de test case a tag | RF-065: las etiquetas se comparten entre proyectos y usuarios; borrar un test case solo elimina sus asociaciones | RF-039, RF-046, RF-056, RF-065 |
| D8 | Paginación con un `PageResult<T>` propio en dominio y un método de puerto `findPageByProjectId(projectId, page, size)`. El adaptador usa `PageRequest` y `Page` de Spring Data; la aplicación no los ve | Exponer `Page`/`Pageable` de Spring Data en el puerto, paginar en memoria o devolver una lista completa | Mantiene `domain/` y `application/` libres de Spring Data, permite tests unitarios sencillos y conserva el orden y los totales en el adaptador | RF-021 a RF-026 |
| D9 | El tamaño de página es una constante de aplicación de 15; no hay propiedad configurable ni parámetro `size` | Añadir `app.test-cases.page-size` o aceptar `size` en la petición | La spec fija 15 y prohíbe `size`; un valor configurable permitiría desviarse del contrato | RF-021, RF-025, RF-026 |
| D10 | `page` se valida al inicio del caso de uso de listado; si es negativa se lanza `InvalidPageException` (400 con campo `page`). Un `page` no numérico lo traduce el handler de tipo | `@Min(0)` sobre `@RequestParam` con `@Validated` | Evita depender del mecanismo de validación de parámetros de método, que cambia entre versiones de Spring, y garantiza que el 400 se produce antes de comprobar la propiedad del proyecto | RF-025, RF-074, RF-075, RF-077 |
| D11 | `priority` y `status` se reciben como `String` con `@NotBlank` y `@Pattern` sensible a mayúsculas; el servicio los convierte a los enums `Priority` y `Status` | Declarar los DTOs con enums y dejar que Jackson falle al deserializar | Con enum, un valor inválido produce un 400 genérico de cuerpo mal formado y no un error en `priority`/`status`; con `@Pattern` se cumple RF-075 y se conserva la sensibilidad a mayúsculas | RF-012, RF-035, RF-067, RF-068, RF-072, RF-075, RF-078 |
| D12 | La regla «`COMPLETED` exige `expectedResult` no vacío» se implementa como restricción de clase `@CompletedRequiresExpectedResult` sobre `UpdateTestCaseRequest`, que añade el error en `expectedResult` | Comprobarlo en el servicio después de resolver el proyecto, o poner `@NotBlank` siempre en `expectedResult` | Al ejecutarse con `@Valid`, el 400 prevalece sobre el 404 de un recurso ajeno (RF-076); `@NotBlank` permanente rechazaría un `IN_PROGRESS` válido con `expectedResult` nulo | RF-036, RF-041, RF-070, RF-076 |
| D13 | `description` y `expectedResult` vacíos o solo espacios según `trim()` de Java se guardan como `null`; `title` e `instruction` se guardan tal cual. La comprobación se hace en aplicación con un helper de texto | Usar `strip()`/Unicode para todos los campos, recortar siempre, o guardar cadenas vacías | RF-009 a RF-011 y RF-049: `@NotBlank` usa `trim()` de Java; el título y las instrucciones conservan espacios y saltos interiores; NBSP no se considera blanco en estos campos | RF-009, RF-010, RF-011, RF-049 |
| D14 | `createdAt` se asigna con `LocalDateTime.now()` al crear y es inmutable; `TestCaseDtoMapper` lo formatea como `dd/MM/yyyy`. No se expone `updatedAt` ni `userId` | Formatear con `@JsonFormat`, devolver ISO-8601, usar `Instant`/UTC o añadir `updatedAt` | La spec fija `dd/MM/yyyy` y la zona del servidor; el esquema no tiene `updatedAt`; formatear en aplicación evita acoplar el dominio a Jackson | RF-007, RF-031, RF-040, RF-044 |
| D15 | El borrado del test case solo elimina la fila de `test_cases`, sin cargar las colecciones; MySQL elimina pasos y asociaciones por `ON DELETE CASCADE`. El caso de uso no borra hijos | Borrar pasos y asociaciones manualmente antes del test case | Constitución 5 y RF-046/RF-048: las FK ya definen la cascada; duplicarla en la aplicación sería redundante y frágil | RF-045 a RF-048, RF-056 |
| D16 | Errores con excepciones de dominio y `GlobalExceptionHandler`: `TestCaseNotFoundException` (404), `TagConflictException` (409), `InvalidTagNameException` (400 en `tags[i]`), `InvalidPageException` (400 en `page`) e `InvalidTestCaseStateException` (400 en `expectedResult`) | Devolver 500, 403 o 400 genéricos; mezclar mensajes de recursos ajenos e inexistentes | Constitución 7 y RF-073 a RF-084: respuestas `ApiError` estructuradas, mensajes genéricos por tipo y sin detalles internos | RF-073 a RF-084 |
| D17 | Se reutiliza `SecurityConfig`: `/api/projects/**` ya exige `hasAnyRole("USER", "ADMIN")`. El controlador no recibe `@AuthenticationPrincipal`; el servicio obtiene el usuario con `CurrentUserProvider` | Crear una cadena de seguridad específica o inyectar el principal en el controlador | Constitución 4 y RF-001/RF-005: la autorización por rol está centralizada y la propiedad se comprueba en el caso de uso | RF-001, RF-002, RF-005 |
| D18 | Los DTOs solo declaran campos del contrato; los campos desconocidos (`status` en creación, `id`, `projectId`, `createdAt`, `position`) se ignoran con el comportamiento por defecto de Jackson | Rechazar campos desconocidos con `FAIL_ON_UNKNOWN_PROPERTIES` | RF-008, RF-020, RF-040 y RF-078: el servidor resuelve esos valores y el cliente no puede imponerlos | RF-008, RF-020, RF-040, RF-078 |
| D19 | La aplicación ordena los pasos por `position` y las etiquetas por `name` antes de mapear; el adaptador no delega el contrato de orden a la BD | Confiar solo en el `ORDER BY` de las consultas o ordenar en el controlador | El orden forma parte del contrato (RF-026, RF-029, RF-051, RF-063) y así se prueba de forma determinista aunque cambie la consulta | RF-026, RF-029, RF-051, RF-063 |
| D20 | Tests unitarios con JUnit 5 + Mockito, validación con `LocalValidatorFactoryBean` y controlador con MockMvc; los adaptadores se prueban con repositorios mockeados. La cascada real, la unicidad de `position` y el 409 concurrente se verifican manualmente contra MySQL | Testcontainers, H2 o tests de integración con BD | Decisión del proyecto (plan 01): no se añaden dependencias ni se levanta BD en los tests; el esquema es la fuente de verdad y se valida manualmente | Todos; RF-046, RF-048, RF-050, RF-066 manuales |
| D21 | springdoc en el controlador, con `@Operation`, `@ApiResponses`, `@SecurityRequirement(name = "bearerAuth")` y `@Tag`; se reutiliza el esquema bearer de `OpenApiConfig` | Omitir Swagger o documentar solo con anotaciones sueltas | La spec exige los cinco endpoints en Swagger con seguridad bearer y esquemas de petición/respuesta | RF-001 (soporte) |

## 2. Modelo de datos

### 2.1 Base de datos (sin cambios — constitución 3)

El esquema de `docs/esquema_creacion_base_datos.sql` es la única fuente de
verdad. No se crean tablas, columnas, índices, triggers ni restricciones.

| Tabla | Columnas relevantes | Restricciones / relaciones | RF |
|---|---|---|---|
| `test_cases` | `id`, `project_id`, `title`, `description`, `expected_result`, `priority`, `status`, `created_at` | PK `id`; FK `project_id` → `projects.id` con `ON DELETE CASCADE`; `CHECK` de `priority` y `status`; índices por proyecto, estado y prioridad | RF-007 a RF-048, RF-067 a RF-072 |
| `test_steps` | `id`, `test_case_id`, `instruction`, `position` | PK `id`; FK `test_case_id` → `test_cases.id` con `ON DELETE CASCADE`; `UNIQUE (test_case_id, position)`; índice por `test_case_id` | RF-013, RF-014, RF-038, RF-049 a RF-057 |
| `tags` | `id`, `name`, `normalized_name` | PK `id`; `UNIQUE (normalized_name)` con colación binaria | RF-015, RF-016, RF-058 a RF-066 |
| `test_case_tags` | `test_case_id`, `tag_id` | PK compuesta; FK a `test_cases` y `tags` con `ON DELETE CASCADE`; índice por `tag_id` | RF-039, RF-046, RF-056, RF-065 |

### 2.2 Modelo de dominio

| Modelo | Campos | Invariantes / responsabilidad | RF |
|---|---|---|---|
| `TestCase` | `id`, `projectId`, `title`, `description`, `expectedResult`, `priority`, `status`, `createdAt`, `steps`, `tags` | POJO sin anotaciones JPA. `projectId` y `createdAt` inmutables tras crear. En creación `status` es `IN_PROGRESS`. `steps` y `tags` nunca son `null`; las listas vacías se representan con `List.of()` | RF-007 a RF-048, RF-067 a RF-072 |
| `TestStep` | `id`, `testCaseId`, `instruction`, `position` | Entidad hija. `position` empieza en 1 y no tiene huecos. El `id` no se expone en la API | RF-013, RF-014, RF-049 a RF-057 |
| `Tag` | `id`, `name`, `normalizedName` | Etiqueta global. `name` es el nombre visible persistido; `normalizedName` es la clave de reutilización | RF-058 a RF-066 |
| `TagName` | `visibleName`, `normalizedName` | Resultado puro de `TagNormalizer`; transporta el nombre visible tras el recorte y el normalizado tras los cuatro pasos | RF-058 a RF-062 |
| `Priority` | `HIGH`, `MEDIUM`, `LOW` | Enum de dominio; se serializa en mayúsculas | RF-012, RF-067, RF-072 |
| `Status` | `IN_PROGRESS`, `COMPLETED` | Enum de dominio; se serializa en mayúsculas | RF-035, RF-068, RF-072 |
| `PageResult<T>` | `content`, `page`, `size`, `totalElements`, `totalPages` | Resultado de paginación independiente de Spring Data; `totalPages` es 0 si no hay elementos | RF-021 a RF-026 |

### 2.3 DTOs (`application/dto/testcase`)

| DTO | Contenido | Reglas / formato | RF |
|---|---|---|---|
| `StepRequest` | `instruction` | `@NotBlank`, `@Size(max = 255)`; se guarda tal cual | RF-013, RF-049 |
| `CreateTestCaseRequest` | `title`, `description`, `expectedResult`, `priority`, `steps`, `tags` | `title` `@NotBlank` + `@Size(255)`; `description`/`expectedResult` `@Size(255)`; `priority` `@NotBlank` + `@Pattern(HIGH\|MEDIUM\|LOW)`; `steps` `@NotEmpty` + `List<@Valid StepRequest>`; `tags` opcional + `List<@NotBlank @Size(max = 50) String>` | RF-007 a RF-020, RF-049, RF-058 a RF-062 |
| `UpdateTestCaseRequest` | Igual que creación más `status` | `status` `@NotBlank` + `@Pattern(IN_PROGRESS\|COMPLETED)`; restricción de clase `@CompletedRequiresExpectedResult` | RF-032 a RF-044, RF-067 a RF-072 |
| `TestCaseSummaryResponse` | `id`, `projectId`, `title`, `description`, `priority`, `status`, `tags`, `createdAt` | Sin `expectedResult` ni `steps`; `tags` como `[{id, name}]` ordenadas por `name`; `createdAt` en `dd/MM/yyyy`; no expone `userId` | RF-021 a RF-027 |
| `TestCaseDetailResponse` | Todos los campos del resumen más `expectedResult` y `steps` | `steps` como `[{position, instruction}]` ordenados por `position`; listas vacías como `[]`; opcionales sin valor como `null` | RF-028 a RF-031, RF-044 |
| `TagResponse` | `id`, `name` | Identificador global y nombre visible | RF-026, RF-063 |
| `TestCaseStepResponse` | `position`, `instruction` | Sin `id` ni `testCaseId` | RF-029, RF-051 |
| `TestCasePageResponse` | `testCases`, `page`, `size`, `totalElements`, `totalPages` | `size` siempre 15; `page` es el solicitado; `testCases` vacío como `[]` | RF-021 a RF-026 |

Los DTOs de creación y edición se mantienen separados, sin herencia, para que
las rutas de Bean Validation (`steps[i].instruction`, `tags[i]`,
`expectedResult`) sean claras y estables.

### 2.4 Persistencia JPA

| Elemento | Responsabilidad | RF |
|---|---|---|
| `TestCaseJpaEntity` | Mapea `test_cases`; `priority` y `status` con `@Enumerated(EnumType.STRING)`; `@OneToMany(fetch = LAZY, orphanRemoval = true)` hacia `TestStepJpaEntity` y `TestCaseTagJpaEntity`, sin `CascadeType.REMOVE` para que el borrado del padre lo ejecute MySQL | RF-003, RF-007, RF-021 a RF-031, RF-046 |
| `TestStepJpaEntity` | Mapea `test_steps`; `@ManyToOne(fetch = LAZY)` hacia `TestCaseJpaEntity`; `instruction` y `position` como columnas | RF-013, RF-014, RF-038, RF-049 a RF-057 |
| `TagJpaEntity` | Mapea `tags`; `name`, `normalizedName`; no se borra en cascada desde el test case | RF-058 a RF-066 |
| `TestCaseTagJpaEntity` + `TestCaseTagId` | Conecta test case y tag; PK compuesta con `@EmbeddedId` + `@MapsId`; `@ManyToOne(fetch = LAZY)` hacia `TestCaseJpaEntity` y `TagJpaEntity`; se elimina como huérfano al vaciar la colección del test case | RF-039, RF-046, RF-065 |
| `TestCaseJpaRepository` | `findByIdAndProjectId`, `findByProjectId(Pageable)`, `deleteById`, `flush` heredado | RF-003, RF-004, RF-021 a RF-026, RF-045 a RF-048 |
| `TestStepJpaRepository` | Consulta de pasos por test case ordenados por `position` | RF-029, RF-038, RF-051 |
| `TagJpaRepository` | `findByNormalizedNameIn`, `saveAllAndFlush` | RF-058 a RF-066 |
| `TestCaseTagJpaRepository` | Proyección `TestCaseTagRow(testCaseId, tagId, name)` que recorre `TestCaseTagJpaEntity` y su `tag` para cargar las etiquetas de una página en una consulta | RF-026, RF-039, RF-063 |
| `TestCaseMapper`, `TestStepMapper`, `TagMapper` | MapStruct entre entidades y dominio; no formatean fechas ni construyen DTOs | RF-003, RF-009, RF-013, RF-063 |
| `TestCaseRepositoryAdapter` | Implementa `TestCaseRepository`: compone dominio, pagina, carga pasos/etiquetas, crea y reemplaza hijos vaciando colecciones + `flush`, borra solo el test case | RF-021 a RF-048 |
| `TagRepositoryAdapter` | Implementa `TagRepository`: busca por nombres normalizados y crea etiquetas; traduce el conflicto de unicidad | RF-058 a RF-066 |

**Orden de escritura en creación:** guardar el test case con sus colecciones
de pasos y asociaciones; Hibernate inserta `test_cases`, después
`test_steps` con `position` 1..N y después `test_case_tags`. Todo en la misma
transacción. En las relaciones bidireccionales, el adaptador mantiene ambos
lados: añade el hijo a la colección y fija la referencia `@ManyToOne` del
lado propietario.

**Orden de escritura en edición:** actualizar la fila de `test_cases`;
vaciar las colecciones `steps` y `tagLinks` de la entidad gestionada;
`flush()` para que Hibernate ejecute los `DELETE` de huérfanos; añadir los
pasos y asociaciones nuevos; `flush()`/commit para los `INSERT`. El `flush()`
intermedio es obligatorio para que `UNIQUE (test_case_id, position)` nunca vea
posiciones duplicadas. `createdAt` y `projectId` no se tocan.

**Lectura del listado:** una consulta paginada de `test_cases` y una sola
consulta por lote de `test_case_tags` para todos los ids de la página. El
mapper de resumen solo lee campos escalares y las etiquetas de la proyección;
no toca las colecciones `steps` ni `tagLinks`. Así se evita el N+1 y no se
cargan pasos ni `expected_result`.

**Lectura del detalle:** el test case se carga por `id + projectId` y el
adaptador accede a sus colecciones de pasos y asociaciones dentro de la
transacción. Se compone el dominio y se mapea al DTO de detalle; no se
exponen entidades JPA.

### 2.5 Paginación

- `TestCaseService` define `PAGE_SIZE = 15`.
- `TestCaseRepository.findPageByProjectId(projectId, page, PAGE_SIZE)`
  devuelve `PageResult<TestCase>`.
- El adaptador construye `PageRequest.of(page, size, Sort.by(desc("createdAt"),
  desc("id")))`; el orden se calcula sobre el timestamp, no sobre la fecha
  formateada.
- `PageResult.totalPages` sale de Spring Data: 0 si no hay elementos; en caso
  contrario, el número de páginas de 15 necesario.
- Una página posterior a la última devuelve `content` vacío y conserva
  `page`, `totalElements` y `totalPages`.
- No hay parámetro `size`, filtros, búsqueda ni orden configurable.

## 3. Módulos y estructura

### 3.1 Dominio (`domain/`)

| Módulo | Responsabilidad | RF |
|---|---|---|
| `model/TestCase` | Modelo del test case y sus listas de pasos y etiquetas | RF-007 a RF-048, RF-067 a RF-072 |
| `model/TestStep` | Paso ordenado del test case | RF-013, RF-014, RF-049 a RF-057 |
| `model/Tag`, `model/TagName` | Etiqueta global y resultado de normalización | RF-058 a RF-066 |
| `model/Priority`, `model/Status` | Enums de prioridad y estado | RF-012, RF-035, RF-067, RF-068, RF-072 |
| `model/PageResult` | Resultado de paginación independiente de Spring Data | RF-021 a RF-026 |
| `port/in/CreateTestCaseUseCase` | Contrato: crear un test case en un proyecto propio | RF-007 a RF-020 |
| `port/in/ListTestCasesUseCase` | Contrato: listar paginado los test cases de un proyecto propio | RF-021 a RF-027 |
| `port/in/GetTestCaseUseCase` | Contrato: obtener el detalle de un test case propio | RF-028 a RF-031 |
| `port/in/UpdateTestCaseUseCase` | Contrato: reemplazar un test case propio | RF-032 a RF-044 |
| `port/in/DeleteTestCaseUseCase` | Contrato: borrar un test case propio | RF-045 a RF-048 |
| `port/out/TestCaseRepository` | Guardar, buscar por `id + projectId`, paginar por proyecto, borrar y reemplazar hijos | RF-003, RF-004, RF-021 a RF-048 |
| `port/out/TagRepository` | Buscar etiquetas por nombre normalizado y crear las nuevas | RF-058 a RF-066 |
| `service/TagNormalizer` | Algoritmo puro de recorte Unicode, minúsculas, NFC, NFD y conservación de la `ñ` | RF-015, RF-016, RF-058 a RF-066 |
| `exception/TestCaseNotFoundException` | Test case inexistente o ajeno | RF-004, RF-030, RF-041, RF-047 |
| `exception/TagConflictException` | Conflicto de unicidad al crear una etiqueta | RF-019, RF-039, RF-066 |
| `exception/InvalidTagNameException` | Etiqueta vacía tras el recorte o normalizado de más de 50 unidades | RF-015, RF-060, RF-061 |
| `exception/InvalidPageException` | `page` negativa | RF-025, RF-074, RF-075 |
| `exception/InvalidTestCaseStateException` | `COMPLETED` sin `expectedResult` no vacío, como defensa de dominio | RF-036, RF-070 |

### 3.2 Aplicación (`application/`)

| Módulo | Responsabilidad | RF |
|---|---|---|
| `dto/testcase/CreateTestCaseRequest`, `UpdateTestCaseRequest`, `StepRequest` | Contrato de entrada y validaciones declarativas | RF-007 a RF-020, RF-032 a RF-044, RF-049, RF-058 a RF-062 |
| `dto/testcase/TestCaseSummaryResponse`, `TestCaseDetailResponse`, `TagResponse`, `TestCaseStepResponse`, `TestCasePageResponse` | Contrato de salida | RF-021 a RF-031, RF-044, RF-063 |
| `validation/CompletedRequiresExpectedResult` y su validador | Regla cruzada de `COMPLETED` + `expectedResult` | RF-036, RF-041, RF-070, RF-076 |
| `mapper/TestCaseDtoMapper` | MapStruct de dominio a DTOs; formatea `createdAt` como `dd/MM/yyyy`; conserva listas vacías | RF-021 a RF-031, RF-044 |
| `service/TagResolver` | Deduplica, busca, reutiliza y crea etiquetas; aplica el 409 de concurrencia | RF-015, RF-016, RF-019, RF-039, RF-058 a RF-066 |
| `service/TestCaseService` | Implementa los cinco casos de uso: usuario actual, propiedad, validación de página, estado, posiciones, reemplazo, paginación y mapeo | RF-001 a RF-084 |

`TagResolver` se separa de `TestCaseService` porque la normalización,
deduplicación, reutilización y concurrencia de etiquetas son una
responsabilidad propia y se prueban mejor de forma aislada. La creación y el
reemplazo de pasos quedan en `TestCaseService` y en el adaptador de
persistencia.

### 3.3 Infraestructura (`infraestructure/`)

| Módulo | Responsabilidad | RF |
|---|---|---|
| `adapter/in/rest/TestCaseController` | Cinco endpoints anidados bajo `/api/projects/{projectId}/test-cases`; `@Valid`; `page` por defecto 0; sin `@AuthenticationPrincipal`; anotaciones springdoc | RF-001, RF-007, RF-021, RF-028, RF-032, RF-045 |
| `adapter/out/persistence/TestCaseJpaEntity`, `TestStepJpaEntity`, `TagJpaEntity`, `TestCaseTagJpaEntity`, `TestCaseTagId` | Mapeo de las cuatro tablas; las relaciones JPA viven solo aquí: test case → pasos, test case → asociaciones y asociación → tag | RF-003, RF-004, RF-021 a RF-048, RF-058 a RF-066 |
| `adapter/out/persistence/TestCaseJpaRepository`, `TestStepJpaRepository`, `TagJpaRepository`, `TestCaseTagJpaRepository` | Consultas, paginación, búsqueda por propietario, proyección de etiquetas y `flush` para el reemplazo de hijos | RF-003, RF-004, RF-021 a RF-048, RF-058 a RF-066 |
| `adapter/out/persistence/TestCaseRepositoryAdapter`, `TagRepositoryAdapter` | Implementan los puertos y traducen entidades ↔ dominio; el de test cases reemplaza hijos vaciando colecciones + `flush`; el de etiquetas traduce el conflicto de unicidad | RF-003, RF-004, RF-019, RF-021 a RF-066 |
| `mapper/TestCaseMapper`, `TestStepMapper`, `TagMapper` | MapStruct entidad ↔ dominio | RF-003, RF-009, RF-013, RF-063 |
| `exception/GlobalExceptionHandler` | Traduce las nuevas excepciones y los errores de validación a `ApiError` | RF-073 a RF-084 |
| `config/SecurityConfig` | Sin cambios: `/api/projects/**` ya exige `USER` o `ADMIN` | RF-001, RF-002, RF-005 |
| `config/OpenApiConfig` | Sin cambios: reutiliza el esquema `bearerAuth` | RF-001 (soporte) |
| `application.properties` | Sin propiedades nuevas: el tamaño de página es una constante de la spec | RF-021, RF-025 |

### 3.4 Flujos por caso de uso

**Crear** (`POST /api/projects/{projectId}/test-cases`)

1. `@Valid` valida el DTO; los errores de `title`, `priority`, `steps` y
   `tags` responden 400 antes del caso de uso.
2. `TagNormalizer` recorta y normaliza las etiquetas; si alguna queda vacía o
   su normalizado supera 50 unidades, 400 en `tags[i]`.
3. El servicio obtiene el usuario actual y busca el proyecto por
   `projectId + userId`; si no existe o es ajeno, 404 «Proyecto no
   encontrado».
4. `TagResolver` reutiliza las etiquetas existentes y crea las nuevas; si una
   inserción choca con `UNIQUE(normalized_name)`, 409 «Conflicto al crear la
   etiqueta» y no se guarda nada.
5. El servicio asigna `status = IN_PROGRESS`, `createdAt` del servidor,
   `position` 1..N y convierte `description`/`expectedResult` vacíos a `null`.
6. El repositorio guarda el test case con sus colecciones; Hibernate inserta
   test case, pasos y asociaciones en una transacción.
7. Se devuelve 201 con `TestCaseDetailResponse`.

RF: RF-007 a RF-020, RF-049, RF-058 a RF-062.

**Listar** (`GET /api/projects/{projectId}/test-cases?page=0`)

1. El servicio valida `page >= 0`; si no, 400 en `page`.
2. Obtiene el usuario actual y el proyecto por `projectId + userId`; si no
   existe o es ajeno, 404 «Proyecto no encontrado».
3. Pide al repositorio la página de 15, ordenada por `createdAt DESC,
   id DESC`.
4. Carga las etiquetas de todos los test cases de la página en una sola
   consulta y las agrupa.
5. Mapea a `TestCaseSummaryResponse`, ordena las etiquetas por `name` y
   construye el envoltorio con `page`, `size: 15`, `totalElements` y
   `totalPages`.
6. Devuelve 200.

RF: RF-021 a RF-027.

**Detalle** (`GET /api/projects/{projectId}/test-cases/{testCaseId}`)

1. Usuario actual y proyecto por `projectId + userId`; si no, 404 «Proyecto
   no encontrado».
2. Test case por `testCaseId + projectId`; si no, 404 «Test case no
   encontrado».
3. Carga pasos ordenados por `position` y etiquetas ordenadas por `name`.
4. Devuelve 200 con `TestCaseDetailResponse`, `createdAt` en `dd/MM/yyyy` y
   sin `userId` ni `updatedAt`.

RF: RF-028 a RF-031.

**Editar** (`PUT /api/projects/{projectId}/test-cases/{testCaseId}`)

1. `@Valid` valida el DTO, incluida la regla `COMPLETED` +
   `expectedResult`; los errores responden 400 antes del caso de uso.
2. `TagNormalizer` valida y normaliza las etiquetas; un nombre inválido
   responde 400 en `tags[i]`.
3. Usuario actual y proyecto por `projectId + userId`; si no, 404 «Proyecto
   no encontrado».
4. Test case por `testCaseId + projectId`; si no, 404 «Test case no
   encontrado».
5. `TagResolver` reutiliza o crea etiquetas; un conflicto responde 409 y no
   modifica nada.
6. El servicio reemplaza título, descripción, resultado esperado, prioridad,
   estado, pasos y etiquetas; asigna posiciones 1..N; conserva `createdAt` y
   `projectId`.
7. El repositorio actualiza la fila, vacía las colecciones de pasos y
   asociaciones, hace `flush()` para ejecutar los `DELETE` y añade las filas
   nuevas; todo en la misma transacción.
8. Devuelve 200 con `TestCaseDetailResponse`.

RF: RF-032 a RF-044, RF-049 a RF-053, RF-058 a RF-063.

**Borrar** (`DELETE /api/projects/{projectId}/test-cases/{testCaseId}`)

1. Usuario actual y proyecto por `projectId + userId`; si no, 404 «Proyecto
   no encontrado».
2. Test case por `testCaseId + projectId`; si no, 404 «Test case no
   encontrado».
3. El repositorio elimina solo la fila de `test_cases` dentro de una
   transacción, sin cargar las colecciones; MySQL elimina pasos y
   asociaciones por las FK `ON DELETE CASCADE`.
4. Devuelve 204 sin cuerpo. Las etiquetas globales permanecen. Un segundo
   borrado responde 404.

RF: RF-045 a RF-048, RF-056.

## 4. Validaciones de entrada

| Regla | Mecanismo previsto | RF |
|---|---|---|
| `title` obligatorio, no vacío, máximo 255 unidades UTF-16; se guarda tal cual | `@NotBlank` + `@Size(max = 255)`; sin `trim` al persistir | RF-009 |
| `description` opcional, máximo 255; vacío o solo espacios `trim()` → `null` | `@Size(max = 255)`; normalización en aplicación | RF-010 |
| `expectedResult` opcional, máximo 255; vacío o solo espacios `trim()` → `null`; obligatorio con `COMPLETED` | `@Size(max = 255)` + `@CompletedRequiresExpectedResult` en edición; normalización en aplicación | RF-011, RF-036 |
| `priority` obligatoria y solo `HIGH`, `MEDIUM`, `LOW` | `@NotBlank` + `@Pattern` sensible a mayúsculas | RF-012, RF-067 |
| `status` obligatorio en edición y solo `IN_PROGRESS`, `COMPLETED` | `@NotBlank` + `@Pattern` sensible a mayúsculas | RF-035, RF-068 |
| `steps` obligatorio y no vacío | `@NotEmpty` sobre la lista | RF-013, RF-052 |
| `instruction` obligatoria, no vacía, máximo 255; se guarda tal cual | `@Valid` anidado + `@NotBlank` + `@Size(max = 255)` | RF-013, RF-049 |
| `position` la asigna el servidor, 1..N según el array; el cliente no la fija | Asignación en aplicación; `position` no está en `StepRequest` | RF-014, RF-050, RF-053 |
| `tags` opcional; cada nombre `@NotBlank` y `@Size(max = 50)` sobre el valor recibido | Restricciones de contenedor sobre `List<String>` | RF-015, RF-060, RF-061 |
| Recorte Unicode y normalización de etiquetas; normalizado no vacío y máximo 50 | `TagNormalizer`; si falla, 400 en `tags[i]` | RF-015, RF-058 a RF-061 |
| Deduplicación de etiquetas por nombre normalizado | `TagResolver`; conserva el primer visible para una nueva y el persistido para una existente | RF-016, RF-062 |
| `page` base 0, por defecto 0; negativa o no numérica → 400 en `page` | Validación en servicio + handler de tipo | RF-025, RF-074, RF-075, RF-077 |
| `projectId` y `testCaseId` enteros; no numérico → 400; 0 o negativo → 404 con cuerpo válido | `Long` en el controlador + handler de tipo; búsqueda por propietario | RF-077 |
| Campos desconocidos ignorados; `status` en creación, `id`, `projectId`, `createdAt` y `position` no se persisten | DTOs sin esos campos; Jackson ignora desconocidos | RF-008, RF-020, RF-040, RF-078 |
| `POST` y `PUT` con `Content-Type: application/json` | Configuración de Spring + handler 415 | RF-081 |
| JSON mal formado o no deserializable → 400 | `HttpMessageNotReadableException` | RF-082 |
| Método no soportado → 405; `Accept` no aceptable → 406 | Handlers existentes | RF-074, RF-083 |
| La validación 400 prevalece sobre el 404 cuando el cuerpo es inválido | `@Valid` y la validación pura de etiquetas/página se ejecutan antes de resolver el recurso | RF-018, RF-041, RF-076 |

## 5. Errores y códigos HTTP

| Excepción / condición | HTTP | Cuerpo | RF |
|---|---|---|---|
| `MethodArgumentNotValidException` | 400 | `ApiError("Datos de entrada inválidos", fields)` con `title`, `description`, `expectedResult`, `priority`, `status`, `steps`, `steps[i].instruction`, `tags` o `tags[i]` | RF-073, RF-075, RF-076 |
| `InvalidPageException` | 400 | `ApiError` con `FieldError("page", "La página no es válida")` | RF-025, RF-074, RF-075 |
| `MethodArgumentTypeMismatchException` con parámetro `page` | 400 | `ApiError` con `FieldError("page", "La página no es válida")` | RF-025, RF-074, RF-075 |
| `MethodArgumentTypeMismatchException` con `projectId` o `testCaseId` | 400 | `ApiError("Identificador no válido")` | RF-074, RF-077 |
| `InvalidTagNameException` | 400 | `ApiError` con `FieldError("tags[i]", mensaje)` | RF-015, RF-060, RF-061, RF-075 |
| `TagConflictException` | 409 | `ApiError("Conflicto al crear la etiqueta", [FieldError("tags", ...)])` | RF-019, RF-039, RF-066 |
| `ProjectNotFoundException` | 404 | `ApiError("Proyecto no encontrado")`, idéntico para inexistente y ajeno | RF-003, RF-018, RF-027, RF-030, RF-041, RF-080 |
| `TestCaseNotFoundException` | 404 | `ApiError("Test case no encontrado")`, idéntico para inexistente, ajeno o de otro proyecto | RF-004, RF-030, RF-041, RF-047, RF-080 |
| `InvalidTestCaseStateException` | 400 | `ApiError` con `FieldError("expectedResult", mensaje)` | RF-036, RF-070 |
| Token ausente, inválido o caducado | 401 | `ApiError("No autenticado")` desde `JwtAuthenticationEntryPoint` | RF-001, RF-074 |
| Rol no autorizado | 403 | `ApiError("No tienes permisos para acceder a este recurso")` desde `SecurityConfig` | RF-005, RF-074 |
| `HttpRequestMethodNotSupportedException` | 405 | `ApiError("Método HTTP no soportado")` | RF-074, RF-083 |
| `HttpMediaTypeNotAcceptableException` | 406 | `ApiError("Tipo de contenido no aceptable")` | RF-074 |
| `HttpMessageNotReadableException` | 400 | `ApiError("Cuerpo de la petición inválido")` | RF-074, RF-082 |
| `HttpMediaTypeNotSupportedException` | 415 | `ApiError("Tipo de contenido no soportado")` | RF-074, RF-081 |
| `Exception` no controlada | 500 | `ApiError("Error interno del servidor")`, sin detalles internos | RF-074, RF-084 |

Todas las respuestas usan `ApiError` (`message` y `fields`). Los mensajes 404
son genéricos e idénticos por tipo. El 409 de etiqueta no se reintenta ni
relee la etiqueta.

## 6. Estrategia de tests

Solo tests unitarios y de controlador, según la decisión del proyecto. No se
levanta una base de datos en los tests: la cascada real, la unicidad de
`position`, el conflicto 409 concurrente y el comportamiento de MySQL se
verifican manualmente.

### 6.1 Tests unitarios y de controlador

| Test | Qué verifica | RF |
|---|---|---|
| `TagNormalizerTest` | Recorte de espacios ASCII y Unicode (NBSP, espacio eme, ideográfico); minúsculas con `Locale.ROOT`; NFC/NFD; eliminación de tildes, diéresis y cedillas; conservación de la `ñ` precompuesta y descompuesta; `cañon`/`cañón` iguales y `canon` distinto; espacios interiores conservados; sin *case folding*; nombre visible con mayúsculas y tildes; vacío tras el recorte; normalizado de 50/51 unidades; longitudes UTF-16 con emojis | RF-058 a RF-062 |
| `CreateTestCaseRequestValidationTest` | `title` nulo/vacío/solo espacios ASCII/tabuladores/saltos → 400; NBSP aceptado como contenido; 255 aceptadas y 256 rechazadas; 127/128 emojis; `description`/`expectedResult` 255/256 y vacíos aceptados; `priority` nula, vacía, minúscula o desconocida → 400 y valores válidos aceptados; `steps` nulo/vacío → 400; `instruction` nula/vacía/256 → 400 y 255 aceptada; `tags` nulas/vacías aceptadas, nombre vacío/51 → 400, 50 aceptado | RF-009 a RF-016, RF-049, RF-058 a RF-061 |
| `UpdateTestCaseRequestValidationTest` | Todo lo anterior más `status` nulo/vacío/minúscula/desconocido → 400 y valores válidos aceptados; `COMPLETED` sin `expectedResult` no vacío → 400 en `expectedResult`; `IN_PROGRESS` con `expectedResult` nulo aceptado; NBSP en `expectedResult` con `COMPLETED` aceptado por la semántica de `@NotBlank`; `status` inválido no duplica el error de `expectedResult` | RF-035, RF-036, RF-067, RF-068, RF-070, RF-075, RF-078 |
| `TagResolverTest` | Deduplicación por normalizado; primer visible para una nueva; visible persistido para una existente; reutilización sin `save`; creación de las que faltan; orden de primera aparición; etiqueta vacía o normalizado de más de 50 → `InvalidTagNameException` con índice; `DataIntegrityViolationException` → `TagConflictException` sin reintento ni relectura; `tags` nulas o vacías sin acceso a BD | RF-015, RF-016, RF-019, RF-039, RF-058 a RF-066 |
| `TestCaseServiceTest` — creación | Usuario actual del puerto; proyecto por `id + userId`; 404 para proyecto inexistente o ajeno; `status` siempre `IN_PROGRESS`; `createdAt` del servidor; título e instrucciones tal cual; `description`/`expectedResult` vacíos → `null`; posiciones 1..N; etiquetas resueltas; respuesta de detalle; 400 de etiqueta antes de consultar el proyecto; 409 de etiqueta sin guardar el test case | RF-002, RF-003, RF-007 a RF-020, RF-049, RF-058 a RF-062 |
| `TestCaseServiceTest` — listado | `page` negativa antes de consultar el proyecto; 404 de proyecto; llamada al puerto con `page` y tamaño 15; envoltorio con `page`, `size`, `totalElements`, `totalPages`; página vacía con `totalPages: 0`; página posterior a la última con lista vacía y totales; resumen sin `expectedResult` ni `steps`; etiquetas ordenadas; `createdAt` en `dd/MM/yyyy` | RF-021 a RF-027 |
| `TestCaseServiceTest` — detalle | 404 de proyecto; 404 de test case inexistente, ajeno o de otro proyecto del mismo usuario; detalle completo; pasos ordenados; etiquetas ordenadas; `createdAt` formateado; sin `userId` ni `updatedAt` | RF-028 a RF-031 |
| `TestCaseServiceTest` — edición | 404 de proyecto/test case; reemplazo completo; opcionales omitidos o `null` → vacío; transiciones `IN_PROGRESS` ↔ `COMPLETED`; `COMPLETED` sin `expectedResult` → 400 como defensa de dominio; posiciones 1..N; etiquetas reemplazadas; `createdAt` y `projectId` inmutables; títulos repetidos; 409 sin cambios; respuesta de detalle | RF-032 a RF-044, RF-049 a RF-053, RF-058 a RF-063 |
| `TestCaseServiceTest` — borrado | 404 de proyecto/test case; `deleteById` del test case; no se llama a borrados manuales de pasos ni etiquetas; segundo borrado 404; etiquetas globales intactas | RF-045 a RF-048, RF-056, RF-065 |
| `TestCaseDtoMapperTest` | Mapea todos los campos; `createdAt` en `dd/MM/yyyy`; resumen sin `expectedResult` ni `steps`; detalle con pasos y etiquetas; `null` en opcionales; listas vacías como `[]` | RF-021 a RF-031, RF-044, RF-063 |
| `TestCaseMapperTest`, `TestStepMapperTest`, `TagMapperTest` | Entidad ↔ dominio; enums; `createdAt`; `position`; `name` y `normalizedName`; no mapean DTOs ni formatean fechas | RF-003, RF-009, RF-013, RF-063 |
| `TestCaseRepositoryAdapterTest` | Creación guarda el test case con sus colecciones; edición actualiza la fila, vacía las colecciones de pasos/asociaciones, hace `flush()` y añade las nuevas; lectura de detalle compone pasos y etiquetas; listado usa `PageRequest` con página, tamaño 15 y orden `createdAt DESC, id DESC`; el listado no accede a `steps` ni `tagLinks` y carga etiquetas en una sola consulta por página; página vacía sin consulta de etiquetas; `PageResult` con totales; borrado solo del test case | RF-021 a RF-048, RF-063 |
| `TagRepositoryAdapterTest` | Búsqueda por nombres normalizados; creación con `saveAllAndFlush`; `DataIntegrityViolationException` → `TagConflictException`; no se reintenta ni se relee | RF-019, RF-039, RF-058 a RF-066 |
| `TestCaseControllerTest` | 201 con detalle; 200 listado con envoltorio y `size: 15`; 200 detalle; 200 edición; 204 borrado sin cuerpo; 400 de validación con `steps[i].instruction` y `tags[i]`; 400 de `page` negativa y no numérica; 400 de ids no numéricos; 400 de JSON mal formado; 404 de proyecto/test case; 409 de etiqueta; 401 sin token; 405; 406; 415; 500 sin detalles; campos desconocidos ignorados; `status` en creación ignorado; `userId` ausente; el caso de uso no se invoca si el DTO es inválido | RF-001, RF-007 a RF-084 |
| `TestCaseEndpointSecurityTest` | La cadena real (`SecurityConfig` + JWT) rechaza sin token y con token inválido (401); `USER` y `ADMIN` llegan al controlador; una ruta no listada responde 403; los endpoints anidados están cubiertos por `/api/projects/**` | RF-001, RF-002, RF-005 |
| `TestCaseControllerOpenApiTest` | Los cinco métodos tienen `@Operation`, `@SecurityRequirement("bearerAuth")`, `@Tag` y los códigos de respuesta de la spec; el esquema bearer sigue registrado | RF-001 (soporte), RF-074 |
| `GlobalExceptionHandlerTest` (ampliado) | `TestCaseNotFoundException` → 404; `TagConflictException` → 409 con campo `tags`; `InvalidTagNameException` → 400 con `tags[i]`; `InvalidPageException` → 400 con `page`; `InvalidTestCaseStateException` → 400 con `expectedResult`; `MethodArgumentTypeMismatchException` distingue `page` de ids; 406; 500 sin detalles | RF-073 a RF-084 |

### 6.2 Verificación manual contra MySQL

| Verificación | Qué comprueba | RF |
|---|---|---|
| Cascada al borrar un test case | Desaparecen la fila de `test_cases`, sus `test_steps` y sus `test_case_tags`; las filas de `tags` permanecen | RF-046, RF-048, RF-065 |
| Cascada al borrar un proyecto | Desaparecen sus test cases, pasos y asociaciones; las etiquetas globales permanecen | RF-046, RF-056, RF-065 |
| Unicidad de `position` | Insertar a mano dos pasos con la misma `(test_case_id, position)` falla; la aplicación nunca lo intenta porque vacía las colecciones y hace `flush()` antes de insertar | RF-050 |
| Conflicto 409 concurrente | Dos peticiones simultáneas con la misma etiqueta nueva: una crea el test case, la otra recibe 409; no hay etiqueta duplicada, ni test case parcial, ni 500 | RF-019, RF-039, RF-066 |
| Paginación | Proyecto con 0, 1, 15, 16 y 31 test cases: `totalPages` 0, 1, 1, 2 y 3; última página con menos de 15; página posterior a la última con lista vacía y totales | RF-021 a RF-026 |
| N+1 | Con el log SQL activado, un listado de 15 test cases con etiquetas usa un número acotado de consultas (conteo, página y lote de etiquetas), no una por test case | RF-021, RF-026 |
| Normalización Unicode | `Regresión`, `REGRESION`, `regresion` y ` regresion ` comparten etiqueta; `cañon` y `cañón` comparten; `canon` es distinta; NBSP inicial/final se recorta y el interior se conserva | RF-058 a RF-062, RF-066 |
| Fechas y contrato | `createdAt` en `dd/MM/yyyy`; listas vacías como `[]`; opcionales como `null`; etiquetas `[{id, name}]`; pasos `[{position, instruction}]` | RF-026, RF-029, RF-031, RF-063 |
| Errores | 400, 401, 403, 404, 405, 406, 409, 415 y 500 con `ApiError` y sin detalles internos | RF-073 a RF-084 |
| OpenAPI | Swagger muestra los cinco endpoints con seguridad bearer y los esquemas de petición/respuesta | RF-001 (soporte) |
| Esquema y dependencias | El script SQL no ha cambiado y no se han añadido dependencias | Constitución 3 y 11 |

### 6.3 Cobertura de RF

| RF | Dónde se cubre |
|---|---|
| RF-001 a RF-006 | `SecurityConfig` existente, `CurrentUserProvider`, `TestCaseService` (proyecto y test case por propietario), `TestCaseEndpointSecurityTest`, `TestCaseServiceTest` y `TestCaseControllerTest` |
| RF-007 a RF-020 | `CreateTestCaseRequest`, `TestCaseService.create`, `TestCaseRepositoryAdapter`, `TagResolver`, `TestCaseControllerTest` y `TestCaseServiceTest` |
| RF-021 a RF-027 | `ListTestCasesUseCase`, `PageResult`, `TestCaseService.list`, consulta paginada + lote de etiquetas, `TestCaseControllerTest` y verificación manual de paginación |
| RF-028 a RF-031 | `GetTestCaseUseCase`, `TestCaseService.get`, lectura de pasos/etiquetas, `TestCaseDtoMapperTest` y `TestCaseControllerTest` |
| RF-032 a RF-044 | `UpdateTestCaseRequest` + validador cruzado, `TestCaseService.update`, reemplazo de hijos, `TestCaseRepositoryAdapterTest` y `TestCaseControllerTest` |
| RF-045 a RF-048 | `DeleteTestCaseUseCase`, `TestCaseService.delete`, `deleteById` sin borrados manuales, `TestCaseControllerTest` y verificación manual de cascada |
| RF-049 a RF-057 | `StepRequest`, asignación de posiciones, `TestStepJpaEntity`, `TestStepJpaRepository`, `TestCaseServiceTest`, `TestCaseRepositoryAdapterTest` y verificación manual de `UNIQUE` |
| RF-058 a RF-066 | `TagNormalizer`, `TagResolver`, `TagRepositoryAdapter`, `TagNormalizerTest`, `TagResolverTest`, `TestCaseServiceTest` y verificación manual de concurrencia y Unicode |
| RF-067 a RF-072 | Enums `Priority`/`Status`, `@Pattern`, `TestCaseService`, `TestCaseDtoMapper` y tests de validación/servicio |
| RF-073 a RF-084 | `GlobalExceptionHandler`, `ApiError`, `TestCaseControllerTest`, `GlobalExceptionHandlerTest` y verificación manual de códigos |

## 7. Puntos abiertos y supuestos

- **Mapeo JPA cerrado:** las relaciones viven solo en las entidades JPA de
  infraestructura. `TestCaseJpaEntity` conecta con `TestStepJpaEntity` y
  `TestCaseTagJpaEntity`, y esta última con `TagJpaEntity`. El dominio no
  conoce JPA. Se usa `orphanRemoval` para reemplazar hijos; el flujo de
  borrado no carga las colecciones y no se declara `CascadeType.REMOVE`, de
  modo que la cascada del padre la ejecuta MySQL y la aplicación no la
  duplica.
- **Tabla de unión explícita:** `TestCaseTagJpaEntity` modela
  `test_case_tags` con `@EmbeddedId` + `@MapsId` en lugar de un
  `@ManyToMany` directo, para controlar huérfanos y cargar etiquetas en lote.
- **Listado sin colecciones:** el mapper de resumen no accede a `steps` ni a
  `tagLinks`; las etiquetas salen de la proyección por lote. El detalle sí
  accede a las colecciones dentro de la transacción.
- **Orden de reemplazo cerrado:** vaciar las colecciones y hacer `flush()`
  antes de insertar. Si se usara un borrado masivo por consulta, habría que
  sincronizar el contexto de persistencia; el plan usa las colecciones
  gestionadas para que Hibernate ejecute los `DELETE` de huérfanos antes de
  los `INSERT`.
- **Detección del 409:** se envuelve solo la inserción de etiquetas nuevas.
  La única restricción única esperada en esa operación es
  `UNIQUE(normalized_name)`; cualquier otra violación de integridad en esa
  inserción también se traducirá a 409, porque el esquema no debería
  producirla en un flujo válido.
- **Sin tests de integración con BD:** la cascada, la unicidad de `position`
  y la concurrencia real se validan manualmente. No se añaden Testcontainers
  ni H2.
- **`@NotBlank` y NBSP:** en los campos de texto generales (`title`,
  `description`, `expectedResult` e `instruction`) se acepta NBSP como
  contenido, porque `String.trim()` no lo considera blanco. En etiquetas sí
  se recorta con la regla Unicode y un nombre compuesto solo por NBSP acaba
  en 400. Es una diferencia deliberada de la spec.
- **Longitudes:** se miden en unidades UTF-16 (`String.length()`/`@Size`).
  Un emoji cuenta 2; el límite de Java es más estricto que `VARCHAR(255)`.
- **Sin bloqueo optimista:** dos ediciones simultáneas se resuelven
  last-write-wins; no se añade `version` ni detección de conflicto.
- **IDs como números JSON:** un `id` mayor que 2^53-1 perdería precisión en
  JavaScript; no se serializa como string.
- **Zona horaria:** `createdAt` se formatea en la zona del servidor; no se
  normaliza a UTC.
- **Sin máximos:** no hay límite de test cases por proyecto, pasos por test
  case ni etiquetas por test case.
- **`totalPages` vacío:** se confirma que Spring Data devuelve 0 con
  `totalElements = 0`; si no, el adaptador lo calcula explícitamente.
- **Campos desconocidos:** se ignoran; no se activa
  `FAIL_ON_UNKNOWN_PROPERTIES`.
- **Etiquetas globales:** no se borran al borrar test cases o proyectos; solo
  se eliminan sus asociaciones.
- **Sin cambios de esquema ni dependencias:** cualquier necesidad nueva se
  consulta antes de implementarla.

## 8. Secuencia de implementación

1. Modelo de dominio (`TestCase`, `TestStep`, `Tag`, `TagName`, enums,
   `PageResult`), puertos de entrada/salida, excepciones y `TagNormalizer`
   con sus tests.
2. DTOs de entrada/salida, validaciones declarativas, validador cruzado de
   `COMPLETED` y `TestCaseDtoMapper` con sus tests.
3. `TagResolver` y `TestCaseService` con tests unitarios de creación,
   listado, detalle, edición y borrado.
4. Entidades JPA, repositorios, proyección de etiquetas, mappers y
   adaptadores con tests de adaptador.
5. `TestCaseController`, ampliación de `GlobalExceptionHandler`, anotaciones
   springdoc y tests de controlador, seguridad y OpenAPI.
6. `mvnw.cmd test` en verde, `mvnw.cmd spotless:apply` y verificación manual
   contra MySQL de cascadas, `UNIQUE`, 409 concurrente, paginación y N+1.

## 9. Criterios de finalización

- [ ] Los cinco endpoints responden con los códigos, cuerpos y envoltorios de
  la spec.
- [ ] El propietario sale del JWT; un proyecto o test case ajeno responde 404,
  también para `ADMIN`.
- [ ] Un test case que no pertenece al proyecto de la ruta responde 404.
- [ ] Al crear, el estado es `IN_PROGRESS`, `createdAt` lo pone el servidor y
  la respuesta es el detalle completo.
- [ ] `title`, `priority`, `status`, `steps` y `tags` cumplen las reglas de
  validación y los límites de longitud.
- [ ] Todo test case tiene al menos un paso al crear y al editar; las
  posiciones son 1..N y se renumeran al reordenar.
- [ ] `COMPLETED` exige `expectedResult` no vacío; `IN_PROGRESS` permite
  `null`; las transiciones en ambos sentidos funcionan.
- [ ] `PUT` reemplaza todos los datos, pasos y etiquetas; `projectId` y
  `createdAt` son inmutables.
- [ ] El listado solo contiene test cases del proyecto, se pagina de 15 en 15,
  se ordena por `createdAt DESC, id DESC` y no incluye pasos.
- [ ] El detalle incluye `expectedResult` y pasos ordenados.
- [ ] Las etiquetas se normalizan, reutilizan, deduplican, conservan la `ñ`,
  respetan los límites y responden 409 ante una creación concurrente.
- [ ] El borrado responde 204, delega la cascada en MySQL y conserva las
  etiquetas globales; un segundo borrado responde 404.
- [ ] Todos los errores usan `ApiError`; ids no numéricos y páginas inválidas
  dan 400; ajenos o inexistentes dan 404; el conflicto de etiqueta da 409.
- [ ] `mvnw.cmd test` en verde con tests unitarios, de validación, de
  adaptador, de controlador y de seguridad.
- [ ] Swagger muestra los cinco endpoints con seguridad bearer y sus
  esquemas.
- [ ] Verificación manual contra MySQL de cascadas, `UNIQUE(position)` y 409
  concurrente.
- [ ] No se ha modificado el script SQL ni se han añadido dependencias.
