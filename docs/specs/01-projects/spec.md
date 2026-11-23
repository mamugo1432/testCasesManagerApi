# Spec 01 — Gestión de proyectos

## Contexto y objetivo

TestCase Manager organiza los casos de prueba dentro de proyectos. Cada
proyecto pertenece a un único usuario y agrupa sus test cases. Esta spec
define la creación, el listado, la consulta de detalle y el borrado de
proyectos, que son las operaciones incluidas en el alcance actual. El
objetivo es que un usuario autenticado pueda gestionar únicamente sus
propios proyectos, con un contrato REST claro y sin revelar la existencia
de proyectos ajenos. El borrado debe eliminar en cascada los test cases y
sus datos dependientes, sin borrar las etiquetas globales.

Para mantener el listado pequeño y evitar paginación en esta iteración,
cada usuario puede poseer como máximo 10 proyectos, según
`alcance_proyecto.md` §3.2 y §5. Si alcanza el límite, debe eliminar uno
antes de crear otro. El límite se aplica en el backend, es configurable
con 10 por defecto y no elimina automáticamente proyectos ya existentes.

## Usuarios / actores

- **Usuario autenticado (`USER` o `ADMIN`)**: crea, lista, consulta y
  elimina sus propios proyectos, con un máximo de 10. En esta
  funcionalidad no hay diferencias de permisos entre roles.
- **Visitante no autenticado**: no puede acceder a ningún endpoint de
  proyectos; recibe 401.
- **Backend**: valida la entrada, resuelve el propietario desde el JWT,
  aplica la regla de propiedad y el límite de proyectos, y persiste los
  cambios.

## Historias de usuario

- H1: Como usuario autenticado quiero crear un proyecto indicando solo su
  título para agrupar los test cases de una misma aplicación o
  funcionalidad.
- H2: Como usuario autenticado quiero ver el listado de mis proyectos para
  elegir en cuál trabajar.
- H3: Como usuario autenticado quiero consultar el detalle de un proyecto
  para entrar en él y conocer sus datos básicos.
- H4: Como usuario autenticado quiero eliminar un proyecto para borrar
  también sus test cases y dejar de verlo en mi listado.
- H5: Como usuario autenticado quiero que mis proyectos sean privados para
  que otros usuarios no puedan verlos ni eliminarlos.
- H6: Como usuario autenticado quiero recibir un error claro cuando el
  título no sea válido o el proyecto no exista.
- H7: Como usuario autenticado quiero saber cuándo he alcanzado el límite
  de proyectos para eliminar uno antes de crear otro.

## Requisitos funcionales (criterios de aceptación en EARS)

### Contrato de API

| Método | Ruta | Autenticación | Cuerpo | Respuesta correcta | Errores |
|---|---|---|---|---|---|
| POST | `/api/projects` | Bearer JWT (`USER`/`ADMIN`) | `{ "title": "..." }` | 201 `{id, title, createdAt}` | 400, 401, 403, 405, 409, 415, 500 |
| GET | `/api/projects` | Bearer JWT (`USER`/`ADMIN`) | — | 200 `[{id, title, createdAt}]` | 401, 403, 405, 500 |
| GET | `/api/projects/{id}` | Bearer JWT (`USER`/`ADMIN`) | — | 200 `{id, title, createdAt}` | 400, 401, 403, 404, 405, 500 |
| DELETE | `/api/projects/{id}` | Bearer JWT (`USER`/`ADMIN`) | — | 204 sin cuerpo | 400, 401, 403, 404, 405, 500 |

El campo `createdAt` se devuelve con formato `dd/MM/yyyy` en la zona
horaria del servidor. El campo `user_id` no se expone en ninguna respuesta.
El 409 por límite de proyectos lleva el mensaje «No puedes crear más de
{límite} proyectos. Elimina uno para crear otro.».

### Autenticación y propiedad

- **RF-001** — EL SISTEMA exigirá un token JWT válido en todos los
  endpoints de proyectos. SI el token falta, no es válido o ha caducado,
  ENTONCES responderá 401 con el formato de error existente.
