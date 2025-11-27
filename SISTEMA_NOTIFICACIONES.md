# Sistema de Notificaciones Push con Firebase Cloud Messaging

## 📱 Descripción

Sistema completo de notificaciones push en tiempo real para la aplicación móvil LivUp usando Firebase Cloud Messaging (FCM).

---

## 🔧 Configuración del Backend

### 1. Dependencias Maven

Ya agregadas en `pom.xml`:
```xml
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>
```

### 2. Configuración de Firebase

#### Paso 1: Crear proyecto en Firebase Console
1. Ve a https://console.firebase.google.com/
2. Crea un nuevo proyecto o usa uno existente
3. En "Configuración del proyecto" > "Cuentas de servicio"
4. Genera una nueva clave privada (archivo JSON)
5. Guarda el archivo como `firebase-service-account.json`

#### Paso 2: Agregar configuración en application.properties
```properties
# Ruta al archivo de configuración de Firebase
firebase.config.path=c:/ruta/a/firebase-service-account.json
```

**Nota:** Para producción, usa variables de entorno:
```properties
firebase.config.path=${FIREBASE_CONFIG_PATH}
```

### 3. Estructura de Base de Datos

Se crean automáticamente 2 nuevas tablas:

#### Tabla: `device_tokens`
- Almacena los tokens FCM de los dispositivos de cada usuario
- Campos: `id`, `usuario_id`, `fcm_token`, `device_type`, `device_name`, `is_active`, `created_at`, `updated_at`

#### Tabla: `notificaciones`
- Registro histórico de todas las notificaciones
- Campos: `id`, `usuario_id`, `tipo`, `titulo`, `mensaje`, `entidad_tipo`, `entidad_id`, `leida`, `enviada_push`, `created_at`

---

## 📲 Integración en Flutter (App Móvil)

### 1. Agregar dependencia FCM

En `pubspec.yaml`:
```yaml
dependencies:
  firebase_core: ^2.24.0
  firebase_messaging: ^14.7.0
```

### 2. Configurar Firebase en Flutter

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

### 3. Obtener y registrar el token FCM

```dart
class NotificationService {
  final FirebaseMessaging _messaging = FirebaseMessaging.instance;
  
  Future<void> initialize() async {
    // Obtener token FCM
    String? token = await _messaging.getToken();
    
    if (token != null) {
      // Registrar token en el backend
      await registerToken(token);
    }
    
    // Escuchar cambios de token
    _messaging.onTokenRefresh.listen(registerToken);
    
    // Escuchar notificaciones en primer plano
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      print('Notificación recibida: ${message.notification?.title}');
      // Mostrar notificación local
      _showLocalNotification(message);
    });
    
    // Manejar tap en notificación
    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      print('Notificación abierta: ${message.data}');
      // Navegar a la pantalla correspondiente
      _handleNotificationTap(message.data);
    });
  }
  
  Future<void> registerToken(String token) async {
    final response = await http.post(
      Uri.parse('http://tu-api.com/api/notifications/register-token'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $jwtToken',
      },
      body: json.encode({
        'fcmToken': token,
        'deviceType': Platform.isAndroid ? 'android' : 'ios',
        'deviceName': await _getDeviceName(),
      }),
    );
  }
  
  void _handleNotificationTap(Map<String, dynamic> data) {
    String? tipo = data['tipo'];
    String? entidadId = data['entidadId'];
    
    switch (tipo) {
      case 'SOLICITUD_NUEVA':
        // Navegar a pantalla de solicitudes
        break;
      case 'SOLICITUD_ACEPTADA':
        // Navegar a detalle de solicitud
        break;
      case 'CONTRATO_NUEVO':
        // Navegar a contratos
        break;
      default:
        // Navegar a notificaciones
    }
  }
}
```

---

## 🔔 Endpoints de la API

### 1. Registrar Token FCM
```
POST /api/notifications/register-token
Authorization: Bearer {JWT_TOKEN}

Body:
{
  "fcmToken": "token_fcm_del_dispositivo",
  "deviceType": "android|ios|web",
  "deviceName": "Nombre del dispositivo"
}
```

### 2. Eliminar Token FCM
```
DELETE /api/notifications/unregister-token

Body:
{
  "fcmToken": "token_a_eliminar"
}
```

### 3. Obtener Notificaciones del Usuario
```
GET /api/notifications/my-notifications
Authorization: Bearer {JWT_TOKEN}

Response: Array de notificaciones
```

### 4. Obtener Notificaciones No Leídas
```
GET /api/notifications/unread
Authorization: Bearer {JWT_TOKEN}

Response: Array de notificaciones no leídas
```

### 5. Contar Notificaciones No Leídas
```
GET /api/notifications/unread/count
Authorization: Bearer {JWT_TOKEN}

Response:
{
  "count": 5
}
```

