## 1. Descripción general

**TestCase Manager** será una aplicación web para organizar proyectos y
gestionar sus casos de prueba manuales (*test cases*). Permitirá crear y
mantener pruebas, clasificarlas mediante etiquetas, asignarles una
prioridad y consultar estadísticas del testing de cada proyecto.

El proyecto se desarrollará como una aplicación full-stack con:

-   **Frontend:** React + TypeScript.
-   **Backend:** Java + Spring Boot.
-   **Persistencia:** base de datos relacional.
-   **Diseño del backend:** arquitectura hexagonal, aplicada de forma
    proporcionada al tamaño del proyecto.
-   **Desarrollo:** enfoque de *Spec-Driven Development* (SDD),
    definiendo el alcance y los requisitos antes de implementar.

La base de datos será **MySQL**. La aplicación incluirá registro e
inicio de sesión, y cada usuario tendrá sus propios proyectos. El
mecanismo de autenticación y los detalles finales del modelo de usuario
se concretarán en las especificaciones técnicas.

## 2. Objetivos

-   Organizar los casos de prueba por proyecto.
-   Facilitar la creación, consulta, edición y eliminación de pruebas
    manuales.
-   Clasificar los test cases con etiquetas reutilizables.
-   Identificar la prioridad y el estado de cada test case.
-   Mostrar estadísticas que ayuden a entender el progreso y la
    distribución de las pruebas de un proyecto.

## 3. Funcionalidades

### 3.1. Autenticación y usuarios

La aplicación incluirá registro e inicio de sesión. Cada usuario tendrá
sus propios proyectos y solo podrá consultar, crear, modificar o
eliminar proyectos de su propiedad y los test cases pertenecientes a
ellos. La implementación del mecanismo de autenticación y los campos
definitivos de usuario se concretarán en las especificaciones técnicas.

### 3.2. Gestión de proyectos

El usuario podrá:

-   Crear un proyecto indicando únicamente su **título**. Cada proyecto
    estará asociado al usuario autenticado que lo haya creado.
-   Consultar el listado de proyectos.
-   Entrar en un proyecto para consultar y gestionar sus test cases.
-   Eliminar un proyecto.

Un proyecto no tendrá descripción ni otros campos funcionales en el
alcance actual.

**Regla al eliminar:** la interfaz pedirá confirmación antes de eliminar
un proyecto. Al confirmar, también se eliminarán los test cases que
pertenezcan a él y sus datos dependientes, como los pasos y las
asociaciones con etiquetas. Las etiquetas globales no se eliminarán por
borrar un proyecto. El backend ejecutará la operación de forma consistente
para no dejar datos dependientes huérfanos.

### 3.3. Gestión de test cases

Cada proyecto tendrá sus propios casos de prueba. Dentro de un proyecto,
el usuario podrá:

-   Crear un test case.
-   Consultar el listado de test cases.
-   Abrir un test case para ver su detalle.
-   Editar un test case existente.
-   Eliminar un test case, previa confirmación en la interfaz.

Cada test case tendrá los siguientes datos. Los campos de texto
indicados en el modelo usarán `VARCHAR(255)`, excepto los nombres de
etiquetas/categorías, que usarán `VARCHAR(50)`.

  -----------------------------------------------------------------------
  Campo                               Descripción
  ----------------------------------- -----------------------------------
  Título                              Nombre breve que identifique la
                                      prueba. Obligatorio, máximo 255
                                      caracteres.

  Descripción                         Explicación de qué se quiere
                                      comprobar. Opcional, máximo 255
                                      caracteres.

  Pasos                               Secuencia ordenada de acciones que
                                      deben realizarse para ejecutar la
                                      prueba.

  Resultado esperado                  Comportamiento o resultado que se
                                      espera obtener al ejecutar los
                                      pasos. Opcional, máximo 255
                                      caracteres.

  Prioridad                           Nivel de importancia asignado al
                                      test case. Obligatoria: alta, media
                                      o baja.

  Etiquetas                           Cero o varias etiquetas para
                                      clasificar la prueba, por ejemplo,
                                      `backend`, `frontend` o
                                      `funcional`. Cada nombre tendrá un
                                      máximo de 50 caracteres.

  Estado                              Estado de progreso del test case:
                                      `IN_PROGRESS` o `COMPLETED`. Será
                                      obligatorio y por defecto comenzará
                                      en `IN_PROGRESS`.

  Fecha de creación                   Fecha en la que se creó el test
                                      case.
  -----------------------------------------------------------------------

El sistema asociará cada test case al proyecto en el que se haya creado.

#### Pasos de ejecución

