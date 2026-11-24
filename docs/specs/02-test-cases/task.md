# Tareas — Gestión de test cases y pasos

Descomposición ejecutable de `docs/specs/02-test-cases/plan.md` para la spec
`docs/specs/02-test-cases/spec.md`. Cada tarea está pensada para menos de 30
minutos, se ejecuta en el orden indicado y termina con una verificación
observable.

**Objetivo:** implementar los cinco endpoints de test cases, sus pasos,
etiquetas, validaciones, errores y documentación, sin modificar el esquema SQL
ni añadir dependencias.

**Arquitectura:** hexagonal estricta. Dominio y aplicación no conocen Spring
Data ni JPA; las relaciones JPA viven solo en las entidades de infraestructura.

**Stack:** Java 21, Spring Boot 4.1.1, Spring Data JPA, MySQL 8, MapStruct,
Lombok, JUnit 5, Mockito, MockMvc, springdoc.

**Spec:** `docs/specs/02-test-cases/spec.md`
**Plan:** `docs/specs/02-test-cases/plan.md`

## Reglas de ejecución

- Trabajar una tarea cada vez y no marcar la casilla hasta que su línea
  `Hecho cuando:` se cumpla.
- TDD: escribir primero el test, verlo fallar, implementar lo mínimo y verlo
  pasar.
- Un commit por tarea, con mensaje `feat(test-cases): ...` o
  `test(test-cases): ...`.
- No modificar `docs/esquema_creacion_base_datos.sql` ni `pom.xml`.
- Si una tarea revela una necesidad fuera del plan, parar y consultar antes de
  ampliar el alcance.
- Comando base de verificación: `.\mvnw.cmd test -q`.

## Fase 0 — Punto de partida

- [x] **T0.1 — Confirmar la suite base** (~5 min)
  - RF: Todos.
  - Depende de: —
  - Hecho cuando: `.\mvnw.cmd test -q` termina en `BUILD SUCCESS`, con 119 tests y 0 fallos.

## Fase 1 — Dominio

- [x] **T1.1 — Crear los enums `Priority` y `Status`** (~10 min)
  - RF: RF-012, RF-035, RF-067, RF-068, RF-072.
  - Depende de: T0.1.
  - Hecho cuando: `Priority` contiene exactamente `HIGH`, `MEDIUM`, `LOW` y `Status` contiene `IN_PROGRESS`, `COMPLETED`, y `.\mvnw.cmd -q -DskipTests compile` termina en `BUILD SUCCESS`.

- [ ] **T1.2 — Crear los POJOs de dominio auxiliares** (~20 min)
  - RF: RF-013, RF-014, RF-021 a RF-026, RF-049 a RF-057, RF-058 a RF-066.
  - Depende de: T1.1.
  - Hecho cuando: existen `TestStep`, `Tag`, `TagName` y `PageResult` con los campos del plan y `.\mvnw.cmd -q -DskipTests compile` termina en `BUILD SUCCESS`.

- [ ] **T1.3 — Crear el modelo `TestCase`** (~15 min)
  - RF: RF-007 a RF-048, RF-067 a RF-072.
  - Depende de: T1.1, T1.2.
  - Hecho cuando: `TestCase` expone `id`, `projectId`, `title`, `description`, `expectedResult`, `priority`, `status`, `createdAt`, `steps` y `tags`, inicializa las listas vacías y compila.

- [ ] **T1.4 — Crear los puertos de entrada** (~10 min)
  - RF: RF-007 a RF-048.
  - Depende de: T1.3.
  - Hecho cuando: existen `CreateTestCaseUseCase`, `ListTestCasesUseCase`, `GetTestCaseUseCase`, `UpdateTestCaseUseCase` y `DeleteTestCaseUseCase` con un método cada uno y `.\mvnw.cmd -q -DskipTests compile` pasa.