### 6. Marcar Notificación Como Leída
```
PUT /api/notifications/{id}/mark-as-read

Response:
{
  "message": "Notificación marcada como leída"
}
```

### 7. Marcar Todas Como Leídas
```
PUT /api/notifications/mark-all-as-read
Authorization: Bearer {JWT_TOKEN}

Response:
{
  "message": "Todas las notificaciones marcadas como leídas"
}
```

---

## 📨 Tipos de Notificaciones Implementadas

### 1. Solicitudes de Alojamiento

#### Nueva Solicitud (→ Propietario)
```
Tipo: SOLICITUD_NUEVA
Título: "Nueva solicitud de alojamiento"
Mensaje: "Tienes una nueva solicitud para [Nombre Residencia]"
```

#### Solicitud Aceptada (→ Cliente)
```
Tipo: SOLICITUD_ACEPTADA
Título: "Solicitud aceptada"
Mensaje: "Tu solicitud de alojamiento ha sido aceptada"
```

#### Solicitud Rechazada (→ Cliente)
```
Tipo: SOLICITUD_RECHAZADA
Título: "Solicitud rechazada"
Mensaje: "Tu solicitud de alojamiento ha sido rechazada"
```

---

## 🚀 Agregar Más Notificaciones

### Ejemplo: Notificación de Nuevo Contrato

En `ContratoServiceImpl.java`:

```java
@Autowired
private NotificationService notificationService;

@Override
public Contrato create(Contrato contrato) {
    Contrato saved = repository.save(contrato);
    
    // Notificar al arrendatario
    notificationService.createNotification(
        contrato.getUsuario(),
        "CONTRATO_NUEVO",
        "Nuevo contrato generado",
        "Se ha generado tu contrato para " + contrato.getHabitacion().getResidencia().getNombre(),
        "Contrato",
        saved.getId()
    );
    
    return saved;
}
```

### Ejemplo: Notificación de Pago Recibido

En `PagoServiceImpl.java`:

```java
@Override
public Pago create(Pago pago) {
    Pago saved = repository.save(pago);
    
    // Notificar al propietario
    if (pago.getContrato() != null && 
        pago.getContrato().getHabitacion() != null) {
        
        var propietario = pago.getContrato().getHabitacion()
            .getResidencia().getUsuario();
        
        notificationService.createNotification(
            propietario,
            "PAGO_RECIBIDO",
            "Nuevo pago recibido",
            "Se registró un pago de S/ " + pago.getMonto(),
            "Pago",
            saved.getId()
        );
    }
    
    return saved;
}
```

---

## 📋 Checklist de Implementación

### Backend ✅
- [x] Agregar dependencia Firebase Admin SDK
- [x] Crear entidades `DeviceToken` y `Notificacion`
- [x] Crear repositorios
- [x] Implementar `NotificationService`
- [x] Crear `NotificationController`
- [x] Integrar notificaciones en `SolicitudAlojamientoServiceImpl`
- [ ] Agregar notificaciones en `ContratoServiceImpl`
- [ ] Agregar notificaciones en `PagoServiceImpl`
- [ ] Configurar Firebase (archivo JSON y properties)

### Frontend (Flutter) 📱
- [ ] Agregar dependencias FCM
- [ ] Inicializar Firebase
- [ ] Solicitar permisos de notificaciones
- [ ] Obtener y registrar token FCM
- [ ] Escuchar notificaciones
- [ ] Manejar tap en notificaciones
- [ ] Mostrar badge con contador de no leídas
- [ ] Crear pantalla de notificaciones

---

## 🔒 Seguridad

1. **Archivo de Configuración Firebase:**
   - Nunca subas `firebase-service-account.json` a git
   - Agrégalo a `.gitignore`
   - En producción usa variables de entorno

2. **Tokens FCM:**
   - Los tokens se validan en cada envío
   - Los tokens inválidos se marcan como inactivos automáticamente
   - Los usuarios solo pueden registrar tokens para su propia cuenta (validación JWT)

3. **Notificaciones:**
   - Solo el usuario autenticado puede ver sus notificaciones
   - Los endpoints están protegidos con JWT

---

## 🧪 Pruebas

### Probar notificación manualmente (Postman):

```
POST http://localhost:8080/api/notifications/test
Authorization: Bearer {JWT_TOKEN}

Body:
{
  "titulo": "Notificación de prueba",
  "mensaje": "Este es un mensaje de prueba"
}
```

### Monitorear en consola:
```bash
# Logs de Firebase
[NotificationService] Firebase inicializado correctamente
[NotificationService] Push enviado: projects/...

# Logs de error
[NotificationService] Error al enviar push a token ...: invalid-registration-token
```

---

## 📞 Soporte

Para más información sobre Firebase Cloud Messaging:
- Documentación oficial: https://firebase.google.com/docs/cloud-messaging
- Flutter FCM: https://firebase.flutter.dev/docs/messaging/overview
