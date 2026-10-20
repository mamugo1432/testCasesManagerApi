---
name: hexagonal-architecture
description: Úsala siempre que crees, modifiques o revises código en el backend de TestCase Manager. Define dónde va cada cosa, cómo crear casos de uso, endpoints y qué dependencias usar.
---

# Arquitectura Hexagonal — TestCase Manager Backend

El backend sigue una arquitectura hexagonal estricta. No mezcles capas ni saltes
reglas de dependencia: el dominio no conoce la infraestructura, y la aplicación
solo habla a través de puertos.

## Estructura de paquetes

```
src/main/java/com/testcasemanager/
├── domain/                    → Núcleo del negocio
│   ├── model/                 → Entidades (User, Project, TestCase, TestStep, Tag)
│   │                            y enums (Priority, Status)
│   ├── port/
│   │   ├── in/                → Interfaces de casos de uso
│   │   │                        (RegisterUseCase, ProjectUseCase, TestCaseUseCase,
│   │   │                         TagUseCase, StatsUseCase...)
│   │   └── out/               → Interfaces de repositorios y servicios externos
│   │                        (UserRepository, ProjectRepository, TestCaseRepository,
│   │                         TestStepRepository, TagRepository, TokenService...)
│   └── exception/             → Excepciones del dominio
│                            (ProjectNotFoundException, TestCaseNotFoundException,
│                             InvalidStatusTransitionException...)
├── application/               → Capa de aplicación
│   ├── dto/
│   │   ├── auth/              → DTOs de autenticación
│   │   ├── project/           → DTOs de proyectos
│   │   ├── testcase/          → DTOs de test cases y pasos
│   │   ├── tag/               → DTOs de etiquetas
│   │   ├── stats/             → DTOs del dashboard de estadísticas
│   │   └── error/             → DTOs de error
│   └── service/               → Implementación de casos de uso
└── infraestructure/           → Adaptadores
    ├── adapter/
    │   ├── in/rest/           → Controladores REST
    │   │                        (AuthController, ProjectController, TestCaseController,
    │   │                         TagController, StatsController)
    │   └── out/persistence/   → Adaptadores JPA (entidades JPA y repositorios
    │                            Spring Data que implementan los puertos de salida)
    ├── config/                → Configuración (SecurityConfig, OpenApiConfig, CorsConfig)
    ├── mapper/                → Mapeadores (MapStruct)
    ├── security/              → Adaptadores de seguridad (JWT, BCrypt)
    │                            (TokenServiceAdapter, JwtAuthenticationFilter)
    └── exception/             → Manejador global de excepciones (GlobalExceptionHandler)
```

## Reglas

- Los servicios implementan interfaces de `domain/port/in/`.
- Los adapters implementan interfaces de `domain/port/out/`.
- Los DTOs viven en `application/dto/`, organizados por funcionalidad.
- Los nombres de DTOs deben coincidir exactamente entre backend y frontend.
- Nunca importes clases de `infraestructure/` en `domain/` o `application/`.
- Usa Lombok (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`) en
  DTOs y servicios. En entidades JPA usa `@Getter`, `@Setter` y `@NoArgsConstructor`
  (JPA exige un constructor sin argumentos).
- Usa MapStruct para mapear entre entidades JPA y modelos de dominio, y entre
  modelos de dominio y DTOs.
- Los modelos de dominio de `domain/model/` son POJOs con Lombok, sin anotaciones
  JPA: la persistencia vive solo en `adapter/out/persistence/`.
- El usuario autenticado se obtiene del contexto de seguridad (JWT), nunca de la
  URL. Toda operación sobre proyectos o test cases verifica la propiedad
  (404 si no es del usuario).
- La normalización de etiquetas (trim → minúsculas → sin diacríticas) se hace en
  el backend, en el dominio/aplicación, nunca en el cliente.

## Reglas de negocio transversales

- `Status`: solo `IN_PROGRESS` y `COMPLETED`; un test case nuevo empieza en
  `IN_PROGRESS`. Para pasar a `COMPLETED` debe tener al menos un paso y un
  `expectedResult` no vacío (lanzar excepción de dominio si no se cumple).
- `Priority`: `HIGH`, `MEDIUM`, `LOW`, obligatoria.
- Las etiquetas son globales: se reutilizan por `normalized_name` único; ante una
  carrera de creación, reintentar buscando la existente.
- El borrado de proyecto elimina test cases, pasos y asociaciones de etiquetas
  (cascada transaccional), pero nunca las etiquetas globales.
- Las estadísticas se calculan al vuello, nunca se guardan precalculadas.

## Cómo crear un nuevo caso de uso

1. Crear interfaz en `domain/port/in/` (ej: `CreateProjectUseCase.java`).
2. Crear servicio en `application/service/` que implemente la interfaz.
3. Crear DTOs en `application/dto/` si son necesarios.
4. Usar puertos (`domain/port/out/`) para acceso a datos o servicios externos.
5. Registrar el servicio como `@Service` con `@RequiredArgsConstructor`.

## Cómo crear un nuevo endpoint

1. Crear o actualizar el controlador en `infraestructure/adapter/in/rest/`.
2. Usar `@Valid @RequestBody` para validar la entrada.
3. Devolver `ResponseEntity<T>` con el código de estado apropiado.
4. Manejar errores con excepciones del dominio + `GlobalExceptionHandler`.

## Dependencias clave

| Dependencia | Uso |
|---|---|
| Spring Security | Autenticación y autorización |
| JWT (jjwt 0.12.6) | Tokens de autenticación (access token + `GET /api/auth/me`) |
| MapStruct 1.6.3 | Mapeo entre entidades y DTOs |
| Lombok | Reducción de boilerplate |
| Hibernate Validator | Validación de DTOs (`@Valid`) |
| MySQL (driver) | Base de datos |
| Hibernate validate | `ddl-auto=none`; el esquema lo define el script SQL externo (fuente de verdad). Sin Flyway |
| springdoc-openapi | Documentación de la API (Swagger UI) |

## Checklist de revisión

- [ ] ¿El servicio implementa una interfaz de `domain/port/in/`?
- [ ] ¿Los adapters implementan interfaces de `domain/port/out/`?
- [ ] ¿Los DTOs están en `application/dto/`, en su subcarpeta de funcionalidad?
- [ ] ¿Los nombres de DTOs coinciden con el frontend?
- [ ] ¿No hay imports de `infraestructure/` en `domain/` o `application/`?
- [ ] ¿Se usan anotaciones de Lombok correctamente (según tipo de clase)?
- [ ] ¿Las excepciones son del dominio, no genéricas de Java?
- [ ] ¿Se verifica la propiedad del usuario en toda operación sobre recursos?
- [ ] ¿La lógica de negocio (estado COMPLETED, normalización de etiquetas) vive
      en dominio/aplicación y no en el controlador?

## Al terminar

Indica qué puntos del checklist has comprobado y cómo.
