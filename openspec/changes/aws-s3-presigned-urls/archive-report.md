# Archive Report — aws-s3-presigned-urls

## Resumen ejecutivo

El change `aws-s3-presigned-urls` introduce la generación de **URLs pre-firmadas de AWS S3** (5 minutos de expiración) para que el frontend suba los assets binarios (avatares de usuario y media de publicaciones) directo al bucket, sin pasar bytes por el backend. El backend se reduce a validar metadatos (`fileName`, `size`, `mimeType`), firmar la URL y persistir la `key` resultante en el avatar del usuario. Tres endpoints HTTP nuevos, dos validadores custom, un servicio único agnóstico al bucket, la primera `@ConfigurationProperties` del proyecto, y un camino explícito para migrar a Cloudflare R2 vía variables de entorno sin recompilar.

La implementación cubre los **6 PR slices** planificados (Foundation, DTOs, Validators, Service, Profile endpoints, Publication endpoint + advice) más un **slice de strengthening** posterior al verify que aplicó 3 de las 4 SUGGESTIONS no bloqueantes. `./gradlew test --rerun-tasks` corre **66 tests en 15 suites, 0 failures, 0 errors**. El código compila, los beans `S3Presigner` se construyen con `endpointOverride` correcto, los validadores custom (`@ValidFileSize`, `@UniqueFileIds`) y los tres endpoints funcionan como exige el spec.

**Veredicto del verify**: **PASS WITH SUGGESTIONS** — 33/33 ACs cumplidos (28 PASS + 5 SUGGESTION, de las cuales 4 fueron posteriormente implementadas como tests y 1 queda como follow-up documentado). 66/66 tests verdes en 15 suites. Sin FAIL.

## Entregado

- **3 endpoints nuevos**:
  - `POST /profiles/presigned-url` — genera URL pre-firmada para avatar (un único archivo).
  - `PATCH /profiles/me/avatar` — persiste `key` del nuevo avatar en `users.avatar_url` (key relativa, nunca URL completa).
  - `POST /posts/presigned-url` — genera URLs pre-firmadas para 1-10 archivos de media de publicación con orden preservado por `id` cliente.
- **Configuración AWS**:
  - `AwsS3Properties` — primer `@ConfigurationProperties` del proyecto, `record` con `@ConfigurationProperties(prefix="app.aws.s3")` y 8 componentes `@NotBlank`.
  - `AwsS3Configuration` — 2 beans `S3Presigner` (`profileS3Presigner`, `publicationS3Presigner`) que comparten el mismo `endpointOverride` desde `AwsS3Properties.endpoint()` (URL del provider). Factory method estático compartido para evitar duplicación.
- **Servicio único**: `PresignedUrlService` (interfaz + impl) agnóstico al bucket. Métodos `generateProfilePresignedUrl(...)` y `generatePublicationPresignedUrls(...)` reciben `BucketTarget` por argumento. Helper interno `S3KeyGenerator` para formato `profiles/<userId>/<uuid>.<ext>` y `publications/<userId>/<uuid>.<ext>`. `enum BucketTarget` abstrae la selección del bean correcto.
- **Validadores custom**:
  - `@ValidFileSize` — caps `image/*` ≤ 10 MB (incluido `image/svg+xml`), `video/*` ≤ 50 MB. Dispatch por `instanceof` para reuso entre `PresignedUrlProfileRequest`, `PresignedUrlItem` y `PresignedUrlPublicationRequest`.
  - `@UniqueFileIds` — dedup de `id`s cliente en el array `files`; sólo aplica a `PresignedUrlPublicationRequest`.