- [ ] **T1.5 — Crear los puertos de salida** (~15 min)
  - RF: RF-003, RF-004, RF-019, RF-021 a RF-066.
  - Depende de: T1.3.
  - Hecho cuando: `TestCaseRepository` expone guardar, buscar por `id + projectId`, paginar por proyecto, borrar y reemplazar hijos, y `TagRepository` expone buscar por nombres normalizados y crear, con `.\mvnw.cmd -q -DskipTests compile` en verde.

- [ ] **T1.6 — Crear las excepciones de dominio** (~15 min)
  - RF: RF-004, RF-015, RF-019, RF-025, RF-036, RF-039, RF-041, RF-047, RF-060, RF-061, RF-066, RF-070, RF-074, RF-075.
  - Depende de: T1.3.
  - Hecho cuando: existen `TestCaseNotFoundException`, `TagConflictException`, `InvalidTagNameException`, `InvalidPageException` e `InvalidTestCaseStateException` con los mensajes de la spec y `.\mvnw.cmd -q -DskipTests compile` pasa.

- [ ] **T1.7 — Implementar el núcleo de `TagNormalizer`** (~30 min)
  - RF: RF-058, RF-059, RF-062, RF-066.
  - Depende de: T1.2.
  - Hecho cuando: `TagNormalizerTest` pasa en verde para recorte Unicode, minúsculas con `Locale.ROOT`, NFC, NFD sin marcas `Mn`, conservación de la `ñ` precompuesta y descompuesta, y `cañon`/`cañón` iguales frente a `canon` distinto.

- [ ] **T1.8 — Completar las validaciones de `TagNormalizer`** (~25 min)
  - RF: RF-015, RF-060, RF-061.
  - Depende de: T1.7.
  - Hecho cuando: `TagNormalizerTest` pasa en verde para visible vacío, normalizado vacío, normalizado de 50/51 unidades, longitudes UTF-16 con emojis y NBSP inicial/final frente a NBSP interior.

## Fase 2 — DTOs y validación

- [ ] **T2.1 — Crear `StepRequest` y `CreateTestCaseRequest`** (~30 min)
  - RF: RF-007 a RF-016, RF-049, RF-058 a RF-061.
  - Depende de: T1.1.
  - Hecho cuando: `CreateTestCaseRequestValidationTest` pasa en verde con los límites y errores de `title`, `description`, `expectedResult`, `priority`, `steps`, `instruction` y `tags`.

- [ ] **T2.2 — Crear `UpdateTestCaseRequest`** (~25 min)
  - RF: RF-032 a RF-036, RF-067 a RF-070, RF-075, RF-078.
  - Depende de: T2.1.
  - Hecho cuando: `UpdateTestCaseRequestValidationTest` pasa en verde para `status` obligatorio, valores válidos, minúsculas, desconocidos y `COMPLETED` sin `expectedResult`.

- [ ] **T2.3 — Implementar `CompletedRequiresExpectedResult`** (~20 min)
  - RF: RF-036, RF-041, RF-070, RF-076.
  - Depende de: T2.2.
  - Hecho cuando: el validador añade el error solo en `expectedResult`, acepta `IN_PROGRESS` con resultado nulo y `COMPLETED` con NBSP, y sus tests pasan.

- [ ] **T2.4 — Crear los DTOs de respuesta** (~20 min)
  - RF: RF-021 a RF-031, RF-044, RF-063.
  - Depende de: T1.2.
  - Hecho cuando: existen `TestCaseSummaryResponse`, `TestCaseDetailResponse`, `TagResponse`, `TestCaseStepResponse` y `TestCasePageResponse` con los campos del plan y `.\mvnw.cmd -q -DskipTests compile` pasa.

- [ ] **T2.5 — Implementar `TestCaseDtoMapper`** (~25 min)
  - RF: RF-021 a RF-031, RF-044, RF-063.
  - Depende de: T1.3, T2.4.
  - Hecho cuando: `TestCaseDtoMapperTest` pasa en verde para todos los campos, `createdAt` en `dd/MM/yyyy`, resumen sin `expectedResult` ni `steps`, detalle con pasos y etiquetas, `null` en opcionales y listas vacías como `[]`.

