# Plan — Autenticación y usuarios

Plan técnico de implementación para la spec `specs/00-authentication/spec.md`.
Define módulos, modelo de datos, decisiones justificadas y estrategia de
tests. Cada parte indica los RF que cubre. Sin código: el detalle concreto
(clases, firmas, anotaciones) se resuelve en `tasks.md`.

## Decisiones técnicas

| # | Decisión | Alternativa descartada | Por qué | RF |
|---|---|---|---|---|
| D1 | Sesión con **access token JWT** (jjwt 0.12.6) sin estado en servidor | Sesiones guardadas en servidor con cookie | La API es independiente del frontend (SPA + API separada); el backend no necesita guardar estado de sesión | RF-010, RF-012 |
| D2 | **Sin refresh token**: un solo access token con expiración configurable (24h por defecto) | Access + refresh token persistente | El refresh obligaría a persistir tokens y a tocar el esquema SQL (prohibido sin aprobación); el alcance actual no lo requiere | RF-012 |
| D3 | Cifrado con **BCrypt** vía `PasswordEncoder` de Spring Security | Argon2, SHA-256 puro | Argon2 requiere dependencia extra; SHA-256 sin sal es inseguro. BCrypt viene en Spring Security y aplica sal automática | RF-009 |
| D4 | La **unicidad case-insensitive de username/email la garantiza la BD** (colación `utf8mb4_0900_ai_ci` del script existente); no se normaliza en la aplicación | Guardar username/email en minúsculas; comparar en el servicio antes de insertar | El índice único ya compara sin mayúsculas; normalizar en app perdería el formato original mostrado y exigiría lógica duplicada | RF-008 |
| D5 | El registro aplica **trim de espacios exteriores al username en la capa de aplicación** antes de validar y guardar | Trim solo en el controlador; trim en BD | La normalización es una regla de negocio y vive en aplicación (constitución 5); el controlador no debe transformar datos | RF-002 |
| D6 | El login **no aplica trim**; identificador y contraseña se comparan tal cual | Trim automático en login por simetría con el registro | La spec lo prohíbe explícitamente: `  Ana` no es `Ana` | RF-011 |
| D7 | La confirmación de contraseña se valida con **`@AssertTrue`** en el DTO de registro (validación a nivel de clase entre campos) | Comprobación manual en el servicio | Es una validación de entrada, no una regla de negocio; así el servicio solo recibe datos ya coherentes (constitución 6) | RF-005 |
| D8 | El login resuelve el identificador (**username o email**) con **una única consulta** `WHERE username = ? OR email = ?` | Dos consultas consecutivas (buscar por username, luego por email) | La colación `ai_ci` hace ambas comparaciones case-insensitive gratis; una consulta es más simple y hace un solo viaje a BD. Ambos campos son únicos, así que el resultado es a lo sumo uno | RF-010, RF-013 |
| D9 | El 401 de login fallido sale de **una única excepción de dominio** (`InvalidCredentialsException`) con mensaje genérico | Excepciones distintas para «usuario no existe» y «contraseña incorrecta» | Revelaría qué usernames/emails existen (enumeración de usuarios) | RF-013 |
| D10 | El identificador del usuario autenticado viaja en el **JWT** (claims: id, username, email) y `/me` lo lee de ahí sin consultar BD | Consultar la BD en cada validación | Como no hay gestión de cuenta (email/username inmutables), los claims no se desactualizan; se evita un viaje a BD por petición | RF-014, RF-016 |
| D11 | La validación de token la hace un **filtro de seguridad** que puebla el contexto de autenticación; los endpoints protegidos simplemente leen el contexto | Validar el token manualmente en cada controlador/método | Evita repetir la validación en cada endpoint y garantiza un 401 único y coherente para token ausente, inválido o caducado | RF-015, RF-016 |
| D12 | Errores con **excepciones de dominio + `GlobalExceptionHandler`** y cuerpo estructurado (mensaje + campos) | Cuerpos de error por defecto de Spring / respuestas sueltas por controlador | Constitución 7; un único formato facilita el manejo en el frontend | RF-002 a RF-007, RF-013, RF-015 |
| D13 | La expiración del token se configura en `application.yml` (propiedad tipo duración, por ejemplo 24h) | Valor fijo en código | RF-012 exige que sea ajustable sin cambiar código | RF-012 |