- **RF-002** — EL SISTEMA obtendrá el id del propietario del contexto de
  seguridad (claims del JWT), nunca de la URL ni del cuerpo de la petición.
- **RF-003** — EL SISTEMA solo devolverá y modificará proyectos cuyo
  `user_id` coincida con el usuario autenticado. La regla se aplica también
  a los usuarios con rol `ADMIN`. Además, los endpoints exigen un rol
  `USER` o `ADMIN` en `SecurityFilterChain`; un rol no autorizado recibe
  403.
- **RF-004** — SI el proyecto solicitado no existe o pertenece a otro
  usuario, ENTONCES responderá 404 con un mensaje genérico idéntico en
  ambos casos, sin revelar si el proyecto existe.

### Creación

- **RF-005** — CUANDO un usuario autenticado envíe `POST /api/projects`
  con un título válido, EL SISTEMA creará un proyecto asociado a su id y
  responderá 201 con `{id, title, createdAt}`.
- **RF-006** — EL SISTEMA guardará el título tal cual llega, sin recortar
  los espacios exteriores ni normalizarlo; los espacios interiores también
  se conservan.
- **RF-007** — SI el título falta, está en blanco o está compuesto solo por
  espacios, ENTONCES responderá 400 con un error estructurado en el campo
  `title`.
- **RF-008** — SI el título supera los 255 caracteres, ENTONCES responderá
  400 con un error estructurado en el campo `title`. La longitud se mide en
  unidades UTF-16 (`String.length()` / `@Size`) sobre el valor tal cual se
  recibe; un emoji cuenta como 2 unidades y un grafema compuesto cuenta por
  todas sus unidades, no como un solo carácter visible. Este límite es más
  estricto que `VARCHAR(255)` de MySQL, por lo que nunca se desborda la
  columna.
- **RF-009** — EL SISTEMA asignará `createdAt` en el servidor en el
  momento de la creación; el cliente no puede fijarlo. Se devuelve con
  formato `dd/MM/yyyy`.
- **RF-010** — EL SISTEMA aceptará títulos repetidos, incluso dentro del
  mismo usuario, porque el esquema no impone unicidad sobre
  `projects.title`.
- **RF-011** — EL SISTEMA solo persistirá el campo `title`; cualquier otro
  campo enviado en el cuerpo se ignorará.
- **RF-012** — SI el usuario autenticado ya posee el número máximo de
  proyectos permitido o más, ENTONCES lanzará
  `ProjectLimitExceededException` y responderá 409 con el mensaje «No
  puedes crear más de {límite} proyectos. Elimina uno para crear otro.»,
  usando el límite vigente.
- **RF-013** — EL SISTEMA obtendrá el límite de la propiedad de
  configuración `app.projects.max-per-user`, con valor por defecto 10, y
  lo aplicará a todos los roles, incluido `ADMIN`.
- **RF-014** — EL SISTEMA comprobará el límite con un recuento previo a la
  inserción, dentro de la misma transacción que la creación. La comprobación
  es best-effort: si dos creaciones del mismo usuario llegan a la vez con el
  límite ya alcanzado, ambas podrían superar la comprobación y crear sendos
  proyectos, dejando el total por encima del límite (hasta el límite + 2 en
  el peor caso). Este caso se acepta y se documenta; no se garantiza el
  límite bajo concurrencia.
- **RF-015** — EL SISTEMA no eliminará automáticamente los proyectos
  existentes si el límite se reduce o se activa por primera vez; el usuario
  deberá borrarlos manualmente hasta quedar por debajo del límite. Mientras
  tenga más proyectos que el límite, podrá listarlos y borrarlos, pero no
  crear nuevos.

### Listado

- **RF-016** — CUANDO un usuario autenticado solicite `GET /api/projects`,
  EL SISTEMA devolverá 200 con la lista de todos sus proyectos, cada uno
  con `{id, title, createdAt}`, aunque su número supere el límite vigente.
