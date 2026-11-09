# Tasks — Autenticación y usuarios

Tareas derivadas de `plan.md`, ordenadas por dependencia, cada una de menos
de 30 minutos con su RF y su criterio «Hecho cuando:» verificable.
Convención: `[ ]` pendiente → `[x]` hecho y verificado.

## Fase 0 — Base del proyecto

- [x] **T1. Esqueleto Maven y estructura de paquetes**
  - Crea `pom.xml` (Java 21, padre Spring Boot 4.1.1) con las dependencias de
    la constitución: web, security, validation, data-jpa, mysql, jjwt
    0.12.6, mapstruct, lombok, springdoc, spotless. Crea los paquetes
    vacíos hexagonales (`domain/model|port/in|port/out|exception`,
    `application/dto|service`, `infraestructure/adapter|config|mapper|security|exception`).
  - RF: — (base)
  - Hecho cuando: `mvn -q compile` termina sin errores y la distribución de
    carpetas coincide con la skill hexagonal.

- [x] **T2. Configuración base (`application.properties`)**
  - Propiedades de datasource (MySQL), `ddl-auto: none`, secret JWT (variable
    de entorno con valor por defecto de desarrollo) y expiración JWT
    configurable (24h por defecto).
  - Desviación aprobada: se conserva `application.properties` con el
    datasource ya configurado por el equipo (sin placeholders ni
    `application.yml`) y las claves `app.jwt.*` se añaden en ese fichero.
  - RF: RF-012
  - Hecho cuando: el fichero contiene la propiedad de expiración del token
    con valor 24h y el secret sale de variable de entorno; el arranque no
    pide tocar código para cambiar la expiración.

## Fase 1 — Dominio

- [x] **T3. Modelo `User`**
  - POJO de dominio con Lombok (`id`, `username`, `email`, `role`,
    `passwordHash`, `createdAt`), sin anotaciones JPA.
  - RF: — (soporte de todos)
  - Hecho cuando: la clase compila sin imports de infraestructura ni JPA.

- [x] **T4. Excepciones de dominio**
  - `InvalidUsernameException` (username ya registrado),
    `InvalidEmailException` (email ya registrado),
    `InvalidCredentialsException` e `InvalidTokenException`, todas con
    mensaje en español.
  - RF: RF-006, RF-007, RF-013, RF-015
  - Hecho cuando: las cuatro existen en `domain/exception` y llevan mensaje
    en español.

- [x] **T5. Puertos de entrada (`port/in`)**
  - Interfaces `RegisterUseCase`, `LoginUseCase`, `SessionUseCase` con los
    DTOs de aplicación como parámetros/retorno.
  - RF: RF-001, RF-010, RF-014
  - Hecho cuando: las tres interfaces existen y solo referencian DTOs de
    `application/dto` o modelos de dominio.

- [x] **T6. Puertos de salida (`port/out`)**
  - Interfaces `UserRepository` (buscar por id, guardar, buscar por
    username-or-email) y `TokenService` (generar token, extraer datos del
    usuario de un token).
  - RF: RF-006, RF-007, RF-010, RF-012, RF-015
  - Hecho cuando: `UserRepository` expone la búsqueda por identificador
    único (username o email) y `TokenService` expone generar y validar.

## Fase 2 — Aplicación

- [x] **T7. DTOs de autenticación con validaciones**
  - `RegisterRequest` (username `@NotBlank`+`@Size(3,50)`, email
    `@NotBlank`+`@Email`, password `@NotBlank`+`@Size(8,20)`+`@Pattern` sin
    espacios, `@AssertTrue` de confirmación), `LoginRequest`, `AuthResponse`,
    `UserResponse`.
  - RF: RF-001, RF-002, RF-003, RF-004, RF-005
  - Hecho cuando: `RegisterRequestValidationTest` en verde: rechaza username
    corto/en blanco, email inválido, contraseña con espacios o de 7 y 21
    caracteres, y confirmación distinta; acepta un registro válido.

- [x] **T8. DTOs de error**
  - `ApiError` (mensaje general) y `FieldError` (campo + mensaje), con Lombok.
  - RF: RF-002 a RF-007
  - Hecho cuando: los dos DTOs compilan en `application/dto/error` y un
    `ApiError` puede llevar lista de `FieldError`.

- [x] **T9. `AuthService` — registro**
  - Implementa `RegisterUseCase`: guarda el username tal cual llega (sin
    trim), cifra la contraseña con `PasswordEncoder` y lanza
    `InvalidUsernameException` o `InvalidEmailException` si el username o
    el email ya existen.
  - RF: RF-001, RF-002, RF-006, RF-007, RF-009
  - Hecho cuando: `AuthServiceTest` en verde para registro: el repositorio
    recibe el username exactamente como se envió (con espacios si los
    trae) y la contraseña NO llega en texto plano; cada duplicado lanza su
    excepción sin llegar a guardar.

