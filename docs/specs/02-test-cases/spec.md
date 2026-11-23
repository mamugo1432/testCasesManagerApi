# Spec 02 — Gestión de test cases y pasos

## Contexto y objetivo

TestCase Manager agrupa los casos de prueba manuales dentro de proyectos.
Cada test case pertenece a un único proyecto y define qué se quiere
comprobar, cómo se ejecuta y con qué prioridad y estado se trabaja. Esta
spec define la creación, el listado, el detalle, la edición y el borrado de
test cases, incluidos sus pasos ordenados, su prioridad, su estado y sus
etiquetas globales. El objetivo es que un usuario autenticado pueda
gestionar únicamente los test cases de sus propios proyectos, sin revelar
la existencia de recursos ajenos y sin dejar datos huérfanos.

El esquema de base de datos ya está definido en
`docs/esquema_creacion_base_datos.sql` y no se modifica. Los test cases se
guardan en `test_cases`; sus pasos, en `test_steps` (relación 1:N); y sus
etiquetas, en `test_case_tags` (relación N:M con `tags`). No existe una
columna `steps` ni `tags` en `test_cases`.

Las estadísticas del proyecto y el dashboard quedan en la spec 04. Esta
spec define el contrato completo de etiquetas: normalización,
reutilización, nombre visible, límites y concurrencia, sin depender de
otra spec.

## Modelo de datos

El esquema de `docs/esquema_creacion_base_datos.sql` es la única fuente de
verdad y no se modifica. Las entidades relevantes para esta spec son las
siguientes.

### `TestCase` (`test_cases`)

| Columna | Tipo | Observaciones |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Clave primaria, autoincremental |
| `project_id` | `BIGINT UNSIGNED` | FK a `projects.id` con `ON DELETE CASCADE` y `ON UPDATE CASCADE` |
| `title` | `VARCHAR(255) NOT NULL` | Título visible |
| `description` | `VARCHAR(255) NULL` | Descripción opcional |
| `expected_result` | `VARCHAR(255) NULL` | Resultado esperado opcional; obligatorio no vacío si `status = COMPLETED` |
| `priority` | `VARCHAR(10) NOT NULL` | `HIGH`, `MEDIUM` o `LOW`; la BD tiene `MEDIUM` por defecto, pero la API exige el valor |
| `status` | `VARCHAR(20) NOT NULL` | `IN_PROGRESS` o `COMPLETED`; la BD tiene `IN_PROGRESS` por defecto |
| `created_at` | `TIMESTAMP NOT NULL` | Fecha de creación asignada por el servidor |

`test_cases` no tiene columnas `steps` ni `tags`.

### `TestStep` (`test_steps`)

| Columna | Tipo | Observaciones |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Clave primaria, autoincremental; no se expone en la API |
| `test_case_id` | `BIGINT UNSIGNED` | FK a `test_cases.id` con `ON DELETE CASCADE` y `ON UPDATE CASCADE` |
| `instruction` | `VARCHAR(255) NOT NULL` | Instrucción del paso; máximo 255 unidades UTF-16 en la API |
| `position` | `INT UNSIGNED NOT NULL` | Orden dentro del test case; empieza en 1 y no tiene huecos |

Restricciones y comportamiento:

- `UNIQUE (test_case_id, position)` impide posiciones duplicadas dentro del
  mismo test case.
- Índice `idx_test_steps_test_case_id` sobre `test_case_id`.
- Al borrar un test case, la FK elimina sus pasos en cascada; al borrar un
  proyecto, la cascada de `test_cases` elimina también sus pasos.
- El mínimo de un paso, el orden 1..N y la regla de `COMPLETED` se validan
  en dominio/aplicación. El esquema solo garantiza la integridad
  referencial y la unicidad de `position`.

En dominio, `TestStep` es una entidad hija de `TestCase` con `id`,
`testCaseId`, `instruction` y `position`. La API no expone `id` ni
`testCaseId` del paso: devuelve `{position, instruction}`. El reemplazo de
pasos en `PUT` elimina los anteriores e inserta los nuevos en una misma
transacción.

### `Tag` y `TestCaseTag`

- `Tag` (`tags`): `id`, `name` (`VARCHAR(50)`) y `normalized_name`
  (`VARCHAR(50)`, único y con colación binaria). Las etiquetas son
  globales.
- `TestCaseTag` (`test_case_tags`): tabla de relación N:M con
  `test_case_id` y `tag_id`. La FK de `test_case_id` tiene
  `ON DELETE CASCADE`, por lo que borrar un test case elimina sus
  asociaciones pero no las etiquetas de `tags`.

#### Normalización, reutilización y concurrencia de etiquetas

La normalización se realiza siempre en backend y es la misma al crear y al
editar test cases:

1. Se eliminan los espacios iniciales y finales. A estos efectos se
   considera espacio cualquier carácter para el que `Character.isWhitespace`
   o `Character.isSpaceChar` sea verdadero; esto incluye NBSP y los espacios
   Unicode de categoría `Zs`.
2. Se convierte el texto a minúsculas con `Locale.ROOT`.
3. Se normaliza a NFC.
4. Se descompone canónicamente a NFD y se eliminan las marcas diacríticas
   (categoría Unicode `Mn`, que incluye acentos, diéresis y cedillas),
   salvo U+0303 cuando va inmediatamente después de una `n`: esa
   combinación es la `ñ`, que se conserva como letra propia. Después se
   recompone a NFC. La regla se aplica igual si la entrada trae `ñ`
   precompuesta o `n` seguida de U+0303.
5. No se aplica *case folding* adicional ni se colapsan los espacios
   interiores.

El **nombre visible** es el texto tras el paso 1; conserva mayúsculas,
tildes y `ñ`. El **nombre normalizado** es el resultado de los pasos 1 a 4.
Se busca por `normalized_name`; si existe, se reutiliza y se conserva el
nombre visible existente; si no existe, se crea con el nombre visible.

El nombre visible y el normalizado tendrán máximo 50 unidades UTF-16. La
validación de entrada aplica `@Size(max = 50)` al valor recibido de cada
etiqueta; el recorte Unicode posterior solo puede reducir esa longitud.
La longitud del normalizado se comprueba tras el paso 4. Si el visible
queda vacío, si el normalizado queda vacío o si el normalizado supera 50
unidades, se responde 400 en `tags[i]`. Los espacios interiores se
conservan y no se colapsan.

Dentro de una misma petición, las etiquetas se deduplican por nombre
normalizado. Para una etiqueta nueva se conserva el primer nombre visible
recibido; para una etiqueta existente se conserva el nombre visible ya
persistido.

La creación concurrente de una etiqueta se apoya en
`UNIQUE(normalized_name)`. Si dos peticiones intentan crear la misma
etiqueta nueva a la vez, una transacción inserta la etiqueta y la otra
recibe un conflicto 409; no se duplica, no se reintenta y no se responde
500. La petición rechazada no crea ni modifica el test case. El nombre
visible que prevalece es el de la etiqueta que quedó persistida.

## Usuarios / actores

