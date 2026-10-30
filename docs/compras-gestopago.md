# Integración de pagos de servicios con GestoPago (sendTx)

Este documento describe el flujo completo de una compra (recarga o pago de
servicio): la autenticación y validación del cliente, la comunicación con
GestoPago y el tratamiento del resultado. También documenta los riesgos
analizados, los escenarios de fallo y por qué se tomó cada decisión de
diseño.

## 1. Flujo general

La operación se divide en dos pasos porque exige un segundo factor de
autenticación (PIN por correo) antes de mover dinero.

```
POST /compras              POST /compras/confirmar
(cliente con JWT)          (mismo cliente con JWT)
      |                           |
      v                           v
 1. Cuenta del cliente?      7. Operación existe y es suya?
 2. Cliente activo?          8. PIN correcto?
 3. Sin otra compra          9. Bloqueo pesimista de la cuenta
    pendiente (SETNX)       10. ¿Sigue pendiente? (doble envío)
 4. Producto en catálogo    11. Saldo suficiente (bajo bloqueo)
 5. Monto = precio          12. sendTx cifrado -> GestoPago
    del catálogo                   |
 6. Saldo suficiente           timeout/5xx? -> confirmTx (mismos parámetros)
    -> guarda la operación         |
       en Redis (TTL 5 min)  13. CODIGO 01 -> descuenta saldo (movimiento COMPRA)
    -> manda PIN por correo      otro código -> rechazo, no descuenta
                                 indeterminado -> conciliación, 202
```

| Endpoint | Body | Respuesta exitosa |
|---|---|---|
| `POST /compras` | `numeroCuenta`, `idProducto`, `monto`, `telefono` o `referencia` | `200` `{operacionId, mensaje}` |
| `POST /compras/confirmar` | `operacionId`, `codigoVerificacion` | `200` `{numeroAutorizacion, idTransaccion, saldoRestante, mensaje}` |

Ambos exigen `Authorization: Bearer <jwt>` de un cliente con sesión activa.

## 2. Seguridad

**Credenciales y token de GestoPago.** La contraseña del distribuidor
(`GESTOPAGO_AUTH_PASSWORD`) y la llave/IV AES del payload
(`GESTOPAGO_SENDTX_SECRET_KEY`, `GESTOPAGO_SENDTX_IV`) solo existen como
variables de entorno, sin valor por defecto en el repositorio. El token
Bearer lo obtiene `GestoPagoTokenServiceImpl` en tiempo de ejecución y lo
guarda en la tabla `gestopago_tokens`; se inyecta en cada llamada con un
interceptor de Feign (`GestoPagoSenTxFeignConfig`) y nunca se escribe en
código, propiedades, respuestas HTTP ni logs. Se reutiliza el token del día,
como exige la especificación (no se pide uno nuevo por cada petición).

**Payload cifrado.** El JSON de la transacción se cifra con AES-256-CBC
(`GestoPagoAesCipher`) y viaja como el campo `signed` del body.

**Identidad del cliente.** El `clienteId` se toma del JWT (principal que
deja `JwtAuthenticationFilter`), no del body. Así un cliente con sesión
válida no puede comprar con el `numeroCuenta` de otro (`403`), ni confirmar
una operación iniciada por otro (`403`).

**Segundo factor.** El PIN es de 6 dígitos, se genera con `SecureRandom`,
expira a los 5 minutos y no se escribe en ningún log.

**Información sensible en respuestas y logs.** Todos los errores pasan por
`GlobalExceptionHandler`. Hay un manejador de último recurso
(`Exception.class`) para que ningún error no previsto devuelva el stack
trace; los errores del proveedor (`FeignException`) se responden con un
mensaje genérico sin URL ni cuerpo de GestoPago. Los logs de cada
transacción guardan solo trazabilidad (operación, cliente, UPC, código,
idTx, autorización), nunca el PIN, el token ni el payload.

## 3. Validaciones antes de ejecutar la operación

| Validación | Dónde | Respuesta |
|---|---|---|
| Campos obligatorios y formato (cuenta de 10 dígitos, monto > 0, teléfono de 10 dígitos, PIN de 6 dígitos) | Bean Validation en `CompraRequest` / `CompraConfirmarRequest` | `400` |
| La cuenta pertenece al cliente autenticado | `CompraServiceImpl` | `403` |
| Cliente activo (un cliente inactivo tampoco puede iniciar sesión, ver `LoginServiceImpl`) | `CompraServiceImpl` | `403` |
| El producto existe en el catálogo local | `GestoPagoProductoRepository.findByIdProducto` | `404` |
| El monto corresponde al precio del catálogo (recargas de monto fijo) | `validarMontoCorrespondeAlCatalogo` | `400` |
| `telefono` o `referencia` según el `tipoFront` del producto | `resolverTelefono` / `resolverReferencia` | `400` |
| Saldo suficiente | al iniciar y otra vez, bajo bloqueo, al confirmar | `409` |
| Sin otra compra pendiente en la misma cuenta | SETNX en Redis | `409` |

