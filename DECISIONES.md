# DECISIONES.md — Gestor de Inventario

Documento de decisiones técnicas del entregable "API auto-descriptiva".

---

## 1. Resumen de lo construido

| Pieza | Archivo |
|-------|---------|
| Propiedades tipadas y validadas | `src/main/java/org/example/proyecto1/config/AppInfoProperties.java` |
| DTO de respuesta | `src/main/java/org/example/proyecto1/dto/AppInfoResponse.java` |
| Endpoint versionado | `src/main/java/org/example/proyecto1/controller/InfoController.java` |
| Configuración de seguridad | `src/main/java/org/example/proyecto1/config/SecurityConfig.java` |
| Configuración base | `src/main/resources/application.properties` |
| Perfil dev | `src/main/resources/application-dev.properties` |
| Perfil prod | `src/main/resources/application-prod.properties` |
| Tests | `src/test/java/org/example/proyecto1/controller/InfoControllerTest.java` |

Endpoint: `GET /api/info` (alias versionado `GET /api/v1/info`).

Respuesta de ejemplo (perfil `dev`):

```json
{
  "name" : "Gestor de Inventario",
  "version" : "0.0.1-SNAPSHOT",
  "description" : "API REST para la gestion de inventario",
  "environment" : "dev",
  "developer" : { "name" : "Dev Inventario", "email" : "dev@inventario.com" },
  "timestamp" : "2026-10-06T17:34:34.422585300Z"
}
```

---

## 2. Decisiones de diseño

### P1. ¿Por qué `@ConfigurationProperties` en lugar de `@Value`?

Usé `@ConfigurationProperties(prefix = "app")` porque:

1. **Agrupación y jerarquía**: `app.developer.name` y `app.developer.email` se mapean a un objeto
   `Developer` anidado. Con `@Value` habría que declarar un campo por cada clave y repetir
   `@Value("${app.developer.email}")` en cada uno; el binding no entiende jerarquía.
2. **Validación integrada**: al combinar `@Validated` + `@NotBlank` / `@Email` / `@Pattern`, Spring
   valida **al arrancar**. Con `@Value` la validación no se dispara de forma declarativa; habría que
   validar a mano en el constructor o en un `@PostConstruct`.
3. **Relajación de nombres (relaxed binding)**: `app.developer.email` también acepta
   `APP_DEVELOPER_EMAIL`, `app.developer.email`, etc. Ideal para variables de entorno en prod.
4. **Testabilidad**: es un bean normal; se puede inyectar y también registrar en un slice de test con
   `@EnableConfigurationProperties(AppInfoProperties.class)`.
5. **Autocompletado/IDE** y documentación de la estructura del archivo de configuración.

Con `@Value` el código es más frágil: si agrego una propiedad tengo que crear el campo + la anotación,
y un typo en el nombre de la clave solo se detecta en tiempo de arranque sin un mensaje claro.

### P2. ¿Cómo garantizas que la versión no se desincronice del `pom.xml`?

No escribo la versión a mano. En `application.properties` uso el placeholder de Maven:

```properties
app.version=@project.version@
```

El `spring-boot-starter-parent` configura el `maven-resources-plugin` con **filtrado de recursos** y
delimitador `@`, por lo que al construir, `@project.version@` se reemplaza por el `<version>` del
`pom.xml` (`0.0.1-SNAPSHOT`). Verificación:

```text
target\classes\application.properties:5:app.version=0.0.1-SNAPSHOT
```

Es decir: la única fuente de verdad es el `pom.xml`; la aplicación solo la lee.

> Nota práctica: como el filtrado ocurre en el **build** de Maven, hay que compilar/empaquetar antes de
> arrancar. Si se ejecuta desde un IDE que no aplica filtrado, `app.version` quedaría como el literal
> `@project.version@` y la validación `@Pattern` lo rechazaría. Por eso en IntelliJ conviene ejecutar
> vía Maven (`mvnw spring-boot:run`) o confiar en los recursos filtrados de `target/classes`.