## Fase 3 — Aplicación

- [ ] **T3.1 — Implementar `TagResolver`** (~30 min)
  - RF: RF-015, RF-016, RF-019, RF-039, RF-058 a RF-066.
  - Depende de: T1.5, T1.7, T1.8.
  - Hecho cuando: `TagResolverTest` pasa en verde para deduplicación, primer visible, visible persistido, reutilización, creación de faltantes, orden de aparición, `InvalidTagNameException` con índice, `DataIntegrityViolationException` traducida a `TagConflictException` sin reintento y listas nulas o vacías sin acceso a base de datos.

- [ ] **T3.2 — Implementar `TestCaseService.create`** (~30 min)
  - RF: RF-001 a RF-020, RF-049, RF-058 a RF-062.
  - Depende de: T1.4, T1.5, T2.1, T2.5, T3.1.
  - Hecho cuando: los tests de creación de `TestCaseServiceTest` pasan en verde para usuario actual, proyecto por propietario, 404, `IN_PROGRESS`, `createdAt` del servidor, textos tal cual, opcionales vacíos a `null`, posiciones 1..N, etiquetas resueltas, 400 antes de consultar el proyecto y 409 sin guardar.

- [ ] **T3.3 — Implementar `TestCaseService.list`** (~25 min)
  - RF: RF-021 a RF-027.
  - Depende de: T1.4, T1.5, T2.4, T2.5.
  - Hecho cuando: los tests de listado de `TestCaseServiceTest` pasan en verde para página negativa antes del proyecto, 404, llamada con página y tamaño 15, envoltorio y totales, página vacía, página posterior a la última, resumen sin pasos, etiquetas ordenadas y fecha formateada.

- [ ] **T3.4 — Implementar `TestCaseService.get`** (~20 min)
  - RF: RF-028 a RF-031.
  - Depende de: T1.4, T1.5, T2.5.
  - Hecho cuando: los tests de detalle de `TestCaseServiceTest` pasan en verde para 404 de proyecto, 404 de test case inexistente, ajeno o de otro proyecto, detalle completo, pasos y etiquetas ordenados, fecha formateada y ausencia de `userId` y `updatedAt`.

- [ ] **T3.5 — Implementar el reemplazo de `TestCaseService.update`** (~30 min)
  - RF: RF-032 a RF-040, RF-042 a RF-044, RF-049 a RF-053.
  - Depende de: T2.2, T2.3, T3.1.
  - Hecho cuando: los tests de edición feliz de `TestCaseServiceTest` pasan en verde para reemplazo completo, opcionales omitidos a vacío, transiciones de estado, posiciones 1..N, etiquetas reemplazadas, `createdAt` y `projectId` inmutables, títulos repetidos y respuesta de detalle.

- [ ] **T3.6 — Cubrir errores y conflicto de `TestCaseService.update`** (~25 min)
  - RF: RF-036, RF-039, RF-041, RF-042, RF-058 a RF-063.
  - Depende de: T3.5.
  - Hecho cuando: los tests de edición con error de `TestCaseServiceTest` pasan en verde para 404 de proyecto y test case, `COMPLETED` sin resultado como defensa de dominio, 409 sin cambios y `InvalidTagNameException` antes de tocar el repositorio.

- [ ] **T3.7 — Implementar `TestCaseService.delete`** (~20 min)
  - RF: RF-045 a RF-048, RF-056, RF-065.
  - Depende de: T1.4, T1.5.
  - Hecho cuando: los tests de borrado de `TestCaseServiceTest` pasan en verde para 404 de proyecto y test case, `deleteById` del test case, ausencia de borrados manuales de pasos o etiquetas, segundo borrado 404 y etiquetas globales intactas.

## Fase 4 — Persistencia

