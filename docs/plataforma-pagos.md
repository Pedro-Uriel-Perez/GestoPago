# Plataforma de pagos (web + móvil): servicios del backend

Este documento describe lo que el backend expone para que la plataforma web y
la app móvil (Flutter) ofrezcan las mismas funciones con las mismas
credenciales. El flujo de pago en dos pasos (PIN por correo, `sendTx`,
conciliación con `confirmTx`) se explica en `compras-gestopago.md`.

## 1. Sesiones por dispositivo

Un cliente puede tener **una sesión activa por dispositivo** (`WEB` y `MOVIL`)
al mismo tiempo. Las sesiones viven en la tabla `sesiones` (migración V13).

| Endpoint | Descripción |
|---|---|
| `POST /login` | Body: `correoElectronico`, `password`, `dispositivo` (`WEB` o `MOVIL`, opcional; por defecto `WEB`). Regresa `{ "jwt": "..." }`. |
| `GET /login/sesiones` | Sesiones activas del cliente autenticado: `dispositivo`, `fechaInicio`, `fechaUltimoAcceso`, `actual`. |
| `POST /login/{clienteId}/cerrar` | Cierra **solo** la sesión del dispositivo que hace la petición. |

Reglas:

- Iniciar sesión en un dispositivo **no** cierra la sesión del otro. Volver a
  iniciar sesión en el mismo tipo de dispositivo sí reemplaza la anterior.
- El JWT lleva el claim `sid` (identificador de la sesión). En cada petición,
  `JwtAuthenticationFilter` exige que esa sesión siga activa y que el token
  coincida con el guardado (cifrado con AES).
- **Alerta de nuevo inicio de sesión:** si al iniciar sesión el cliente ya
  tenía otra sesión abierta, se le envía un correo de aviso. Además, cada app
  consulta `GET /login/sesiones` y muestra un aviso cuando aparece una sesión
  de otro dispositivo. Si el correo falla, el inicio de sesión no se bloquea.
- **Inactividad:** cada petición autenticada cuenta como actividad. Una sesión
  sin actividad durante `security.session.inactividad-minutos` (5) se cierra.
  La consulta periódica a `GET /login/sesiones` no cuenta como actividad.

## 2. Catálogo

`GET /productos` regresa el catálogo sincronizado desde `getProductList.do`
(nunca datos fijos). Cada producto incluye además:

| Campo | Origen |
|---|---|
| `categoria` | Nombre de la tabla `idCatTipoServicio` de la especificación de GestoPago. |
| `logoUrl` | Logotipo público del proveedor: `/sistema/images/gestopago/servicios/{idServicio}.png`. |
| `imagenReferenciaUrl` | Imagen de ayuda de la referencia: `/sistema/images/gestopago/referencias/{idServicio}.jpg`. |
| `permiteValidarReferencia` | `true` si el servicio tiene un producto de consulta (`tipoFront` 4). |

Tipos de producto (`tipoFront`): 1 = recarga (pide teléfono), 2 = pago de
servicio (pide referencia y monto), 4 = consulta de saldo/referencia.

## 3. Validación de referencias

`POST /compras/verificar-referencia` — body: `idProducto`, `referencia`
(alfanumérica, máximo 60 caracteres).

El backend llama a `verifyReference.do` de GestoPago con el producto de
consulta (`tipoFront` 4) del mismo servicio y responde:

| `estado` | Significado |
|---|---|
| `VALIDA` | GestoPago respondió código `01`. `saldoPendiente` trae el adeudo si lo informa. |
| `INVALIDA` | GestoPago respondió otro código. `mensaje` trae su texto. |
| `NO_VERIFICABLE` | GestoPago no ofrece consulta en línea para ese servicio. |

Con `gestopago.referencias.exigir-validacion=true` (por defecto), `POST
/compras` rechaza con 400 un pago de servicio cuya referencia sea `INVALIDA`.

## 4. Historial de pagos

Cada pago confirmado con PIN se guarda en la tabla `pagos` (migración V14) con
su resultado.

| Endpoint | Descripción |
|---|---|
| `GET /compras` | Pagos del cliente autenticado, del más reciente al más antiguo. |
| `GET /compras/{pagoId}` | Detalle de un pago propio. 404 si no existe o es de otro cliente. |

Campos: `nombreServicio`, `nombreProducto`, `logoUrl`, `fecha`, `referencia`,
`telefono`, `monto`, `idTransaccion` (folio), `numeroAutorizacion`, `estado`,
`mensaje`.

| `estado` | Cuándo |
|---|---|
| `EXITOSO` | GestoPago confirmó la operación (código `01`). |
| `RECHAZADO` | GestoPago respondió con un rechazo. |
| `PENDIENTE` | No se pudo confirmar el resultado (timeout en `sendTx` y en `confirmTx`). |

## 5. Depósitos

`POST /cuentas/depositos` — body: `numeroCuenta`, `monto` (0.01 a 50,000.00).
Abona saldo a la cuenta del cliente autenticado (movimiento `DEPOSITO`) y
regresa la cuenta con su saldo actualizado. 403 si la cuenta es de otro cliente.

## 6. Requisitos de la especificación de GestoPago

- **Un token por día.** La especificación pide no sacar un token por cada
  petición. El token se guarda en `gestopago_tokens` y se reutiliza mientras
  tenga menos de `gestopago.auth.vigencia-horas` (23); el servidor solo pide
  otro cuando el guardado ya va a vencer, también después de un reinicio.
- **Log evidence.** Cada llamada a `sendTx`, `confirmTx` y `verifyReference`
  queda en la tabla `gestopago_bitacora` (migración V15): operación, UPC,
  payload en claro (antes de cifrarlo, sin token ni llaves), respuesta,
  código, error si no hubo respuesta y duración. Se guarda en una
  transacción aparte para que quede aunque la operación falle.

## 7. Variables de entorno nuevas

| Variable | Uso | Valor por defecto |
|---|---|---|
| `CORS_ORIGENES` | Orígenes web que pueden llamar a la API. | `localhost` y `*.onrender.com` |
| `GESTOPAGO_EXIGIR_VALIDACION_REFERENCIA` | Bloquear pagos con referencia inválida. | `true` |
| `MAIL_HOST` / `MAIL_PORT` | Servidor y puerto SMTP. | `smtp.gmail.com` / `587` |
| `MAIL_FROM` | Dirección remitente de los correos. | el valor de `MAIL_USERNAME` |

**Correo en Render:** el plan gratis de Render bloquea la salida a los puertos
SMTP 25, 465 y 587, así que Gmail no funciona desde ahí. Hay que usar un
proveedor SMTP que acepte el puerto 2525 y configurar `MAIL_HOST`,
`MAIL_PORT=2525`, `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM`. El envío
tiene un límite de 10 segundos; la alerta de inicio de sesión se envía en
segundo plano y nunca retrasa el login.
