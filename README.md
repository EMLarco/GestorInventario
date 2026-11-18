<h1 align="center">Gestor de Inventario — Self-Describing API</h1>

<p align="center">
  Spring Boot foundation for an inventory management system.<br/>
  It exposes a versioned identity endpoint backed by typed, validated and environment-specific configuration,
  with HTTP Basic security, health monitoring and multi-language support.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" />
  <img src="https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white" />
  <img src="https://img.shields.io/badge/H2-Database-1E3A8A?style=for-the-badge" />
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" />
</p>

---

## Features

- **Self-describing API** — `GET /api/info` (and its versioned alias `GET /api/v1/info`) returns the application's name, version, description, active environment, developer contact and a timestamp.
- **Typed, validated configuration** — `app.*` properties are bound with `@ConfigurationProperties` and validated at startup (`@NotBlank`, `@Email`, `@Pattern`). Invalid values stop the app immediately (fail-fast).
- **Version from Maven** — `app.version` is injected from `pom.xml` at build time through resource filtering.
- **Environment profiles** — separate `dev` and `prod` configurations for port, logging, database and session cookies.
- **Security** — HTTP Basic authentication with an in-memory user; `HttpOnly` session cookies, marked `Secure` in production.
- **JDBC-backed sessions** — HTTP sessions stored in the database with Spring Session JDBC.
- **Health monitoring** — Spring Boot Actuator exposes `/actuator/health` (with full details) and `/actuator/info`.
- **Internationalization** — greeting page available in Spanish, English and Portuguese. Language is chosen from the `?lang=` parameter, a `lang` cookie or the browser's `Accept-Language` header (default: Spanish).
- **Clean JSON output** — indented responses with `null` fields omitted.
- **File logging** — logs written to `logs/inventario.log`.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/info` | Application identity (business contract) |
| `GET` | `/api/v1/info` | Versioned alias of `/api/info` |
| `GET` | `/` or `/home` | Localized greeting page (Thymeleaf). Optional `?name=` and `?lang=es\|en\|pt` |
| `GET` | `/home/text` | Localized greeting as plain text |
| `GET` | `/actuator/health` | Health status with details |
| `GET` | `/actuator/info` | Operational info from Actuator |

### Example response — `GET /api/info`

```json
{
  "name": "Gestor de Inventario",
  "version": "0.0.1-SNAPSHOT",
  "description": "...",
  "environment": "dev",
  "developer": {
    "name": "Esteban Larco",
    "email": "..."
  },
  "timestamp": "..."
}
```

## Profiles

| Setting | `dev` | `prod` |
|---|---|---|
| Port | `8080` | `9090` |
| Log level (own packages) | `DEBUG` | `WARN` |
| Database | H2 (in memory) | PostgreSQL |
| Session cookie `Secure` | Off | On |

Shared settings: HikariCP pool (max size 10, 30 s connection timeout) and `server.error.include-message=always`.

## Project Structure

```
GestorInventario/
├── pom.xml
├── DECISIONES.md                         # Technical design decisions (Spanish)
└── src/
    ├── main/
    │   ├── java/org/example/proyecto1/
    │   │   ├── Proyecto1Application.java
    │   │   ├── config/
    │   │   │   ├── AppInfoProperties.java   # Typed and validated app.* properties
    │   │   │   ├── SecurityConfig.java      # HTTP Basic + in-memory user
    │   │   │   └── WebConfig.java           # Locale resolver (cookie / header / default)
    │   │   ├── controller/
    │   │   │   ├── HelloController.java     # Localized greeting
    │   │   │   └── InfoController.java      # /api/info and /api/v1/info
    │   │   └── dto/
    │   │       └── AppInfoResponse.java
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       ├── application-prod.properties
    │       ├── messages.properties          # Spanish (default)
    │       ├── messages_en.properties
    │       ├── messages_pt.properties
    │       └── templates/hello.html
    └── test/java/org/example/proyecto1/
        ├── Proyecto1ApplicationTests.java
        └── controller/
            ├── HelloControllerTest.java
            └── InfoControllerTest.java
```

## Getting Started

### Prerequisites

- Java 17 or later
- PostgreSQL (only for the `prod` profile)

The project includes the Maven Wrapper, so you do not need to install Maven.

### Run in development

```bash
git clone https://github.com/EMLarco/GestorInventario.git
cd GestorInventario
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

> Build with Maven before running from an IDE: `app.version` is filled in by Maven resource filtering, and without it the startup validation rejects the raw placeholder.

### Run in production mode

Configure the PostgreSQL connection in `application-prod.properties` (or with environment variables), then:

```bash
./mvnw clean package
java -jar target/proyecto1-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Try it

```bash
# Without credentials -> 401 Unauthorized
curl -i http://localhost:8080/api/info

# With credentials -> 200 OK
curl -u admin:<password> http://localhost:8080/api/info

# Greeting in Portuguese
curl -u admin:<password> "http://localhost:8080/home/text?name=Esteban&lang=pt"
```

## Tests

```bash
./mvnw test
```

Six tests cover the web layer: HTTP status codes, JSON structure, email format and the versioned endpoint.

## Design Decisions

The reasoning behind each technical choice (`@ConfigurationProperties` vs `@Value`, `/api/info` vs `/actuator/info`, profile separation, fail-fast behavior) is documented in [`DECISIONES.md`](DECISIONES.md).

## Roadmap

- [ ] Product, category and supplier entities
- [ ] Stock movements (inbound / outbound)
- [ ] REST CRUD endpoints for inventory
- [ ] Move credentials to environment variables

## Author

**Esteban Larco** — Information Technology Engineering student at Universidad de las Fuerzas Armadas ESPE.

[![GitHub](https://img.shields.io/badge/GitHub-EMLarco-181717?style=flat&logo=github)](https://github.com/EMLarco)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Esteban%20Larco-0077B5?style=flat&logo=linkedin)](https://www.linkedin.com/in/esteban-larco-66491843b)