- **Usuario autenticado (`USER` o `ADMIN`)**: crea, lista, consulta, edita
  y elimina los test cases de sus propios proyectos. En esta funcionalidad
  no hay diferencias de permisos entre roles.
- **Visitante no autenticado**: no puede acceder a ningún endpoint de test
  cases; recibe 401.
- **Backend**: valida la entrada, resuelve el propietario desde el JWT,
  aplica la propiedad del proyecto y del test case, valida las reglas de
  estado y de pasos, normaliza y reutiliza etiquetas, y persiste los
  cambios de forma transaccional.

## Historias de usuario

- H1: Como usuario autenticado quiero crear un test case dentro de un
  proyecto con título, descripción, resultado esperado, prioridad, pasos y
  etiquetas para dejar documentada una prueba manual.
- H2: Como usuario autenticado quiero ver el listado de test cases de un
  proyecto para saber qué pruebas contiene y en qué estado están.
- H3: Como usuario autenticado quiero abrir un test case y ver sus pasos
  en el orden correcto para poder ejecutarlo.
- H4: Como usuario autenticado quiero editar todos los datos de un test
  case, incluidos sus pasos, etiquetas y estado, para mantenerlo al día.
- H5: Como usuario autenticado quiero eliminar un test case, con
  confirmación previa en la interfaz, para dejar de trabajar con él.
- H6: Como usuario autenticado quiero que mis test cases y los de mis
  proyectos sean privados para que otros usuarios no puedan verlos ni
  modificarlos.
- H7: Como usuario autenticado quiero marcar un test case como completado
  solo cuando tenga pasos y un resultado esperado para que el estado sea
  fiable.
- H8: Como usuario autenticado quiero reutilizar etiquetas globales sin
  crear duplicados por mayúsculas, espacios o tildes.
- H9: Como usuario autenticado quiero recibir errores claros cuando un
  dato no sea válido o el recurso no exista.
- H10: Como usuario autenticado quiero añadir, editar, eliminar y
  reordenar los pasos de un test case sin perder el orden.

## Requisitos funcionales (criterios de aceptación en EARS)

### Contrato de API

| Método | Ruta | Autenticación | Cuerpo | Respuesta correcta | Errores |
|---|---|---|---|---|---|
| POST | `/api/projects/{projectId}/test-cases` | Bearer JWT (`USER`/`ADMIN`) | `CreateTestCaseRequest` | 201 `TestCaseDetailResponse` | 400, 401, 403, 404, 405, 406, 409, 415, 500 |
| GET | `/api/projects/{projectId}/test-cases?page=0` | Bearer JWT (`USER`/`ADMIN`) | — | 200 `{ "testCases": [...], "page": 0, "size": 15, "totalElements": ..., "totalPages": ... }` | 400, 401, 403, 404, 405, 406, 500 |
| GET | `/api/projects/{projectId}/test-cases/{testCaseId}` | Bearer JWT (`USER`/`ADMIN`) | — | 200 `TestCaseDetailResponse` | 400, 401, 403, 404, 405, 406, 500 |
| PUT | `/api/projects/{projectId}/test-cases/{testCaseId}` | Bearer JWT (`USER`/`ADMIN`) | `UpdateTestCaseRequest` | 200 `TestCaseDetailResponse` | 400, 401, 403, 404, 405, 406, 409, 415, 500 |
| DELETE | `/api/projects/{projectId}/test-cases/{testCaseId}` | Bearer JWT (`USER`/`ADMIN`) | — | 204 sin cuerpo | 400, 401, 403, 404, 405, 406, 500 |

Las rutas son anidadas: el proyecto sale de la URL y el test case debe
pertenecer a ese proyecto. `createdAt` se devuelve con formato
`dd/MM/yyyy` en la zona horaria del servidor. `userId` no se expone en
ninguna respuesta. La creación responde 201 con el recurso en el cuerpo;
no se exige cabecera `Location`.

El listado se pagina de 15 en 15. `page` es base 0 y por defecto 0; el
tamaño de página es fijo y no hay parámetro `size`. No se admiten filtros,
búsqueda ni orden configurable: el orden es siempre `createdAt DESC,
id DESC`.

#### DTOs de entrada

**`CreateTestCaseRequest`**

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `title` | string | Sí | `@NotBlank` (semántica de `trim()` de Java) y `@Size(max = 255)`; se guarda tal cual, sin `trim` |
| `description` | string | No | `@Size(max = 255)`; si es vacío o solo espacios según `trim()` de Java se guarda como `null`; si tiene contenido se guarda tal cual |
| `expectedResult` | string | No | `@Size(max = 255)`; si es vacío o solo espacios según `trim()` de Java se guarda como `null`; si tiene contenido se guarda tal cual. Al crear no se exige porque el estado inicial es `IN_PROGRESS` |
| `priority` | string | Sí | `@NotBlank`; `HIGH`, `MEDIUM` o `LOW`, en mayúsculas |
| `steps` | array de objetos | Sí | `@NotEmpty`; cada objeto `{ "instruction": "..." }` con `instruction` validada con `@NotBlank` y `@Size(max = 255)` |
| `tags` | array de strings | No | Cada nombre con `@NotBlank` y `@Size(max = 50)` sobre el valor recibido; se recorta con espacios Unicode y se normaliza y reutiliza en backend |

El DTO de creación no declara `status`, `id`, `projectId` ni `createdAt`.
Esos campos se resuelven en servidor; si llegan en el cuerpo, se ignoran
como campos desconocidos y el estado inicial siempre es `IN_PROGRESS`.

**`UpdateTestCaseRequest`**

Tiene los mismos campos que `CreateTestCaseRequest`, más:

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `status` | string | Sí | `@NotBlank`; `IN_PROGRESS` o `COMPLETED`, en mayúsculas; si es `COMPLETED`, `expectedResult` debe tener contenido no vacío según `@NotBlank` |

`PUT` es de reemplazo completo. Los campos opcionales omitidos o enviados
como `null` se vacían (`null` o lista vacía). No existe `PATCH`.

#### DTOs de salida

**`TestCaseSummaryResponse`** (listado)

| Campo | Tipo | Observaciones |
|---|---|---|
| `id` | number | Identificador del test case |
| `projectId` | number | Proyecto al que pertenece |
| `title` | string | Título visible |
| `description` | string \| null | Descripción o `null` |
| `priority` | string | `HIGH`, `MEDIUM` o `LOW` |
| `status` | string | `IN_PROGRESS` o `COMPLETED` |
| `tags` | array de `{id, name}` | Ordenadas por `name` ascendente |
| `createdAt` | string | `dd/MM/yyyy` |

**`TestCaseDetailResponse`** (creación, detalle y edición)

Incluye todos los campos del resumen y añade:

| Campo | Tipo | Observaciones |
|---|---|---|
| `expectedResult` | string \| null | Resultado esperado o `null` |
| `steps` | array de `{position, instruction}` | Ordenados por `position` ascendente; `position` empieza en 1 y no tiene huecos |

**Ejemplo de creación**

