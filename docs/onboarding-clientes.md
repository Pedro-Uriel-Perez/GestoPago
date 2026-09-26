# Onboarding de Clientes Personas Físicas

## Objetivo
Registrar clientes personas físicas, validar su información, crear
automáticamente una cuenta bancaria con saldo inicial, y exponer las
consultas/actualizaciones solicitadas — todo en Postgres, siguiendo la
arquitectura por capas ya usada en el proyecto (`client`/`config`/`entity`/
`exception`/`mapper`/`model`/`repositorys`/`service`/`controller`).

## Diagrama entidad-relación

```mermaid
erDiagram
    CLIENTES ||--o| DOMICILIOS : "tiene"
    CLIENTES ||--o| CUENTAS : "tiene"
    CLIENTES ||--o| LOGINS : "tiene"
    CUENTAS ||--o{ SALDOS : "historial"

    CLIENTES {
        int id PK
        text nombre
        text segundo_nombre
        text apellido_paterno
        text apellido_materno
        date fecha_nacimiento
        text curp UK
        text rfc UK
        text sexo
        text nacionalidad
        text estado_civil
        text correo_electronico UK
        text telefono_movil
        text telefono_alternativo
        text ocupacion
        text empresa
        numeric ingreso_mensual
        boolean activo
        timestamp fecha_registro
        timestamp fecha_actualizacion
    }

    DOMICILIOS {
        int id PK
        int cliente_id FK
        text calle
        text numero_exterior
        text numero_interior
        text colonia
        text municipio
        text estado
        text codigo_postal
        text pais
    }

    CUENTAS {
        int id PK
        int cliente_id FK
        text numero_cuenta UK
        text estatus
        timestamp fecha_apertura
    }

    SALDOS {
        int id PK
        int cuenta_id FK
        numeric monto
        text tipo_movimiento
        timestamp fecha_movimiento
        text descripcion
    }

    LOGINS {
        int id PK
        int cliente_id FK
        text correo_login "cifrado"
        text nombre_cifrado "cifrado"
        text jwt_token "cifrado"
        text datos_biometricos "cifrado"
        boolean sesion_activa
        timestamp fecha_ultimo_acceso
        timestamp fecha_creacion
    }
```

## Script de creación de base de datos
Migraciones Flyway (`src/main/resources/db/migration/`):
`V4__create_clientes.sql`, `V5__create_domicilios.sql`,
`V6__create_cuentas.sql`, `V7__create_saldos.sql`, `V8__create_logins.sql`.

## Decisiones de diseño y por qué

### Tipos de datos
- **`TEXT` en vez de `VARCHAR`**: en PostgreSQL no hay diferencia de
  rendimiento entre ambos (documentado oficialmente); `TEXT` + `CHECK` para
  longitudes es la práctica recomendada.
- **Códigos postales, teléfonos, CURP, RFC, número de cuenta → `TEXT`**,
  no numérico: nunca se usan en operaciones aritméticas y pueden llevar
  ceros a la izquierda.
- **`ingreso_mensual` y `monto` → `NUMERIC`**, no `double`: el dinero no debe
  representarse con punto flotante binario (pierde precisión).
- **Campos categóricos** (`sexo`, `estado_civil`, `estatus`, `tipo_movimiento`)
  → `TEXT` + `CHECK constraint` en vez de `ENUM` de Postgres: más simple y
  no requiere `ALTER TYPE` para agregar valores nuevos después.

### `saldos` como historial (1:N), no una columna en `cuentas`
Cada movimiento de saldo (apertura, depósito, retiro, ajuste) es una fila
nueva con fecha. El saldo vigente de una cuenta es su registro más reciente
(`findFirstByCuentaIdOrderByFechaMovimientoDesc`), no una columna
desnormalizada — evita inconsistencias entre "el saldo" y "el historial".

### Datos biométricos: `double[]` en Java, `TEXT` cifrado en la base de datos
Los sistemas reales de reconocimiento facial (ej. **MediaPipe
Face Detection/Embedding**, o alternativas como `face_recognition`/FaceNet)
no comparan fotos directamente: generan un vector numérico ("embedding") de
decenas/cientos de posiciones. Por eso el campo se modeló como `double[]`
del lado de Java. Como además debe cifrarse, la columna en Postgres es
`TEXT` (el texto cifrado en Base64) — un valor cifrado deja de ser un
número que Postgres pueda leer como arreglo nativo.
**No se integra ninguna API de reconocimiento facial todavía** (tal como se
indicó explícitamente); el campo queda listo para cuando se implemente.

### Cifrado (tabla `logins`)
AES-256/GCM implementado en Java puro (sin librerías nuevas), vía
`AttributeConverter` de JPA (`AesStringConverter`, `AesDoubleArrayConverter`)
aplicados explícitamente con `@Convert` en cada campo sensible (`correo_login`,
`nombre_cifrado`, `jwt_token`, `datos_biometricos`). La clave se deriva con
PBKDF2 a partir de `security.aes.secret-key` (`application.properties`).

**`sesion_activa` y `fecha_ultimo_acceso` quedan sin cifrar a propósito**:
la tarea programada que cierra sesiones inactivas necesita compararlas
directamente en SQL (`WHERE fecha_ultimo_acceso < ahora - 5 minutos`). Si se
cifraran, habría que traer todas las sesiones activas a memoria y descifrar
una por una para compararlas — mucho menos eficiente.