## Modelo de datos

### BD (script existente, sin cambios — constitución 3)

Tabla `users` ya definida en `esquema_creacion_base_datos.sql`:

| Columna | Tipo | Observaciones |
|---|---|---|
| `id` | BIGINT UNSIGNED PK | Autoincremental |
| `username` | VARCHAR(255) NOT NULL, UNIQUE | La comparación es case-insensitive por la colación de la tabla |
| `email` | VARCHAR(255) NOT NULL, UNIQUE | Igual que username |
| `password_hash` | VARCHAR(255) NOT NULL | Cabe el hash BCrypt (60 chars) |
| `created_at` | TIMESTAMP | Default `CURRENT_TIMESTAMP` |

No se crea ninguna tabla nueva: no hay refresh tokens ni sesiones.

### Modelo de dominio

- **`User`** (domain/model): id, username, email, passwordHash, createdAt.
  POJO con Lombok, sin anotaciones JPA (la entidad JPA va en el adaptador).

### DTOs (application/dto/auth)

| DTO | Contenido | RF |
|---|---|---|
| `RegisterRequest` | username, email, password, confirmPassword | RF-001 |
| `LoginRequest` | identifier (username o email), password | RF-010 |
| `AuthResponse` | accessToken (string) | RF-010 |
| `UserResponse` | id, username, email | RF-014 |

### Token (claims)

| Claim | Contenido | RF |
|---|---|---|
| subject | id del usuario | RF-014, RF-016 |
| username | username visible | RF-014 |
| email | email | RF-014 |
| exp | fecha de expiración (configurable) | RF-012 |

## Módulos y estructura

Siguiendo la estructura hexagonal de la skill, con los paquetes de
`com.testcasemanager`:

### Dominio (`domain/`)

| Módulo | Responsabilidad | RF |
|---|---|---|
| `model/User` | Modelo de usuario del negocio | — |
| `port/in/RegisterUseCase` | Contrato: registrar usuario | RF-001 |
| `port/in/LoginUseCase` | Contrato: iniciar sesión | RF-010 |
| `port/in/SessionUseCase` | Contrato: obtener usuario del token actual | RF-014 |
| `port/out/UserRepository` | Contrato: buscar y guardar usuarios | RF-006, RF-007, RF-010 |
| `port/out/TokenService` | Contrato: generar y validar tokens | RF-010, RF-012, RF-015 |
| `exception/UserAlreadyExistsException` | Username o email duplicado | RF-006, RF-007 |
| `exception/InvalidCredentialsException` | Login fallido (mensaje genérico) | RF-013 |
| `exception/InvalidTokenException` | Token ausente/inválido/caducado | RF-015 |

### Aplicación (`application/`)

| Módulo | Responsabilidad | RF |
|---|---|---|
| `dto/auth/*` | DTOs de entrada y salida (tabla anterior) | RF-001, RF-010, RF-014 |
| `dto/error/ApiError`, `dto/error/FieldError` | Cuerpo de error estructurado | RF-002 a RF-007 |
| `service/AuthService` | Casos de uso: valida entrada normalizada, comprueba duplicados, cifra con BCrypt, busca por identificador, genera/valida tokens | RF-001 a RF-014 |

### Infraestructura (`infraestructure/`)

| Módulo | Responsabilidad | RF |
|---|---|---|
| `adapter/in/rest/AuthController` | `POST /api/auth/register` (201), `POST /api/auth/login` (200), `GET /api/auth/me` (200) | RF-001, RF-010, RF-014 |
| `adapter/out/persistence/UserJpaEntity`, `UserJpaRepository`, `UserRepositoryAdapter` | Adaptador JPA que implementa `UserRepository` | RF-006 a RF-010 |
| `security/JwtTokenService` | Implementa `TokenService` con jjwt: generar, parsear, detectar caducidad | RF-010, RF-012, RF-015 |
| `security/JwtAuthenticationFilter` | Extrae el token de la cabecera, lo valida y puebla el contexto; sin token válido no continúa (401) | RF-015, RF-016 |
| `security/SecurityConfig` | Reglas de seguridad: `/api/auth/register` y `/api/auth/login` públicos, el resto autenticado; bean del `PasswordEncoder` | RF-001, RF-009, RF-016 |
| `config/OpenApiConfig` | Swagger con esquema de seguridad JWT | — |
| `mapper/UserMapper` | MapStruct: entidad JPA ↔ modelo dominio ↔ DTOs | — |
| `exception/GlobalExceptionHandler` | Convierte excepciones de dominio y errores de validación en respuestas 400/401/409 con `ApiError` | RF-002 a RF-007, RF-013, RF-015 |

