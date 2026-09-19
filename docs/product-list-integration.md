# Integración: Catálogo de Productos (GestoPago)

## Objetivo
Consumir `GET /sistema/service/getProductList.do` del servicio externo
**GestoPago** (especificación PuntoRed) y exponer el catálogo como
`GET /productos` en esta API, respetando las restricciones de uso reales
del proveedor.

## Restricciones del proveedor (clave para el diseño)
La especificación oficial de PuntoRed indica textualmente:

> *"This method should be called once a day... can only be used up to 3
> times in a single day, otherwise the IP will be blocked as an incorrect
> implementation or denial of service attack."*
>
> *"This endpoint should be used for the implementer to store the
> information obtained in its DB. It should never be used directly as a
> source of information for your frontend."*

Por eso **`GET /productos` nunca llama a GestoPago en el momento de la
petición**. La arquitectura separa dos responsabilidades:

1. **Sincronización** (`GestoPagoProductListSyncServiceImpl`): una tarea
   programada (`@Scheduled`, una vez al día) consulta GestoPago y guarda el
   catálogo en la tabla local `gestopago_productos`.
2. **Lectura** (`ProductListServiceImpl` / `GET /productos`): siempre lee de
   esa tabla local, nunca de GestoPago directamente.

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
`build.gradle` desde antes (se usan también para XML de correo). Spring Boot
detecta JAXB en el classpath y registra automáticamente un conversor XML que
Feign usa sin configuración adicional; el `@GetMapping` del cliente solo
declara `produces = MediaType.APPLICATION_XML_VALUE` para pedir XML
explícitamente.

Hay una prueba dedicada (`ProductListApiResponseXmlTest`) que deserializa un
XML con la forma exacta de la especificación real, sin necesitar red ni
mocks, para confirmar que el mapeo es correcto.

## Configuración (`application.properties`)
```properties
feign.client.config.gestoPagoProductList.connect-timeout=5000
feign.client.config.gestoPagoProductList.read-timeout=10000
gestopago.productos.sync-cron=0 0 3 * * *
```
No se agregó una URL ni un token nuevos: se reutilizan `gestopago.auth.url`
y el token ya gestionado por `GestoPagoTokenServiceImpl`.

## Capas implementadas
| Capa | Clase | Responsabilidad |
|---|---|---|
| Config | `GestoPagoProductListFeignConfig` | Agrega `Authorization: Bearer <token>` obteniéndolo de `GestoPagoTokenService.obtenerTokenActivo(...)` en cada llamada. |
| Client | `GestoPagoProductListClient` | Interfaz Feign para `GET /sistema/service/getProductList.do` (XML). |
| Model | `ProductoExternoDTO`, `MensajeExternoDTO`, `ProductListApiResponse` | Forma XML real de la respuesta del proveedor (JAXB). |
| Model | `ProductoResponse` | DTO público de `GET /productos`, desacoplado del formato externo. |
| Entity | `GestoPagoProducto` | Copia local (cache) de cada producto/servicio del catálogo. Sin `VARCHAR`: columnas de texto usan `TEXT`. |
| Repository | `GestoPagoProductoRepository` | Acceso a la tabla `gestopago_productos`. |
| Mapper | `ProductoMapper` (MapStruct) | `ProductoExternoDTO → GestoPagoProducto` (para guardar) y `GestoPagoProducto → ProductoResponse` (para exponer). |
| Service (sincronización) | `GestoPagoProductListSyncService` / `Impl` | Llama a GestoPago una vez al día, valida el `MENSAJE.CODIGO`, traduce errores y reemplaza el catálogo local. |
| Service (lectura) | `ProductListService` / `ProductListServiceImpl` | Lee y mapea el catálogo local. Nunca llama a GestoPago. |
| Controller | `ProductoController` | Expone `GET /productos`. |
| Excepciones | `exception/ProductList*Exception`, `GestoPagoTokenNoDisponibleException` | Ver manejo de errores. |

## Manejo de errores
Ocurren dentro de `GestoPagoProductListSyncServiceImpl.consultarCatalogoVigente()`,
distinguidos por **tipo de excepción** (sin `if`):

| Situación | Excepción lanzada |
|---|---|
| No hay token GestoPago activo todavía | `ProductListAuthenticationException` |
| GestoPago rechaza el token (401/403) | `ProductListAuthenticationException` |
| Timeout de conexión/lectura (`RetryableException`) | `ProductListTimeoutException` |
| GestoPago responde con otro status de error HTTP | `ProductListUnsuccessfulResponseException` |
| `MENSAJE.CODIGO` distinto de `"01"` (error de negocio con HTTP 200) | `ProductListUnsuccessfulResponseException` |
| Cualquier otro error (red, IO, etc.) | `ProductListCommunicationException` |

Como la sincronización corre en una tarea programada (no en una petición
HTTP), estas excepciones **no llegan a `GlobalExceptionHandler`**: se
capturan y registran (`log.error`) en el propio job programado
(`sincronizarProductosProgramado()`), igual que ya hace
`GestoPagoTokenServiceImpl.renovarToken()` con la renovación del token.
`GlobalExceptionHandler` conserva solo el manejo de
`MethodArgumentNotValidException` (validación de los `*Request`), que sí
ocurre en el contexto de una petición HTTP.

## Logging
Se registra inicio y fin de cada sincronización y de cada consulta a
`GET /productos` (`log.info`), y cada tipo de error de integración por
separado (`log.error`) — nunca el token ni el cuerpo completo de la
respuesta del proveedor.

## Pruebas unitarias
- `GestoPagoProductListSyncServiceImplTest` (7 casos): sincronización
  exitosa, respuesta con código de negocio distinto de éxito, sin token
  activo, error de autenticación HTTP, timeout, otro error HTTP, error de
  comunicación inesperado.
- `ProductListServiceImplTest` (2 casos): catálogo con datos, catálogo vacío
  (aún no sincronizado).
- `ProductListApiResponseXmlTest` (1 caso): deserializa el XML con la forma
  exacta de la especificación real de PuntoRed.

## Decisiones técnicas y supuestos
1. **Por qué se separó sincronización de lectura**: es un requisito
   explícito del proveedor (límite de 3 llamadas/día, prohibición de usarlo
   como fuente directa para el frontend), no una preferencia de diseño.
2. **Estrategia de refresco**: reemplazo completo (`deleteAllInBatch` +
   `saveAll`) en una transacción, ya que es un catálogo pequeño que se
   sincroniza una vez al día; no se justifica una lógica de upsert más
   compleja para este volumen.
3. **Campos del catálogo**: se modelaron los atributos documentados en la
   especificación (`idProducto`, `idServicio`, `servicio`, `producto`,
   `idCatTipoServicio`, `tipoFront`, `hasDigitoVerificador`, `legend`). No
   incluye precio ni existencia porque la API de GestoPago no los expone en
   este endpoint (son datos de catálogo/servicio, no de inventario).
4. **Sin `VARCHAR`**: la tabla `gestopago_productos` usa `TEXT` para todos
   los campos de texto, siguiendo la convención pedida.
5. **Pendiente fuera de esta corrección**: aún no se cuenta con credenciales
   reales de GestoPago (`idDistribuidor`, `codigoDispositivo`, `password`)
   para validar esto contra el servicio real; deben solicitarse al
   proveedor/profesor. Sin ellas, el flujo se valida con pruebas unitarias y
   la prueba de deserialización XML contra el contrato documentado.
