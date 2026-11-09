# Spec — Autenticación y usuarios

## Contexto y objetivo

TestCase Manager gestiona proyectos y casos de prueba **privados de cada
usuario**. Sin autenticación no hay forma de garantizar que solo el dueño
acceda a sus datos. Esta spec define el registro de usuarios, el inicio de
sesión y la validación de la sesión. El objetivo es que un usuario pueda
crear su cuenta, identificarse y que el resto de la API pueda saber en todo
momento quién está conectado, sin exponer información sensible.

## Requisitos funcionales (RF) — EARS

### Registro

- **RF-001** — CUANDO un visitante proporcione un username, un email, una
  contraseña y su confirmación válidos, el sistema debe crear la cuenta y
  responder con un código **201** que confirme el registro. La respuesta
  no concede sesión: tras registrarse, el frontend decidirá si inicia
  sesión automáticamente (llamando al inicio de sesión) o muestra la
  página de login.
  - *Por qué:* crear la cuenta y abrir sesión son pasos separados; el
    frontend elige su flujo de usuario.

- **RF-002** — El username debe ser obligatorio y tener entre 3 y 50
  caracteres. No se recorta: se guarda y se valida tal cual lo escribe el
  usuario, incluidos los espacios exteriores. Sí se rechazan con un error
  de validación (400) los usernames en blanco o compuestos solo por
  espacios.
  - *Por qué:* el sistema no altera lo que el usuario eligió; los espacios
    cuentan para la longitud y `Ana` y ` Ana` son usernames distintos.

- **RF-003** — El email debe ser obligatorio y tener un formato válido.
  - *Por qué:* sirve como identificador de contacto y como alternativa de
    inicio de sesión.

- **RF-004** — La contraseña debe tener entre 8 y 20 caracteres, no puede
  estar en blanco ni contener espacios; si los contiene, se rechaza con un
  error de validación (400).
  - *Por qué:* la longitud aporta seguridad práctica sin reglas de
    complejidad; el límite superior evita tecleos accidentales largos. El
    límite se aplica a la contraseña que escribe el usuario, no a su
    representación cifrada, que ocupa más.

- **RF-005** — El registro debe pedir la contraseña dos veces (contraseña y
  confirmación) y, SI las dos no coinciden, debe rechazar la petición con un
  error de validación (400) que indique el campo afectado, sin crear la
  cuenta.
  - *Por qué:* evita que el usuario se registre con una contraseña con un
    error de tecleo que después no podrá usar.

- **RF-006** — SI el username proporcionado ya pertenece a otra cuenta, el
  sistema debe rechazar el registro con un error de conflicto (409) e
  indicar que el username no está disponible.
  - *Por qué:* el username es único y el usuario debe saber por qué falló
    el registro.

- **RF-007** — SI el email proporcionado ya pertenece a otra cuenta, el
  sistema debe rechazar el registro con un error de conflicto (409) e
  indicar que el email no está disponible.
  - *Por qué:* cada cuenta debe tener un email único para poder usarlo en el
    inicio de sesión sin ambigüedad.

- **RF-008** — El username y el email deben considerarse únicos **sin
  distinguir mayúsculas y minúsculas**: no podrán coexistir dos cuentas cuyo
  username o email solo difieran en mayúsculas (ej. `Ana` y `ana`).
  - *Por qué:* la base de datos compara ambos campos sin distinguir
    mayúsculas en su índice único; la regla se alinea con el comportamiento
    real del sistema y evita duplicados confusos.

- **RF-009** — La contraseña debe cifrarse con BCrypt (a través del
  `PasswordEncoder` de Spring Security) antes de guardarse; nunca debe
  almacenarse ni registrarse en texto plano.
  - *Por qué:* si la base de datos se filtra, las contraseñas no deben
    poder descifrarse. BCrypt aplica sal automática y coste configurable.

### Inicio de sesión

- **RF-010** — CUANDO un usuario proporcione un identificador (username o
  email) y una contraseña correctos, el sistema debe concederle una sesión
  respondiendo con un código **200** y un token de acceso que lo identifica.
  - *Por qué:* el frontend necesita un token para hacer peticiones
    autenticadas; el usuario puede identificarse con cualquiera de sus dos
    identificadores.

- **RF-011** — En el inicio de sesión no se aplica ningún recorte de
  espacios: el identificador y la contraseña se comparan tal cual los
  escribe el usuario. Así, `  Ana` no coincide con el username `Ana`.
  - *Por qué:* las credenciales se comparan de forma literal; lo que se
    guardó limpio en el registro debe escribirse igual al entrar.

