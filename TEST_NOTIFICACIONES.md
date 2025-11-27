# 🔔 Guía de Pruebas - Sistema de Notificaciones Push

## ✅ Estado del Sistema
- **Firebase:** ✅ Inicializado correctamente
- **Backend:** ✅ Corriendo en puerto 8080
- **Base de datos:** ✅ Conectada

---

## 📋 Endpoints Disponibles

### 1️⃣ Registrar Token FCM (Para app móvil)
```http
POST http://localhost:8080/api/notifications/register-token
Authorization: Bearer TU_TOKEN_JWT
Content-Type: application/json

{
  "fcmToken": "token_generado_por_firebase_en_app",
  "deviceType": "android",
  "deviceName": "Samsung Galaxy S21"
}
```

### 2️⃣ Ver Mis Notificaciones
```http
GET http://localhost:8080/api/notifications/my-notifications
Authorization: Bearer TU_TOKEN_JWT
```

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "tipo": "SOLICITUD_NUEVA",
    "titulo": "Nueva solicitud de alojamiento",
    "mensaje": "Tienes una nueva solicitud para tu residencia",
    "leida": false,
    "enviadaPush": true,
    "createdAt": "2025-11-27T04:35:00"
  }
]
```

### 3️⃣ Contar No Leídas
```http
GET http://localhost:8080/api/notifications/unread/count
Authorization: Bearer TU_TOKEN_JWT
```

**Respuesta:**
```json
{
  "count": 5
}
```

### 4️⃣ Marcar como Leída
```http
PUT http://localhost:8080/api/notifications/1/mark-as-read
Authorization: Bearer TU_TOKEN_JWT
```

### 5️⃣ Marcar Todas como Leídas
```http
PUT http://localhost:8080/api/notifications/mark-all-as-read
Authorization: Bearer TU_TOKEN_JWT
```

---

## 🧪 Pruebas Paso a Paso

### **Prueba 1: Crear Solicitud y Verificar Notificación**

#### Paso 1: Login como estudiante
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "estudiante1",
  "password": "password123"
}
```
➡️ Guarda el `token` del estudiante

#### Paso 2: Crear solicitud de alojamiento
```http
POST http://localhost:8080/api/solicitudes-alojamiento
Authorization: Bearer TOKEN_ESTUDIANTE
Content-Type: application/json

{
  "estudianteId": 1,
  "residenciaId": 1,
  "duracionMeses": 6,
  "fijo": true,
  "comentarios": "Interesado en la habitación"
}
```

#### Paso 3: Login como propietario
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "propietario1",
  "password": "password123"
}
```
➡️ Guarda el `token` del propietario

#### Paso 4: Ver notificaciones del propietario
```http
GET http://localhost:8080/api/notifications/my-notifications
Authorization: Bearer TOKEN_PROPIETARIO
```

✅ **Resultado esperado:** Deberías ver una notificación tipo `SOLICITUD_NUEVA`

---

### **Prueba 2: Aceptar/Rechazar Solicitud**

#### Paso 1: Actualizar estado de solicitud (como propietario)
```http
PUT http://localhost:8080/api/solicitudes-alojamiento/1
Authorization: Bearer TOKEN_PROPIETARIO
Content-Type: application/json

{
  "id": 1,
  "estado": "ACEPTADA"
}
```

#### Paso 2: Ver notificaciones del estudiante
```http
GET http://localhost:8080/api/notifications/my-notifications
Authorization: Bearer TOKEN_ESTUDIANTE
```

✅ **Resultado esperado:** El estudiante verá notificación `SOLICITUD_ACEPTADA`

---

## 🗄️ Verificar en Base de Datos

Conéctate a la BD y ejecuta:

```sql
-- Ver todas las notificaciones
SELECT * FROM notificaciones ORDER BY created_at DESC;

-- Ver tokens de dispositivos registrados
SELECT * FROM device_tokens WHERE is_active = 1;

-- Ver notificaciones no leídas por usuario
SELECT n.*, u.username 
FROM notificaciones n
JOIN usuarios u ON n.usuario_id = u.id
WHERE n.leida = FALSE
ORDER BY n.created_at DESC;
```

---

## 📱 Integración con Flutter (Siguiente Paso)

Para recibir las notificaciones push en la app móvil, necesitarás:

### 1. Agregar Firebase a tu proyecto Flutter:
```yaml
# pubspec.yaml
dependencies:
  firebase_core: ^2.24.0
  firebase_messaging: ^14.7.0
```

### 2. Inicializar Firebase en Flutter:
```dart
// main.dart
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  
  // Solicitar permisos
  await FirebaseMessaging.instance.requestPermission();
  
  runApp(MyApp());
}
```

### 3. Obtener y registrar token FCM:
```dart
// Obtener token
String? token = await FirebaseMessaging.instance.getToken();

// Registrar en backend
final response = await http.post(
  Uri.parse('http://tu-servidor:8080/api/notifications/register-token'),
  headers: {
    'Authorization': 'Bearer $jwtToken',
    'Content-Type': 'application/json',
  },
  body: jsonEncode({
    'fcmToken': token,
    'deviceType': 'android',
    'deviceName': 'Mi dispositivo',
  }),
);
```

### 4. Escuchar notificaciones:
```dart
// Cuando la app está en foreground
FirebaseMessaging.onMessage.listen((RemoteMessage message) {
  print('Notificación recibida: ${message.notification?.title}');
  // Mostrar notificación local o actualizar UI
});

// Cuando el usuario toca la notificación
FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
  print('Notificación tocada: ${message.data}');
  // Navegar a la pantalla correspondiente
});
```

---

## 📊 Tipos de Notificaciones Actuales

| Tipo | Cuándo se envía | Destinatario |
|------|----------------|--------------|
| `SOLICITUD_NUEVA` | Cuando se crea una solicitud | Propietario de la residencia |
| `SOLICITUD_ACEPTADA` | Cuando se acepta una solicitud | Estudiante solicitante |
| `SOLICITUD_RECHAZADA` | Cuando se rechaza una solicitud | Estudiante solicitante |

---

## 🔮 Próximas Notificaciones a Implementar

- `CONTRATO_NUEVO`: Cuando se crea un contrato
- `PAGO_RECIBIDO`: Cuando se registra un pago
- `PAGO_VENCIDO`: Recordatorio de pago pendiente
- `REVIEW_NUEVA`: Cuando alguien deja una reseña
- `MENSAJE_NUEVO`: Chat entre usuarios

---

## 🐛 Troubleshooting

### Problema: No se crean notificaciones
✅ **Solución:** Verifica en los logs del backend que diga:
```
[NotificationService] Firebase inicializado correctamente
```

### Problema: Notificación en BD pero no llega push a móvil
✅ **Solución:** 
1. Verifica que el token FCM esté registrado: `SELECT * FROM device_tokens`
2. Verifica que `is_active = 1`
3. En Flutter, confirma que Firebase está inicializado

### Problema: Error "Token inválido"
✅ **Solución:** El token FCM puede expirar. La app debe:
```dart
FirebaseMessaging.instance.onTokenRefresh.listen((newToken) {
  // Re-registrar el nuevo token en el backend
  registerToken(newToken);
});
```

---

## ✨ ¡Sistema Listo!

Tu backend está completamente configurado para enviar notificaciones push. 
Ahora puedes:
1. ✅ Probar los endpoints con Postman
2. ✅ Verificar registros en la base de datos
3. ✅ Integrar con tu app Flutter para recibir notificaciones reales
