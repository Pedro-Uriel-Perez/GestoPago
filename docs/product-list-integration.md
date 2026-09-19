# Integración: Catálogo de Productos (GestoPago)

## Objetivo
Consumir `GET /sistema/service/getProductList.do` del servicio externo
**GestoPago** (especificación PuntoRed: mismo dominio que ya usa
`GestoPagoAuthClient`, endpoint listado como `obtenerListaDeProductos`) y
exponer el catálogo como `GET /productos`, cumpliendo las restricciones de
uso reales del proveedor.

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
   programada (`@Scheduled`, una vez al día) consulta GestoPago, guarda el
   resultado en Postgres (`gestopago_productos`) e invalida el cache de
   Redis.
2. **Lectura** (`ProductListServiceImpl` / `GET /productos`): lee de Redis
   si el dato está en cache; en cache-miss, lee de Postgres y lo vuelve a
   cachear.

## Postgres (fuente de verdad) + Redis (cache)
- **Postgres** (`gestopago_productos`) es la fuente de verdad/respaldo:
  persiste el catálogo completo, sin `VARCHAR` (columnas de texto en
  `TEXT`), con una migración Flyway dedicada (`V2__create_gestopago_productos.sql`).
- **Redis** (cache `"productos"`) acelera las lecturas repetidas de
  `GET /productos` sin golpear Postgres en cada petición.
- `GestoPagoProductListSyncServiceImpl.sincronizarProductos()` está anotado
  con `@Scheduled` + `@Transactional` + `@CacheEvict(cacheNames = "productos", allEntries = true)`
  **en el mismo método**: si `@CacheEvict` viviera en un método aparte
  invocado internamente (`this.metodo()`) desde el método programado, el
  proxy de Spring nunca interceptaría esa llamada y el cache jamás se
  invalidaría (auto-invocación no pasa por el proxy de AOP) — este es un
  error clásico de Spring que se evita unificando ambas anotaciones.
- Si la sincronización falla, el catálogo en Postgres **no se toca**
  (el `deleteAllInBatch()`/`saveAll()` nunca se ejecuta si
  `consultarCatalogoVigente()` lanza una excepción), así que no se pierde
  el último catálogo bueno por una falla temporal de GestoPago.
- `ProductListServiceImpl.obtenerListaProductos()` está anotado con
  `@Cacheable(cacheNames = "productos")`: en cache-hit, Spring retorna el
  valor de Redis sin ejecutar el método; en cache-miss, el método consulta
  Postgres, retorna la lista (vacía si aún no ha corrido ninguna
  sincronización) y esa respuesta se cachea.