Los pasos se guardarán como elementos ordenados vinculados al test case.
Cada paso tendrá una instrucción de hasta 255 caracteres y una posición
dentro de la secuencia. Al consultar o editar el test case, los pasos
deberán mostrarse en el orden correcto. El usuario podrá añadir, editar,
eliminar y reordenar pasos.

### 3.4. Estado de los test cases

Solo habrá dos estados posibles:

-   **En progreso (`IN_PROGRESS`):** la prueba todavía no se ha marcado
    como completada.
-   **Completado (`COMPLETED`):** la prueba se ha marcado como
    terminada.

No habrá estados adicionales como pendiente o bloqueado. Un test case
nuevo comenzará por defecto en `IN_PROGRESS`, y el usuario podrá cambiar
su estado al editarlo. Para marcar un test case como `COMPLETED`, deberá
tener al menos un paso y un resultado esperado no vacío.

El estado indica el progreso de la prueba, no si el resultado de la
ejecución ha sido satisfactorio. En el alcance actual no se ha definido
un campo independiente para registrar si una ejecución ha pasado o
fallado.

### 3.5. Prioridades

Cada test case tendrá una única prioridad, con estos valores permitidos:
alta (`HIGH`), media (`MEDIUM`) y baja (`LOW`).

La prioridad se utilizará para clasificar las pruebas y para generar las
estadísticas del proyecto.

### 3.6. Etiquetas globales y normalización

Las etiquetas serán **globales**, de modo que una misma etiqueta pueda
reutilizarse en test cases de distintos proyectos.

Un test case podrá tener varias etiquetas. Algunos ejemplos son
`backend`, `frontend`, `funcional` o `regresion`.

Al crear o modificar etiquetas, el backend normalizará su nombre antes
de guardarlo o buscar una etiqueta existente:

1.  Eliminar los espacios iniciales y finales.
2.  Convertir el texto a minúsculas.
3.  Eliminar las tildes y marcas diacríticas.
4.  Buscar una etiqueta con el mismo nombre normalizado.
5.  Reutilizar la etiqueta existente si ya existe; crearla solo si no
    existe ninguna equivalente.

Por ejemplo, `Regresión`, `REGRESIÓN`, `regresion` y ` regresion ` deben
resolverse como la misma etiqueta.

El nombre visible y el nombre normalizado de cada etiqueta tendrán un
máximo de 50 caracteres. Se recomienda guardar tanto el nombre de la
etiqueta como su nombre normalizado. La base de datos deberá garantizar
que el nombre normalizado sea único para evitar duplicados, incluso si
dos solicitudes intentan crear la misma etiqueta al mismo tiempo.

La eliminación de un test case o de un proyecto eliminará sus
asociaciones con etiquetas, pero no borrará las etiquetas globales. La
gestión independiente (listar, renombrar o eliminar etiquetas globales)
no forma parte del alcance acordado hasta ahora.

### 3.7. Dashboard y estadísticas por proyecto

Cada proyecto tendrá un apartado de estadísticas. Los datos se
calcularán a partir de los test cases que pertenezcan al proyecto
seleccionado. Todas las métricas se representarán mediante indicadores
circulares.

#### A. Progreso de los test cases

Se mostrará el porcentaje de test cases completados mediante un
indicador circular de progreso.

**Fórmula:**

`test cases completados / total de test cases × 100`

Si un proyecto no tiene test cases, se mostrará un progreso del 0 % para
evitar dividir entre cero.

#### B. Distribución por etiquetas

Se mostrará un indicador circular para cada etiqueta con el porcentaje
de test cases asociados a ella. Un mismo test case puede tener varias
etiquetas, por lo que puede contabilizarse en varias categorías y la
suma de los porcentajes puede superar el 100 %.

Para cada etiqueta:

`test cases del proyecto con esa etiqueta / total de test cases del proyecto × 100`

Las etiquetas que no estén asociadas a ningún test case del proyecto no
aparecerán en su distribución.

#### C. Distribución por prioridades

Se mostrará un indicador circular para cada prioridad con el porcentaje
de test cases correspondiente a ella.

Para cada prioridad:

`test cases del proyecto con esa prioridad / total de test cases del proyecto × 100`

Como cada test case tendrá una sola prioridad, los porcentajes deberían
sumar aproximadamente el 100 %, siempre que todos los test cases tengan
una prioridad asignada. Si el proyecto no contiene test cases, los
porcentajes se mostrarán como 0 % o como datos vacíos, sin realizar
divisiones entre cero.

Las estadísticas serán calculadas por el backend y devueltas al frontend
mediante una respuesta estructurada, por ejemplo, un DTO específico para
el dashboard. No se guardarán como valores independientes en la base de
datos: se recalcularán a partir de los datos existentes para evitar
inconsistencias.