- [x] **T10. `AuthService` — login y sesión**
  - Implementa `LoginUseCase` (delega la verificación en el puerto
    `CredentialsAuthenticator`, sin trim, y genera el token; fallo →
    `InvalidCredentialsException` con mensaje genérico) y `SessionUseCase`
    (extrae usuario del token; fallo → `InvalidTokenException`).
  - Nota: el login usa la cadena de Spring Security (`AuthenticationManager`
    → `DaoAuthenticationProvider` → `JpaUserDetailsService.loadUserByUsername`
    + `PasswordEncoder`), no una comparación manual en el servicio.
  - RF: RF-010, RF-011, RF-013, RF-014, RF-015
  - Hecho cuando: `AuthServiceTest` en verde para login: username y email
    funcionan, `  Ana` contra `Ana` lanza `InvalidCredentialsException`,
    usuario inexistente y contraseña mala lanzan la MISMA excepción.

## Fase 3 — Persistencia

- [x] **T11. Adaptador JPA de usuarios**
  - `UserJpaEntity` (mapea la tabla `users`), `UserJpaRepository` (Spring
    Data, con la consulta `WHERE username = ? OR email = ?`) y
    `UserRepositoryAdapter` que implementa el puerto `UserRepository`.
  - RF: RF-006, RF-007, RF-010
  - Hecho cuando: el adaptador compila implementando `UserRepository`, la
    entidad no tiene lógica de negocio y el nombre de la tabla/columnas
    coincide con el script SQL.

## Fase 4 — Seguridad

- [x] **T12. `JwtTokenService`**
  - Implementa `TokenService` con jjwt: genera el token con claims
    (id, username, email, role) y expiración de la propiedad de configuración;
    valida y extrae el usuario; token caducado o manipulado →
    `InvalidTokenException`.
  - RF: RF-010, RF-012, RF-014, RF-015
  - Hecho cuando: `JwtTokenServiceTest` en verde: el token generado devuelve
    los claims correctos, y un token caducado y uno manipulado se rechazan
    ambos con `InvalidTokenException`.

- [x] **T13. Filtro JWT y `SecurityConfig`**
  - Filtro que lee la cabecera `Authorization`, valida y puebla el contexto
    (sin token válido no continúa); cadena de seguridad con
    `/api/auth/register` y `/api/auth/login` públicos, resto autenticado,
    sesión stateless y bean `BCryptPasswordEncoder`.
  - RF: RF-009, RF-015, RF-016
  - Hecho cuando: la configuración declara los dos endpoints públicos, el
    resto requiere autenticación y existe el bean del `PasswordEncoder`.
  - Nota: se añade `JpaUserDetailsService`, un adaptador que carga el usuario
    desde la BD, para que Boot no cree su usuario en memoria con contraseña
    temporal; `httpBasic` y `formLogin` quedan desactivados.

## Fase 5 — Web

- [x] **T14. `AuthController`**
  - `POST /api/auth/register` → 201 sin token; `POST /api/auth/login` →
    200 con `AuthResponse`; `GET /api/auth/me` → 200 con `UserResponse`
    desde el contexto de autenticación. Entradas con `@Valid`.
  - RF: RF-001, RF-010, RF-014
  - Hecho cuando: `AuthControllerTest` en verde: 201 registro, 200 login,
    200 `/me` con un token Bearer válido (token → filtro → usuario), y el
    body de login contiene el token.

- [x] **T15. `GlobalExceptionHandler`**
  - Convierte: errores de validación → 400 con `ApiError` y `FieldError`;
    `InvalidUsernameException` e `InvalidEmailException` → 409 con el
    campo afectado; `InvalidCredentialsException` e `InvalidTokenException`
    → 401 con mensaje genérico.
  - RF: RF-002 a RF-007, RF-013, RF-015
  - Hecho cuando: `GlobalExceptionHandlerTest` en verde: cada excepción
    produce su código (400/401/409) con `ApiError` y sin stack trace.

## Fase 6 — Documentación y cierre

- [x] **T16. OpenAPI con seguridad JWT**
  - `OpenApiConfig` con esquema de seguridad bearer para probar los
    endpoints protegidos en Swagger.
  - RF: — (soporte)
  - Hecho cuando: Swagger UI muestra los tres endpoints de auth y permite
    autorizarse con un token.

- [x] **T17. Verificación final de la spec**
  - Ejecutar `mvn test` completo, repasar el checklist de la skill
    hexagonal y los criterios de finalización de la spec uno a uno.
  - RF: todos
  - Hecho cuando: `mvn test` está en verde, el checklist de la skill está
    verificado y los criterios de finalización de `spec.md` están marcados
    (excepto RF-008, documentado como cubierto por el script).

## Fase 7 — Roles

- [x] **T18. Rol de usuario (`USER`/`ADMIN`)**
  - Enum `Role`, columna `users.role` (`VARCHAR(20) NOT NULL DEFAULT 'USER'`
    con `CHECK`), registro siempre con `USER`, autoridad
    `ROLE_USER`/`ROLE_ADMIN`, claim `role` en el JWT y `role` en
    `UserResponse`.
  - Nota: el rol `ADMIN` se asigna manualmente
    (`UPDATE users SET role = 'ADMIN' WHERE username = '...';`). En una base
    ya creada hay que aplicar antes de arrancar:
    `ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER'
    AFTER password_hash, ADD CONSTRAINT chk_users_role CHECK (role IN
    ('ADMIN', 'USER'));`
  - RF: RF-017
  - Hecho cuando: los tests verifican que el registro crea `USER`, que
    `SecurityUser` expone la autoridad del rol y que el token conserva
    `ADMIN`/`USER`.