- [ ] **T4.1 — Mapear `TestCaseJpaEntity` y `TestStepJpaEntity`** (~25 min)
  - RF: RF-003, RF-007, RF-013, RF-021 a RF-031, RF-038, RF-046, RF-049 a RF-057.
  - Depende de: T1.3.
  - Hecho cuando: `TestCaseJpaEntity` declara `@OneToMany(fetch = LAZY, orphanRemoval = true)` hacia pasos y asociaciones, `TestStepJpaEntity` declara su `@ManyToOne(fetch = LAZY)` y `.\mvnw.cmd -q -DskipTests compile` pasa.

- [ ] **T4.2 — Mapear `TagJpaEntity` y `TestCaseTagJpaEntity`** (~25 min)
  - RF: RF-039, RF-046, RF-058 a RF-066.
  - Depende de: T4.1.
  - Hecho cuando: `TagJpaEntity` mapea `name` y `normalizedName`, y `TestCaseTagJpaEntity` usa `@EmbeddedId` + `@MapsId` con `TestCaseTagId` y `@ManyToOne(fetch = LAZY)` hacia test case y tag.

- [ ] **T4.3 — Crear los repositorios Spring Data y la proyección** (~25 min)
  - RF: RF-003, RF-004, RF-019, RF-021 a RF-048, RF-058 a RF-066.
  - Depende de: T4.1, T4.2.
  - Hecho cuando: existen `TestCaseJpaRepository`, `TestStepJpaRepository`, `TagJpaRepository` y `TestCaseTagJpaRepository`, la proyección `TestCaseTagRow(testCaseId, tagId, name)` y `.\mvnw.cmd -q -DskipTests compile` pasa.

- [ ] **T4.4 — Implementar los mappers de persistencia** (~25 min)
  - RF: RF-003, RF-009, RF-013, RF-063.
  - Depende de: T4.1, T4.2.
  - Hecho cuando: `TestCaseMapperTest`, `TestStepMapperTest` y `TagMapperTest` pasan en verde para entidad ↔ dominio, enums, `createdAt`, `position`, `name` y `normalizedName`, sin mapear DTOs ni formatear fechas.

- [ ] **T4.5 — Implementar `TagRepositoryAdapter`** (~25 min)
  - RF: RF-019, RF-039, RF-058 a RF-066.
  - Depende de: T1.5, T4.2, T4.3, T4.4.
  - Hecho cuando: `TagRepositoryAdapterTest` pasa en verde para búsqueda por normalizados, creación con `saveAllAndFlush`, traducción de `DataIntegrityViolationException` a `TagConflictException` y ausencia de reintento o relectura.

- [ ] **T4.6 — Implementar creación y detalle del adaptador de test cases** (~30 min)
  - RF: RF-007 a RF-020, RF-028 a RF-031, RF-049 a RF-057.
  - Depende de: T1.5, T4.1, T4.3, T4.4.
  - Hecho cuando: `TestCaseRepositoryAdapterTest` pasa en verde para guardar el test case con sus colecciones y para componer pasos y etiquetas en la lectura de detalle.

- [ ] **T4.7 — Implementar listado y paginación del adaptador** (~25 min)
  - RF: RF-021 a RF-027.
  - Depende de: T4.6.
  - Hecho cuando: `TestCaseRepositoryAdapterTest` pasa en verde para `PageRequest` con página, tamaño 15 y orden `createdAt DESC, id DESC`, etiquetas en una sola consulta por página, página vacía sin consulta de etiquetas, `PageResult` con totales y listado sin tocar `steps` ni `tagLinks`.

- [ ] **T4.8 — Implementar el reemplazo de hijos en el adaptador** (~30 min)
  - RF: RF-032 a RF-044, RF-049 a RF-053.
  - Depende de: T4.6.
  - Hecho cuando: `TestCaseRepositoryAdapterTest` pasa en verde verificando que la edición actualiza la fila, vacía las colecciones de pasos y asociaciones, hace `flush()` antes de insertar las nuevas y conserva `createdAt` y `projectId`.

