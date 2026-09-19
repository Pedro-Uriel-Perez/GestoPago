# Integración: Catálogo de Productos (GestoPago)

## Objetivo
Consumir `GET /sistema/service/getProductList.do` del servicio externo
**GestoPago** (especificación PuntoRed: mismo dominio que ya usa
`GestoPagoAuthClient`, endpoint listado como `obtenerListaDeProductos`) y
exponer el catálogo como `GET /productos`, cumpliendo las restricciones de
uso reales del proveedor y usando **Redis** (no Postgres) como único
almacén, por indicación explícita del profesor.

## Restricciones del proveedor (clave para el diseño)
La especificación oficial de PuntoRed indica textualmente:

> *"This method should be called once a day... can only be used up to 3
> times in a single day, otherwise the IP will be blocked as an incorrect
> implementation or denial of service attack."*
>
> *"This endpoint should be used for the implementer to store the
> information obtained in its DB. It should never be used directly as a
> source of information for your frontend."*

Por eso `GET /productos` nunca llama a GestoPago en el momento de la
petición. La arquitectura separa dos responsabilidades:

1. **Sincronización** (`GestoPagoProductListSyncServiceImpl`): una tarea
   programada (`@Scheduled`, una vez al día) consulta GestoPago y escribe el
   resultado en el cache de Redis `"productos"`.
2. **Lectura** (`ProductListServiceImpl` / `GET /productos`): siempre lee de
   ese mismo cache, nunca de GestoPago directamente.

## Solo Redis, sin Postgres
El profesor indicó explícitamente que el catálogo **no debe persistirse en
Postgres**. Por eso no existe ninguna entidad JPA, repositorio ni migración
Flyway para productos — Redis es el único almacén:

- `GestoPagoProductListSyncServiceImpl.sincronizarProductos()` está anotado
  con `@CachePut(cacheNames = "productos", unless = "#result == null")`: el
  valor que retorna el método se escribe en Redis. Si la sincronización
  falla, el método retorna `null` y `unless` evita sobrescribir el cache
  existente (no se pierde el último catálogo bueno por una falla temporal).
- `ProductListServiceImpl.obtenerListaProductos()` está anotado con
  `@Cacheable(cacheNames = "productos")`: si el cache ya tiene datos (porque
  la sincronización ya corrió), Spring devuelve ese valor sin ejecutar el
  método. Si el cache está vacío (primera vez, antes de cualquier
  sincronización), el método se ejecuta, retorna una lista vacía y esa
  lista vacía queda cacheada hasta la primera sincronización exitosa.
- Ambos métodos no reciben parámetros, así que Spring genera la misma clave
  por defecto para los dos — por eso comparten el mismo dato bajo el mismo
  nombre de cache (`"productos"`) sin necesidad de una clave explícita.
- **`@Scheduled` y `@CachePut` están en el mismo método a propósito**: si el
  método con `@CachePut` se llamara internamente (`this.metodo()`) desde
  otro método de la misma clase anotado con `@Scheduled`, el proxy de Spring
  nunca interceptaría esa llamada y el cache jamás se actualizaría
  (auto-invocación no pasa por el proxy de AOP). Unificarlos en un solo
  método evita ese problema clásico de Spring.

Postgres (Personas, `gestopago_tokens`) sigue existiendo para las demás
funcionalidades del proyecto que ya lo usaban antes de esta tarea; solo el
catálogo de productos queda fuera de Postgres.

## La respuesta real es XML, no JSON
El "Example Response" de la especificación es XML:
```xml
<RESPONSE>
  <MENSAJE>
    <CODIGO>01</CODIGO>
    <TEXTO>Operacion realizada con exito</TEXTO>
  </MENSAJE>
  <PRODUCTOS>
    <producto servicio="AGUAKAN (Cancun)" producto="Agua Cancun (Mun. de Benito Juarez)"
              idServicio="12" idProducto="345" idCatTipoServicio="2" tipoFront="1"
              hasDigitoVerificador="true">
      <legend><![CDATA[Para cualquier aclaracion con tu pago, comunicate...]]></legend>
    </producto>
  </PRODUCTOS>
</RESPONSE>
```
Los DTOs (`ProductListApiResponse`, `MensajeExternoDTO`, `ProductoExternoDTO`)
usan anotaciones **JAXB** (`@XmlRootElement`, `@XmlElement`,
`@XmlElementWrapper`, `@XmlAttribute`), no Jackson. Las dependencias
necesarias (`jakarta.xml.bind-api`, `jaxb-runtime`) ya estaban en
`build.gradle` desde antes. Spring Boot detecta JAXB en el classpath y
registra automáticamente un conversor XML que Feign usa sin configuración
adicional; el `@GetMapping` del cliente solo declara
`produces = MediaType.APPLICATION_XML_VALUE` para pedir XML explícitamente.

Hay una prueba dedicada (`ProductListApiResponseXmlTest`) que deserializa un
XML con la forma exacta de la especificación real, sin necesitar red ni
mocks, para confirmar que el mapeo es correcto.

## Configuración (`application.properties`)
```properties
feign.client.config.gestoPagoProductList.connect-timeout=5000
feign.client.config.gestoPagoProductList.read-timeout=10000
gestopago.productos.sync-cron=0 0 3 * * *

spring.cache.type=redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
gestopago.productos.cache-ttl-hours=25
```
No se agregó una URL ni un token nuevos para GestoPago: se reutilizan
`gestopago.auth.url` y el token ya gestionado por
`GestoPagoTokenServiceImpl`. Redis sí requiere su propia conexión
(host/puerto/password vía variables de entorno).