```json
{
  "title": "Login con credenciales válidas",
  "description": "Comprobar el acceso con usuario y contraseña correctos",
  "expectedResult": "El usuario accede al listado de proyectos",
  "priority": "HIGH",
  "steps": [
    { "instruction": "Abrir la pantalla de login" },
    { "instruction": "Introducir credenciales válidas" },
    { "instruction": "Pulsar Entrar" }
  ],
  "tags": ["funcional", "Regresión"]
}
```

**Ejemplo de detalle**

```json
{
  "id": 12,
  "projectId": 3,
  "title": "Login con credenciales válidas",
  "description": "Comprobar el acceso con usuario y contraseña correctos",
  "expectedResult": "El usuario accede al listado de proyectos",
  "priority": "HIGH",
  "status": "IN_PROGRESS",
  "tags": [
    { "id": 1, "name": "funcional" },
    { "id": 2, "name": "Regresión" }
  ],
  "steps": [
    { "position": 1, "instruction": "Abrir la pantalla de login" },
    { "position": 2, "instruction": "Introducir credenciales válidas" },
    { "position": 3, "instruction": "Pulsar Entrar" }
  ],
  "createdAt": "17/11/2026"
}
```

**Ejemplo de listado paginado**

```json
{
  "testCases": [
    {
      "id": 12,
      "projectId": 3,
      "title": "Login con credenciales válidas",
      "description": "Comprobar el acceso con usuario y contraseña correctos",
      "priority": "HIGH",
      "status": "IN_PROGRESS",
      "tags": [
        { "id": 1, "name": "funcional" },
        { "id": 2, "name": "Regresión" }
      ],
      "createdAt": "17/11/2026"
    }
  ],
  "page": 0,
  "size": 15,
  "totalElements": 16,
  "totalPages": 2
}
```

### Autenticación y propiedad

- **RF-001** — EL SISTEMA exigirá un token JWT válido en todos los
  endpoints de test cases. SI el token falta, no es válido o ha caducado,
  ENTONCES responderá 401 con `ApiError` y el mensaje «No autenticado».
- **RF-002** — EL SISTEMA obtendrá el id del usuario autenticado del
  contexto de seguridad (claims del JWT), nunca de la URL ni del cuerpo de
  la petición.
- **RF-003** — EL SISTEMA resolverá el proyecto por `projectId` y el
  `userId` del usuario autenticado. SI el proyecto no existe o pertenece a
  otro usuario, ENTONCES responderá 404 con «Proyecto no encontrado»,
  también para `ADMIN`.
- **RF-004** — EL SISTEMA resolverá el test case por `testCaseId` y
  `projectId` de la ruta. SI no existe, pertenece a otro proyecto (aunque
  sea del mismo usuario) o a otro usuario, ENTONCES responderá 404 con
  «Test case no encontrado».
- **RF-005** — Los endpoints exigirán rol `USER` o `ADMIN` en
  `SecurityFilterChain`. SI el rol no está autorizado, ENTONCES responderá
  403. No hay diferencias de permisos entre ambos roles sobre recursos
  propios.
- **RF-006** — EL SISTEMA no incluirá ni expondrá `userId` en ninguna
  respuesta. La propiedad se comprobará siempre antes de leer o modificar
  datos.

### Creación

- **RF-007** — CUANDO un usuario autenticado envíe
  `POST /api/projects/{projectId}/test-cases` con un cuerpo válido sobre un
  proyecto propio, EL SISTEMA creará el test case, lo asociará al proyecto,
  asignará `createdAt` en el servidor y responderá 201 con
  `TestCaseDetailResponse`.
- **RF-008** — EL SISTEMA creará siempre el test case en estado
  `IN_PROGRESS`. El cuerpo de creación no admite `status`; si se envía, se
  ignora como campo desconocido. Tampoco admite `id`, `projectId` ni
  `createdAt`.
- **RF-009** — EL SISTEMA exigirá `title` con `@NotBlank` y
  `@Size(max = 255)`. `@NotBlank` usa la semántica de `trim()` de Java:
  rechaza nulo, vacío y solo espacios ASCII, tabuladores o saltos de línea.
  El título se guarda tal cual se recibe, sin `trim` ni normalización; los
  espacios exteriores, interiores y saltos de línea se conservan. SI no
  cumple, ENTONCES responderá 400 con el error en `title`.
- **RF-010** — `description` será opcional y tendrá `@Size(max = 255)`. SI
  es nula, vacía o solo espacios según `trim()` de Java, ENTONCES se
  guardará como `null`; si tiene contenido, se guarda tal cual, sin `trim`.
- **RF-011** — `expectedResult` será opcional y tendrá `@Size(max = 255)`.
  SI es nulo, vacío o solo espacios según `trim()` de Java, ENTONCES se
  guardará como `null`; si tiene contenido, se guarda tal cual, sin `trim`.
  Al crear no se exige porque el estado inicial es `IN_PROGRESS`.
- **RF-012** — `priority` será obligatoria (`@NotBlank`) y solo admitirá
  `HIGH`, `MEDIUM` o `LOW`, en mayúsculas. SI falta o no es uno de esos
  valores, ENTONCES responderá 400 en `priority`.
- **RF-013** — `steps` será obligatorio (`@NotEmpty`) y contendrá al menos
  un paso. Cada paso tendrá `instruction` con `@NotBlank` y
  `@Size(max = 255)`; se guarda tal cual, sin `trim`. SI falta o alguna
  instrucción no cumple, ENTONCES responderá 400 en `steps` o en
  `steps[i].instruction`, según corresponda.
- **RF-014** — EL SISTEMA asignará `position` a los pasos en el orden del
  array, empezando en 1 y sin huecos. El cliente no envía `position`; si
  lo envía, se ignora.
- **RF-015** — `tags` será opcional. Cada etiqueta se enviará por nombre y
  se validará con `@NotBlank` y `@Size(max = 50)` sobre el valor recibido.
  El backend la recortará con espacios Unicode, la normalizará y reutilizará
  la existente según la sección «Normalización, reutilización y concurrencia
  de etiquetas». SI un nombre está vacío o compuesto solo por espacios
  Unicode, ENTONCES responderá 400. El recorte posterior solo puede reducir
  la longitud; si el nombre normalizado supera 50 unidades UTF-16 o queda
  vacío, también responderá 400.
- **RF-016** — EL SISTEMA deduplicará las etiquetas de una misma petición
  por nombre normalizado, conservando el primer nombre visible para una
  etiqueta nueva.
- **RF-017** — EL SISTEMA permitirá títulos repetidos de test cases,
  incluso dentro del mismo proyecto.
- **RF-018** — SI el proyecto no existe o es ajeno, ENTONCES responderá 404
  con «Proyecto no encontrado» y no creará nada. Si el cuerpo además es
  inválido, prevalece el 400 de validación.
- **RF-019** — EL SISTEMA ejecutará la creación de forma transaccional:
  test case, pasos y asociaciones con etiquetas se guardan todos o no se
  guarda nada. Las etiquetas nuevas se crean en la misma transacción; las
  existentes se reutilizan. Si la inserción de una etiqueta nueva choca con
  `UNIQUE(normalized_name)` porque otra transacción la creó, la petición
  responderá 409 con el error en `tags` y el mensaje «Conflicto al crear la
  etiqueta», y no se guardará nada; no se reintenta ni se relee la etiqueta.