## La respuesta real es XML, no JSON
El "Example Response" de la especificación es XML:
```xml
<RESPONSE>
  <MENSAJE>
    <CODIGO>01</CODIGO>
    <TEXTO>Operacion realizada con exito</TEXTO>
  </MENSAJE>
  <PRODUCTOS>
    <producto servicio="ABIB" producto="ABIB 100"
              idServicio="2284" idProducto="14302" idCatTipoServicio="1" tipoFront="1">
      <legend><![CDATA[Recibe soporte las 24h del dia...]]></legend>
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

**Confirmado contra el servicio real**: con las credenciales de demo de la
especificación (`idDistribuidor=83`, `codigoDispositivo=GPS83-TPV-17`,
`password=12345678`), la app renovó el token real y `GET /productos` trajo
el catálogo real de GestoPago (~900 productos) end-to-end.

Hay una prueba dedicada (`ProductListApiResponseXmlTest`) que deserializa un
XML con la forma exacta de la especificación, sin necesitar red ni mocks.

## Formato de respuesta de `GET /productos`
```json
{
  "mensaje": "Datos consultados correctamente",
  "data": [
    { "idProducto": 14302, "idServicio": 2284, "servicio": "ABIB", "nombre": "ABIB 100", "descripcion": "..." }
  ]
}
```
`ProductoListResponse` envuelve la lista en `{"mensaje", "data"}`, confirmado
como formato válido para este endpoint.

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
| Config | `RedisCacheConfig` | Define el TTL del cache `"productos"` en Redis (respaldo adicional al `@CacheEvict` explícito). |
| Client | `GestoPagoProductListClient` | Interfaz Feign para `GET /sistema/service/getProductList.do` (XML). |
| Model | `ProductoExternoDTO`, `MensajeExternoDTO`, `ProductListApiResponse` | Forma XML real de la respuesta del proveedor (JAXB). |
| Model | `ProductoResponse` | Un producto/servicio del catálogo, en la respuesta pública. |
| Model | `ProductoListResponse` | Envoltura de `GET /productos`: `{"mensaje": "...", "data": [...]}`. |
| Entity | `GestoPagoProducto` | Copia local en Postgres del catálogo (sin `VARCHAR`, columnas `TEXT`). |
| Repository | `GestoPagoProductoRepository` | Acceso a la tabla `gestopago_productos`. |
| Mapper | `ProductoMapper` (MapStruct) | `ProductoExternoDTO → GestoPagoProducto` (guardar) y `GestoPagoProducto → ProductoResponse` (exponer). |
| Service (sincronización) | `GestoPagoProductListSyncService` / `Impl` | Llama a GestoPago una vez al día, valida el `MENSAJE.CODIGO`, traduce errores, guarda en Postgres e invalida Redis. |
| Service (lectura) | `ProductListService` / `ProductListServiceImpl` | Lee de Redis o Postgres. Nunca llama a GestoPago. |
| Controller | `ProductoController` | Expone `GET /productos`. |
| Excepciones | `exception/ProductList*Exception`, `GestoPagoTokenNoDisponibleException` | Ver manejo de errores. |

## Manejo de errores
Ocurren dentro de `GestoPagoProductListSyncServiceImpl.consultarCatalogoVigente()`,
distinguidos por **tipo de excepción** (sin `if`):

| Situación | Excepción lanzada | Efecto en `sincronizarProductos()` |
|---|---|---|
| No hay token GestoPago activo todavía | `ProductListAuthenticationException` | Se captura, se loguea; Postgres no se toca |
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
Se registra inicio y fin de cada sincronización y de cada consulta a
`GET /productos` (`log.info`), y cada tipo de error de integración por
separado (`log.error`) — nunca el token ni el cuerpo completo de la
respuesta del proveedor.

## Pruebas unitarias (11 en total)
- `GestoPagoProductListSyncServiceImplTest` (7 casos): sincronización
  exitosa (verifica `deleteAllInBatch`/`saveAll`), y 6 escenarios de error
  donde se verifica que el repositorio **no** se toca.
- `ProductListServiceImplTest` (2 casos): catálogo con datos, catálogo
  vacío (aún no sincronizado).
- `ProductoControllerTest` (1 caso): confirma el envoltorio
  `{"mensaje", "data"}`.
- `ProductListApiResponseXmlTest` (1 caso): deserializa el XML con la forma
  exacta de la especificación real de PuntoRed.

## Decisiones técnicas y supuestos
1. **Postgres + Redis**: instrucción explícita del profesor. Postgres es la
   fuente de verdad/respaldo; Redis acelera lecturas.
2. **`@Scheduled` + `@CacheEvict` en el mismo método**: necesario para
   evitar el problema de auto-invocación de Spring AOP.
3. **Campos del catálogo**: se modelaron los atributos documentados en la
   especificación (`idProducto`, `idServicio`, `servicio`, `producto`,
   `legend`). No incluye precio ni existencia porque la API de GestoPago no
   los expone en este endpoint.
4. **Formato de respuesta `{"mensaje", "data"}`**: confirmado como válido
   para este endpoint específico.
5. **Sin `VARCHAR`**: la tabla `gestopago_productos` usa `TEXT` para todos
   los campos de texto.
