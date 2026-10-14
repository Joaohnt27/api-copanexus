# CopaNexus API

REST API for CopaNexus, a sticker collection manager. It supports user accounts, JWT authentication, password recovery by email, collected stickers, duplicate inventory, purchase/sale records, and an announcements feed.

**Project status:** The production API was shut down on October 13, 2026. This repository is retained for reference and local development. The production domains referenced in configuration and email templates are historical references.

The application uses Portuguese resource paths and JSON field names. This README documents the implementation in this repository, including its current limitations.

## Contents

- [Technology and structure](#technology-and-structure)
- [Local setup](#local-setup)
- [Configuration](#configuration)
- [Application lifecycle and data model](#application-lifecycle-and-data-model)
- [Authentication and access](#authentication-and-access)
- [Endpoint reference](#endpoint-reference)
- [Request examples](#request-examples)
- [Email delivery](#email-delivery)
- [Errors and validation](#errors-and-validation)
- [Build and testing](#build-and-testing)
- [Deployment and operational notes](#deployment-and-operational-notes)
- [Current implementation limitations](#current-implementation-limitations)
- [License](#license)

## Technology and structure

| Component | Implementation |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| HTTP | Spring Web MVC / embedded Tomcat |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL; configuration comments refer to Neon |
| Security | Spring Security, BCrypt, stateless JWT authentication |
| JWT library | Auth0 `java-jwt` 4.4.0 |
| Email | Resend Java SDK 3.1.0 |
| Validation | Jakarta Bean Validation |
| Boilerplate generation | Lombok |
| Build | Maven; wrapper configured for Maven 3.9.14 |

The Maven project lives in `api/`, one level below this README:

```text
.
├── README.md
└── api/
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    ├── .mvn/wrapper/maven-wrapper.properties
    ├── system.properties
    └── src/
        ├── main/
        │   ├── java/com/copanexus/api/
        │   │   ├── ApiApplication.java
        │   │   ├── config/       # Security, CORS, exception handlers
        │   │   ├── controller/   # User, transaction, announcement endpoints
        │   │   ├── dto/          # User response/update and error types
        │   │   ├── model/        # JPA entities and announcement enum
        │   │   ├── repository/   # Spring Data repositories
        │   │   ├── security/     # JWT generation and request filtering
        │   │   └── service/      # Email delivery and HTML templates
        │   └── resources/application.properties
        └── test/java/com/copanexus/api/ApiApplicationTests.java
```

Requests pass through Spring Security and its JWT filter before reaching the controllers. Controllers perform most business logic and call JPA repositories directly. `EmailService` is the dedicated service for outbound email. There is no separate collection, transaction, or account service layer.

## Local setup

### Prerequisites

- JDK 17.
- An accessible PostgreSQL database and a database user allowed to create/update the application schema.
- A Resend API key and authorization to send from the sender addresses configured in `EmailService` for working email delivery.
- Network access for the first Maven wrapper run to download Maven and project dependencies.

### Configure the environment

From the repository root:

```bash
cd api
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/copanexus'
export SPRING_DATASOURCE_USERNAME='copanexus'
export SPRING_DATASOURCE_PASSWORD='replace-with-database-password'
export JWT_SECRET='replace-with-a-long-random-secret'
export ADMIN_KEY='replace-with-a-separate-random-admin-key'
export RESEND_API_KEY='replace-with-your-resend-key'
export PORT='8080'
```

Create the database before launching the application. For a hosted PostgreSQL service requiring TLS, use its JDBC connection URL and required parameters, such as `?sslmode=require`.

The repository does not include a `.env` loader. Export variables in the process environment or configure them in your IDE/deployment platform. Do not commit real credentials.

### Start the API

From `api/`:

```bash
sh ./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd spring-boot:run` after configuring the environment in your shell. The shell invocation above also works when the Unix wrapper has no executable permission.

The default base URL is `http://localhost:8080`. There is no root welcome endpoint, Swagger UI, or Actuator health endpoint in this repository. On startup, Hibernate updates the schema and a runner initializes the sticker catalog if its table is empty.

## Configuration

| Environment variable | Required | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | Yes | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Yes | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | HMAC signing and verification secret |
| `ADMIN_KEY` | Yes | Shared secret accepted by announcement creation through `X-Admin-Key` |
| `RESEND_API_KEY` | Yes | Resend API credential |
| `PORT` | No | HTTP port; defaults to `8080` |

These settings are defined in `api/src/main/resources/application.properties`:

| Setting | Value / effect |
| --- | --- |
| `spring.application.name` | `api` |
| `spring.jpa.hibernate.ddl-auto` | `update`; schema changes are applied automatically |
| `spring.jpa.open-in-view` | `false` |
| Hikari maximum pool size | 3 connections |
| Hikari minimum idle | 1 connection |
| Hikari idle timeout | 30,000 ms |
| Hikari maximum lifetime | 60,000 ms |
| Tomcat maximum threads | 20 |
| Tomcat minimum spare threads | 2 |
| JMX | Disabled |
| Lazy bean initialization | Enabled |
| SQL / formatted SQL logs | Disabled |
| Root and Spring Web log levels | `ERROR` |

No separate development/test/production profile configuration is supplied. Spring Boot property overrides can be used when a different deployment needs different settings.

## Application lifecycle and data model

### Startup catalog initialization

`ApiApplication.popularFigurinhas` runs after startup:

1. Count rows in `figurinhas`.
2. If the count is zero, insert **980 stickers numbered 0 through 979**, inclusive.
3. Otherwise, skip initialization entirely.

A partially populated table is not repaired. There is no public endpoint for listing or editing the sticker catalog.

### Stored entities

| Table | Main fields and behavior |
| --- | --- |
| `users` | Generated `id`, `nome`, unique non-null `email`, BCrypt `senha`, recovery code and expiration |
| `figurinhas` | Generated `id`, unique non-null integer `numero` |
| `usuario_figurinhas` | Many-to-many relation between users and collected stickers |
| `usuario_repetidas` | Per-user map of sticker number (`figurinha_numero`) to quantity (`quantidade`) |
| `transacoes` | Generated `id`, required `label`, `valor`, `tipo`, timestamp, required `usuario_id` |
| `avisos` | Generated `id`, `titulo`, text `mensagem`, string enum `tipo`, `cidade`, `loja`, creation time |

Collections use a `Set`, so adding an already collected sticker does not increase collection size. Duplicate quantities are stored independently from the collection. Adding a duplicate does not collect that sticker, and collecting/removing a sticker does not adjust duplicate stock.

Transactions belong to a user and are configured with cascading deletion and orphan removal. Account deletion clears collection and duplicate relationships before deleting the account. Sticker catalog entries remain available to other accounts.

### Dates, numbers, and progress

- Transaction timestamps are assigned on insertion using the server's local time and serialized as `dd/MM/yyyy HH:mm`.
- Announcement timestamps are assigned on insertion using UTC and serialized as `yyyy-MM-dd'T'HH:mm:ss`, without a timezone suffix.
- Recovery expiration uses server-local `LocalDateTime`.
- Transaction amounts use Java `Double`; there is no currency field, decimal-money type, or server-side balance calculation.
- Progress uses a hardcoded total of **994**, with `round((collection size / 994 × 100) × 100) / 100`. The seeded 980 stickers therefore do not produce 100% completion.

## Authentication and access

### Login lifecycle

1. Register an account or use an existing account.
2. Submit `email` and `senha` to `POST /api/usuarios/login`.
3. Save the returned `token` and `usuario.id`.
4. Send `Authorization: Bearer <token>` to protected routes.

Passwords are hashed with BCrypt. Tokens use HMAC SHA-256, issuer `copanexus-api`, and the user's email as subject. Expiration is calculated from the current local date/time plus 365 days, converted using a fixed `-03:00` offset; a server configured to another timezone can shift the effective lifetime.

The filter verifies the token and looks up the subject email on every authenticated request. An invalid/expired token or an email no longer present in the database leaves the request unauthenticated. Authentication is stateless, CSRF is disabled, and no session-based login, refresh endpoint, logout endpoint, or token revocation store is implemented. All authenticated accounts receive `ROLE_USER`.

Changing an email requires logging in again to obtain a token for the new email. Changing/resetting a password does not explicitly invalidate previously issued tokens. Deleting an account prevents its token from authenticating while its subject email has no corresponding account.

### Access rules

| Route | Access |
| --- | --- |
| `POST /api/usuarios/registrar` | Public |
| `POST /api/usuarios/login` | Public |
| `POST /api/usuarios/esqueci-senha` | Public |
| `POST /api/usuarios/redefinir-senha` | Public |
| `POST /api/avisos` | No JWT required; controller checks `X-Admin-Key` |
| `GET /api/avisos` | JWT required |
| Every other request | JWT required |

**Resource ownership is not checked.** An authenticated account can supply another user's ID in collection, duplicate, profile, transaction, and user routes. Password checks exist for sensitive profile changes and account deletion, but they do not provide general ownership enforcement. Transaction deletion checks only the transaction ID, ignoring whether it belongs to the user ID in the URL.

### CORS

The global security CORS configuration allows:

```text
https://copanexus.vercel.app
https://copanexus.com.br
https://www.copanexus.com.br
http://localhost:5173
http://localhost:4173
```

Allowed methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`. All request headers are allowed. Credentialed cross-origin access is not explicitly enabled. The user controller also declares `@CrossOrigin("*")`, and the transaction controller declares `http://localhost:5173`; the security filter uses the explicit global configuration above.

## Endpoint reference

Paths below are relative to the base URL. JSON request bodies use `Content-Type: application/json`. Response messages remain in Portuguese. Status codes listed here describe controller branches; security, request parsing, validation, and persistence failures may add other responses.

### Accounts and password recovery

| Method | Path | Input | Controller response |
| --- | --- | --- | --- |
| `POST` | `/api/usuarios/registrar` | `nome`, `email`, `senha` | `201` user DTO; `400` blocked email domain; `409` existing email |
| `POST` | `/api/usuarios/login` | `email`, `senha` | `200` token and user DTO; `401` invalid credentials |
| `GET` | `/api/usuarios` | Bearer token | `200` list of user entities; no pagination |
| `PUT` | `/api/usuarios/{id}/perfil` | Optional `nome`, `email`, `senhaAntiga`, `novaSenha` | `200` message; `401` wrong/missing current password for sensitive changes; `400` duplicate email; `404` user missing |
| `DELETE` | `/api/usuarios/{id}` | `{"senha":"current-password"}` | `200` message; `401` incorrect/missing password; `404` user missing; `500` deletion error |
| `POST` | `/api/usuarios/esqueci-senha` | `email` | `200` recovery requested; `400` unknown email; `500` if an exception escapes email invocation |
| `POST` | `/api/usuarios/redefinir-senha` | `email`, `codigo`, `novaSenha` | `200` message; `400` unknown user, invalid code, or expired code |

Registration validates a nonblank name, a nonblank valid email, and a nonblank password of at least six characters. It also rejects these email domains, comparing the domain in lowercase:

```text
teste.com, abc.com, 123.com, email.com, mailinator.com, tempmail.com, exemplo.com
```

Email lookup uses the supplied value without explicit normalization. Registration and login return the safe user DTO:

```json
{"id":1,"nome":"Alex Collector","email":"alex@example.org"}
```

Login wraps that DTO:

```json
{
  "token": "<signed-jwt>",
  "usuario": {
    "id": 1,
    "nome": "Alex Collector",
    "email": "alex@example.org"
  }
}
```

`GET /api/usuarios` returns entities instead of this DTO. Password fields/getters are not marked `@JsonIgnore`, so this route can expose password hashes and additional `UserDetails` properties. Treat this as a current security limitation, not as a safe public profile endpoint.

Profile updates ignore null/blank fields. Nonblank names have HTML-like tags removed using a regular expression. Changing email or password requires `senhaAntiga`; a name-only update does not. This route does not use `@Valid` on the update DTO and does not apply the registration domain blocklist. Registration's six-character minimum is not explicitly applied to password updates or resets.

Recovery stores a zero-padded six-digit code generated with `java.util.Random.nextInt(999999)` and a 15-minute expiration. A new request replaces the previous code. Successful reset hashes the new password and clears both recovery fields. There is no separate code-verification endpoint. Existing and unknown emails receive different statuses/messages, so the response can reveal account existence.

### Collected stickers and progress

| Method | Path | Controller response |
| --- | --- | --- |
| `GET` | `/api/usuarios/{id}/colecao` | `200` array of collected sticker numbers; `404` user missing |
| `POST` | `/api/usuarios/{id}/colar/{numero}` | `200` empty body; `400` user or catalog sticker missing |
| `DELETE` | `/api/usuarios/{id}/descolar/{numero}` | `200` message; `400` sticker absent from collection; `404` user missing |
| `GET` | `/api/usuarios/{id}/progresso` | `200` progress object; `404` user missing |

Collection example (order is unspecified):

```json
[0, 10, 42]
```

Progress for three collected stickers:

```json
{"total":994,"obtidas":3,"porcentagem":0.3}
```

`colar` looks up an existing catalog number; it does not create new catalog entries. Repeated successful calls for the same sticker leave a single collection entry.

### Duplicate inventory

| Method | Path | Controller response |
| --- | --- | --- |
| `GET` | `/api/usuarios/{id}/repetidas` | `200` number-to-quantity map; `404` user missing |
| `POST` | `/api/usuarios/{id}/repetidas/adicionar/{numero}` | `200` updated map; `404` user missing |
| `POST` | `/api/usuarios/{id}/repetidas/remover/{numero}` | `200` updated map; `400` number absent; `404` user missing |

Example:

```json
{"10":2,"42":1}
```

JSON object keys are strings even though the Java map keys are integers. Addition increments the count by one. Removal decrements it by one, deleting the entry when its previous quantity was one or less. These routes do not validate numbers against the sticker catalog or enforce a numeric range.

### Transactions

| Method | Path | Controller response |
| --- | --- | --- |
| `GET` | `/api/usuarios/{usuarioId}/transacoes` | `200` list ordered by transaction timestamp descending |
| `POST` | `/api/usuarios/{usuarioId}/transacoes` | `200` saved transaction; `404` user missing |
| `DELETE` | `/api/usuarios/{usuarioId}/transacoes/{transacaoId}` | `200` empty body; `404` transaction missing |

Create body:

```json
{"label":"Sticker pack","valor":7.5,"tipo":"buy"}
```

Response shape:

```json
{
  "id": 1,
  "label": "Sticker pack",
  "valor": 7.5,
  "tipo": "buy",
  "dataTransacao": "14/10/2026 14:30"
}
```

`usuario` is excluded from transaction JSON. The timestamp is overwritten on insertion. The intended `tipo` values are `buy` and `sell`, but the field is a free-form string without enum validation. Required database fields are `label`, `valor`, `tipo`, and user association; there is no request-level `@Valid`, positive-amount rule, or automatic effect on sticker inventory. Listing a nonexistent user's transactions returns an empty list. No update, pagination, totals, or balance endpoint exists.

### Announcements

| Method | Path | Input | Controller response |
| --- | --- | --- | --- |
| `GET` | `/api/avisos` | Bearer token | `200` latest 20 announcements, newest first |
| `POST` | `/api/avisos` | `X-Admin-Key` header and announcement body | `201` saved announcement; `401` incorrect key |

Create body:

```json
{
  "titulo": "New sticker stock",
  "mensagem": "Sticker packs are available at the store.",
  "tipo": "ESTOQUE",
  "cidade": "São Paulo",
  "loja": "Example Store"
}
```

`tipo` supports `DADOS`, `ESTOQUE`, `URGENTE`, and `COMUM`. Creation assigns `id` and UTC `dataCriacao`, for example `2026-10-14T17:30:00`. The header is required by request binding; a missing header fails before the controller's key comparison. There are no bean validation annotations on announcement fields and no update/delete endpoint. The feed's 20-item limit is fixed, with no pagination parameter.

## Request examples

Run the application first. Replace example IDs with the IDs returned by your own requests.

### Register and log in

```bash
curl -i http://localhost:8080/api/usuarios/registrar \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Alex Collector","email":"alex@example.org","senha":"example-password"}'

curl -i http://localhost:8080/api/usuarios/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alex@example.org","senha":"example-password"}'
```

Copy the returned token, then set:

```bash
export TOKEN='paste-the-returned-token'
export USER_ID='1'
```

### Collect a sticker, add a duplicate, and check progress

```bash
curl -i -X POST "http://localhost:8080/api/usuarios/$USER_ID/colar/10" \
  -H "Authorization: Bearer $TOKEN"

curl -i -X POST "http://localhost:8080/api/usuarios/$USER_ID/repetidas/adicionar/10" \
  -H "Authorization: Bearer $TOKEN"

curl -i "http://localhost:8080/api/usuarios/$USER_ID/progresso" \
  -H "Authorization: Bearer $TOKEN"
```

### Record a purchase

```bash
curl -i "http://localhost:8080/api/usuarios/$USER_ID/transacoes" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"label":"Sticker pack","valor":7.5,"tipo":"buy"}'
```

### Update the profile

```bash
curl -i -X PUT "http://localhost:8080/api/usuarios/$USER_ID/perfil" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Alex","senhaAntiga":"example-password","novaSenha":"new-example-password"}'
```

### Recover a password

```bash
curl -i http://localhost:8080/api/usuarios/esqueci-senha \
  -H 'Content-Type: application/json' \
  -d '{"email":"alex@example.org"}'

# Replace 123456 with the code received by email.
curl -i http://localhost:8080/api/usuarios/redefinir-senha \
  -H 'Content-Type: application/json' \
  -d '{"email":"alex@example.org","codigo":"123456","novaSenha":"new-example-password"}'
```

### Publish and read announcements

Run publishing from a trusted environment where `ADMIN_KEY` is configured:

```bash
curl -i http://localhost:8080/api/avisos \
  -H "X-Admin-Key: $ADMIN_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Stock update","mensagem":"New packs available.","tipo":"ESTOQUE","cidade":"São Paulo","loja":"Example Store"}'

curl -i http://localhost:8080/api/avisos \
  -H "Authorization: Bearer $TOKEN"
```

The admin key is a server-side shared secret and should not be embedded in a public frontend.

### Delete an account

This removes the account, its collection links, duplicate inventory, and associated transactions:

```bash
curl -i -X DELETE "http://localhost:8080/api/usuarios/$USER_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"senha":"new-example-password"}'
```

## Email delivery

`ApiApplication` enables asynchronous execution, and both email methods use `@Async`:

| Event | Sender | Subject |
| --- | --- | --- |
| Registration | `CopaNexus <boasvindas@copanexus.com.br>` | `Bem-vindo ao CopaNexus! ⚽` |
| Password recovery | `CopaNexus <suporte@copanexus.com.br>` | `Recuperação de Senha - CopaNexus 🔐` |

Both use `copanexusofc@gmail.com` as reply-to. Templates are hardcoded Portuguese HTML in `EmailService`; the welcome email links to `https://copanexus.com.br`.

Registration saves the account before requesting email delivery. Recovery saves the code before requesting delivery. The email service catches and logs `ResendException`, and asynchronous worker failures do not propagate to the controller's `try/catch`. Consequently, a successful HTTP response does **not** confirm that Resend accepted or delivered the email. There is no retry queue or delivery-status endpoint.

## Errors and validation

Responses do not share a uniform error envelope. Controller branches return Portuguese text, empty bodies, DTOs, maps, lists, or entities. Examples of text responses include:

```text
E-mail ou senha inválidos.
Senha atual necessária para alterar dados sensíveis.
Figurinha não encontrada nas repetidas.
Chave inválida!
```

`GlobalExceptionHandler` maps `IllegalArgumentException` to `400` and other `RuntimeException` instances to `500`. Its intended `ErroResposta` fields are `mensagem`, `status`, and an epoch-millisecond `timestamp`. However, that class has private fields with no getters or serialization annotations, so the intended JSON envelope is not reliably serializable as written. Do not depend on it as a stable error contract.

Bean validation is explicitly enabled on the registration request only. Missing headers, malformed JSON, enum parsing, security failures, and database constraints may be handled outside explicit controller branches. The source does not configure a custom JSON authentication/authorization error response, so clients should not assume all missing-token failures return `401` or the same body format.

Account deletion catches exceptions and includes the exception message in its `500` text response. Registration checks duplicate email before saving, but the database uniqueness constraint remains the final safeguard against concurrent inserts.

## Build and testing

Run commands from `api/` with the environment configured:

```bash
# Compile production source without starting the application.
sh ./mvnw compile

# Run the existing test suite.
sh ./mvnw test

# Package an executable JAR, including tests.
sh ./mvnw clean package

# Package without running tests.
sh ./mvnw clean package -DskipTests

# Launch the packaged application.
java -jar target/api-0.0.1-SNAPSHOT.jar
```

The only test is `ApiApplicationTests.contextLoads`, annotated with `@SpringBootTest`. There are no endpoint, authorization, repository isolation, or business-rule tests. The context test uses the normal application configuration, so it needs the required variables and an accessible PostgreSQL database. It may update the schema and seed an empty catalog; use a dedicated test database. No in-memory test database or Testcontainers setup is supplied.

## Deployment and operational notes

- Set the build/project directory to `api/`, or otherwise invoke Maven against `api/pom.xml`.
- Supply all required variables and set `PORT` when the host assigns a port.
- `api/system.properties` declares `java.runtime.version=17` for hosting platforms that recognize that file.
- Persistent application state lives in PostgreSQL; there is no file-upload or local-storage subsystem.
- Hibernate manages schema updates directly. No Flyway/Liquibase migration history or rollback scripts are included.
- Initial startup needs database connectivity and can insert 980 catalog rows.
- Most framework logs are suppressed by the configured `ERROR` levels. Explicit stdout/stderr messages still exist for seeding and email operations.
- The source includes no Dockerfile, Compose configuration, CI workflow, dedicated health check, API version prefix, or OpenAPI specification.

## Current implementation limitations

These are observable properties of the present code and should be considered when integrating or extending it:

1. **Authorization:** authenticated routes do not enforce account ownership; transaction deletion ignores the URL's user association. There is no separate administrator role.
2. **User data exposure:** listing users serializes entities with password-related getters instead of safe response DTOs.
3. **Catalog consistency:** seeding creates 980 stickers, while progress assumes 994; duplicate numbers are not checked against the catalog.
4. **Token lifecycle:** tokens have a long lifetime and are not revoked by password changes/resets. Their subject is a mutable email address.
5. **Input validation:** profile, reset, transaction, and announcement requests lack the registration request's validation coverage. Entity objects are accepted directly for several creation endpoints rather than dedicated input DTOs.
6. **Recovery controls:** codes use `java.util.Random`; there is no implemented attempt limit, rate limiter, or lockout, and recovery responses distinguish existing accounts.
7. **Error contract:** errors mix plain text and framework responses, and the custom error DTO lacks accessors.
8. **Delivery reliability:** asynchronous email failures are logged without a delivery guarantee, retry mechanism, or client-visible status.
9. **Financial representation:** transaction values use floating-point numbers and have no enforced currency, sign, or transaction-type rules.
10. **Coverage and operations:** only a context-loading test exists; no migration tooling, health endpoint, or deployment automation is included.

## License

This project is licensed under the [MIT License](LICENSE).

Copyright (c) 2026 João Henrique.