### P3. ¿Qué pasaría si quitas el `@Component` de `AppInfoProperties`? (probado)

Quité `@Component` (dejando solo `@ConfigurationProperties` + `@Validated`) y ejecuté
`./mvnw test -Dtest=Proyecto1ApplicationTests`. La app **no arranca**:

```text
APPLICATION FAILED TO START

Description:
Parameter 0 of constructor in org.example.proyecto1.controller.InfoController required a bean of type
'org.example.proyecto1.config.AppInfoProperties' that could not be found.

Action:
Consider defining a bean of type 'org.example.proyecto1.config.AppInfoProperties' in your configuration.

Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with
name 'infoController' ... No qualifying bean of type 'org.example.proyecto1.config.AppInfoProperties'
available: expected at least 1 bean which qualifies as autowire candidate.
```

**Explicación**: `@ConfigurationProperties` **no** registra el bean por sí solo; solo describe cómo
enlazar propiedades. El registro del bean lo aporta `@Component` (mediante el component scan), o bien
`@EnableConfigurationProperties(AppInfoProperties.class)`, o `@ConfigurationPropertiesScan`. Al quitar
`@Component` no hay definición de bean, `InfoController` no puede satisfacer su dependencia por
constructor y el contexto falla.

Decisión final: dejo `@Component` en la clase. Alternativa igualmente válida sería quitarlo y anotar la
`Proyecto1Application` con `@ConfigurationPropertiesScan`.

### P4. ¿Qué diferencia hay entre mi `/api/info` y el `/actuator/info` de Spring?

| Aspecto | `/api/info` (mío) | `/actuator/info` (Spring Boot) |
|---------|-------------------|-------------------------------|
| Origen | Código propio + `AppInfoProperties` | `InfoContributor`s del starter de Actuator |
| Forma del JSON | Diseñada por el equipo (DTO). Incluye `environment` y `timestamp` | Estructura fija del framework (`{"app": {...}, "git": {...}, "build": {...}}`) |
| Versionado | Sí: `/api/v1/info` | No versionado, ruta fija `management.endpoints.web.base-path` |
| Contenido | Metadata del dominio de MI app (qué es, versión, developer, entorno) | Metadata técnica: variables de entorno expuestas (`management.info.env.enabled`), git, build, health |
| Control | Total: validación, perfiles, campos | Requiere `management.info.*` y a menudo plugins (git-commit-id, build-info) |
| Propósito | Contrato público para clientes ("¿quién eres y qué versión?") | Diagnóstico operativo (operaciones/tooling) |

En resumen: `/actuator/info` es una **vista de diagnóstico estandarizada** que expone lo que el
framework sabe; `/api/info` es **mi contrato de negocio**, versionado y estable, que puedo cambiar sin
depender de Actuator. De hecho aquí uso Actuator para *health* (`/actuator/health`) y dejo `/api/info`
como endpoint de identidad de la API.

---

## 3. Exploración de propiedades (2+ por categoría)

Todas se agregaron a `application.properties` y se sobreescriben por perfil cuando aplica.
"Si no la configuro" se contrasta con el valor por defecto documentado de Spring Boot.

### Servidor
| Propiedad | Valor | Qué cambió | Cómo lo verifiqué | Si no la configuro |
|-----------|-------|-----------|-------------------|--------------------|
| `server.port` | `8080` (dev) / `9090` (prod) | El puerto donde escucha Tomcat. Prod levanta en otro puerto | `curl` a `:8080/api/info` (dev) y `:9090/api/info` (prod) | Default `8080`; sin perfil no podría separar dev/prod |
| `server.error.include-message` | `always` | Incluye el campo `message` en el JSON de error | Provocado un `404` en `/api/does-not-exist` | Default `never`: los errores **ocultan** el motivo |