## 4. Modelo de datos conceptual

El modelo inicial tendrá estas entidades y relaciones. Todos los campos
de texto usarán `VARCHAR(255)`, excepto `Tag.name` y
`Tag.normalizedName`, que usarán `VARCHAR(50)`.

### `User`

-   `id`
-   `username`
-   `email`
-   `passwordHash`

Los campos definitivos y sus validaciones se concretarán en la
especificación de autenticación. La contraseña se almacenará mediante un
hash seguro, nunca en texto plano.

### `Project`

-   `id`
-   `userId` --- usuario propietario del proyecto
-   `title` --- `VARCHAR(255)`

Relación: un proyecto puede tener muchos test cases.

### `TestCase`

-   `id`
-   `projectId`
-   `title` --- `VARCHAR(255)`
-   `description` --- `VARCHAR(255)`, opcional
-   `expectedResult` --- `VARCHAR(255)`, opcional
-   `priority`
-   `status`
-   `createdAt`

Relaciones: - Pertenece a un proyecto. - Tiene una lista ordenada de
pasos. - Puede estar asociado a varias etiquetas.

### `TestStep`

-   `id`
-   `testCaseId`
-   `instruction` --- `VARCHAR(255)`
-   `position`

Cada paso pertenece a un test case. La posición permite recuperar los
pasos en el orden correcto.

### `Tag`

-   `id`
-   `name` --- `VARCHAR(50)`
-   `normalizedName` --- `VARCHAR(50)`, único

Las etiquetas son globales y `normalizedName` debe ser único.

### `TestCaseTag`

Entidad de relación entre `TestCase` y `Tag`, que permite asociar varias
etiquetas a un test case y reutilizar una etiqueta en muchos test cases.

### Relaciones resumidas

-   `User` 1 --- N `Project`
-   `Project` 1 --- N `TestCase`
-   `TestCase` 1 --- N `TestStep`
-   `TestCase` N --- M `Tag`, mediante `TestCaseTag`

Las estadísticas se obtendrán mediante consultas o lógica de servicio a
partir de estas relaciones; no necesitan entidades propias.

## 5. Reglas de negocio

1.  Un proyecto solo necesita un título y queda asociado al usuario
    autenticado que lo crea.
2.  Los test cases siempre pertenecen a un proyecto.
3.  Cada test case tiene una prioridad obligatoria (`HIGH`, `MEDIUM` o
    `LOW`) y un estado obligatorio.
4.  Los únicos estados disponibles son `IN_PROGRESS` y `COMPLETED`; el
    estado inicial es `IN_PROGRESS`. Para cambiar a `COMPLETED`, debe
    existir al menos un paso y un resultado esperado no vacío.
5.  Los pasos de un test case tienen un orden explícito y se pueden
    añadir, editar, eliminar y reordenar.
6.  Un test case puede tener cero, una o varias etiquetas, salvo que
    posteriormente se decida hacer obligatoria alguna etiqueta.
7.  Las etiquetas se reutilizan globalmente mediante su nombre
    normalizado.
8.  La normalización de etiquetas se realiza en el backend para que no
    dependa del cliente que haga la petición.
9.  El borrado de un proyecto elimina sus test cases y los datos que
    dependan de ellos, pero no las etiquetas globales.
10. Las estadísticas se calculan usando exclusivamente los test cases
    del proyecto consultado.
11. Un test case completado cuenta como completado para el indicador de
    progreso, con independencia del resultado de ejecución, ya que el
    resultado de pasar o fallar no forma parte del modelo definido hasta
    ahora.

## 6. Arquitectura y responsabilidades

### Frontend --- React + TypeScript

Será responsable de:

-   Mostrar el listado y los formularios de proyectos.
-   Mostrar los test cases de un proyecto.
-   Ofrecer formularios para crear y editar test cases, incluidos sus
    pasos, prioridad, estado y etiquetas.
-   Mostrar el detalle de cada test case.
-   Presentar el dashboard y sus gráficos o indicadores.
-   Validar los formularios en el cliente para mejorar la experiencia de
    uso.

Las validaciones del frontend no sustituyen a las validaciones del
backend. Los títulos de proyecto y test case, las descripciones, los
resultados esperados y las instrucciones de los pasos tendrán un máximo
de 255 caracteres. Los nombres de etiquetas tendrán un máximo de 50
caracteres.

### Backend --- Java + Spring Boot

Será responsable de:

-   Exponer una API REST para el registro, inicio de sesión y las
    operaciones de proyectos y test cases.
-   Garantizar que cada usuario solo pueda consultar y modificar sus
    propios proyectos y los test cases asociados.