- **RF-020** — EL SISTEMA solo persistirá los campos declarados en el DTO.
  Cualquier otro campo del cuerpo se ignorará. `projectId` e `id` se
  resuelven en servidor; no se permite crear un test case en varios
  proyectos ni moverlo.

### Listado

- **RF-021** — CUANDO un usuario autenticado solicite
  `GET /api/projects/{projectId}/test-cases?page=0` de un proyecto propio,
  EL SISTEMA responderá 200 con
  `{ "testCases": [...], "page": ..., "size": 15, "totalElements": ...,
  "totalPages": ... }` y solo los test cases de ese proyecto. El listado se
  pagina de 15 en 15.
- **RF-022** — EL SISTEMA ordenará el listado por `createdAt` descendente
  y, en caso de empate, por `id` descendente. El orden se calcula sobre el
  timestamp almacenado, no sobre la fecha formateada, y es el mismo en
  todas las páginas.
- **RF-023** — SI el proyecto no tiene test cases, ENTONCES responderá 200
  con `testCases: []`, `page` con el valor solicitado, `size: 15`,
  `totalElements: 0` y `totalPages: 0`.
- **RF-024** — Cada elemento del listado incluirá `id`, `projectId`,
  `title`, `description`, `priority`, `status`, `tags` y `createdAt`. No
  incluirá `expectedResult` ni `steps`.
- **RF-025** — EL SISTEMA paginará el listado de 15 en 15 mediante el
  parámetro `page`, base 0 y por defecto 0. No habrá parámetro `size`,
  filtros, búsqueda ni orden configurable. SI `page` es negativa o no
  numérica, ENTONCES responderá 400. SI `page` supera la última página,
  ENTONCES responderá 200 con `testCases: []` y los totales.
- **RF-026** — Las etiquetas del resumen se devolverán como `[{id, name}]`,
  ordenadas por `name` ascendente; `createdAt` se devolverá como
  `dd/MM/yyyy`. `totalPages` será `0` si no hay test cases y, en caso
  contrario, el número de páginas de 15 elementos necesario para contener
  `totalElements`.
- **RF-027** — SI el proyecto no existe o es ajeno, ENTONCES responderá 404
  con «Proyecto no encontrado».

### Detalle

- **RF-028** — CUANDO un usuario autenticado solicite
  `GET /api/projects/{projectId}/test-cases/{testCaseId}` de un test case
  propio, EL SISTEMA responderá 200 con `TestCaseDetailResponse`.
- **RF-029** — EL SISTEMA devolverá `expectedResult` (o `null`) y `steps`
  ordenados por `position` ascendente, con `position` e `instruction`.
- **RF-030** — SI el proyecto no existe o es ajeno, ENTONCES 404
  «Proyecto no encontrado». SI el test case no existe o no pertenece a ese
  proyecto, ENTONCES 404 «Test case no encontrado».
- **RF-031** — EL SISTEMA devolverá `createdAt` como `dd/MM/yyyy` y no
  expondrá `updatedAt` ni `userId`.

### Edición

- **RF-032** — CUANDO un usuario autenticado envíe
  `PUT /api/projects/{projectId}/test-cases/{testCaseId}` con un cuerpo
  válido sobre un test case propio, EL SISTEMA reemplazará sus datos y
  responderá 200 con `TestCaseDetailResponse`.
- **RF-033** — `PUT` será de reemplazo completo: `title`, `priority`,
  `status` y `steps` obligatorios; `description`, `expectedResult` y `tags`
  opcionales. SI un campo opcional se omite o se envía `null`, ENTONCES se
  vaciará (`null` o lista vacía). No habrá `PATCH` ni actualizaciones
  parciales.
- **RF-034** — `title` seguirá las reglas de RF-009; `description` y
  `expectedResult`, las de RF-010 y RF-011; `priority`, la de RF-012;
  `steps`, las de RF-013 y RF-014; `tags`, las de RF-015, RF-016 y RF-058 a
  RF-066.
  SI algo no cumple, ENTONCES 400 con el campo afectado.
- **RF-035** — `status` será obligatorio en la edición (`@NotBlank`) y solo
  admitirá `IN_PROGRESS` o `COMPLETED`, en mayúsculas. SI falta o no es
  válido, ENTONCES 400 en `status`.
- **RF-036** — SI `status` es `COMPLETED`, ENTONCES `expectedResult`
  deberá tener contenido no vacío según `@NotBlank`; en caso contrario, 400
  en `expectedResult`. La regla se valida con los datos del mismo `PUT`, no
  con los anteriores.
- **RF-037** — EL SISTEMA permitirá pasar de `IN_PROGRESS` a `COMPLETED` y
  de `COMPLETED` a `IN_PROGRESS`. En `IN_PROGRESS` el resultado esperado
  puede ser `null`; en `COMPLETED` no. Un test case `COMPLETED` puede tener
  uno o varios pasos, pero nunca cero.
- **RF-038** — EL SISTEMA reemplazará todos los pasos del test case:
  eliminará los anteriores e insertará los del cuerpo en el orden recibido,
  con `position` 1..N. SI `steps` está vacío o falta, ENTONCES 400. La
  operación se hará en la misma transacción.
- **RF-039** — EL SISTEMA reemplazará las asociaciones de etiquetas del
  test case por las del cuerpo. Las etiquetas globales que dejen de estar
  asociadas no se eliminarán. Las etiquetas nuevas se crearán y las
  existentes se reutilizarán. SI una etiqueta nueva choca con
  `UNIQUE(normalized_name)` por una creación concurrente, ENTONCES la
  petición responderá 409 con el error en `tags` y el mensaje «Conflicto al
  crear la etiqueta», y no se modificará nada; no se reintenta ni se relee
  la etiqueta.
- **RF-040** — `createdAt` y `projectId` serán inmutables. `id`,
  `projectId` y `createdAt` enviados en el cuerpo se ignorarán. No se
  permite mover un test case a otro proyecto.
- **RF-041** — SI el proyecto no existe o es ajeno, ENTONCES 404
  «Proyecto no encontrado». SI el test case no existe o no pertenece a ese
  proyecto, ENTONCES 404 «Test case no encontrado». Si el cuerpo es
  inválido, prevalece el 400. Si hay un conflicto de concurrencia de
  etiquetas, responde 409.
- **RF-042** — EL SISTEMA ejecutará la edición de forma transaccional:
  datos, pasos y asociaciones de etiquetas se actualizan todos o no se
  actualiza nada; un 409 de etiqueta no deja cambios parciales.
- **RF-043** — EL SISTEMA permitirá títulos repetidos también al editar.
- **RF-044** — EL SISTEMA devolverá en el `PUT` el test case completo, con
  pasos ordenados y etiquetas, igual que el detalle.

### Borrado

- **RF-045** — CUANDO un usuario autenticado solicite
  `DELETE /api/projects/{projectId}/test-cases/{testCaseId}` de un test
  case propio, EL SISTEMA eliminará el test case y responderá 204 sin
  cuerpo.