- **RF-017** — EL SISTEMA ordenará el listado por el timestamp de creación
  descendente y, en caso de empate, por `id` descendente, para que el
  resultado sea determinista. El orden se calcula sobre el valor almacenado,
  no sobre la fecha formateada `dd/MM/yyyy`.
- **RF-018** — SI el usuario no tiene proyectos, ENTONCES responderá 200
  con una lista vacía.
- **RF-019** — EL SISTEMA no incluirá en el listado proyectos de otros
  usuarios ni contadores o datos de test cases.

### Detalle

- **RF-020** — CUANDO un usuario autenticado solicite
  `GET /api/projects/{id}` de un proyecto propio, EL SISTEMA responderá
  200 con `{id, title, createdAt}`.
- **RF-021** — SI `{id}` no corresponde a un proyecto propio, ENTONCES
  responderá 404 (ver RF-004).

### Borrado

- **RF-022** — CUANDO un usuario autenticado solicite
  `DELETE /api/projects/{id}` de un proyecto propio, EL SISTEMA eliminará
  el proyecto y responderá 204 sin cuerpo.
- **RF-023** — CUANDO se elimine un proyecto, la clave foránea
  `ON DELETE CASCADE` de `test_cases.project_id` eliminará todos sus test
  cases, y las de `test_steps` y `test_case_tags` eliminarán sus pasos y
  asociaciones de etiquetas. El caso de uso solo elimina la fila del
  proyecto dentro de una transacción; la cascada física la garantiza la
  base de datos y no se dejan datos huérfanos.
- **RF-024** — EL SISTEMA no eliminará las etiquetas globales al borrar un
  proyecto.
- **RF-025** — SI el proyecto no existe o no es del usuario, ENTONCES
  responderá 404 y no modificará ningún dato.
- **RF-026** — EL SISTEMA ejecutará el borrado de forma transaccional: o
  se elimina el proyecto con todos sus datos dependientes, o no se elimina
  nada.
- **RF-027** — CUANDO se elimine un proyecto correctamente, EL SISTEMA
  liberará su hueco para que el usuario pueda crear otro hasta el límite.

### Errores y validación

- **RF-028** — EL SISTEMA devolverá todos los errores con el formato
  estructurado existente (`ApiError` con `message` y `fields`), sin stack
  traces ni detalles internos, usando el código HTTP adecuado:
  - 400 para validación del cuerpo, JSON malformado o `id` de ruta no
    numérico.
  - 401 para token ausente, inválido o caducado.
  - 404 para proyecto inexistente o ajeno.
  - 405 para método HTTP no soportado en la ruta.
  - 409 para límite de proyectos alcanzado.
  - 415 para `Content-Type` no soportado en `POST`.
  - 500 para cualquier error no controlado, con mensaje genérico.
- **RF-029** — SI el identificador de la ruta no es un número entero
  válido, ENTONCES responderá 400 con el formato de error estructurado.

## Requisitos no funcionales

- **Seguridad**: API stateless con JWT; la autorización por rol se centraliza
  en `SecurityFilterChain` y toda operación verifica la propiedad del usuario
  autenticado; se responde 404, nunca 403, si el recurso no existe o es
  ajeno; no se expone `user_id`.
- **Consistencia**: el borrado se apoya en las claves foráneas con
  `ON DELETE CASCADE` del esquema y se ejecuta en una transacción; no se
  dejan test cases, pasos ni asociaciones huérfanas. El límite de
  proyectos se comprueba dentro de la misma transacción que la creación.
- **Rendimiento**: el listado y el recuento de proyectos se resuelven con
  consultas filtradas por `user_id`, apoyadas en `idx_projects_user_id`;
  no hay N+1. En esta iteración no hay paginación.
- **Configuración**: el límite se define en `app.projects.max-per-user`
  (10 por defecto) para ajustarlo sin cambiar código; no se usa un literal
  disperso.
- **Contrato**: JSON con nombres en camelCase (`createdAt`); fechas en
  formato `dd/MM/yyyy` en la zona horaria del servidor.
- **Idioma**: mensajes de error y validación en español; identificadores,
  clases y paquetes en inglés.
