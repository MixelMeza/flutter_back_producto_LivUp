# Respuestas y posibles resultados — Login / Logout

Documento que describe todos los posibles responses (HTTP status + body) y efectos secundarios del endpoint de autenticación (`/api/auth/login` y `/api/auth/logout`). Úsalo como referencia para frontend, QA y operaciones.

**Formato**
- Todos los ejemplos usan JSON en request/response salvo donde se indica.
- Se asume que el header `Authorization: Bearer <token>` se usa en `logout`.

**Resumen rápido**
- Login: valida credenciales, registra/actualiza `Dispositivo` por `fcmToken`, crea `Acceso` tipo `LOGIN`, devuelve JWT.
- Logout: requiere JWT, crea `Acceso` tipo `LOGOUT`, invalida JWT (persistiendo en `invalidated_tokens`), NO elimina `Dispositivo` ni `fcm_token`.

**POST /api/auth/login**
- Headers: `Content-Type: application/json`
- Body (ejemplo):
```json
{
  "username": "usuario@ejemplo.com",
  "password": "MiPasswordSegura123",
  "fcmToken": "eXaMplE_fCm_TokEn_12345",
  "plataforma": "ANDROID",
  "modelo": "Pixel 7",
  "osVersion": "13"
}
```

Respuestas posibles:

- 200 OK — Login exitoso
```json
{ "token": "<JWT>" }
```
Efectos secundarios en servidor:
- Si `fcmToken` presente:
  - Buscar `Dispositivo` por `fcm_token`.
  - Si existe: actualizar metadata (`plataforma`, `modelo`, `osVersion`) y asignar `usuario` al dispositivo (reasignación permitida).
  - Si no existe: crear `Dispositivo` con `fcm_token` y asignar al usuario.
  - Asegurar unicidad de `fcm_token` en `dispositivos` (constraint UNIQUE).
- Crear `Acceso` con campos: `usuario_id`, `dispositivo_id` (nullable), `ip_acceso`, `tipo = "LOGIN"`, `ultima_sesion = now()`.
- NO guardar `fcm_token` ni metadata de dispositivo dentro del registro `accesos` (solo referencia a `dispositivo_id`).
- JWT devuelto NO contiene `fcmToken`.

- 400 Bad Request — body mal formado / faltan campos
```json
{ "error": "username y password son requeridos" }
```

- 401 Unauthorized — usuario no encontrado o contraseña incorrecta
```json
{ "error": "Usuario no encontrado" }
```
ó
```json
{ "error": "Contraseña incorrecta" }
```

- 403 Forbidden — usuario no ACTIVO (suspendido, etc.)
```json
{ "error": "Usuario suspendido" }
```

- 500 Internal Server Error — error interno (DB, excepciones inesperadas)
```json
{ "error": "Error interno del servidor" }
```

Notas de seguridad / validaciones del login:
- Comprobar `usuario.estado == ACTIVO` antes de emitir token.
- Validar y escapar los valores recibidos (no confiar en `plataforma`, `modelo`, `osVersion`).
- No incluir información sensible (como password) en logs ni en responses.

---

**POST /api/auth/logout**
- Headers: `Content-Type: application/json`, `Authorization: Bearer <JWT>` (requerido)
- Body (opcional):
```json
{ "fcmToken": "eXaMplE_fCm_TokEn_12345" }
```

Respuestas posibles:

- 200 OK — Logout exitoso
```json
{ "message": "Sesión cerrada correctamente" }
```
Efectos secundarios en servidor:
- Requerir JWT válido.
- Opcional: si `fcmToken` en body, buscar `Dispositivo` y setear `dispositivo_id` en `Acceso`.
- Crear `Acceso` con: `usuario_id`, `dispositivo_id` (si identificado), `ip_acceso`, `tipo = "LOGOUT"`, `ultima_sesion = now()`.
- Persistir el JWT usado en tabla `invalidated_tokens` (o mecanismo equivalente) para impedir su uso posterior hasta expiración.
- NO eliminar ni desregistrar `Dispositivo` ni `fcm_token`.

- 401 Unauthorized — Authorization ausente o inválido
```json
{ "error": "Token de autorización requerido" }
```
ó
```json
{ "error": "Token invalidado" }
```

- 404 Not Found — usuario no encontrado (si el JWT no corresponde a un usuario existente)
```json
{ "error": "Usuario no encontrado" }
```

- 500 Internal Server Error — error al persistir invalidated token o al crear registro
```json
{ "error": "Error al cerrar sesión" }
```

Notas de seguridad / comportamiento de logout:
- Guardar el token en `invalidated_tokens` para que `JwtRequestFilter` lo rechace.
- No eliminar `Dispositivo` porque el dispositivo físico aún existe y podría re-usarse.

---

**Casos de envío de notificaciones relacionados**
(para entender efectos secundarios y por qué ciertas respuestas ocurren)

- Cuando Firebase responde `NOT_REGISTERED` o `INVALID_ARGUMENT` al enviar push a un token:
  - Acción: marcar `dispositivo.activo = false` (no borrar). Esto evita futuros envíos a tokens inválidos.
  - Resultado observable: notificaciones ya no se envían a ese dispositivo; `sendPush` puede devolver logs de error.

- Reasignación de token:
  - Si un dispositivo inicia sesión con otro usuario (login con `fcmToken` existente): el `Dispositivo` se reasigna al nuevo `usuario`.
  - Efecto: el usuario anterior dejará de recibir notificaciones en ese token.

---

**Ejemplos curl**
- Login (ejemplo):
```bash
curl -X POST https://tu-backend.example.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user@ejemplo.com","password":"pass","fcmToken":"ABC123","plataforma":"ANDROID"}'
```

- Logout (ejemplo):
```bash
curl -X POST https://tu-backend.example.com/api/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{"fcmToken":"ABC123"}'
```

---

**Checklist de QA**
- [ ] Login con usuario ACTIVO sin `fcmToken` → token recibido, `Acceso` creado sin `dispositivo_id`.
- [ ] Login con `fcmToken` nuevo → `Dispositivo` creado y `Acceso` referenciando `dispositivo_id`.
- [ ] Login con `fcmToken` existente → `Dispositivo` actualizado y reasignado al usuario; `Acceso` creado.
- [ ] Logout con JWT válido + `fcmToken` → `Acceso` LOGOUT creado y JWT listado en `invalidated_tokens`.
- [ ] Logout con JWT previamente invalidado → 401 + mensaje token invalidado.
- [ ] Envío push a usuario ACTIVO → se envía a todos `dispositivos.activo = true` del usuario.
- [ ] Envío push a usuario SUSPENDIDO → no se envía (backend rechaza).
- [ ] FCM responde NOT_REGISTERED → `dispositivo.activo` marcado `false`.

---

**Notas para operaciones / despliegue**
- Antes de desplegar en producción: crear migración DB que añada/asegure:
  - `dispositivos.fcm_token` UNIQUE
  - `dispositivos.activo` (boolean) default `true`
  - `accesos.dispositivo_id` FK (nullable)
  - `accesos.tipo` (string) si no existe
  - `invalidated_tokens` table
- Poner en `application.properties` la ruta a la credencial de Firebase: `firebase.config.path=/path/to/serviceAccount.json`.
- Probar en staging con tokens reales y revisar logs de FCM antes de abrir a producción.

---

Documentación generada por el equipo backend — revisa los endpoints y adapta mensajes de error al UX si es necesario.