Para los pagos de servicio (`tipoFront = 2`) el monto no se compara contra el
catálogo, porque lo define el recibo a pagar, no un precio fijo.

## 4. Estrategia de descuento del saldo

**Decisión: descontar solo después de que GestoPago confirma éxito, dentro
de una sección crítica con bloqueo pesimista sobre la cuenta.**

En `confirmarCompra`, bajo una sola transacción:

1. `CuentaRepository.findWithLockingById` (`SELECT ... FOR UPDATE`) bloquea
   la fila de la cuenta.
2. Se vuelve a verificar el saldo (el de `iniciarCompra` era solo un
   chequeo previo; el que cuenta es este, bajo bloqueo).
3. Se llama a GestoPago.
4. Solo si responde `CODIGO 01` se inserta el movimiento `COMPRA` con el
   saldo nuevo.

Alternativas consideradas:

- *Descontar antes de llamar a GestoPago y devolver si falla.* Se descartó
  porque si el proceso cae entre el descuento y la devolución, el cliente
  pierde dinero, y porque en un timeout no se sabe si hay que devolver.
- *Reserva en dos fases (apartar saldo, liberar el bloqueo, llamar y luego
  confirmar o cancelar la reserva).* Escala mejor, pero agrega estados y
  casos de recuperación. No se justifica para el volumen de este sistema.

Trade-off aceptado: el bloqueo se mantiene durante la llamada externa (hasta
62 s). Eso serializa las confirmaciones **de una misma cuenta**, pero no
afecta a otras cuentas en paralelo.

`@Transactional` se usa aquí a pesar de la regla general del proyecto de
evitarlo: es la única forma de sostener el bloqueo pesimista. Usa
`noRollbackFor = TransaccionGestoPagoIndeterminadaException` para que el
registro de conciliación sobreviva (en ese camino no hay ninguna otra
escritura).

## 5. Concurrencia y prevención de cargos duplicados

| Riesgo | Protección |
|---|---|
| Dos `POST /compras` simultáneos de la misma cuenta | Reserva atómica con `SETNX` (`compra:cuenta-activa:{cuentaId}`). Revisar primero si existe la clave y luego crearla dejaría pasar a ambas. Si una validación posterior falla, la reserva se libera. |
| Dos confirmaciones simultáneas de cuentas distintas compitiendo por el saldo | Bloqueo pesimista por cuenta y re-verificación del saldo bajo bloqueo. |
| Doble envío de la misma confirmación (doble clic, reintento del cliente) | La segunda espera el bloqueo de la primera. Cuando lo obtiene, comprueba que la operación siga en Redis; la primera ya la borró, así que responde `404` sin volver a llamar a GestoPago. |
| Reintento de una operación ya cobrada | Cada operación lleva un `UPC` único, generado al iniciarla y congelado en Redis. GestoPago rechaza un UPC repetido con `CODIGO 06`, lo que sirve de segunda barrera. |
| Reusar un PIN / `operacionId` ya confirmado | La operación se borra de Redis al llegar a un resultado definitivo (`404` después). |

Las pruebas `concurrencia_dosComprasSimultaneasDeLaMismaCuenta_soloUnaSeInicia`
y `concurrencia_dobleConfirmacionSimultaneaDeLaMismaOperacion_cobraUnaSolaVez`
lo verifican con hilos reales contra la aplicación completa.

## 6. Tiempos de espera, errores del proveedor y conciliación

El cliente Feign de `sendTx` tiene un *read timeout* de 62 s, que es el
umbral de la especificación de PuntoRed. Pasado ese tiempo, un timeout **no
significa que la transacción no se procesó**.

| Lo que pasa con `sendTx` | Qué hace el sistema | Respuesta |
|---|---|---|
| `CODIGO 01` | Descuenta saldo, registra movimiento `COMPRA`, libera la cuenta | `200` |
| Otro código (`02` saldo del distribuidor, `06` duplicado, etc.) | Rechazo definitivo: no descuenta, libera la cuenta, reenvía el mensaje de GestoPago | `409` |
| Timeout (`RetryableException`) o 5xx | **No asume fallo.** Llama `confirmTx` con exactamente los mismos parámetros (mismo payload congelado, mismo UPC y `horaLocal`) | según `confirmTx` |
| `confirmTx` responde `01` | Se trata como éxito y se descuenta | `200` |
| `confirmTx` responde otro código | Se trata como "no aplicada", no se descuenta | `409` |
| `confirmTx` también falla o vence | **Estado indeterminado**: no descuenta, no reintenta solo, guarda un registro en `conciliaciones_pendientes` y deja la cuenta reservada hasta que expire la reserva, para que no se lance un segundo intento mientras no se sepa si el primero se cobró | `202` |
| Error claro no recuperable (401/403 del proveedor, token no disponible) | No se procesó; mensaje genérico | `502` / `503` |

