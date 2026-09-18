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
| GET | `/productos` | Consulta la lista de productos vía GestoPago |

## Integración con GestoPago: lista de productos

La consulta `GET /productos` consume `GET /sistema/service/getProductList.do`
del servicio externo GestoPago, reutilizando el mismo host y el mismo token
que ya usa la autenticación existente. Incluye manejo de errores tipado
(autenticación, timeout, respuesta no exitosa, comunicación) y pruebas
unitarias con escenarios de éxito y de error.

Documentación técnica detallada (arquitectura, decisiones y supuestos):
[`docs/product-list-integration.md`](docs/product-list-integration.md)