## Capas implementadas
| Capa | Clase | Responsabilidad |
|---|---|---|
| Config | `GestoPagoProductListFeignConfig` | Agrega `Authorization: Bearer <token>` obteniéndolo de `GestoPagoTokenService.obtenerTokenActivo(...)` en cada llamada. |
| Config | `RedisCacheConfig` | Define el TTL del cache `"productos"` en Redis (respaldo adicional al control explícito de `@CachePut`/`@Cacheable`). |
| Client | `GestoPagoProductListClient` | Interfaz Feign para `GET /sistema/service/getProductList.do` (XML). |
| Model | `ProductoExternoDTO`, `MensajeExternoDTO`, `ProductListApiResponse` | Forma XML real de la respuesta del proveedor (JAXB). |
| Model | `ProductoResponse` | Un producto/servicio del catálogo, en la respuesta pública. |
| Model | `ProductoListResponse` | Envoltura de `GET /productos`: `{"mensaje": "...", "data": [...]}`. |
| Mapper | `ProductoMapper` (MapStruct) | `ProductoExternoDTO → ProductoResponse` directo (sin entidad intermedia). |
| Service (sincronización) | `GestoPagoProductListSyncService` / `Impl` | Llama a GestoPago una vez al día, valida el `MENSAJE.CODIGO`, traduce errores y escribe el resultado en Redis. |
| Service (lectura) | `ProductListService` / `ProductListServiceImpl` | Lee del cache de Redis. Nunca llama a GestoPago. |
| Controller | `ProductoController` | Expone `GET /productos`. |
| Excepciones | `exception/ProductList*Exception`, `GestoPagoTokenNoDisponibleException` | Ver manejo de errores. |

## Manejo de errores
Ocurren dentro de `GestoPagoProductListSyncServiceImpl.consultarCatalogoVigente()`,
distinguidos por **tipo de excepción** (sin `if`):

| Situación | Excepción lanzada | Efecto en `sincronizarProductos()` |
|---|---|---|
| No hay token GestoPago activo todavía | `ProductListAuthenticationException` | Se captura, se loguea, retorna `null` (cache no se toca) |
| GestoPago rechaza el token (401/403) | `ProductListAuthenticationException` | idem |
| Timeout de conexión/lectura (`RetryableException`) | `ProductListTimeoutException` | idem |
| GestoPago responde con otro status de error HTTP | `ProductListUnsuccessfulResponseException` | idem |
| `MENSAJE.CODIGO` distinto de `"01"` (error de negocio con HTTP 200) | `ProductListUnsuccessfulResponseException` | idem |
| Cualquier otro error (red, IO, etc.) | `ProductListCommunicationException` | idem |

Como la sincronización corre en una tarea programada (no en una petición
HTTP), estas excepciones **no llegan a `GlobalExceptionHandler`**: se
capturan y registran (`log.error`) dentro del propio
`sincronizarProductos()`. `GlobalExceptionHandler` conserva solo el manejo
de `MethodArgumentNotValidException` (validación de los `*Request`).

## Logging
Se registra inicio y fin de cada sincronización (`log.info`) y cada tipo de
error de integración por separado (`log.error`) — nunca el token ni el
cuerpo completo de la respuesta del proveedor.

## Pruebas unitarias
- `GestoPagoProductListSyncServiceImplTest` (7 casos): sincronización
  exitosa, respuesta con código de negocio distinto de éxito, sin token
  activo, error de autenticación HTTP, timeout, otro error HTTP, error de
  comunicación inesperado. En cada escenario de error se verifica que el
  método retorna `null` (que es lo que hace que `@CachePut` no toque el
  cache existente en tiempo de ejecución real).
- `ProductListServiceImplTest` (1 caso): cache vacío (aún no sincronizado) →
  lista vacía.
- `ProductListApiResponseXmlTest` (1 caso): deserializa el XML con la forma
  exacta de la especificación real de PuntoRed.

Nota: estas pruebas instancian las clases directamente (sin contexto de
Spring), por lo que `@Scheduled`, `@Cacheable` y `@CachePut` no se activan
(son AOP de Spring) — se verifica el comportamiento puro del método.

## Decisiones técnicas y supuestos
1. **Redis como único almacén (sin Postgres)**: instrucción explícita del
   profesor para esta funcionalidad. El resto del proyecto (Personas,
   `gestopago_tokens`) sigue usando Postgres sin cambios.
2. **`@Scheduled` + `@CachePut` en el mismo método**: necesario para evitar
   el problema de auto-invocación de Spring AOP (ver comentario en el
   código). Si se separan en dos métodos de la misma clase, el cache nunca
   se actualizaría al correr por cron.
3. **`unless = "#result == null"` en `@CachePut`**: para que una falla
   temporal de GestoPago no borre el último catálogo bueno cacheado.
4. **Campos del catálogo**: se modelaron los atributos documentados en la
   especificación (`idProducto`, `idServicio`, `servicio`, `producto`,
   `legend`). No incluye precio ni existencia porque la API de GestoPago no
   los expone en este endpoint.
5. **Origen del host y del token**: según la especificación de PuntoRed,
   `getProductList.do` pertenece al mismo servicio GestoPago ya integrado,
   por lo que se reutilizan `gestopago.auth.url` y el token ya gestionado
   por `GestoPagoTokenServiceImpl`, en vez de credenciales/host nuevos.
6. **Pendiente**: validar contra el servicio real de GestoPago y una
   instancia de Redis en ejecución; hoy el flujo está validado con pruebas
   unitarias y la prueba de deserialización XML contra el contrato
   documentado.