### Logging
| Propiedad | Valor | Qué cambió | Cómo lo verifiqué | Si no la configuro |
|-----------|-------|-----------|-------------------|--------------------|
| `logging.level.org.example.proyecto1` | `INFO` base, `DEBUG` en dev, `WARN` en prod | Nivel de detalle de mis paquetes. En dev se ve más, en prod menos ruido | Arranque en dev vs prod (logs de Tomcat/Hikari visibles con distinto detalle) | Default `INFO` (hereda de `root`) |
| `logging.file.name` | `logs/inventario.log` | Además de consola, escribe a archivo | Archivo creado al arrancar | Sin él, solo logs de consola (no hay historial en disco) |

### Bases de datos
| Propiedad | Valor | Qué cambió | Cómo lo verifiqué | Si no la configuro |
|-----------|-------|-----------|-------------------|--------------------|
| `spring.datasource.url` / `driver` | H2 en base y dev; PostgreSQL en prod | Fuente de datos distinta por entorno | `/actuator/health` muestra `"database":"H2"` en dev | Sin datasource y con session-jdbc, el arranque falla por falta de `DataSource` |
| `spring.datasource.hikari.maximum-pool-size` | `10` | Tamaño máximo del pool de conexiones | Pool creado por Hikari en el arranque (`HikariPool-1`) | Default **10**; en cargas altas un pool pequeño limita concurrencia |
| `spring.datasource.hikari.connection-timeout` | `30000` | Tiempo máximo de espera de conexión (ms) | Documentado; valor usado por Hikari | Default **30000** ms |

### JSON (Jackson)
| Propiedad | Valor | Qué cambió | Cómo lo verifiqué | Si no la configuro |
|-----------|-------|-----------|-------------------|--------------------|
| `spring.jackson.serialization.indent-output` | `true` | JSON pretty-printed (con saltos/indentación) | Salida `curl` de `/api/info` viene formateada | Default `false`: JSON en una sola línea |
| `spring.jackson.default-property-inclusion` | `non_null` | Omite campos `null` del JSON | Respuesta de `/api/info` sin nulos | Default `always`: aparecerían campos con `null` |

### Actuator
| Propiedad | Valor | Qué cambió | Cómo lo verifiqué | Si no la configuro |
|-----------|-------|-----------|-------------------|--------------------|
| `management.endpoints.web.exposure.include` | `health,info` | Expone esos endpoints por HTTP | `GET /actuator/health` responde `UP` | Sin exponer, los endpoints existen pero **no** son accesibles por web |
| `management.endpoint.health.show-details` | `always` | Muestra detalle de cada componente de salud | El health devolvió `components.db`, `diskSpace`, `ssl`, etc. | Default `never`: solo `{"status":"UP"}` |
| `management.info.env.enabled` | `true` | Permite que `/actuator/info` muestre `info.*` del entorno | Documentado; complementa el endpoint `/api/info` | Default `false`: `/actuator/info` queda vacío |

### Seguridad
| Propiedad | Valor | Qué cambió | Cómo lo verifiqué | Si no la configuro |
|-----------|-------|-----------|-------------------|--------------------|
| `spring.security.user.name` / `password` | `admin` / `admin123` | Usuario en memoria para autenticación básica | `curl` a `/` da `401` sin credenciales y `200` con `-u admin:admin123` | Spring genera una contraseña aleatoria al arrancar (impracticable para automatizar) |
| `server.servlet.session.cookie.http-only` | `true` | La cookie de sesión no es accesible desde JS | Documentado; mitiga XSS sobre la cookie | Default `true` |
| `server.servlet.session.cookie.secure` | `false` (dev) / `true` (prod) | En prod, la cookie solo viaja por HTTPS | Inspección de la config por perfil | Default `false`; en prod conviene `true` |

Evidencia de seguridad y actuator (perfil dev):

```text
=== /actuator/health ===
{"components":{"db":{...},"status":"UP"},"status":"UP"}

=== / (sin credenciales) === 401
=== / (admin:admin123)    === 200
```

**Conclusión de la exploración**: cada propiedad cambió un comportamiento observable (puerto, nivel de
log, motor de BD, formato de JSON, endpoints expuestos o autenticación). Si se omiten, Spring Boot usa
sus defaults, que casi siempre son razonables pero **no** los que queremos para separar dev de prod
(p. ej. sin `server.port` por perfil, dev y prod chocarían en el mismo puerto; sin `logging.level` por
perfil, prod sería igual de verboso que dev).

