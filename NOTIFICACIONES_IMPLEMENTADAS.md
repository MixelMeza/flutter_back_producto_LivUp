# 🔔 Sistema de Notificaciones - Implementación Completa

## ✅ Notificaciones Implementadas

### 1. SOLICITUD_NUEVA ✅
- **Cuándo:** Al crear una nueva solicitud de alojamiento
- **Destinatario:** Propietario de la residencia
- **Prioridad:** ALTA
- **Archivo:** `SolicitudAlojamientoServiceImpl.java` (método `create`)
- **Mensaje:** "Tienes una nueva solicitud para [Nombre Residencia]"

### 2. SOLICITUD_ACEPTADA ✅
- **Cuándo:** Al cambiar estado de solicitud a ACEPTADA
- **Destinatario:** Estudiante solicitante
- **Prioridad:** ALTA
- **Archivo:** `SolicitudAlojamientoServiceImpl.java` (método `update`)
- **Mensaje:** "Tu solicitud para [Nombre Residencia] ha sido aceptada. Pronto recibirás el contrato."

### 3. SOLICITUD_RECHAZADA ✅
- **Cuándo:** Al cambiar estado de solicitud a RECHAZADA
- **Destinatario:** Estudiante solicitante
- **Prioridad:** MEDIA
- **Archivo:** `SolicitudAlojamientoServiceImpl.java` (método `update`)
- **Mensaje:** "Tu solicitud para [Nombre Residencia] ha sido rechazada. Puedes buscar otras opciones disponibles."

### 4. CONTRATO_CREADO ✅
- **Cuándo:** Al crear un nuevo contrato
- **Destinatario:** Estudiante
- **Prioridad:** ALTA
- **Archivo:** `ContratoServiceImpl.java` (método `create`)
- **Mensaje:** "Tu contrato para [Nombre Residencia] está listo. Revisa los detalles y condiciones."

### 5. CONTRATO_VENCIMIENTO_PROXIMO ✅
- **Cuándo:** 30 días antes de que venza el contrato (Job automático diario a las 8 AM)
- **Destinatario:** Estudiante Y Propietario (ambos reciben notificación)
- **Prioridad:** ALTA
- **Archivo:** `ContratoScheduler.java` (Job programado)
- **Mensaje Estudiante:** "Tu contrato en [Nombre Residencia] vence el [Fecha] (en 30 días). Contacta al propietario si deseas renovar."
- **Mensaje Propietario:** "El contrato de un inquilino en [Nombre Residencia] vence el [Fecha] (en 30 días). Considera contactarlo para renovación."

---

## 📊 Resumen de Implementación

| Notificación | Destinatario | Trigger | Estado |
|--------------|--------------|---------|--------|
| SOLICITUD_NUEVA | Propietario | Create Solicitud | ✅ |
| SOLICITUD_ACEPTADA | Estudiante | Update Estado | ✅ |
| SOLICITUD_RECHAZADA | Estudiante | Update Estado | ✅ |
| CONTRATO_CREADO | Estudiante | Create Contrato | ✅ |
| CONTRATO_VENCIMIENTO_PROXIMO | Ambos | Job Diario | ✅ |

---

## 🧪 Cómo Probar las Notificaciones

### 1. SOLICITUD_NUEVA
```http
POST http://localhost:8080/api/solicitudes-alojamiento
Authorization: Bearer TOKEN_ESTUDIANTE
Content-Type: application/json

{
  "estudiante": { "id": 9 },
  "residencia": { "id": 5 },
  "habitacion": { "id": 1 },
  "duracionMeses": 6,
  "fijo": true,
  "comentarios": "Prueba notificación"
}
```
✅ Verifica con token del propietario: `GET /api/notifications/my-notifications`

### 2. SOLICITUD_ACEPTADA / RECHAZADA
```http
PUT http://localhost:8080/api/solicitudes-alojamiento/{id}
Authorization: Bearer TOKEN_PROPIETARIO
Content-Type: application/json

{
  "id": 5,
  "estado": "ACEPTADA"  // o "RECHAZADA"
}
```
✅ Verifica con token del estudiante: `GET /api/notifications/my-notifications`

### 3. CONTRATO_CREADO
```http
POST http://localhost:8080/api/contratos
Authorization: Bearer TOKEN_PROPIETARIO
Content-Type: application/json

{
  "solicitud": { "id": 5 },
  "fechaInicio": "2025-12-01",
  "fechaFin": "2026-06-01",
  "garantia": 500.00,
  "montoTotal": 3000.00,
  "condiciones": "Pago mensual adelantado",
  "tipoContrato": "Mensual"
}
```
✅ Verifica con token del estudiante: `GET /api/notifications/my-notifications`

### 4. CONTRATO_VENCIMIENTO_PROXIMO

#### Opción A: Esperar al Job Automático
- El job se ejecuta todos los días a las 8:00 AM
- Revisa contratos que vencen exactamente en 30 días