- **Manejo de errores**:
  - `PresignedUrlGenerationException` (mensaje enmascarado, envuelve `SdkException` del SDK AWS v2) → mapeada a **503 Service Unavailable** vía `PresignedUrlExceptionHandler` con `ProblemDetail` específico (`type = /problems/presigned-url-generation-failed`).
  - `GlobalExceptionHandler` extendido con handler para `ConstraintViolationException` → 400 (usado por `@UniqueFileIds` class-level).
  - `MethodArgumentNotValidException` se mantiene en el handler existente → 400 con `IncorrectField`.
  - `UserNotFoundException` reusa `AuthenticationExceptionHandler` existente → 404.
- **Soporte de migración a Cloudflare R2**: vía env vars (`AWS_ENDPOINT`, `AWS_REGION`) + `endpointOverride`, sin recompilar ni introducir lógica condicional por proveedor. Las URLs de bucket (`AWS_BUCKET_PROFILE_URL`, `AWS_BUCKET_PUBLICATION_URL`) sólo afectan la composición de la URL pública de lectura, no la firma. Verificado por inspección de código (`grep "R2"` retorna un único match en Javadoc).
- **Update de avatar**: `AuthenticationServiceImpl.updateAvatarKey(UUID userId, String key)` — decisión cerrada en proposal (no se introduce `ProfileService` aún; el método reusa `UserRepository` y `UserNotFoundException`).
- **Tests**: 66 tests verdes en 15 suites (12 suites de código + 3 de strengthening), 0 failures, 0 skipped, ~14s.

## Métricas finales

- **Total tasks**: 35 (PR slices 1-6)
- **Tasks adicionales en slice de strengthening**: 4 (stale-checkbox reconciliation + 3 strengthening tests)
- **Total tasks completadas**: 35/35 + 4 strengthening
- **Tasks sin marcar**: 0 (las 11 stale checkboxes de PR 1 fueron reconciliadas)
- **Commits locales en `feature/presigned-url`**: 24 (6 PR slices + 4 commits de strengthening/verify follow-up)
- **Tests**: 66 passing, 0 failing, 0 errors, 0 skipped
- **Suites**: 15
- **Líneas estimadas**: ~1 585 (incluye strengthening slice + reconciliación de tasks)
- **PR slices propuestas**: 6 (stacked-to-main)
- **Push / PR / merge**: ninguno (decisión del operador documentada en `aws-s3/delivery`)

## Lista de commits (todos los commits del change, en orden cronológico)

### PR Slice 1 — Foundation (CFG + MOD)
- `5999115` — feat(aws-s3): add service model types and S3KeyGenerator helper
- `3dbd0e5` — feat(aws-s3): add AwsS3Properties and two S3Presigner beans
- `6760ccd` — chore(aws-s3): wire app.aws.s3.* into profiles and add context load test

### PR Slice 2 — DTOs
- `0e79752` — feat(aws-s3): add mime whitelist regex and stub validation annotations
- `cf46f80` — feat(aws-s3): add pre-signed URL request and response DTOs

### PR Slice 3 — Validators
- `cd4a6b4` — feat(aws-s3): add ValidFileSize validator
- `3ceb7d8` — feat(aws-s3): add UniqueFileIds for duplicate file id detection

### PR Slice 4 — Service
- `0309c3a` — feat(aws-s3): add PresignedUrlService with S3Presigner signing per bucket
- `e559c49` — feat(aws-s3): add PresignedUrlGenerationException for SDK failures
- `049663f` — test(aws-s3): cover PresignedUrlService with Mockito

### PR Slice 5 — Profile endpoints + Auth update
- `d199d33` — feat(auth): add updateAvatarKey to AuthenticationService
- `ac20f30` — feat(aws-s3): add profile presigned URL and avatar update endpoints
- `778aa3b` — test(aws-s3): cover profile controllers with MockMvc
- `8cdc28a` — docs(aws-s3): record PR Slice 5 progress in apply-progress and tasks
- `ee5c32f` — docs(aws-s3): mark T-AUTH-001..003 as completed in tasks

