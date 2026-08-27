# Explore — aws-s3-presigned-urls

## Context

The `aws-s3-presigned-urls` change introduces AWS S3 pre-signed URL generation so the
frontend can upload binary assets (user avatars, publication media) directly to S3 instead
of streaming them through the backend. The backend's role shrinks to:

1. Receive metadata about the file(s) (name, size, mimeType).
2. Validate the metadata against a whitelist + size limits.
3. Mint a 5-minute pre-signed PUT URL keyed off the target bucket.
4. Optionally accept the resulting object key back to persist on the owning entity.

Two consumers are in scope today:

- **Profile avatar** — single file, tied to the authenticated user, mapped to `users.avatar_url`.
- **Publication media** — one or more files (images/videos) tied to a future publication entity.

A future `/posts` create endpoint that consumes the resulting keys is explicitly **out of
scope** of this change (per the requirements doc).

Pre-signed URLs depend on the AWS SDK v2 `S3Presigner`, already declared in
`build.gradle` (`software.amazon.awssdk:s3:2.54.3`). No additional runtime dependency is
required. Spring Boot is `4.1.1`, Java toolchain `21`.

## Current state of relevant subsystems

### Auth flow

- **Controller** `presentation/controller/AuthenticationController.java` — only auth
  endpoints (`/auth/signup`, `/auth/login`, `/auth/refresh`, `/auth/logout`). Sets JWT
  cookies (`access-token`, `refresh-token`). No profile/post controllers exist yet.
- **Service interface** `service/interfaces/AuthenticationService.java` exposes
  `register`, `authenticate`, `refreshTokenAndGenerateAccessToken`, `logout`.
- **Implementation** `service/implementation/AuthenticationServiceImpl.java` uses
  `@RequiredArgsConstructor` constructor injection. `@Transactional` on all public methods.
  Uses `UserMapper.toAuthenticatedUser` to build the security principal.
- **Principal model** `persistence/model/AuthenticatedUser.java` — final class implementing
  `UserDetails` with `username` (email), `password`, `userId` (UUID), `universityId` (UUID).
  This is what `@AuthenticationPrincipal` will yield on the new endpoints.
- **Request/Response DTOs** (records): `RegisterRequest`, `RegisterResponse`,
  `AuthenticationRequest`, `AuthenticationResponse`, `RefreshTokenRequest`, `TokenBundle`.
  `TokenBundle` is `@JsonIgnore`d in responses because tokens are returned via cookies, not
  body. `RegisterResponse`/`AuthenticationResponse` both carry an `avatarUrl` (currently
  always `null` — the presigned-URL flow will populate it).
- **Security** `configuration/SecurityConfiguration.java` — stateless, JWT-cookie driven.
  `permitAll` only on `POST /auth/login, /auth/signup, /auth/refresh`; `anyRequest().authenticated()`.
  The new `/profiles` and `/posts` controllers will land under the authenticated default.

### DTOs (`presentation/dto`)

Inventory of all records:

| File | Type | Notes |
|------|------|-------|
| `RegisterRequest` | request | Signup payload (email, password, names). |
| `RegisterResponse` | response | Carries `avatarUrl` (currently `null`). |
| `AuthenticationRequest` | request | Login payload. |
| `AuthenticationResponse` | response | Carries `avatarUrl`, `isVerified`, `id`. |
| `RefreshTokenRequest` | request | `userId` only. |
| `TokenBundle` | internal | `@JsonIgnore` — access + refresh tokens. |
| `IncorrectField` | internal | Used by `GlobalExceptionHandler` for validation errors. |

All are records, all use Jakarta Validation where applicable, all response types annotate
`@JsonInclude(NON_NULL)` (TokenBundle is `@JsonIgnore`d on responses). There is **no DTO**
yet for presigned URL requests, presigned URL responses, or profile update payloads.

### Services (`service/`)