`202 Accepted` es intencional: decirle al cliente "falló" podría ser falso
(pudo haberse cobrado), y decirle "éxito" también.

La tabla `conciliaciones_pendientes` (migración V12) guarda cuenta, cliente,
UPC, producto y monto de cada operación indeterminada, para revisarla
después contra el estado real en GestoPago. `confirmTx` solo es válido
durante 24 h después del `sendTx`.

## 7. Diseño del código

- **Controller** (`CompraController`): solo recibe el request validado y el
  `clienteId` del JWT, y delega.
- **Service** (`CompraService` / `CompraServiceImpl`): reglas de negocio,
  orquestación y estrategia de descuento/conciliación.
- **Repository**: solo métodos derivados de Spring Data, sin `@Query`.
  `findWithLockingById` usa `@Lock`, que es una anotación de bloqueo, no una
  consulta.
- **Integración externa**: `GestoPagoSenTxClient` (Feign) y
  `GestoPagoSenTxFeignConfig` (interceptor del token), aislados del dominio.
- **DTOs** separados para la entrada (`CompraRequest`), la operación
  pendiente (`CompraPendiente`), el payload hacia GestoPago
  (`GestoPagoSenTxRequest`) y la respuesta (`CompraResultadoResponse`).
- **Excepciones de dominio** específicas, cada una con su status HTTP en
  `GlobalExceptionHandler`.

## 8. Pruebas

| Tipo | Clase | Qué cubre |
|---|---|---|
| Unitarias | `CompraServiceImplTest` (22) | Cada regla de negocio y cada rama de error, timeout, conciliación y liberación de reservas |
| Unitarias | `GestoPagoAesCipherTest` | Cifrado/descifrado AES-256-CBC |
| Integración | `CompraIntegracionTest` (13) | Aplicación completa (Spring Security + JWT real, controllers, services, repositorios, Flyway, transacciones y bloqueo reales sobre H2). Solo se simulan GestoPago, Gmail y Redis. |

Las pruebas de integración usan el perfil `test`
(`src/test/resources/application-test.properties`) y un esquema equivalente
al de producción para H2 (`src/test/resources/db/migration-h2`).

Además se probó de punta a punta contra el sandbox real de GestoPago: PIN
real por Gmail, `sendTx` real con `CODIGO 01` y autorización `100076595`,
descuento de saldo correcto, y rechazo al intentar reusar la operación.

## 9. Configuración (variables de entorno)

| Variable | Uso |
|---|---|
| `GESTOPAGO_AUTH_PASSWORD` | Contraseña del distribuidor para obtener el token Bearer |
| `GESTOPAGO_AUTH_ID_DISTRIBUIDOR`, `GESTOPAGO_AUTH_CODIGO_DISPOSITIVO` | Identificadores del distribuidor y del dispositivo (tienen valor por defecto, no son secretos) |
| `GESTOPAGO_SENDTX_SECRET_KEY`, `GESTOPAGO_SENDTX_IV` | Llave e IV AES-256 para cifrar el payload |
| `GESTOPAGO_SENDTX_API_KEY` | Header `X-API-Key`, opcional |
| `GESTOPAGO_SENDTX_UNIDAD` | Unidad o sucursal comercial (por defecto `083`) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Cuenta de Gmail y contraseña de aplicación para enviar el PIN |
| `REDIS_PASSWORD` | Redis (operaciones pendientes y reservas) |

## 10. Limitaciones conocidas

- La conciliación es manual: queda el registro en
  `conciliaciones_pendientes`, pero no hay un proceso que la resuelva solo.
  El siguiente paso natural sería un job programado que vuelva a consultar
  `confirmTx` (dentro de las 24 h permitidas) y marque `resuelta`.
- La reserva de cuenta de una operación indeterminada expira junto con el
  TTL de 5 minutos. Después de eso el sistema vuelve a permitir compras en
  esa cuenta aunque la conciliación siga pendiente.
- Un timeout de `sendTx` más el de `confirmTx` pueden sumar unos 2 minutos
  en el peor caso antes de responder `202`.