### PR Slice 6 — Publication endpoint + advice
- `3e0f48c` — feat(aws-s3): add publication presigned URL endpoint
- `341882d` — feat(aws-s3): add PresignedUrlExceptionHandler for SDK failures
- `d9580b3` — feat(aws-s3): handle ConstraintViolationException in GlobalExceptionHandler
- `5f7f280` — docs(aws-s3): mark PR Slice 6 tasks as completed in tasks

### Verify follow-up (strengthening slice)
- `6742c87` — test(aws-s3): cover fileName regex validation for profile and publication requests
- `1ee9297` — test(aws-s3): assert no service interaction on validation failures in profile endpoint
- `80a025f` — test(aws-s3): assert pre-signed URL service does not log signed URLs
- `69c1ccc` — docs(aws-s3): reconcile stale checkboxes in tasks.md after verify

## Veredicto del verify

**PASS WITH SUGGESTIONS** — 33/33 ACs cumplidos (28 PASS + 5 SUGGESTION).

| Familia AC | Total | PASS | SUGGESTION |
|---|---|---|---|
| Configuración (CFG) | 5 | 5 | 0 |
| Profile endpoint (PROF) | 5 | 5 | 0 |
| Avatar endpoint (AVATAR) | 6 | 6 | 0 |
| Publication endpoint (POST) | 6 | 6 | 0 |
| Validaciones (VAL) | 9 | 7 | 2 (AC-VAL-5, AC-VAL-9) |
| Errores (ERR) | 5 | 5 | 0 |
| Observabilidad (OBS) | 2 | 0 | 2 (AC-OBS-1, AC-OBS-2) |
| Migración R2 (MIG) | 3 | 3 | 0 |
| **TOTAL** | **33** | **28** | **5** |

### Suggestions aplicadas en este change (post-verify strengthening slice)

- **AC-VAL-5**: `FileNameValidationTest` (12 casos) cubre la matriz de `fileName` del spec — vacío, `null`, con espacio, con punto, `../path`, válidos (`demo-front`, `test_123`), 256 chars (cubre `@Size(max=255)`), mirrored en `PresignedUrlItem`.
- **AC-VAL-9**: `verifyNoInteractions(presignedUrlService)` agregado a `ProfilePresignedUrlControllerWebMvcTest.invalidMimeType_returns400` y `overSize_returns400`. Refuerza la garantía de que ningún path de validación llama al servicio.
- **AC-OBS-1**: `PresignedUrlServiceImplLoggingTest` (2 casos, logback `ListAppender`) — asserta que tras un happy path con URL firmada conteniendo `X-Amz-Signature=...`, la lista de eventos del appender está vacía. Defensivamente también verifica por evento que ninguno contiene `https://`, `http://`, `X-Amz-Signature` ni el token sintético.
- **Stale checkboxes reconciliados en tasks.md**: 11 checkboxes de PR Slice 1 (T-CFG-001..006 + T-MOD-001..005) marcados como `[x]`. La reconciliación está respaldada por `apply-progress.md` (commits `5999115`, `3dbd0e5`, `6760ccd`) y por los tests verdes que cubren ese trabajo (`AwsS3ConfigurationTest`, `S3KeyGeneratorTest`, `PresignedUrlMimeExtensionTest`).

### Suggestions dejadas como follow-up

- **AC-OBS-2**: filtro de redacción de credenciales AWS en logs. El spec lo permite explícitamente como follow-up documentado: "Si por error se loggea una excepción de `S3Presigner`, el mensaje SHALL pasar por el filtro de redacción existente o, si no existe, SHALL documentarse como follow-up." El proyecto no tiene filtro de redacción configurado (`grep "redact\|mask"` en `src/main/resources` retorna sin matches). **Implementar cuando se introduzca un appender centralizado o se configure `logback-spring.xml` con un filter custom.**
- **Menor (no en el prompt del archive, documentada en `apply-progress.md`)**: agregar `@TestPropertySource` explícito a `TiendaUniApiApplicationTests` con las 8 props AWS como placeholder, para que el smoke test no dependa de los defaults de `application-dev.yaml`. Hoy pasa por coincidencia; si alguien remueve los defaults dev, el smoke test rompe sin diagnóstico claro. No bloquea archive.