- **Arquitectura**: hexagonal estricta; `domain/` y `application/` no
  importan de `infraestructure/`; puertos de entrada y salida; MapStruct y
  Lombok según la skill. La regla del límite de proyectos se implementa en
  dominio/aplicación (caso de uso y puerto de salida), nunca en el
  controlador ni en el adaptador web. La cascada de borrado la ejecuta la
  base de datos mediante `ON DELETE CASCADE`; el caso de uso solo verifica
  la propiedad y elimina el proyecto.
- **Validación**: siempre en el backend con `@Valid` y DTOs.
- **Documentación**: los cuatro endpoints se documentan con springdoc y
  aparecen en Swagger con el esquema de seguridad bearer.
- **Pruebas**: tests unitarios con JUnit 5 y Mockito; `mvn test` en verde.
  El límite se verifica mockeando 10 proyectos en el puerto de salida y
  comprobando que la creación del undécimo lanza
  `ProjectLimitExceededException`; el borrado en cascada se verifica de
  forma manual contra el esquema, porque depende de las claves foráneas de
  la base de datos.
- **Base de datos**: no se modifica el script SQL; se usa la tabla
  `projects` existente. El límite no se implementa con una columna nueva.

## Casos límite

- Título vacío o compuesto solo por espacios: 400.
- Título con espacios exteriores: se conserva tal cual, incluidos los
  espacios iniciales y finales.
- Título de 255 caracteres: aceptado; 256: 400.
- Título con espacios interiores o saltos de línea: se conservan tal cual.
- Título con mayúsculas, minúsculas o tildes: se conserva tal cual; no se
  normaliza.
- Dos proyectos con el mismo título, incluso del mismo usuario: permitido.
- Usuario sin proyectos: 200 con lista vacía.
- Usuario con 9 proyectos: puede crear el décimo.
- Usuario con 10 proyectos: la creación responde 409 con el mensaje «No
  puedes crear más de 10 proyectos. Elimina uno para crear otro.»; tras
  borrar uno, puede crear otro.
- Usuario con más de 10 proyectos por una reducción o activación inicial
  del límite: puede listarlos y borrarlos, pero no crear hasta bajar del
  límite.
- Título inválido y límite alcanzado a la vez: prevalece el 400 de
  validación del título.
- Dos creaciones simultáneas del mismo usuario en el límite: ambas podrían
  superar la comprobación y crear sendos proyectos, dejando el total por
  encima del límite. Es el caso aceptado de best-effort; no se garantiza el
  límite bajo concurrencia.
- Creación concurrente con un borrado: si la creación no ve el hueco
  liberado, puede recibir 409 y el usuario puede reintentar. No se garantiza
  el resultado bajo concurrencia.
- Proyecto inexistente, ajeno, con id 0 o negativo: 404 con el mismo
  mensaje.
- Borrado repetido del mismo proyecto: la primera petición responde 204 y
  la segunda 404.
- `id` no numérico (`/api/projects/abc`): 400 con `ApiError`.
- Token ausente, inválido o caducado: 401.
- Proyecto con muchos test cases: el borrado en cascada se ejecuta en una
  transacción y no deja datos huérfanos.
- `createdAt` se devuelve como `dd/MM/yyyy`; el orden del listado se
  calcula sobre el timestamp almacenado y, si coincide, por `id`.
- Títulos con emojis o grafemas compuestos: la longitud se cuenta en
  unidades UTF-16. Un emoji cuenta como 2 unidades y un grafema compuesto
  por varias unidades cuenta todas; por ejemplo, 127 emojis (254 unidades)
  se aceptan y 128 (256) se rechazan. La base de datos aceptaría más, pero
  el límite de la API es el de `@Size` y nunca se desborda la columna.

## Fuera de alcance

- Editar o renombrar proyectos (no se exponen `PUT` ni `PATCH`).
- Descripción u otros campos del proyecto.
- Paginación, búsqueda, filtros u orden configurable del listado.
- Devolver contadores o estadísticas de test cases en el listado o en el
  detalle.