- [ ] **T4.9 — Implementar el borrado en el adaptador** (~15 min)
  - RF: RF-045 a RF-048, RF-056, RF-065.
  - Depende de: T4.6.
  - Hecho cuando: `TestCaseRepositoryAdapterTest` pasa en verde verificando que el borrado elimina solo el test case y no ejecuta borrados manuales de pasos ni asociaciones.

## Fase 5 — Web y errores

- [ ] **T5.1 — Ampliar `GlobalExceptionHandler`** (~25 min)
  - RF: RF-073 a RF-084.
  - Depende de: T1.6.
  - Hecho cuando: `GlobalExceptionHandlerTest` pasa en verde para `TestCaseNotFoundException` 404, `TagConflictException` 409 con campo `tags`, `InvalidTagNameException` 400 con `tags[i]`, `InvalidPageException` 400 con `page`, `InvalidTestCaseStateException` 400 con `expectedResult`, distinción de `page` frente a ids, 406 y 500 sin detalles.

- [ ] **T5.2 — Implementar el endpoint de creación** (~30 min)
  - RF: RF-001, RF-007 a RF-020, RF-073 a RF-084.
  - Depende de: T2.1, T3.2, T5.1.
  - Hecho cuando: `TestCaseControllerTest` pasa en verde para `POST` 201 con detalle, 400 de validación con `steps[i].instruction` y `tags[i]`, 404 de proyecto, 409 de etiqueta, 415, JSON mal formado, campos desconocidos ignorados, `status` ignorado y ausencia de `userId`.

- [ ] **T5.3 — Implementar el endpoint de listado** (~20 min)
  - RF: RF-001, RF-021 a RF-027.
  - Depende de: T3.3, T5.1.
  - Hecho cuando: `TestCaseControllerTest` pasa en verde para `GET` 200 con envoltorio y `size: 15`, `page` por defecto 0, 400 de página negativa y no numérica, 404 de proyecto y listado vacío con totales.

- [ ] **T5.4 — Implementar el endpoint de detalle** (~15 min)
  - RF: RF-001, RF-028 a RF-031.
  - Depende de: T3.4, T5.1.
  - Hecho cuando: `TestCaseControllerTest` pasa en verde para `GET` 200 con detalle, 404 de proyecto, 404 de test case y 400 de id no numérico.

- [ ] **T5.5 — Implementar el endpoint de edición** (~25 min)
  - RF: RF-001, RF-032 a RF-044.
  - Depende de: T3.5, T3.6, T5.1.
  - Hecho cuando: `TestCaseControllerTest` pasa en verde para `PUT` 200 con detalle, 400 de validación y de `COMPLETED` sin resultado, 404 de proyecto y test case, y 409 de etiqueta.

- [ ] **T5.6 — Implementar el endpoint de borrado** (~15 min)
  - RF: RF-001, RF-045 a RF-048.
  - Depende de: T3.7, T5.1.
  - Hecho cuando: `TestCaseControllerTest` pasa en verde para `DELETE` 204 sin cuerpo, 404 de proyecto y test case, y 400 de id no numérico.

- [ ] **T5.7 — Verificar la seguridad real de los endpoints** (~25 min)
  - RF: RF-001, RF-002, RF-005.
  - Depende de: T5.2, T5.3, T5.4, T5.5, T5.6.
  - Hecho cuando: `TestCaseEndpointSecurityTest` pasa en verde verificando 401 sin token y con token inválido, acceso de `USER` y `ADMIN`, 403 en una ruta no listada y cobertura de las rutas anidadas por `/api/projects/**`.

- [ ] **T5.8 — Verificar la documentación OpenAPI** (~20 min)
  - RF: RF-001 (soporte), RF-074.
  - Depende de: T5.2, T5.3, T5.4, T5.5, T5.6.
  - Hecho cuando: `TestCaseControllerOpenApiTest` pasa en verde verificando que los cinco métodos tienen `@Operation`, `@SecurityRequirement("bearerAuth")`, `@Tag` y los códigos de respuesta de la spec, y que el esquema bearer sigue registrado.