### Validaciones de entrada (sobre los DTOs, con `spring-boot-starter-validation`)

| Regla | Mecanismo previsto | RF |
|---|---|---|
| Username obligatorio, 3–50, sin espacios exteriores | `@NotBlank` + `@Size` + trim en el servicio antes de validar/guardar | RF-002 |
| Email obligatorio y con formato | `@NotBlank` + `@Email` | RF-003 |
| Contraseña 8–20, no en blanco, sin espacios | `@NotBlank` + `@Size(min=8, max=20)` + `@Pattern` sin espacios | RF-004 |
| Confirmación igual a contraseña | `@AssertTrue` a nivel de clase en `RegisterRequest` | RF-005 |

## Estrategia de tests

Solo unit tests (JUnit 5 + Mockito), según decisión de proyecto. Sin
integración con BD: el comportamiento de la colación y de los índices únicos
se asume cubierto por el script SQL validado.

| Test | Qué verifica | RF |
|---|---|---|
| `AuthServiceTest` — registro | Crea el usuario con la contraseña cifrada (nunca el texto plano llega al repositorio) y sin espacios exteriores en el username | RF-001, RF-002, RF-009 |
| `AuthServiceTest` — duplicados | Username o email existente lanza `UserAlreadyExistsException` antes de guardar | RF-006, RF-007 |
| `AuthServiceTest` — login ok | Identificador por username o por email devuelve token con los claims correctos | RF-010 |
| `AuthServiceTest` — login literal | `  Ana` no coincide con `Ana` (no se recorta) | RF-011 |
| `AuthServiceTest` — login fallido | Usuario inexistente y contraseña incorrecta lanzan la misma `InvalidCredentialsException` | RF-013 |
| `JwtTokenServiceTest` | Generar y validar token (id, username, email); token caducado y token manipulado se rechazan igual | RF-012, RF-014, RF-015 |
| `RegisterRequestValidationTest` | Validación bean de cada regla: username corto/en blanco, email inválido, contraseña con espacios o fuera de rango, confirmación distinta | RF-002 a RF-005 |
| `AuthControllerTest` | Códigos de estado: 201 registro, 200 login y `/me`, 400 validación con campos, 409 duplicado, 401 credenciales/token | RF-001, RF-010, RF-014 |
| `GlobalExceptionHandlerTest` | Cada excepción de dominio produce el `ApiError` y código esperados, sin filtrar detalles internos | RF-002 a RF-007, RF-013, RF-015 |

**Cobertura de RF**: RF-001 (AuthService, Controller), RF-002 (validación, trim), RF-003
(email), RF-004 (contraseña), RF-005 (`@AssertTrue`), RF-006/RF-007
(duplicados), RF-008 (BD; sin test unitario posible — verificado a nivel de
script), RF-009 (cifrado), RF-010 (login), RF-011 (literal), RF-012
(expiración), RF-013 (genérico), RF-014 (claims), RF-015 (filtro/token),
RF-016 (SecurityConfig + filtro). El único RF sin test unitario es RF-008,
que depende de la colación de MySQL y queda fuera del alcance de los tests
unitarios decididos para el proyecto.

## Puntos abiertos

- El manejo de la carrera de registro simultáneo (dos registros iguales a la
  vez) quedó descartado como caso límite; el índice único de BD es la última
  línea de defensa, pero su fallo podría producir un 500 si no se traduce a
  409. Se asume aceptado hasta que se decida lo contrario.