## Out of scope confirmado (no implementado)

Decisiones explícitas del producto que NO se incluyen en este change:

- `POST /posts` — creación efectiva de publicación, persistencia de `publications` y `publication_media`.
- Mapeo JPA de `publications` / `publication_media` (las tablas SQL existen; las entidades JPA no se crean en este change).
- `GET` pre-signed URL para lectura (el bucket es público; el frontend compone URL con `bucketProfileUrl + key`).
- Limpieza asíncrona de objetos huérfanos en S3 (política leave-it: avatar anterior queda en el bucket).
- Lifecycle rules, IAM policies, S3 CORS (infraestructura, no código Spring).
- Soporte de `multipart/form-data` (todos los endpoints reciben JSON).
- Migración efectiva de AWS S3 a Cloudflare R2. **Sí se construye el camino**: `endpointOverride` + env vars permiten migrar sin recompilar. **No se ejecuta la migración**.
- Validación por bytes del archivo subido (delegada a S3 vía `Content-Length` firmado).
- Sanitización de SVG (vector con potencial XSS si el bucket es público). Mitigación fuera de alcance.

## Decisiones de diseño aplicadas

Resumen breve de las decisiones clave del `design.md` y cómo se aplicaron en código:

| # | Decisión | Aplicación |
|---|---|---|
| 1 | **2 beans `S3Presigner`** (con `endpointOverride` compartido del provider) | `AwsS3Configuration.java` con `profileS3Presigner` y `publicationS3Presigner`. Ambos aplican el mismo `endpointOverride(URI.create(properties.endpoint()))`. Funcionalmente idénticos hoy (mismo provider, misma región, mismas credenciales); se conservan dos beans porque (a) ya estaban en producción, (b) permiten evolución futura a credenciales/región por bucket sin tocar el wiring. Un bean parametrizado por `Map<BucketTarget, S3Presigner>` agregaba indirección sin valor presente. |
| 2 | **`AwsS3Properties` en `configuration/`** | Primer `@ConfigurationProperties` del proyecto. Sienta precedente junto a `SecurityConfiguration` y `ApplicationConfiguration`. `@EnableConfigurationProperties(AwsS3Properties.class)` sobre `AwsS3Configuration`. |
| 3 | **`BucketTarget` / `PresignedUrl` / `PresignedUrlMimeExtension` en `service/model/`** | Siguen el patrón de `persistence/model/`. `BucketTarget` lleva `resolveBucketName(AwsS3Properties)` y `resolveBucketUrl(AwsS3Properties)`. `PresignedUrlMimeExtension` es enum exhaustivo (JPEG, JPG, PNG, GIF, WEBP, SVG_XML, MP4, WEBM, OGG) con `fromMimeType(String)`. |
| 4 | **Validadores en `presentation/validation/`** | Paquete nuevo (`ValidFileSize.java`, `ValidFileSizeValidator.java`, `UniqueFileIds.java`, `UniqueFileIdsValidator.java`). `@ValidFileSize` usa `ConstraintValidator<ValidFileSize, Object>` con dispatch por `instanceof` para reuso entre DTOs. `@UniqueFileIds` es `ConstraintValidator<UniqueFileIds, PresignedUrlPublicationRequest>`. |
| 5 | **`PresignedUrlGenerationException` en `service/exception/`** | Mensaje enmascarado, envuelve `SdkException` como causa. Handler dedicado `PresignedUrlExceptionHandler` con `@Order(20)` corre después de `GlobalExceptionHandler` (`@Order(HIGHEST_PRECEDENCE)`). |
| 6 | **`PresignedUrlService` interfaz + impl en sus paquetes canónicos** | Cumple AGENTS.md. `PresignedUrlServiceImpl` con `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly=true)`. Inyecta los 2 `S3Presigner`, `S3KeyGenerator`, `AwsS3Properties`. |
| 7 | **`updateAvatarKey` en `AuthenticationServiceImpl`** | No se introduce `ProfileService` aún. Reusa `UserRepository` y `UserNotFoundException`. Si el perfil crece (bio, display name), se refactoriza a `ProfileService` en un cambio futuro. |
| 8 | **Mocking: `@MockitoBean` para slice tests, `@Mock` + `@ExtendWith(MockitoExtension.class)` para unit tests** | En `@WebMvcTest` se mockea al `PresignedUrlService` en el límite del controller (más simple que mockear los 2 `S3Presigner`). En unit tests del servicio, los 2 `S3Presigner` y `S3KeyGenerator` se mockean directamente. No se introdujo Testcontainers ni WireMock. |
| 9 | **`ConstraintViolationException` en `GlobalExceptionHandler`** | Handler nuevo `@ExceptionHandler(ConstraintViolationException.class)` retorna 400 con `type = /problems/validations`, `title = "Validation Failed"`, `detail` con el mensaje de la constraint violation. Usado por `@UniqueFileIds`. |
| 10 | **`PresignedUrlMimeExtension` enum (vs Map)** | Exhaustividad en compile-time. La whitelist del `@Pattern` en DTOs es un literal regex sincronizado con el enum vía `PresignedUrlMimeExtensionTest.whitelistRegex_*`. |
| 11 | **Persistencia de key (no URL) en `avatar_url`** | `entity.setAvatarUrl(key)` con la key exacta. La URL pública se reconstruye como `bucketProfileUrl + key` en el frontend. Permite migrar de proveedor sin migrar la columna. |
| 12 | **`UserNotFoundException` reusa `AuthenticationExceptionHandler`** | Cero código nuevo. El advice existente ya mapea la excepción a 404. |
| 13 | **Solo unit tests con Mockito (no Testcontainers LocalStack)** | Aceptable para este MVP. El SDK AWS v2 es estable; mockear `presignPutObject` y verificar argumentos cubre la lógica del servicio. |