## Fase 6 — Cierre y verificación manual

- [ ] **T6.1 — Ejecutar la suite completa y el formato** (~15 min)
  - RF: Todos.
  - Depende de: T1.1 a T5.8.
  - Hecho cuando: `.\mvnw.cmd test -q` termina en `BUILD SUCCESS` sin fallos y `.\mvnw.cmd spotless:check -q` termina en `BUILD SUCCESS`.

- [ ] **T6.2 — Verificar cascadas y unicidad en MySQL** (~25 min)
  - RF: RF-046, RF-048, RF-050, RF-056, RF-065.
  - Depende de: T6.1.
  - Hecho cuando: borrar un test case elimina su fila, sus `test_steps` y sus `test_case_tags` pero conserva `tags`; borrar un proyecto elimina sus test cases, pasos y asociaciones; insertar a mano dos pasos con la misma `(test_case_id, position)` falla.

- [ ] **T6.3 — Verificar el conflicto 409 concurrente** (~20 min)
  - RF: RF-019, RF-039, RF-066.
  - Depende de: T6.1.
  - Hecho cuando: dos peticiones simultáneas con la misma etiqueta nueva producen un 201 y un 409, no hay etiqueta duplicada, no queda test case parcial y no aparece un 500.

- [ ] **T6.4 — Verificar paginación y N+1** (~25 min)
  - RF: RF-021 a RF-026.
  - Depende de: T6.1.
  - Hecho cuando: con 0, 1, 15, 16 y 31 test cases los `totalPages` son 0, 1, 1, 2 y 3; la página posterior a la última devuelve lista vacía con totales; y el log SQL muestra un número acotado de consultas, sin una por test case.

- [ ] **T6.5 — Verificar normalización Unicode y contrato** (~25 min)
  - RF: RF-026, RF-029, RF-031, RF-058 a RF-063.
  - Depende de: T6.1.
  - Hecho cuando: `Regresión`, `REGRESION`, `regresion` y ` regresion ` comparten etiqueta; `cañon` y `cañón` comparten; `canon` es distinta; NBSP inicial y final se recorta y el interior se conserva; fechas en `dd/MM/yyyy`, etiquetas `[{id, name}]`, pasos `[{position, instruction}]`, listas vacías `[]` y opcionales `null`.

- [ ] **T6.6 — Verificar errores y OpenAPI en ejecución** (~20 min)
  - RF: RF-073 a RF-084, RF-001 (soporte).
  - Depende de: T6.1.
  - Hecho cuando: 400, 401, 403, 404, 405, 406, 409, 415 y 500 devuelven `ApiError` sin detalles internos, y Swagger muestra los cinco endpoints con seguridad bearer y sus esquemas.

- [ ] **T6.7 — Confirmar que no cambian esquema ni dependencias** (~5 min)
  - RF: Constitución 3 y 11.
  - Depende de: T6.1.
  - Hecho cuando: `git diff --exit-code -- docs/esquema_creacion_base_datos.sql pom.xml` termina con código 0.

## Criterios globales de finalización

- [ ] Los cinco endpoints responden con los códigos, cuerpos y envoltorios de la spec.
- [ ] La propiedad se resuelve desde el JWT y los recursos ajenos responden 404, también para `ADMIN`.
- [ ] Crear, editar y borrar son transaccionales; no quedan pasos ni asociaciones huérfanas.
- [ ] Las etiquetas se normalizan, reutilizan, deduplican y conservan la `ñ`.
- [ ] El listado pagina de 15 en 15, ordena por `createdAt DESC, id DESC` y no carga pasos.
- [ ] Todos los errores usan `ApiError` y los códigos HTTP coinciden con la spec.
- [ ] `.\mvnw.cmd test -q` y `.\mvnw.cmd spotless:check -q` terminan en verde.
- [ ] La verificación manual contra MySQL de cascadas, `UNIQUE(position)` y 409 concurrente está completada.
- [ ] No se ha modificado el script SQL ni se han añadido dependencias.