- **RF-046** — EL SISTEMA eliminará en cascada, mediante las claves foráneas
  del esquema, los pasos (`test_steps`) y las asociaciones con etiquetas
  (`test_case_tags`). Las etiquetas globales de `tags` no se eliminarán. El
  esquema verificado define `ON DELETE CASCADE` en
  `test_steps.test_case_id` y `test_case_tags.test_case_id`; por tanto, la
  aplicación no ejecutará borrados manuales de esas filas.
- **RF-047** — SI el test case no existe o no pertenece al proyecto,
  ENTONCES 404 «Test case no encontrado» y no se modificará nada. Un segundo
  borrado del mismo test case responde 404.
- **RF-048** — EL SISTEMA ejecutará el borrado en una transacción; o
  desaparece el test case con sus datos dependientes, o no desaparece
  nada. El caso de uso solo elimina la fila de `test_cases`; la cascada la
  ejecuta la base de datos. Si el esquema no tuviera esas cascadas, el caso
  de uso borraría explícitamente pasos y asociaciones antes del test case;
  no es el caso actual.

### Pasos

- **RF-049** — Cada paso tendrá una `instruction` obligatoria, no vacía,
  no compuesta solo por espacios y con máximo 255 unidades UTF-16. Se
  guarda tal cual, sin `trim`.
- **RF-050** — La `position` será asignada por el servidor, empezando en 1
  y sin huecos, según el orden del array. El cliente no puede fijarla. La
  restricción `UNIQUE (test_case_id, position)` impide duplicados.
- **RF-051** — EL SISTEMA devolverá los pasos como
  `[{position, instruction}]`, ordenados por `position` ascendente. No se
  garantiza un `id` estable de paso entre ediciones.
- **RF-052** — Todo test case tendrá al menos un paso, tanto al crearse
  como al editarse. SI `steps` falta, es `null` o está vacío, ENTONCES 400.
  Esta regla se valida en dominio/aplicación porque el esquema no puede
  imponerla.
- **RF-053** — EL SISTEMA permitirá reordenar pasos enviando el array en el
  nuevo orden; las posiciones se renumerarán 1..N.
- **RF-054** — EL SISTEMA permitirá instrucciones repetidas en pasos
  distintos; no hay unicidad de contenido, solo de posición.
- **RF-055** — EL SISTEMA no impondrá un máximo de pasos por test case en
  esta iteración.
- **RF-056** — El borrado de un test case eliminará sus pasos mediante
  `ON DELETE CASCADE`; el borrado de un proyecto elimina sus test cases y,
  con ellos, sus pasos (spec 01). La aplicación no replica esas cascadas con
  borrados manuales.
- **RF-057** — Los pasos no se gestionan con endpoints independientes en
  esta iteración: se crean, editan, eliminan y reordenan a través del test
  case completo (`POST`/`PUT`).

### Etiquetas

- **RF-058** — EL SISTEMA recibirá las etiquetas por nombre y las
  normalizará en backend antes de buscar o crear, aplicando el algoritmo
  definido en «Normalización, reutilización y concurrencia de etiquetas»:
  espacios Unicode iniciales y finales, minúsculas con `Locale.ROOT`, NFC y
  eliminación de marcas diacríticas conservando la `ñ`.
- **RF-059** — SI ya existe una etiqueta con el mismo nombre normalizado,
  ENTONCES la reutilizará y conservará el nombre visible existente. Por
  ejemplo, si existe `Regresión` y llega `REGRESION`, se reutiliza
  `Regresión`.
- **RF-060** — SI una etiqueta no existe, ENTONCES la creará con el nombre
  visible recibido tras el recorte de espacios Unicode. El valor recibido
  se valida con `@Size(max = 50)` y el nombre normalizado también tendrá
  máximo 50 unidades UTF-16; `normalized_name` es único en base de datos.
  Si el normalizado queda vacío o supera 50 unidades, se responderá 400.
- **RF-061** — SI un nombre de etiqueta está vacío o compuesto solo por
  espacios Unicode, ENTONCES 400. La entrada se valida con `@NotBlank` y
  `@Size(max = 50)`; el recorte Unicode posterior solo puede reducir la
  longitud. Si el nombre normalizado supera 50 unidades, también 400.
- **RF-062** — EL SISTEMA deduplicará por nombre normalizado las etiquetas
  de una misma petición. Si dos variantes apuntan a la misma etiqueta
  nueva, se conserva el primer nombre visible; si apuntan a una etiqueta
  existente, se conserva el nombre visible persistido.
- **RF-063** — EL SISTEMA devolverá las etiquetas como `[{id, name}]`,
  ordenadas por `name` ascendente. `id` es el identificador global de la
  etiqueta.
- **RF-064** — Un test case podrá tener cero, una o varias etiquetas. No
  habrá máximo de etiquetas por test case en esta iteración.
- **RF-065** — Las etiquetas serán globales y reutilizables entre
  proyectos y usuarios. Borrar un test case o un proyecto solo elimina sus
  asociaciones, nunca las etiquetas.
- **RF-066** — La normalización se hará solo en backend, nunca en frontend.
  La `ñ` se conserva; no se elimina su tilde. Si dos peticiones intentan
  crear la misma etiqueta nueva a la vez, una inserta la etiqueta y la otra
  responde 409; no se duplica, no se reintenta y no se relee la etiqueta.

### Estado y prioridad

- **RF-067** — `priority` será obligatoria en crear y editar, con valores
  `HIGH`, `MEDIUM` y `LOW`; se usará para clasificar y para las
  estadísticas de la spec 04.
- **RF-068** — `status` solo admitirá `IN_PROGRESS` y `COMPLETED`. Al
  crear, el test case siempre será `IN_PROGRESS`; al editar, el cliente
  deberá enviar el estado deseado.
- **RF-069** — `COMPLETED` indicará progreso de la prueba, no resultado de
  ejecución. No existirán `PASSED`, `FAILED`, historial ni evidencias en
  esta iteración.
- **RF-070** — EL SISTEMA permitirá cambiar el estado en cualquier
  dirección; la validación de `COMPLETED` se define en RF-036. SI se pasa
  a `IN_PROGRESS`, ENTONCES no se exigirá `expectedResult`.
- **RF-071** — EL SISTEMA no calculará ni almacenará valores derivados de
  estado o prioridad en estos endpoints; las estadísticas se calcularán en
  la spec 04.
- **RF-072** — `priority` y `status` se devolverán como los valores de
  enum en mayúsculas (`HIGH`, `MEDIUM`, `LOW`, `IN_PROGRESS`,
  `COMPLETED`).

### Errores y validación

- **RF-073** — EL SISTEMA devolverá todos los errores con `ApiError`
  (`message` y `fields`), sin stack traces ni detalles internos.
- **RF-074** — Los códigos HTTP serán: 400 para validación, JSON
  malformado, cuerpo vacío, id de ruta no numérico o `page` inválida; 401
  para token ausente, inválido o caducado; 403 para rol no autorizado; 404
  para proyecto o test case inexistente o ajeno; 405 para método no
  soportado; 406 para `Accept` no aceptable; 409 para conflicto de creación
  concurrente de una etiqueta; 415 para `Content-Type` no soportado; 500
  para error no controlado con mensaje genérico.