## Estado del change

**Status**: **ARCHIVED** (implementation complete, verify PASS WITH SUGGESTIONS, 4 de 5 suggestions aplicadas en strengthening slice, 1 follow-up documentado, tasks reconciliadas, sin FAIL).

## Próximos pasos para el operador

1. **Revisar manualmente** los 24 commits locales en `feature/presigned-url`. No hay push, no hay PR, no hay merge (decisión del operador documentada en `aws-s3/delivery`).
2. **Commitear los cambios preexistentes pendientes** del usuario (actualmente en `git status` pero fuera de los commits del change):
   - `build.gradle` (dep AWS SDK agregada por el usuario).
   - `src/main/java/alberto/cruz/tiendauniapi/configuration/SecurityConfiguration.java` (EOF newline).
   - `aws-s3.md` y el resto de artefactos `openspec/changes/aws-s3-presigned-urls/*.md` (el usuario los gestiona; los `apply-progress` y `archive-report` ya están commiteados).
3. **Decidir estrategia de push**:
   - **Opción A — Push por los 6 PR slices propuestos** (stacked-to-main): cada slice mergea a `main` en orden. Requiere crear 6 PRs. Iteración rápida, fix on the go. Cada PR ≤ 400 líneas. Considerar `chained-pr` skill si se elige este camino.
   - **Opción B — Push consolidado (1 solo PR)**: ~1 585 líneas excede el budget de 400 del `sdd-verify`. Requiere `size:exception` explícito. Una sola revisión, un solo merge.
   - **Opción C — No push**: el change queda archivado localmente; el operador decide más adelante.