---

## 4. Reto de comprensión: valor inválido (`server.port=abc`)

Cambié `server.port=8080` por `server.port=abc` y arranqué. Spring Boot **no arranca**:

```text
APPLICATION FAILED TO START

Description:
Invalid value 'abc' for configuration property 'server.port' (originating from 'class path resource
[application.properties] - 12:13'). Validation failed for the following reason:

Caused by: org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException:
Property server.port with value 'abc' is invalid: Failed to convert to type java.lang.Integer

Caused by: java.lang.NumberFormatException: For input string: "abc"
```

**Por qué ocurre**: el enlace de propiedades es **tipado**. `server.port` se mapea a un `int`/`Integer`.
Al arrancar, `ConfigurationPropertiesBinder` intenta convertir la cadena `"abc"` a número mediante la
`ConversionService`; la conversión lanza `NumberFormatException`, que Spring Boot envuelve en
`InvalidConfigurationPropertyValueException` y **aborta el arranque**. Es un *fail-fast* deseable:
mejor detener la app que arrancar en un puerto indeterminado. El mensaje incluso indica el archivo y la
línea (`12:13`) donde está el valor inválido.

---

## 5. Tests y evidencia de ejecución

### Tests (`./mvnw test`) — verde

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`InfoControllerTest` (`@WebMvcTest(InfoController.class)` + `MockMvc`) valida:
1. `GET /api/info` → **200** y estructura JSON (`name`, `version`, `description`, `environment`, `developer.name`).
2. El email del developer cumple formato válido: `^[^@\s]+@[^@\s]+\.[^@\s]+$`.
3. El endpoint versionado `GET /api/v1/info` responde igual.

`@AutoConfigureMockMvc(addFilters = false)` desactiva los filtros de seguridad para aislar el
controlador; la seguridad se verifica aparte con `curl`.

### Evidencia `curl` — perfil dev

```text
$ curl http://localhost:8080/api/info
{
  "name" : "Gestor de Inventario",
  "version" : "0.0.1-SNAPSHOT",
  "description" : "API REST para la gestion de inventario",
  "environment" : "dev",
  "developer" : { "name" : "Dev Inventario", "email" : "dev@inventario.com" },
  "timestamp" : "2026-10-06T17:34:34.422585300Z"
}
```

### Evidencia `curl` — perfil prod (cambia `environment` y puerto)

```text
$ curl http://localhost:9090/api/info
{
  "name" : "Gestor de Inventario",
  "version" : "0.0.1-SNAPSHOT",
  "description" : "API REST para la gestion de inventario",
  "environment" : "prod",
  "developer" : { "name" : "Dev Inventario", "email" : "dev@inventario.com" },
  "timestamp" : "2026-10-06T17:35:01.464558300Z"
}
```

Comandos usados:

```powershell
java -jar target/proyecto1-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
java -jar target/proyecto1-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod --spring.session.jdbc.initialize-schema=never
```

> En la demo local de `prod` desactivé la inicialización del esquema de sesión porque el perfil apunta
> a PostgreSQL y no había un servidor Postgres disponible; `/api/info` no toca la BD, así que la
> respuesta es válida. En un entorno real, prod se levanta contra PostgreSQL.

---

## 6. Reglas de uso de IA

- **Permitido y usado**: consulté documentación de Spring Boot para los valores por defecto de las
  propiedades de la sección 3 y para la explicación del *fail-fast* de la sección 4. También contrasté
  el enfoque `@ConfigurationProperties` vs `@Value`.
- **Verificado por mí**: los experimentos de las secciones P3, P4 y 4 se ejecutaron realmente en este
  repositorio y los errores/JSON citados son salida real de `./mvnw` y `curl`.
- Todo el código de `AppInfoProperties`, `AppInfoResponse`, `InfoController`, `SecurityConfig` y los
  tests lo puedo explicar línea por línea.