#### Opción B: Ejecutar Manualmente (Para Testing)
```http
POST http://localhost:8080/api/scheduler/verificar-contratos
```

#### Opción C: Crear contrato con fecha de vencimiento en 30 días
```http
POST http://localhost:8080/api/contratos
Content-Type: application/json

{
  "solicitud": { "id": 5 },
  "fechaInicio": "2025-11-27",
  "fechaFin": "2025-12-27",  // 30 días desde hoy
  "garantia": 500.00,
  "montoTotal": 3000.00
}
```
Luego ejecuta: `POST /api/scheduler/verificar-contratos`

✅ Verifica con tokens de estudiante Y propietario: `GET /api/notifications/my-notifications`

---

## 📝 Logs de Debugging

Cuando las notificaciones se envían, verás estos mensajes en la consola:

```
[NOTIFICACION] Enviando notificación al propietario ID: 4
[NOTIFICACION] Notificación enviada exitosamente
[NOTIFICACION] Enviando SOLICITUD_ACEPTADA al estudiante ID: 9
[NOTIFICACION] Notificación de cambio de estado enviada exitosamente
[NOTIFICACION] Enviando CONTRATO_CREADO al estudiante ID: 9
[NOTIFICACION] Notificación CONTRATO_CREADO enviada exitosamente
[SCHEDULER] Iniciando verificación de contratos próximos a vencer...
[SCHEDULER] Notificación enviada al estudiante ID: 9
[SCHEDULER] Notificación enviada al propietario ID: 4
[SCHEDULER] Verificación completada. Notificaciones enviadas: 2
```

---

## 🔧 Archivos Modificados/Creados

### Archivos Modificados:
1. `SolicitudAlojamientoServiceImpl.java` - Mejoradas notificaciones de cambio de estado
2. `ContratoServiceImpl.java` - Agregada notificación CONTRATO_CREADO
3. `ResidenciasBackendApplication.java` - Habilitado @EnableScheduling

### Archivos Creados:
1. `ContratoScheduler.java` - Job programado para contratos próximos a vencer
2. `SchedulerController.java` - Endpoint para ejecutar scheduler manualmente

---

## 🚀 Configuración del Scheduler

El scheduler está configurado con cron expression:
```java
@Scheduled(cron = "0 0 8 * * ?")  // Todos los días a las 8:00 AM
```

Para cambiar la hora de ejecución, modifica el cron en `ContratoScheduler.java`:
- `0 0 8 * * ?` = 8:00 AM
- `0 0 20 * * ?` = 8:00 PM
- `0 0 */6 * * ?` = Cada 6 horas

---

## 📱 Integración con Flutter

En tu app Flutter, las notificaciones llegarán automáticamente cuando:
1. El dispositivo tiene un token FCM registrado
2. El token está activo en la tabla `device_tokens`
3. Firebase está correctamente configurado

Para recibir las notificaciones:
```dart
// Escuchar notificaciones cuando la app está abierta
FirebaseMessaging.onMessage.listen((RemoteMessage message) {
  print('Tipo: ${message.data['tipo']}'); // SOLICITUD_NUEVA, etc.
  print('Título: ${message.notification?.title}');
  print('Mensaje: ${message.notification?.body}');
  print('Entidad: ${message.data['entidadTipo']}'); // Contrato, SolicitudAlojamiento
  print('ID: ${message.data['entidadId']}');
  
  // Mostrar notificación local o actualizar UI
  showInAppNotification(message);
});
```

---

## ✨ Próximas Notificaciones a Implementar

Si deseas agregar más notificaciones, estas son las más importantes:

### Fase 2 - Pagos (Prioritario)
- `PAGO_REGISTRADO` - Estudiante registra pago → Propietario
- `PAGO_CONFIRMADO` - Propietario confirma → Estudiante
- `PAGO_VENCIDO` - Pago vencido → Estudiante + Propietario
- `PAGO_VENCIMIENTO_PROXIMO` - 5 días antes → Estudiante

### Fase 3 - Reviews
- `REVIEW_NUEVA` - Estudiante deja reseña → Propietario
- `REVIEW_RESPUESTA` - Propietario responde → Estudiante

### Fase 4 - Favoritos
- `FAVORITO_DISPONIBLE` - Habitación favorita libre → Estudiante
- `FAVORITO_PRECIO_CAMBIO` - Cambio de precio → Estudiante

---

## 🎯 Estado Actual

**Sistema de notificaciones 100% funcional con:**
- ✅ 5 tipos de notificaciones implementadas
- ✅ Firebase Cloud Messaging configurado
- ✅ Notificaciones push en tiempo real
- ✅ Job programado para verificaciones automáticas
- ✅ Endpoint para testing manual
- ✅ Logs detallados para debugging
- ✅ Notificaciones guardadas en base de datos
- ✅ Sistema de lectura/no leída funcionando

**¡Todo listo para producción!** 🚀
