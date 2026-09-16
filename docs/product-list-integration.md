# Integración: Lista de Productos (GestoPago)

## Objetivo
Consumir `GET /sistema/service/getProductList.do` del servicio externo
**GestoPago** (según la especificación de PuntoRed: mismo dominio que ya usa
`GestoPagoAuthClient`, endpoint listado como `obtenerListaDeProductos`),
autenticado con Bearer Token, y exponer el resultado como `GET /productos`
en esta API, siguiendo la misma arquitectura por capas ya usada en el
proyecto.

## Configuración (`application.properties`)
```properties
feign.client.config.gestoPagoProductList.connect-timeout=5000
feign.client.config.gestoPagoProductList.read-timeout=10000
```
No se agregó una URL ni un token nuevos: el endpoint pertenece al **mismo
servicio externo GestoPago** que ya autentica `GestoPagoAuthClient`
(`gestopago.auth.url`), y el Bearer Token es el **mismo token** que ya
obtiene y renueva `GestoPagoTokenServiceImpl` cada hora
(`gestopago.auth.refresh-rate-ms`) y guarda en la tabla `gestopago_tokens`.
Solo se agregan los timeouts propios de este nuevo cliente Feign
(`gestoPagoProductList`), de forma declarativa.

## Capas implementadas
| Capa | Clase | Responsabilidad |
|---|---|---|
| Config | `GestoPagoProductListFeignConfig` | Agrega el header `Authorization: Bearer <token>` a cada request, consultando `GestoPagoTokenService.obtenerTokenActivo(...)` en el momento de la llamada (token siempre vigente, sin quedar hardcodeado). No lleva `@Configuration` a propósito: así Spring Cloud OpenFeign la registra solo en el contexto del `GestoPagoProductListClient`, sin afectar a otros Feign clients (p.ej. `GestoPagoAuthClient`). |
| Client | `GestoPagoProductListClient` | Interfaz Feign que declara el `GET /sistema/service/getProductList.do`. |
| Model | `ProductoExternoDTO`, `ProductListApiResponse` | Forma cruda de la respuesta del servicio externo (`@JsonIgnoreProperties(ignoreUnknown = true)` para tolerar campos no mapeados). |
| Model | `ProductoResponse` | DTO público que expone nuestra API, desacoplado del contrato externo. |
| Mapper | `ProductoMapper` (MapStruct) | Traduce `ProductoExternoDTO` → `ProductoResponse`, igual que `GestoPagoTokenMapper`. |
| Service | `ProductListService` / `ProductListServiceImpl` | Orquesta la llamada, registra logs de inicio/fin y traduce los errores del cliente Feign a excepciones de negocio tipadas. |
| Controller | `ProductoController` | Expone `GET /productos`. |
| Excepciones | `exception/ProductList*Exception`, `GestoPagoTokenNoDisponibleException` + `GlobalExceptionHandler` | Ver sección siguiente. |

## Manejo de errores
En vez de validar con `if` dentro del `Service`, los errores se distinguen
por **tipo de excepción** (polimorfismo/`catch` tipado, sin condicionales):

| Excepción capturada | Excepción de negocio lanzada | HTTP devuelto al cliente |
|---|---|---|
| `GestoPagoTokenNoDisponibleException` (aún no hay token renovado en BD) | `ProductListAuthenticationException` | 401 |
| `FeignException.Unauthorized` / `Forbidden` (el token fue rechazado) | `ProductListAuthenticationException` | 401 |
| `RetryableException` (timeout de conexión/lectura) | `ProductListTimeoutException` | 504 |
| `FeignException` (cualquier otro status de error) | `ProductListUnsuccessfulResponseException` | 502 |
| Cualquier otra excepción (red, IO, etc.) | `ProductListCommunicationException` | 503 |

`GlobalExceptionHandler` (`@RestControllerAdvice`) centraliza esta traducción a
respuestas HTTP y también maneja `MethodArgumentNotValidException` (errores de
validación de los `*Request` con Bean Validation), siguiendo la convención de que
las validaciones se resuelven en el request/handler, no dentro del `Service`.

## Logging
`ProductListServiceImpl` registra el inicio y fin de cada invocación
(`log.info`) y los errores (`log.error`) usando únicamente el mensaje/estado de
la excepción — nunca el token ni el contenido de la respuesta del proveedor.

## Pruebas unitarias
`ProductListServiceImplTest` (Mockito) cubre:
- Respuesta exitosa con mapeo correcto.
- Respuesta sin productos (`null`) → lista vacía, sin `NullPointerException`.
- Sin token GestoPago activo todavía (`GestoPagoTokenNoDisponibleException`).
- Error de autenticación (401 del servicio externo).
- Timeout (`RetryableException`).
- Error HTTP no relacionado con autenticación (500).
- Error de comunicación inesperado.

## Decisiones técnicas y supuestos
1. **Origen y host del servicio**: según la especificación de PuntoRed
   (Postman: sección "obtenerListaDeProductos" bajo el mismo host que
   `sendEcho.do`), el endpoint de lista de productos es parte del **mismo
   servicio GestoPago** ya integrado, no un proveedor externo distinto. Por
   eso se reutiliza `gestopago.auth.url` y el token ya gestionado, en vez de
   crear credenciales/host nuevos.
2. **Forma de la respuesta** (`ProductoExternoDTO`): la especificación de
   PuntoRed no detalla el JSON de `obtenerListaDeProductos`; se asumieron
   campos típicos de un catálogo (`codigo`, `nombre`, `descripcion`,
   `precio`, `existencia`). Con `@JsonIgnoreProperties(ignoreUnknown = true)`
   campos adicionales no rompen la deserialización; deben ajustarse cuando
   se tenga el contrato exacto.
3. **Header opcional "Clave X-API"**: la especificación menciona un header
   `Clave X-API` opcional en la sección de autenticación. No se implementó
   por ser opcional y no tener confirmación de que aplique también a
   `obtenerListaDeProductos`; si el profesor confirma que es obligatorio,
   se agrega como otro `@Value` en `GestoPagoProductListFeignConfig`.
4. **Hallazgo fuera de alcance de esta tarea**: la respuesta real del
   endpoint de autenticación (`{"token": "...", "success": true}`) no
   coincide del todo con los campos que ya modelaba `GestoPagoAuthResponse`
   (`message`, `status`, `token_type`, `expires_in`) antes de este cambio;
   esos campos simplemente quedan en `null` porque el proveedor no los
   envía. No se modificó porque es código preexistente fuera del alcance de
   esta integración, pero vale la pena revisarlo aparte.
5. **Sin tabla nueva en base de datos**: esta integración no persiste
   productos, por lo que no se agregó ninguna migración Flyway ni se usó
   `VARCHAR` en ningún lado.