- **RF-012** — El token de acceso debe caducar transcurrido un tiempo
  configurable, con un valor por defecto de 24 horas.
  - *Por qué:* limita el impacto de un token robado o perdido; la
    expiración debe poder ajustarse sin cambiar código.

- **RF-013** — SI el identificador no corresponde a ninguna cuenta O la
  contraseña no coincide, el sistema debe rechazar el login con un error
  401 y un mensaje genérico idéntico en ambos casos (ej.
  «credenciales inválidas»).
  - *Por qué:* no se debe revelar si un username o email existen en el
    sistema, para impedir que se enumeren usuarios registrados.

### Validación de sesión

- **RF-014** — CUANDO el frontend presente un token válido, el sistema debe
  responder con los datos del usuario autenticado (identificador, username y
  email).
  - *Por qué:* el frontend necesita restaurar la sesión al cargar la
    aplicación y mostrar a quién pertenece.

- **RF-015** — SI el token presentado no existe, no es válido o ha caducado,
  el sistema debe responder con un error 401; el sistema no distingue entre
  token caducado e inválido: ambos reciben el mismo código y tratamiento.
  - *Por qué:* el frontend trata ambos casos igual (redirigir al login); no
    hace falta diferenciarlos.

- **RF-016** — El token identifica al usuario autenticado para el acceso al
  resto de recursos de la API, que solo verán sus propios datos.
  - *Por qué:* es la base sobre la que se apoyan las specs de proyectos y
    test cases.

### Roles

- **RF-017** — Toda cuenta debe tener un rol: `USER` al registrarse y `ADMIN`
  para las cuentas que se designen manualmente en la base de datos. El token
  debe llevar el rol y la respuesta del usuario autenticado debe exponerlo.
  - *Por qué:* el frontend necesita distinguir al administrador y las specs
    siguientes podrán restringir operaciones a `ADMIN`.

## Fuera de alcance

- Gestión de cuenta: cambiar contraseña, editar username/email, borrar
  cuenta.
- Refresh token y renovación automática de la sesión.
- Logout en el servidor (el frontend elimina el token y la sesión termina
  en el cliente).
- Recuperación de contraseña olvidada.
- Verificación del email (doble opt-in).
- Bloqueo de cuentas, límite de intentos (rate limiting) y CAPTCHA.
- Roles, permisos y administración de usuarios: el rol `ADMIN` se asigna
  manualmente en la base de datos y no hay endpoints de gestión. Quedan fuera
  los permisos por recurso más allá de la distinción `ADMIN`/`USER`.

## Criterios de finalización

- [x] El registro con datos válidos crea la cuenta y responde 201 sin
  conceder sesión (RF-001).
- [x] El username admite 3–50 caracteres sin recortar espacios y los
  usernames en blanco o de solo espacios se rechazan con 400 (RF-002).
- [x] El email es obligatorio y con formato válido (RF-003).
- [x] La contraseña admite 8–20 caracteres; en blanco o con espacios se
  rechaza con 400 (RF-004).
- [x] El registro pide la contraseña dos veces y rechaza con 400 si no
  coinciden, sin crear la cuenta (RF-005).
- [x] Un registro con username o email ya existentes responde 409 con
  mensaje claro (RF-006, RF-007).
- [ ] No pueden coexistir usernames o emails que solo difieran en
  mayúsculas (RF-008). _Pendiente de verificación manual contra la BD con
  colación `utf8mb4_0900_ai_ci` y el índice único del script SQL; sin test
  automatizado, según `plan.md`._
- [x] Ninguna contraseña se almacena en texto plano: se cifra con BCrypt vía
  `PasswordEncoder` (RF-009).
- [x] El login funciona indistintamente con username o email y responde 200
  con token (RF-010).
- [x] El login no recorta espacios: `  Ana` no coincide con `Ana`
  (RF-011).
- [x] Un login fallido devuelve 401 con el mismo mensaje exista o no el
  usuario (RF-013).
- [x] Toda cuenta nueva se crea con rol `USER`; el rol `ADMIN` se asigna en
  la base de datos y el token lleva el rol correspondiente (RF-017).
- [x] El token caduca según la propiedad configurable (RF-012); token
  caducado e inválido devuelven el mismo 401 (RF-015).
- [x] La validación de sesión devuelve id, username y email con token
  válido (RF-014).
- [ ] Los RF se han verificado con tests automatizados en verde. _RF-008
  queda pendiente de verificación manual contra la BD._