- **RF-075** — Los errores de validación indicarán el campo afectado con
  estos nombres: `title`, `description`, `expectedResult`, `priority`,
  `status`, `steps`, `steps[i].instruction`, `tags`, `tags[i]`, `page`.
- **RF-076** — SI el cuerpo es inválido y el proyecto o test case es ajeno,
  ENTONCES prevalece el 400 de validación, porque Bean Validation se
  ejecuta antes del caso de uso. Esto no revela si el recurso existe.
- **RF-077** — SI `projectId` o `testCaseId` no son enteros válidos,
  ENTONCES 400 con `ApiError`. Un id 0 o negativo se tratará como
  inexistente y responderá 404 cuando el cuerpo sea válido.
- **RF-078** — EL SISTEMA ignorará los campos desconocidos del cuerpo. Los
  enums serán sensibles a mayúsculas: `high`, `in_progress` o `Completed`
  responderán 400.
- **RF-079** — Los mensajes de validación y error se escribirán en
  español; los identificadores, clases y paquetes, en inglés.
- **RF-080** — Los mensajes 404 serán genéricos e idénticos para
  inexistente y ajeno dentro de cada tipo: «Proyecto no encontrado» y
  «Test case no encontrado».
- **RF-081** — `POST` y `PUT` exigirán `Content-Type: application/json`.
  SI no es compatible, ENTONCES 415 con `ApiError`.
- **RF-082** — SI el cuerpo JSON está mal formado o no se puede
  deserializar, ENTONCES 400 con «Cuerpo de la petición inválido».
- **RF-083** — SI se usa un método no soportado en una ruta existente,
  ENTONCES 405 con `ApiError`.
- **RF-084** — Cualquier error no controlado responderá 500 con «Error
  interno del servidor», sin detalles internos.

## Requisitos no funcionales

- **Seguridad**: API stateless con JWT; la autorización por rol se
  centraliza en `SecurityFilterChain` y toda operación verifica la
  propiedad del usuario autenticado; se responde 404, nunca 403, si el
  proyecto o el test case no existe o es ajeno; no se expone `userId`.
- **Persistencia**: el esquema SQL es la única fuente de verdad y no se
  modifica (`ddl-auto=none`). `test_cases` no tiene columnas `steps` ni
  `tags`; los pasos viven en `test_steps` (1:N) y las etiquetas en `tags` +
  `test_case_tags` (N:M). `test_steps.position` es único por test case y
  `test_steps.test_case_id` / `test_case_tags.test_case_id` tienen
  `ON DELETE CASCADE`. Por tanto, el caso de uso no borra pasos ni
  asociaciones: la cascada la ejecuta MySQL y la aplicación solo elimina la
  fila de `test_cases`. La regla de mínimo un paso y la de `COMPLETED` se
  validan en dominio/aplicación, como indica el propio script SQL; la base
  de datos no puede imponerlas sin triggers o cambios de esquema.
- **Consistencia**: creación, edición y borrado son transaccionales. El
  reemplazo de pasos y etiquetas se hace dentro de la misma transacción que
  los datos del test case. No se dejan pasos ni asociaciones huérfanas. Las
  etiquetas globales no se borran al borrar un test case o un proyecto.
- **Rendimiento**: el listado se filtra por `project_id` y se pagina de 15
  en 15, sin N+1 y sin cargar pasos. Se apoya en
  `idx_test_cases_project_id`, `idx_test_cases_project_status`,
  `idx_test_cases_project_priority`, `idx_test_steps_test_case_id` e
  `idx_test_case_tags_tag_id`.
- **Contrato**: JSON con nombres en camelCase (`projectId`,
  `expectedResult`, `createdAt`); fechas en `dd/MM/yyyy` en la zona horaria
  del servidor; etiquetas como `[{id, name}]`; pasos como
  `[{position, instruction}]`; listados como
  `{ "testCases": [...], "page": ..., "size": 15, "totalElements": ...,
  "totalPages": ... }`; listas vacías como `[]`, no `null`; los campos
  opcionales sin valor se devuelven como `null`.
- **Arquitectura**: hexagonal; `domain/` y `application/` no importan de
  `infrastructure/`; puertos de entrada y salida; las reglas de negocio
  (mínimo de pasos, transición a `COMPLETED`, normalización y reutilización
  de etiquetas) viven en dominio/aplicación, nunca en el controlador. La
  cascada de borrado se delega en el esquema mediante las FK
  `ON DELETE CASCADE`; no se replica con borrados manuales. Si el esquema no
  tuviera esas cascadas, el caso de uso borraría pasos y asociaciones antes
  del test case. MapStruct y Lombok según la skill hexagonal.
- **Validación**: siempre en backend con `@Valid` y DTOs. `title`,
  `instruction`, `priority` y `status` usan `@NotBlank`; las longitudes usan
  `@Size(max = 255)` en los campos de 255 y `@Size(max = 50)` en cada
  etiqueta. `@NotBlank` sigue la semántica de `trim()` de Java. Las
  etiquetas se validan además con el recorte Unicode y la normalización
  definidos en esta spec; si el nombre normalizado queda vacío o supera 50
  unidades UTF-16, se responde 400. El frontend puede validar para mejorar
  la experiencia, pero no sustituye al backend. Las longitudes se miden en
  unidades UTF-16 (`String.length()`).
- **Documentación**: los cinco endpoints se documentan con springdoc y
  aparecen en Swagger con el esquema de seguridad bearer y sus esquemas de
  petición y respuesta.
- **Pruebas**: tests unitarios con JUnit 5 y Mockito y tests de controlador
  con MockMvc; `mvn test` en verde. La normalización, reutilización,
  deduplicación y el conflicto 409 por creación concurrente de etiquetas se
  cubren con tests unitarios. La cascada física, la unicidad de `position` y
  el comportamiento de MySQL se verifican de forma manual contra el esquema,
  porque dependen de las claves foráneas y del motor y no constituyen lógica
  de aplicación.
- **Idioma**: mensajes de error y validación en español; identificadores,
  clases y paquetes en inglés; la spec en español.
- **Concurrencia**: no hay columna `version`; dos ediciones simultáneas se
  resuelven last-write-wins. No se añade bloqueo optimista. La creación
  concurrente de la misma etiqueta se apoya en
  `UNIQUE(normalized_name)`: una transacción inserta y la otra recibe 409;
  no se duplica, no se reintenta y no se responde 500.
- **Límites**: no hay máximo de test cases por proyecto, de pasos por test
  case ni de etiquetas por test case en esta iteración. El listado se pagina
  de 15 en 15; no hay filtros ni búsqueda.
- **Base de datos**: no se crean tablas, columnas, índices, triggers ni
  restricciones nuevas. Cualquier cambio de esquema requiere aprobación
  explícita.

## Dependencias con otras specs

- **Spec 00 — Autenticación**: aporta el JWT, los roles `USER`/`ADMIN` y el
  usuario autenticado del contexto de seguridad.
- **Spec 01 — Proyectos**: aporta la propiedad del proyecto, el 404 para
  proyectos ajenos y la cascada de borrado de test cases, pasos y
  asociaciones al borrar un proyecto.