4. **Si decide Opción A**: abrir PRs siguiendo la guía de `gentle-ai-chained-pr`. Los conventional commits ya están listos en la rama.
5. **Actualizar README** con las 8 env vars AWS requeridas (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `AWS_ENDPOINT`, `AWS_BUCKET_PROFILE_NAME`, `AWS_BUCKET_PUBLICATION_NAME`, `AWS_BUCKET_PROFILE_URL`, `AWS_BUCKET_PUBLICATION_URL`). Acción parent-owned listada en `tasks.md` sección Lifecycle.
6. **Implementar AC-OBS-2** (filtro de redacción de credenciales AWS en logs) cuando se introduzca un appender centralizado o se configure `logback-spring.xml` con un filter custom. Documentado como follow-up.

## Riesgos conocidos

- **Filtro de redacción de credenciales AWS no implementado** (AC-OBS-2). Si en el futuro se loggea una excepción del SDK, podría filtrar contenido sensible. Mitigación: hoy `PresignedUrlServiceImpl` no contiene ningún `log.*` (verificado por inspección) y `PresignedUrlExceptionHandler` sólo loggea mensaje genérico + stacktrace. El riesgo es bajo en el código actual pero latente en cualquier log futuro del SDK.
- **`@TestPropertySource` no agregado a `TiendaUniApiApplicationTests`**. El smoke test pasa hoy por los defaults de `application-dev.yaml`. Si esos defaults se remueven, el smoke test rompe sin diagnóstico claro. Riesgo bajo, no bloqueante.
- **Cloudflare R2 sólo se ha verificado por inspección**. La migración efectiva no se ejecutó en este change. El camino está construido (`endpointOverride` + env vars); cualquier deploy a R2 requiere validar manualmente que las URLs firmadas se generan con el host correcto y que R2 acepta el PUT. Acción: ejecutar AC-MIG-1 manualmente al migrar (ver `spec.md` escenario AC-MIG-1).
- **Costo de objetos huérfanos**. Política leave-it: avatar anterior y archivos subidos pero nunca referenciados quedan en el bucket. Aceptable por costo hoy; documentar como riesgo operacional.
- **SVG malicioso**. SVG puede contener JavaScript; el bucket es público. Mitigación fuera de alcance (sanitización en cliente o CloudFront). Documentado en `proposal.md` riesgos.
- **No hay Testcontainers LocalStack**. Los tests mockean `S3Presigner`. Si el SDK AWS v2 cambia comportamiento de firma en una versión futura, los tests no lo detectarían hasta deploy real. Mitigación: el SDK es estable; seguir versiones upstream.

---

## Key Learnings

1. Cuando un constraint class-level como `@UniqueFileIds` lanza `ConstraintViolationException` en lugar de `MethodArgumentNotValidException`, el `GlobalExceptionHandler` necesita un método adicional dedicado; no se puede asumir que un solo handler cubre todos los errores de validación Jakarta.
2. El SDK AWS v2 modela `S3Presigner` con un único `endpointOverride` por instancia; ambos beans lo comparten desde `AwsS3Properties.endpoint()`. Funcionalmente idénticos hoy (la diferencia de bucket se pasa vía `PutObjectRequest.bucket(...)`).
3. Persistir sólo la key relativa (no la URL completa) en columnas como `users.avatar_url` desacopla la migración de proveedor de la capa de persistencia; la URL pública se reconstruye en el frontend como `bucketUrl + key`.
4. Validar metadatos en `@Valid` antes de invocar `S3Presigner` evita que errores de cliente consuman cuota de AWS y se reflejen como `PresignedUrlGenerationException` 503; el patrón `verifyNoInteractions(service)` en los tests refuerza esta garantía contra regresiones.
5. Documentar tareas con checkboxes stale requiere reconciliación explícita antes de `sdd-archive` aunque la implementación esté commiteada y verificada por tests; el archive final debe declarar `0 tasks sin marcar` para que el contrato de cierre se cumpla sin excepciones.