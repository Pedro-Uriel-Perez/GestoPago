# GestoPago

Servicio backend en Spring Boot que expone operaciones de negocio (gestión de
personas) y consume servicios externos de **GestoPago** (autenticación por
token y consulta de lista de productos).

## Stack

- Java 17 / Spring Boot 3.3.6 / Gradle
- PostgreSQL + Flyway (migraciones)
- Spring Data JPA (Hibernate)
- OpenFeign (clientes de integración con servicios externos)
- MapStruct + Lombok
- JUnit 5 + Mockito (pruebas unitarias)

## Estructura del proyecto

```
src/main/java/com/proyecto/servicios/
├── client/       Clientes Feign hacia servicios externos (GestoPago)
├── config/       Configuración (datasource, Flyway, Feign, OpenAPI)
├── controller/   Endpoints REST
├── entity/       Entidades JPA
├── exception/    Excepciones de negocio + manejador global de errores
├── mapper/       Mappers MapStruct (entidad/DTO externo -> DTO de respuesta)
├── model/        DTOs de request/response
├── repositorys/  Repositorios JPA
└── service/      Lógica de negocio (interfaces + Impl)
```


## Compilar y correr pruebas

```bash
./gradlew build        # compila y corre las pruebas
./gradlew test         # solo pruebas unitarias
```

Reporte de pruebas: `build/reports/tests/test/index.html`

## Endpoints principales

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/personas` | Crea una persona |
| PUT | `/personasActualiza` | Actualiza una persona existente |
| PUT | `/personasElimina` | Elimina una persona |
| GET | `/productos` | Consulta el catálogo de productos (Postgres + cache en Redis, sincronizado a diario con GestoPago) |

## Integración con GestoPago: catálogo de productos

`GET /productos` nunca llama a GestoPago en el momento de la petición: el
proveedor solo permite consultar `GET /sistema/service/getProductList.do`
hasta 3 veces al día. Una tarea programada
(`GestoPagoProductListSyncServiceImpl`) sincroniza el catálogo una vez al
día, guardándolo en Postgres (`gestopago_productos`) e invalidando el cache
de **Redis**; las lecturas (`GET /productos`) usan Redis cuando está
disponible y caen a Postgres en cache-miss. Confirmado end-to-end contra el
servicio real de GestoPago.

Documentación técnica detallada (arquitectura, decisiones y supuestos):
[`docs/product-list-integration.md`](docs/product-list-integration.md)