- **Spec 04 — Estadísticas**: consumirá `status`, `priority` y las
  etiquetas de los test cases para calcular el dashboard. No añade campos
  nuevos a esta spec.

## Casos límite

- Proyecto sin test cases: 200 con `testCases: []`, `page` solicitado,
  `size: 15`, `totalElements: 0` y `totalPages: 0`.
- Proyecto con 0, 1, 15, 16 o 31 test cases: `totalPages` es 0, 1, 1, 2 y
  3 respectivamente; la última página puede contener menos de 15 elementos.
- `page=-1`, `page=abc` o una `page` no entera: 400 en `page`. Una `page`
  posterior a la última: 200 con `testCases: []` y los totales.
- No existe parámetro `size`; el tamaño de página es siempre 15. No se admite
  orden configurable.
- Proyecto inexistente, ajeno, con id 0 o negativo: 404 con «Proyecto no
  encontrado», también para `ADMIN`.
- Test case inexistente, ajeno o perteneciente a otro proyecto del mismo
  usuario: 404 con «Test case no encontrado».
- Proyecto eliminado: sus test cases dejan de existir por la cascada y
  responden 404.
- Título de 255 unidades UTF-16: aceptado; 256: 400. Un título con 127
  emojis (254 unidades) se acepta y con 128 (256) se rechaza.
- Título vacío, solo espacios ASCII, tabuladores o saltos de línea: 400 por
  `@NotBlank`; los saltos de línea y espacios interiores de un título con
  contenido se conservan. NBSP y otros espacios Unicode no se consideran
  blancos por `@NotBlank` y se aceptan como contenido.
- Títulos repetidos, incluso en el mismo proyecto: permitidos.
- `description` o `expectedResult` de 255 unidades: aceptados; 256: 400.
  Vacíos o solo espacios según `trim()` de Java se guardan como `null`; con
  contenido se guardan tal cual. NBSP no se considera vacío.
- `expectedResult` vacío o solo espacios según `@NotBlank` con
  `status = COMPLETED`: 400. Con `status = IN_PROGRESS`: se guarda como
  `null` si está vacío.
- `priority` ausente, `high`, `Medium` o un valor desconocido: 400.
- `status` ausente en `PUT`, `in_progress`, `Completed` o un valor
  desconocido: 400. En creación, cualquier `status` enviado se ignora y el
  test case nace `IN_PROGRESS`.
- `steps` ausente, `null` o `[]`: 400. Instrucción vacía o solo espacios:
  400. Instrucción de 255 unidades: aceptada; 256: 400. Instrucciones
  repetidas en pasos distintos: permitidas.
- Test case `COMPLETED` con un paso y `expectedResult` con contenido: 200.
  `COMPLETED` sin `expectedResult` no vacío: 400. `COMPLETED` con cero
  pasos: imposible por RF-052. `IN_PROGRESS` con pasos y sin resultado
  esperado: 200.
- Transición `COMPLETED` → `IN_PROGRESS` vaciando `expectedResult`: 200.
  Transición `IN_PROGRESS` → `COMPLETED` con resultado esperado: 200.
- `tags` ausente, `null` o `[]`: sin etiquetas. Nombre vacío o solo
  espacios Unicode tras el recorte: 400. `@Size(max = 50)` se aplica al
  valor recibido: 50 unidades UTF-16 se aceptan; 51 se rechazan aunque el
  recorte posterior dejaría 50. Si el nombre normalizado supera 50 unidades
  o queda vacío: 400.
- Variantes `Regresión`, `REGRESION`, `regresion` y ` regresion `: misma
  etiqueta. `cañon` y `cañón` también comparten etiqueta porque la `ñ` se
  conserva y solo se elimina la tilde de la `ó`; `canon` es otra etiqueta.
  Variantes repetidas en la misma petición: una sola asociación. Si la
  etiqueta ya existe, se conserva su nombre visible.
- Etiqueta usada por test cases de otros proyectos: se reutiliza. Etiqueta
  que deja de estar asociada: permanece en `tags`.
- `projectId`, `id` o `createdAt` enviados en el cuerpo: ignorados. No se
  puede mover un test case de proyecto.
- `projectId` o `testCaseId` no numérico (`/abc`): 400. Id 0 o negativo con
  cuerpo válido: 404.
- Borrado repetido del mismo test case: la primera petición responde 204 y
  la segunda 404.
- Dos ediciones simultáneas del mismo test case: last-write-wins; no se
  garantiza detectar el conflicto.
- Dos creaciones simultáneas de la misma etiqueta nueva:
  `UNIQUE(normalized_name)` permite que una inserción prospere; la otra
  petición responde 409, no crea ni modifica el test case y no se reintenta
  ni se relee la etiqueta. No hay duplicado ni 500.
- Muchos test cases en un proyecto: el listado se pagina de 15 en 15; una
  página posterior a la última devuelve una lista vacía. Se evita el N+1.
- JSON mal formado: 400. `Content-Type` no JSON en `POST`/`PUT`: 415.
  Método no soportado: 405. Token ausente, inválido o caducado: 401. Rol no
  autorizado: 403.
- Espacios Unicode exóticos (por ejemplo, NBSP): en etiquetas, los iniciales
  y finales se eliminan con el recorte Unicode y los interiores se
  conservan; una etiqueta que queda vacía tras el recorte responde 400. En
  los campos de texto generales (`title`, `description`, `expectedResult`,
  `instruction`) se aplica `@NotBlank`, que usa el `trim()` de Java: rechaza
  espacios ASCII, tabuladores y saltos de línea, pero no NBSP ni otros
  espacios Unicode, que se aceptan como contenido. Es una diferencia
  deliberada entre etiquetas y campos generales.

## Fuera de alcance

- Registro de resultados de ejecución (`PASSED`/`FAILED`), historial de
  ejecuciones, evidencias, capturas o adjuntos.
- Automatización o ejecución automática de test cases.
- Filtros, búsqueda, orden configurable o notificaciones.
- Operaciones masivas, importación o exportación.
- Mover test cases entre proyectos o compartirlos entre usuarios.
- Endpoints independientes para pasos (`POST`/`PUT`/`DELETE` por paso) o
  actualizaciones parciales `PATCH`.
- Gestión independiente de etiquetas globales (listar, renombrar, eliminar
  o una pantalla de administración). La normalización y la concurrencia de
  etiquetas se definen en esta spec.
- Dashboard y estadísticas del proyecto (spec 04).
- Comentarios, auditoría, historial de cambios o borrado lógico.
- Bloqueo optimista, versionado o detección de conflictos de edición.
- Cambios en el esquema SQL, triggers o restricciones nuevas.
- Implementación del frontend; esta spec define el contrato del backend.
- Roles y permisos más allá de `USER`/`ADMIN`.
- Copiar o duplicar test cases.

## Criterios de finalización

- [ ] Los cinco endpoints responden con los códigos, cuerpos y envoltorios
  definidos (RF-007, RF-021, RF-028, RF-032, RF-045).