- Compartir proyectos o permitir que `ADMIN` acceda a proyectos ajenos.
- Borrado lógico, papelera o restauración.
- Un endpoint para cambiar el límite de proyectos; solo se ajusta por
  configuración.
- Auditoría o historial de cambios.
- Endpoints de administración de proyectos.
- Gestión de test cases, incluidas sus etiquetas, y estadísticas (specs 02
  y 04).

## Criterios de finalización

- [x] Los cuatro endpoints responden con los códigos y cuerpos definidos
  (RF-005, RF-016, RF-020, RF-022).
- [x] El propietario se obtiene del JWT y no de la URL ni del cuerpo
  (RF-002).
- [x] Un usuario no puede ver ni borrar proyectos ajenos: 404, también
  para `ADMIN` (RF-003, RF-004, RF-021, RF-025).
- [x] El título es obligatorio, se guarda tal cual se recibe y admite como
  máximo 255 caracteres; los errores son 400 estructurados (RF-006, RF-007,
  RF-008).
- [x] El listado solo contiene proyectos propios, ordenado por el
  timestamp de creación descendente y `id` descendente, y devuelve `[]` si
  no hay (RF-016, RF-017, RF-018).
- [x] `createdAt` se devuelve como `dd/MM/yyyy` (RF-009).
- [x] El límite de 10 proyectos se aplica en el backend, es configurable y
  responde 409 con el mensaje «No puedes crear más de 10 proyectos. Elimina
  uno para crear otro.» al alcanzarlo (RF-012, RF-013).
- [x] El límite se comprueba antes de crear y es best-effort: el caso de
  dos creaciones concurrentes que superan el límite está documentado y
  aceptado (RF-014).
- [x] Los proyectos existentes por encima del límite no se borran
  automáticamente (RF-015).
- [ ] El borrado elimina el proyecto y la base de datos elimina en cascada
  sus test cases, pasos y asociaciones, conserva las etiquetas globales, es
  transaccional y libera el hueco (RF-023, RF-024, RF-026, RF-027).
  _(pendiente: verificación manual en T23)_
- [x] Token ausente, inválido o caducado: 401 (RF-001).
- [x] Todos los errores usan `ApiError`, sin stack traces (RF-028).
- [x] Un `id` de ruta no numérico devuelve 400 (RF-029).
- [x] `mvn test` en verde con tests unitarios de servicio y controlador,
  incluido el test de límite con 10 proyectos mockeados.
- [ ] Swagger muestra los cuatro endpoints con seguridad bearer.
  _(pendiente: verificación visual en T24; anotaciones y esquema bearer
  verificados por `ProjectControllerOpenApiTest`)_
- [ ] Demo manual del flujo completo: crear → listar → detalle → borrar →
  intentar crear por encima del límite y recibir 409.
  _(pendiente: verificación manual en T24)_

## Decisiones cerradas

- La spec incluye crear, listar, detalle y eliminar; la edición queda
  fuera.
- El título no se recorta: se guarda tal cual y se rechaza si está vacío o
  compuesto solo por espacios.
- Las respuestas usan `{id, title, createdAt}`, con `createdAt` en formato
  `dd/MM/yyyy`, y el listado se ordena por el timestamp de creación
  descendente.
- `ADMIN` no accede a proyectos de otros usuarios.
- Se limita cada usuario a 10 proyectos (configurable con 10 por defecto),
  según `alcance_proyecto.md` §3.2 y §5, en lugar de añadir paginación,
  porque el producto espera pocos proyectos por usuario y el listado no
  está paginado. Si en el futuro se retira el límite, habrá que añadir
  paginación.
- El límite es best-effort: dos creaciones simultáneas podrían superarlo.
  Se acepta y se documenta como caso conocido; no se añade bloqueo ni
  restricción en la base de datos.
- La respuesta 409 es la señal del límite; no se añade un código de error
  adicional a `ApiError`.
- La cascada de borrado es física, mediante `ON DELETE CASCADE`; el caso de
  uso no borra test cases, pasos ni asociaciones a mano.