- **Interfaces**: `AuthenticationService`, `RefreshTokenService` only.
- **Implementations**: `AuthenticationServiceImpl`, `RefreshTokenServiceImpl`,
  `UserDetailsServiceImpl` (implements Spring's `UserDetailsService`).
- **Mappers** live under `utils/mapper/`. Only `UserMapper` exists, with `static` methods:
  `toAuthenticatedUser(UserEntity)`, `toAuthenticatedUser(UserProjection)`,
  `toAuthentication(AuthenticatedUser)`.
- **Exceptions** under `service/exception/`:
  - `EmailAddressAlreadyRegisteredException` (extends RuntimeException)
  - `EmailDomainNotAllowedException`
  - `EmailAddressNotFound` (extends `common.ResourceNotFoundException`)
  - `UserNotFoundException`
  - `InvalidRefreshTokenException`, `InvalidTokenException`
  - `ExpiredRefreshTokenException`, `RefreshTokenNotFoundException`, `RevokedRefreshTokenException`
- **Common exceptions**: `common.UnknownException` (fallback per AGENTS.md),
  `common.ResourceNotFoundException` (base for "not found" semantics).

There is **no exception** today for AWS S3 failures (presigning, configuration, network).
The proposal must decide whether to introduce a custom
`PresignedUrlGenerationException` or rely on `UnknownException` as the AGENTS.md fallback
recommends.

### Mappers (`utils/mapper/`)

Single class `UserMapper` with `static` methods per AGENTS.md. Naming convention
`toXxx` / `toXxxList`. Mappers are stateless utilities — no Spring bean wiring.

### Entities (`persistence/entity`)

Three entities, all extending `common.AuditableEntity`:

- **`UserEntity`** (`users`) — UUID PK, `email VARCHAR(150) UNIQUE`, `password`, `firstName`,
  `lastName`, **`avatarUrl VARCHAR(300) NOT NULL`** (mandatory at SQL layer — see
  `src/main/resources/sql/02_users.sql`), `verified BOOLEAN`, `@ManyToOne UniversityEntity`.
- **`UniversityEntity`** (`universities`) — UUID PK, with `@ElementCollection emailDomains`.
- **`RefreshTokenEntity`** (`refresh_tokens`) — UUID PK + `token UUID`, `expiredAt`,
  `revoked`, `userId`.

Notable: **`avatar_url` is `NOT NULL` in SQL and in `UserEntity`** (length 300). Any
profile update endpoint must always set a non-null value, never blank it out.

There is **no entity yet** for publication media — the future `publication_media` table is
defined in `03_publications.sql` (`id`, `media_type`, `media_url`, `display_order`,
`publication_id`) but not mapped to a JPA entity in code. This change does not need to
create the entity, but the proposal should acknowledge the future mapping it depends on.

### Repositories (`persistence/repository`)

- `UserRepository extends JpaRepository<UserEntity, UUID>` — exposes `findByEmail`,
  `existsByEmail`, plus two `UserProjection` queries (`findUserEntitiesByEmail/Id`).
- `UniversityRepository extends JpaRepository<UniversityEntity, UUID>` — finds by acronym or
  email domain.
- `RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID>`.

Per AGENTS.md: no `@Repository` annotation, derived lookups return `Optional<...>`,
projections preferred for non-full-entity reads. No repository for publication media yet.

### Configuration (`configuration/`)

- **`SecurityConfiguration.java`** — stateless JWT filter chain, CORS configured via
  `app.cors.allowed-origins`, `BCryptPasswordEncoder`, `DaoAuthenticationProvider`.
  `permitAll` is explicit for `/auth/login, /auth/signup, /auth/refresh` only.
- **`ApplicationConfiguration.java`** — `@EnableJpaAuditing` only.

There is **no** `@ConfigurationProperties` record in the codebase yet. The proposal will
introduce the first one (`AwsS3Properties`) — it must follow the AGENTS.md mandate that the
service layer may use `@Value` for non-bean values, but bulk mapping belongs on a record.

### Common (`common/`)

- `AuditableEntity` (`@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`)
  with `createdAt` (`@CreatedDate`) and `updatedAt` (`@LastModifiedDate`) as `Instant`.
  New entities that need auditing should extend it.
- `UnknownException` — fallback per AGENTS.md when a specific exception is missing.
- `ResourceNotFoundException` — base for "not found" semantics.

### Application advice (`presentation/advice`)

Three `@RestControllerAdvice` beans:

- **`GlobalExceptionHandler`** (`@Order(HIGHEST_PRECEDENCE)`) — handles `UnknownException`
  (500), `MethodArgumentNotValidException` (400, populates `IncorrectField` map), and
  `DataIntegrityViolationException` / `DataAccessException` (500). Uses Spring's
  `ProblemDetail` with `type` URIs rooted at
  `https://tiendauniapi.com/problems/{slug}`. Sets `setTitle` + `setType` per error.
- **`AuthenticationExceptionHandler`** — `BadCredentialsException` (401), user-not-found
  (404), `EmailAddressAlreadyRegisteredException` (409), `EmailDomainNotAllowedException`
  (400).
- **`RefreshTokenExceptionHandler`** — `InvalidRefreshTokenException` (400),
  `RefreshTokenNotFoundException` (401), `ExpiredRefreshTokenException` (401),
  `RevokedRefreshTokenException` (401).

Conventions the new advice (or new handlers) must follow: `ProblemDetail.forStatusAndDetail`,
title in English PascalCase, type URI under `GlobalExceptionHandler.DOMAIN_URI`, Spanish
detail message.

### Application resources

- **`application.yaml`** (base) — sets active profile from `${PROFILE:dev}`, enables
  `spring.mvc.problemdetails.enabled: true` (so the new endpoints will benefit from the
  standard 400 response on bean-validation failures), PostgreSQL driver, JPA
  `ddl-auto: validate`.
- **`application-dev.yaml`** — local DB credentials, `app.cors.allowed-origins` for
  localhost:3000 / 5173 / 8080, JWT secret + 15-min expiration, refresh token 7-day
  expiration.
- **`application-prod.yaml`** — every secret from env vars: `DATABASE_USERNAME`,
  `DATABASE_PASSWORD`, `database_url`, `ALLOWED_ORIGINS`, `JWT_SECRET`, `JWT_EXPIRATION`,
  `JWT_ISSUER`, `REFRESH_TOKEN_EXPIRATION`.
- **`sql/`** — `01_university.sql`, `02_users.sql`, `03_publications.sql`,
  `04_comunication.sql`. The `users.avatar_url` column is `NOT NULL VARCHAR(300)`. No
  schema change is required for this change (no new tables/columns).

AWS properties (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`,
`AWS_BUCKET_PROFILE_NAME`, `AWS_BUCKET_PUBLICATION_NAME`, `AWS_BUCKET_PROFILE_URL`,
`AWS_BUCKET_PUBLICATION_URL`) must be added under `app.aws.s3.*` in `application.yaml`,
`application-dev.yaml`, `application-prod.yaml`. Per AGENTS.md, mapping goes through an
`@ConfigurationProperties` record — names from the spec map as:

```
app.aws.s3.access-key-id        → AWS_ACCESS_KEY_ID
app.aws.s3.secret-access-key    → AWS_SECRET_ACCESS_KEY
app.aws.s3.region               → AWS_REGION
app.aws.s3.bucket-profile-name  → AWS_BUCKET_PROFILE_NAME
app.aws.s3.bucket-publication-name → AWS_BUCKET_PUBLICATION_NAME
app.aws.s3.bucket-profile-url   → AWS_BUCKET_PROFILE_URL
app.aws.s3.bucket-publication-url → AWS_BUCKET_PUBLICATION_URL
```

### Existing tests

Only `src/test/java/alberto/cruz/tiendauniapi/TiendaUniApiApplicationTests.java` exists —
a single `@SpringBootTest` `contextLoads()` smoke test. No controller, service, or slice
tests are present. The proposal must call out the testing gap and the bare-minimum tests
required for this change (context still loads, presigned URL service generates a URL with
expected query params, validation rejects bad mime/size/name).

### `build.gradle`

Java 21 toolchain, Spring Boot `4.1.1`, Spring dependency-management `1.1.7`. Dependencies
of note:

- `spring-boot-starter-actuator`, `-data-jpa`, `-validation`, `-webmvc`, `-security`.
- `com.auth0:java-jwt:4.6.0`.
- **`software.amazon.awssdk:s3:2.54.3`** — already present.
- `org.postgresql:postgresql`, Lombok (compileOnly + annotationProcessor).

No new dependencies required. The proposal should explicitly confirm this.

## Open questions for the proposal (product/PRD questions only)

1. **Profile presigned-URL payload shape** — the spec shows a request body with a `files`
   array and a response of `{ "url": "..." }` (singular). Should the profile endpoint
   accept **exactly one** element in the array (validated) and return a single URL, or
   accept an array and only return the first? For product simplicity, do we want the
   profile endpoint to mirror the publication endpoint shape so both clients share one
   contract, or do we keep them asymmetric (profile = single file, publication = list)?

2. **File `id` field semantics for the profile endpoint** — for publications, `id` is a
   client-supplied correlation id so the response preserves ordering. On the single-file
   profile endpoint, is `id` required, optional, or ignored? If required, must it be a
   non-empty string? Same regex as `fileName`?

3. **Publication file count cap** — there is no documented maximum number of files per
   publication. Is there a business limit (e.g. max 10 files per request) we should
   enforce, or do we trust the cap implicitly via per-file size limits + bucket cost
   concerns? This affects both validation and S3 key cardinality.

4. **Orphan/cleanup strategy for replaced avatars** — when a user updates their avatar, the
   previously uploaded object stays in the bucket. Is the orphan policy "leave it" (cheapest,
   acceptable bucket cost), "best-effort async delete", or "block until deleted"? Does the
   product team want the previous `avatar_url` returned on the profile endpoint response so a
   background job could clean it up later?

5. **`avatar_url` storage format** — the spec says "guardando solo el recurso o una parte
   de la URL sin el dominio". Concretely, do we store the **S3 object key** (e.g.
   `profiles/{userId}/{uuid}.jpg`) and reconstruct the public URL from
   `bucket-profile-url` + key on read, or do we store a **relative path** that is treated
   as opaque? This decision affects how `bucket-profile-url` is used at read time and
   whether the column can be migrated to a new provider later.

6. **Profile update payload** — the spec marks `/profiles` as `PUT/PATCH` and says
   *"Sin cuerpo"* (no body). Where does the client send the new avatar object key? Options:
   (a) request body that contains the key only (in which case "Sin cuerpo" is wrong in the
   doc), (b) query parameter, (d) path parameter, (e) header. Need product confirmation.

7. **Profile endpoint authorization** — implicit but worth confirming: only the
   authenticated user can update their own avatar. No "admin sets another user's avatar"
   flow? And is `@AuthenticationPrincipal AuthenticatedUser` the only source of the userId,
   or should we also accept a path/body userId and verify it matches the principal?

8. **Key naming scheme** — the spec gives no fixed key format. Should the key be
   `profiles/{userId}/{uuid}.{ext}` for profile and
   `publications/{placeholder-or-userId}/{uuid}.{ext}` for publication, with extension
   inferred from mimeType? Or pure UUIDs without foldering? Foldering helps with S3
   lifecycle rules and bucket-level metrics; product should weigh simplicity vs.
   operational cost.

9. **5-minute expiration behavior** — does the product want a fixed 5 min (as specified),
   or do they want it exposed as a config knob (`app.aws.s3.presigned-url-expiration`) for
   future tuning without code changes? Per AGENTS.md, only the value can be `@Value`-injected
   unless it's an entity/repo/service — but the proposal may want a dedicated property.

10. **MIME list location** — the spec lists allowed mime types in prose
    (`image/jpeg, image/jpg, image/png, image/gif, image/webp, image/svg+xml,
    video/mp4, video/webm, video/ogg`). Do these belong as a hardcoded `Set<String>`
    constant in the validation code, or as a config-driven list
    (`app.aws.s3.allowed-mime-types`) so they can be tuned without a redeploy?

11. **Size cap mapping** — `image/*` ≤ 10MB and `video/*` ≤ 50MB. Should the cap be
    derived from mime-type prefix (image vs video) at validation time, or do we accept
    that the `image/svg+xml` case (a vector format) might fall under the image cap? Is
    `image/svg+xml` treated as an image for size purposes?

12. **S3 region/auth strategy** — the spec shows `AwsBasicCredentials.create(...)` with a
    static provider. In real AWS deployments the SDK default chain (IAM role, env vars,
    etc.) is preferred. Should the implementation support both (use static credentials only
    when explicitly configured, otherwise fall back to the default provider chain), or is
    static credentials the only contract for now?

13. **Public read of uploaded objects** — the front-end uploads via pre-signed URL but
    later needs to *read* the file (e.g. render `<img>` or `<video>`). Does the product
    expect the bucket to allow public read (so `users.avatar_url` becomes a directly
    loadable URL), or will the front-end request a separate GET pre-signed URL per asset?
    This impacts how `avatar_url` is built (public URL vs. relative key) and may add a
    future endpoint.

## Out of scope

- The `/posts` create endpoint that consumes the uploaded media URLs (explicitly deferred
  per the requirements doc — publication endpoint design has not been planned yet).
- Mapping the `publications` and `publication_media` SQL tables to JPA entities (only the
  presigned-URL side of publications is in scope).
- Async cleanup of orphan S3 objects (question 4 — to be decided later).
- A GET pre-signed-URL endpoint for reading assets (question 13 — to be decided later).
- Bucket lifecycle policies, CORS rules on the S3 bucket itself, or IAM policy authoring.
  These are infrastructure concerns, not Spring code.
- Switching to a different storage provider. The `avatar_url` format is designed to keep the
  provider decoupled, but no migration path is planned in this change.

## References

- `aws-s3.md` — full product requirements (root of the change).
- `AGENTS.md` — project rules (DTOs, services, controllers, mapping, exceptions, code style).
- `.agents/skills/java-spring-boot/SKILL.md` — Spring Boot conventions.
- `.agents/skills/spring-data-jpa/SKILL.md` — JPA/repository/auditing conventions.
- `src/main/java/alberto/cruz/tiendauniapi/presentation/controller/AuthenticationController.java`
- `src/main/java/alberto/cruz/tiendauniapi/service/interfaces/AuthenticationService.java`
- `src/main/java/alberto/cruz/tiendauniapi/service/implementation/AuthenticationServiceImpl.java`
- `src/main/java/alberto/cruz/tiendauniapi/persistence/model/AuthenticatedUser.java`
- `src/main/java/alberto/cruz/tiendauniapi/persistence/model/RefreshToken.java`
- `src/main/java/alberto/cruz/tiendauniapi/persistence/entity/UserEntity.java`
- `src/main/java/alberto/cruz/tiendauniapi/persistence/entity/UniversityEntity.java`
- `src/main/java/alberto/cruz/tiendauniapi/persistence/entity/RefreshTokenEntity.java`
- `src/main/java/alberto/cruz/tiendauniapi/persistence/repository/UserRepository.java`
- `src/main/java/alberto/cruz/tiendauniapi/persistence/repository/UniversityRepository.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/`
- `src/main/java/alberto/cruz/tiendauniapi/service/exception/`
- `src/main/java/alberto/cruz/tiendauniapi/configuration/SecurityConfiguration.java`
- `src/main/java/alberto/cruz/tiendauniapi/configuration/ApplicationConfiguration.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/advice/`
- `src/main/java/alberto/cruz/tiendauniapi/common/AuditableEntity.java`
- `src/main/java/alberto/cruz/tiendauniapi/common/UnknownException.java`
- `src/main/java/alberto/cruz/tiendauniapi/common/ResourceNotFoundException.java`
- `src/main/java/alberto/cruz/tiendauniapi/utils/mapper/UserMapper.java`
- `src/main/java/alberto/cruz/tiendauniapi/utils/JwtUtil.java`
- `src/main/java/alberto/cruz/tiendauniapi/TiendaUniApiApplication.java`
- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yaml`
- `src/main/resources/application-prod.yaml`
- `src/main/resources/sql/02_users.sql`
- `src/main/resources/sql/03_publications.sql`
- `src/test/java/alberto/cruz/tiendauniapi/TiendaUniApiApplicationTests.java`
- `build.gradle`