-   Validar las peticiones y aplicar las reglas de negocio.
-   Normalizar y reutilizar las etiquetas.
-   Gestionar las relaciones entre proyectos, test cases, pasos y
    etiquetas.
-   Calcular las estadísticas de cada proyecto.
-   Gestionar la persistencia y las operaciones de borrado de forma
    consistente.

### Arquitectura hexagonal

Se organizará el backend para separar:

-   **Dominio:** conceptos y reglas de negocio, como proyectos, test
    cases, estados y normalización de etiquetas.
-   **Aplicación:** casos de uso, como crear un proyecto, editar un test
    case o consultar estadísticas.
-   **Puertos:** contratos que necesita la aplicación para acceder a
    datos u otros recursos.
-   **Adaptadores:** implementación de persistencia y adaptación de las
    peticiones/respuestas HTTP de la API REST.

La arquitectura se mantendrá proporcionada: se busca separación de
responsabilidades y facilidad de pruebas, no añadir complejidad
innecesaria.

### SDD --- Spec-Driven Development

Antes de implementar cada parte se definirán sus requisitos y criterios
de aceptación. Las especificaciones se podrán dividir por
funcionalidades, por ejemplo:

1.  Gestión de proyectos.
2.  Gestión de test cases y pasos.
3.  Etiquetas y normalización.
4.  Estadísticas del proyecto.
5.  Integración frontend-backend y validaciones.

## 7. Criterios de aceptación generales

-   Un usuario puede registrarse e iniciar sesión.
-   Cada usuario solo puede acceder a sus propios proyectos y a los test
    cases de estos.
-   Se puede crear un proyecto indicando un título y verlo en el
    listado.
-   Al entrar en un proyecto se muestran únicamente sus test cases.
-   Se pueden crear, consultar, editar y eliminar test cases.
-   El detalle de un test case muestra su información y los pasos en el
    orden correcto.
-   Los pasos se pueden añadir, editar, eliminar y reordenar.
-   Un test case solo puede pasar a `COMPLETED` si tiene al menos un
    paso y un resultado esperado no vacío.
-   Los campos de texto generales admiten hasta 255 caracteres y las
    etiquetas hasta 50 caracteres.
-   Cada test case solo puede tener uno de los dos estados permitidos.
-   Se pueden asignar varias etiquetas a un test case.
-   Variantes de una etiqueta que solo cambian en mayúsculas, espacios
    exteriores o tildes se reutilizan como una misma etiqueta.
-   El borrado de un proyecto no deja test cases huérfanos y no elimina
    las etiquetas globales.
-   Los indicadores circulares reflejan el progreso general, la
    distribución por etiquetas y la distribución por prioridades.
-   La distribución por etiquetas cuenta un test case en cada etiqueta
    que tenga asignada.
-   La distribución por prioridades refleja la proporción de test cases
    de cada prioridad.
-   Las estadísticas de un proyecto no incluyen los test cases de otros
    proyectos.
-   Los proyectos sin test cases muestran estadísticas sin errores de
    división entre cero.

## 8. Fuera del alcance actual

No se consideran requisitos acordados, por ahora:

-   Roles y permisos avanzados. La autenticación básica, el registro y
    el inicio de sesión sí forman parte del alcance.
-   Colaboración entre usuarios o asignación de test cases a personas.
-   Automatización de pruebas o ejecución automática de test cases.
-   Registro de resultados de ejecución como `PASSED` o `FAILED`.
-   Historial de ejecuciones, evidencias, capturas de pantalla o
    adjuntos.
-   Comentarios, auditoría o historial de cambios.
-   Importación o exportación de test cases.
-   Gestión independiente de etiquetas globales (por ejemplo, una
    pantalla para renombrarlas o eliminarlas).
-   Filtros avanzados, búsqueda, paginación o notificaciones.
-   Despliegue. La base de datos elegida será MySQL.

Estos puntos podrían añadirse en futuras versiones si se consideran
necesarios.

## 9. Decisiones pendientes antes de implementar

El alcance funcional principal está definido, pero conviene cerrar estas
decisiones durante el diseño detallado:

1.  **Autenticación:** elegir el mecanismo concreto (por ejemplo, JWT) y
    definir los campos del usuario, el flujo de registro y el inicio de
    sesión.
2.  **Validaciones detalladas:** decidir si hay límites adicionales o
    reglas de formato para los datos del usuario, además de los límites
    ya definidos para los test cases.
3.  **Estadísticas circulares:** concretar la disposición visual cuando
    haya muchas etiquetas, ya que cada etiqueta tendrá su propio
    indicador circular.

Las decisiones funcionales principales están cerradas. Estos detalles
pueden concretarse al redactar las especificaciones SDD.