- [ ] El propietario se obtiene del JWT y no de la URL ni del cuerpo
  (RF-002).
- [ ] Un usuario no puede ver ni modificar proyectos o test cases ajenos:
  404, también para `ADMIN` (RF-003, RF-004, RF-018, RF-027, RF-030,
  RF-041, RF-047).
- [ ] Un test case que no pertenece al proyecto de la ruta responde 404,
  aunque sea del mismo usuario (RF-004).
- [ ] Al crear, el estado es siempre `IN_PROGRESS`, `createdAt` lo pone el
  servidor y la respuesta es el detalle completo (RF-007, RF-008, RF-019).
- [ ] El título es obligatorio, se valida con `@NotBlank` y
  `@Size(max = 255)`, se guarda tal cual y admite hasta 255 unidades
  UTF-16; los errores son 400 estructurados (RF-009, RF-076, RF-079,
  RF-082).
- [ ] La prioridad es obligatoria y solo admite `HIGH`, `MEDIUM` o `LOW`
  (RF-012, RF-067).
- [ ] Todo test case tiene al menos un paso al crear y al editar; las
  posiciones son 1..N y se renumeran al reordenar (RF-013, RF-014, RF-038,
  RF-049, RF-050, RF-052, RF-053).
- [ ] `COMPLETED` exige `expectedResult` no vacío; `IN_PROGRESS` permite
  `null`; las transiciones en ambos sentidos funcionan (RF-035, RF-036,
  RF-037, RF-070).
- [ ] La edición con `PUT` reemplaza todos los datos, pasos y etiquetas, y
  no permite cambiar `projectId` ni `createdAt` (RF-032 a RF-044).
- [ ] El listado solo contiene test cases del proyecto, se pagina de 15 en
  15 con `page` base 0, se ordena por `createdAt DESC, id DESC`, usa el
  envoltorio `{ "testCases": [...], "page": ..., "size": 15,
  "totalElements": ..., "totalPages": ... }` y no incluye pasos (RF-021 a
  RF-026).
- [ ] El detalle incluye `expectedResult` y pasos ordenados (RF-028,
  RF-029).
- [ ] Las etiquetas se normalizan y reutilizan por nombre normalizado, se
  deduplican, conservan la `ñ`, respetan los límites y, ante una creación
  concurrente de la misma etiqueta nueva, una petición prospera y la otra
  responde 409 sin duplicar; se devuelven como `[{id, name}]` (RF-015,
  RF-016, RF-058 a RF-066).
- [ ] El borrado responde 204, elimina el test case y sus pasos y
  asociaciones en cascada, conserva las etiquetas globales y un segundo
  borrado responde 404 (RF-045 a RF-048). La aplicación no ejecuta borrados
  manuales de pasos ni asociaciones.
- [ ] Todos los errores usan `ApiError`; los ids no numéricos dan 400, las
  páginas inválidas dan 400, los ajenos o inexistentes dan 404 y el
  conflicto de etiqueta da 409 (RF-073 a RF-084).
- [ ] `mvn test` en verde con tests unitarios de servicio y controlador,
  incluidos validación anidada de pasos y etiquetas, propiedad y reglas de
  estado.
- [ ] Swagger muestra los cinco endpoints con seguridad bearer y sus
  esquemas.
- [ ] Verificación manual contra MySQL del borrado en cascada, de la
  unicidad de `position` y del conflicto 409 al crear etiquetas
  concurrentemente.
- [ ] No se ha modificado el script SQL ni se han añadido dependencias.

## Decisiones cerradas

- Las rutas son anidadas bajo el proyecto; el test case debe pertenecer al
  proyecto de la ruta y se responde 404 si no es así.
- La edición es `PUT` de reemplazo completo; no hay `PATCH` ni endpoints
  independientes para pasos.
- Al crear, el estado siempre es `IN_PROGRESS`; `status` no forma parte del
  cuerpo de creación y se ignora si llega. Al editar, `status` es
  obligatorio.
- La prioridad es obligatoria en crear y editar; no se asume `MEDIUM` por
  defecto aunque el esquema lo tenga como valor de reserva.
- Todo test case debe tener al menos un paso. La regla se valida en
  backend porque el esquema no puede imponerla; no se añaden triggers.
- Las posiciones de los pasos las asigna el servidor (1..N) según el orden
  del array; el cliente no las controla y no se garantiza un id estable de
  paso.
- Las etiquetas se envían por nombre; la respuesta usa `[{id, name}]`; se
  conserva el nombre visible de la primera creación; se deduplican por
  nombre normalizado. La normalización (recorte de espacios Unicode,
  minúsculas con `Locale.ROOT`, NFC y eliminación de diacríticos conservando
  la `ñ`) se define en esta spec. Si dos peticiones crean la misma etiqueta
  nueva a la vez, una inserta y la otra recibe 409; no se relee ni se
  reintenta.
- El listado usa `{ "testCases": [...], "page": ..., "size": 15,
  "totalElements": ..., "totalPages": ... }`, un resumen sin pasos ni
  `expectedResult`, orden `createdAt DESC, id DESC`, y se pagina de 15 en 15
  con `page` base 0.
- `createdAt` se devuelve como `dd/MM/yyyy`, es inmutable y no hay
  `updatedAt` porque el esquema no lo tiene. `projectId` se incluye en las
  respuestas.
- Los campos opcionales vacíos o solo espacios según `trim()` de Java se
  guardan como `null`; el título y las instrucciones se guardan tal cual,
  sin `trim`. `@NotBlank` y `@Size` son las validaciones de entrada.
- Los campos desconocidos se ignoran; los enums son sensibles a mayúsculas.
- La validación (400) prevalece sobre la comprobación de propiedad (404)
  cuando hay cuerpo; con cuerpo válido, el recurso ajeno o inexistente da
  404. Los mensajes 404 son genéricos e idénticos por tipo. El conflicto de
  creación concurrente de una etiqueta da 409.
- No hay límite de test cases, pasos ni etiquetas, ni filtros; el listado se
  pagina de 15 en 15.
- La concurrencia de edición es last-write-wins; no se añade bloqueo
  optimista ni columna `version`.
- No se modifica el esquema SQL: `test_cases` no tiene `steps` ni `tags`;
  se usan `test_steps` y `test_case_tags`, con las cascadas existentes. El
  borrado de test cases, pasos y asociaciones lo ejecuta MySQL mediante
  `ON DELETE CASCADE`; el caso de uso solo borra la fila de `test_cases` y
  no replica la cascada. Si el esquema no tuviera esas cascadas, el caso de
  uso borraría pasos y asociaciones antes del test case.
- La creación responde 201 con el recurso en el cuerpo; no se exige
  cabecera `Location`.

## Dudas abiertas

Ninguna duda de producto que bloquee la implementación. Las únicas
cuestiones pendientes son de detalle técnico y se cerrarán en `plan.md`:
mapeo JPA exacto de pasos y etiquetas, y orden de borrado/inserción al
reemplazar pasos para respetar `UNIQUE (test_case_id, position)`. La
normalización, reutilización, el conflicto 409 de etiquetas y la paginación
de 15 en 15 quedan cerrados en esta spec.
