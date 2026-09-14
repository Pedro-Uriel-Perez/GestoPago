# Integración: Lista de Productos (servicio externo)

## Objetivo
Consumir `GET /sistema/service/getProductList.do` de un servicio externo autenticado
con Bearer Token, exponiendo el resultado como `GET /productos` en esta API,
siguiendo la misma arquitectura por capas ya usada en el proyecto (la integración
existente con GestoPago).

## Configuración (`application.properties`)
```properties
productlist.api.url=${PRODUCTLIST_API_URL:http://localhost:8082}
productlist.api.token=${PRODUCTLIST_API_TOKEN:}
feign.client.config.productListClient.connect-timeout=5000
feign.client.config.productListClient.read-timeout=10000
```
- `productlist.api.url` y `productlist.api.token` se resuelven desde variables de
  entorno (`PRODUCTLIST_API_URL` / `PRODUCTLIST_API_TOKEN`). El token **nunca** se
  escribe en el código fuente.
- Los timeouts se configuran de forma declarativa para el cliente Feign
  `productListClient`, sin código adicional.

## Capas implementadas
| Capa | Clase | Responsabilidad |
|---|---|---|
| Config | `ProductListFeignConfig` | Agrega el header `Authorization: Bearer <token>` a cada request del cliente (vía `RequestInterceptor`). No lleva `@Configuration` a propósito: así Spring Cloud OpenFeign la registra solo en el contexto del `ProductListClient`, sin afectar a otros Feign clients (p.ej. `GestoPagoAuthClient`). |
| Client | `ProductListClient` | Interfaz Feign que declara el `GET /sistema/service/getProductList.do`. |
| Model | `ProductoExternoDTO`, `ProductListApiResponse` | Forma cruda de la respuesta del servicio externo (`@JsonIgnoreProperties(ignoreUnknown = true)` para tolerar campos no mapeados). |
| Model | `ProductoResponse` | DTO público que expone nuestra API, desacoplado del contrato externo. |
| Mapper | `ProductoMapper` (MapStruct) | Traduce `ProductoExternoDTO` → `ProductoResponse`, igual que `GestoPagoTokenMapper`. |
| Service | `ProductListService` / `ProductListServiceImpl` | Orquesta la llamada, registra logs de inicio/fin y traduce los errores del cliente Feign a excepciones de negocio tipadas. |
| Controller | `ProductoController` | Expone `GET /productos`. |
| Excepciones | `exception/ProductList*Exception` + `GlobalExceptionHandler` | Ver sección siguiente. |

## Manejo de errores
En vez de validar con `if` dentro del `Service`, los errores del cliente Feign se
distinguen por **tipo de excepción** (polimorfismo, no condicionales):

| Excepción capturada de Feign | Excepción de negocio lanzada | HTTP devuelto al cliente |
|---|---|---|
| `FeignException.Unauthorized` / `Forbidden` | `ProductListAuthenticationException` | 401 |
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
- Error de autenticación (401).
- Timeout (`RetryableException`).
- Error HTTP no relacionado con autenticación (500).
- Error de comunicación inesperado.

## Decisiones técnicas y supuestos
1. **Origen del token**: el enunciado indica que el token debe salir "de la
   configuración de la aplicación" y no quedar hardcodeado; se implementó como
   una propiedad (`productlist.api.token`), igual que se hace para credenciales
   en `gestopago.auth.*`. Si el profesor especifica que el token debe obtenerse
   mediante un flujo de login dinámico (como ya existe para GestoPago), este
   sería el punto a ajustar (`ProductListFeignConfig`).
2. **Host del servicio**: se asume un host distinto al de GestoPago,
   parametrizado en `productlist.api.url`. Debe configurarse la URL real vía
   variable de entorno `PRODUCTLIST_API_URL` cuando se conozca.
3. **Forma de la respuesta** (`ProductoExternoDTO`): al no contar con el
   contrato real del servicio externo, se asumieron campos típicos de un
   catálogo de productos (`codigo`, `nombre`, `descripcion`, `precio`,
   `existencia`). Al usar `@JsonIgnoreProperties(ignoreUnknown = true)`, campos
   adicionales no listados no rompen la deserialización; los campos faltantes
   simplemente llegan `null` y deben ajustarse cuando se tenga el contrato real.
4. **Sin tabla nueva en base de datos**: esta integración no persiste
   productos, por lo que no se agregó ninguna migración Flyway ni se usó
   `VARCHAR` en ningún lado.