### JWT
El enunciado pide "una API que proporcione el JWT" — se interpretó como un
endpoint propio (`POST /login`) que **emite** el token (usando la librería
`jjwt`, ya presente en el proyecto), no un servicio externo. De paso se
corrigió un bug preexistente en `build.gradle`: `jjwt-api` estaba declarado
dos veces con versiones distintas (0.12.6 y 0.11.5), y solo existía el
runtime (`jjwt-impl`/`jjwt-jackson`) para la 0.11.5 — si Gradle resolvía la
0.12.6, el JWT habría fallado en tiempo de ejecución por falta de
implementación. Se dejó consistente en 0.11.5.

**Supuesto importante**: el enunciado no define un mecanismo de contraseña
para el login del cliente (el registro tampoco captura una). Por ahora,
`POST /login` identifica al cliente solo por su correo registrado y le
emite un JWT. Esto es una simplificación deliberada, documentada aquí para
que se ajuste cuando se defina el mecanismo de autenticación real
(contraseña, biometría, etc.).

### Inactividad de sesión (5 minutos)
`LoginServiceImpl.cerrarSesionesInactivas()` corre en una tarea programada
(`@Scheduled`, cron configurable en `security.session.check-cron`, cada
minuto por defecto) que busca sesiones activas cuyo último acceso supera
`security.session.inactividad-minutos` (5) y las cierra (`sesion_activa =
false`, borra el JWT).

## Reglas de negocio y validaciones
Todas las validaciones viven en los DTOs `*Request` (Bean Validation), no en
el service — igual que el resto del proyecto:
- Mayoría de edad: anotación personalizada `@EdadMinima(18)` (con
  `@PastOrPresent` para la fecha no futura).
- CURP/RFC: `@Pattern` con la expresión regular oficial.
- Teléfono: `@Pattern` de 10 dígitos exactos.
- Código postal: `@Pattern` de 5 dígitos exactos.
- Ingreso mensual: `@DecimalMin("0.01")`.
- Unicidad de CURP/RFC/correo: se valida en el service (no es posible con
  Bean Validation, requiere consultar la base de datos), lanzando
  excepciones tipadas (`CurpDuplicadaException`, `RfcDuplicadoException`,
  `ClienteYaRegistradoException`) resueltas por `GlobalExceptionHandler` —
  sin `if`, usando `Optional.ifPresent(...)` para disparar la excepción.
- No se permite modificar CURP/RFC/número de cuenta: `ClienteActualizaRequest`
  simplemente no incluye esos campos.

## Excepciones personalizadas
`ClienteNoEncontradoException`, `CuentaNoEncontradaException`,
`CurpDuplicadaException` y `RfcDuplicadoException` (ambas heredan de
`ClienteYaRegistradoException`), manejadas en `GlobalExceptionHandler`
(409 para duplicados, 404 para no encontrados).

## Endpoints
| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/clientes` | Registra cliente + domicilio + cuenta + saldo inicial |
| GET | `/clientes` | Lista todos los clientes |
| GET | `/clientes/{id}` | Cliente por id |
| PUT | `/clientes/{id}` | Actualiza datos personales/contacto/domicilio/laborales |
| DELETE | `/clientes/{id}` | Baja lógica (activo = false) |
| GET | `/clientes/curp/{curp}` | Cliente por CURP |
| GET | `/clientes/rfc/{rfc}` | Cliente por RFC |
| GET | `/clientes/correo/{correo}` | Cliente por correo |
| GET | `/clientes/cuenta/{numeroCuenta}` | Cliente por número de cuenta |
| GET | `/clientes/activos` | Solo clientes activos |
| GET | `/clientes/rango-fechas?desde=...&hasta=...` | Clientes registrados en un rango |
| GET | `/cuentas/{numeroCuenta}` | Cuenta + saldo actual |
| GET | `/cuentas/activas` | Cuentas con estatus ACTIVA |
| GET | `/cuentas/{numeroCuenta}/saldos` | Historial de saldos |
| POST | `/login` | Emite un JWT para el cliente (por correo) |
| POST | `/login/{clienteId}/cerrar` | Cierra la sesión manualmente |

## Pruebas unitarias (25 nuevas, 36 en total en el proyecto)
- `AesEncryptionUtilTest`: cifrado/descifrado de texto y de `double[]`,
  IV aleatorio (dos cifrados del mismo valor no son iguales), valores nulos.
- `EdadMinimaValidatorTest`: casos límite (18 años exactos, un día antes,
  17 años, fecha nula).
- `ClienteServiceImplTest`: alta exitosa (crea domicilio/cuenta/saldo
  inicial), CURP/RFC/correo duplicados, cliente no encontrado, baja lógica
  (no borra físicamente), cuenta no encontrada.
- `CuentaServiceImplTest`: saldo actual desde el historial, cuenta sin
  movimientos (saldo cero), cuenta no encontrada, historial completo.
- `LoginServiceImplTest`: inicio de sesión exitoso, cliente no encontrado,
  cierre de sesión, cierre por inactividad.

## Pendiente / fuera de este alcance
- Integrar de verdad una librería de reconocimiento facial (MediaPipe u
  otra) — el campo ya está listo, pero la integración se deja para cuando
  el profesor lo indique explícitamente.
- Definir un mecanismo real de autenticación con contraseña para el login,
  si el profesor lo requiere más adelante.
