# Tasks — aws-s3-presigned-urls

## Resumen

**Total tasks**: 35
**Total líneas estimadas**: ~1 585 (additions + modifications)
**PR slices**: 6 chained PRs (stacked-to-main)
**Valor entregado**: generación de URLs pre-firmadas S3 para avatar y media de publicación + endpoint de persistencia de avatar + base para migración a Cloudflare R2 sin recompilar.

Cada PR slice compila (`./gradlew compileJava`) y pasa tests (`./gradlew test`) de forma aislada. La aplicación existente sigue arrancando (no se introducen endpoints `multipart/form-data`; sólo JSON + cookies JWT).

## Forecast de carga de revisión (OBLIGATORIO)

**Chained PRs recommended**: Yes — 6 slices
**400-line budget risk**: High (total ≈ 1 585 líneas, ningún agrupamiento lógico cabe en un solo PR ≤ 400)
**Decision needed before apply**: No (la elección de chain strategy y delivery se decide antes de `sdd-apply`; `tasks.md` propone `stacked-to-main` como default alineado con la naturaleza incremental del feature)

```text
Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High
```

> Nota: si el operador prefiere `feature-branch-chain` (tracker branch + hijos) o si la entrega es por release en lugar de iteración diaria, ajustar antes de lanzar `sdd-apply`. El orquestador no re-pregunta en modo automático salvo que `Decision needed` sea `Yes`.

## Convenciones de tasks

- **Prefijos de id**:
  - `T-CFG-NNN` — `configuration/` (AwsS3Properties, AwsS3Configuration, application*.yaml, AwsS3ConfigurationTest).
  - `T-MOD-NNN` — `service/model/` + `service/helper/` (BucketTarget, PresignedUrl, PresignedUrlMimeExtension, S3KeyGenerator + test).
  - `T-VAL-NNN` — `presentation/validation/` (anotaciones + validadores custom + tests).
  - `T-DTO-NNN` — `presentation/dto/` (records Request/Response).
  - `T-SVC-NNN` — `service/interfaces/` + `service/implementation/` (PresignedUrlService + impl + test).
  - `T-CTL-NNN` — `presentation/controller/` + tests web slice.
  - `T-ADV-NNN` — `presentation/advice/` + `service/exception/` + extensión de `GlobalExceptionHandler`.
  - `T-AUTH-NNN` — modificaciones a `AuthenticationService`/`AuthenticationServiceImpl` + tests del método `updateAvatarKey`.
- **Cada task lista**: id, descripción corta, archivos a tocar, líneas estimadas (added/modified), ACs cubiertos (de `spec.md`), dependencias (ids), notas.
- **Ownership**: cada checkbox termina con un marker terminal `<!-- sdd-owner: implementation -->` (trabajo de código/tests) o `<!-- sdd-owner: parent -->` (acciones de lifecycle/review). Ningún heading infiere ownership.
- **Tareas de test**: todo test listada como RED antes de su código de producción (Strict TDD Mode activo). En slices donde el "código de producción" es trivial (records, anotaciones), el test queda en la misma slice como verificación de aceptabilidad.
- **Compromiso de testing**: `./gradlew test` corre unit + web slice tests (JUnit 5 + Mockito + Spring Test). No hay integración con AWS real; `S3Presigner` se mockea siempre.

## PR slices propuestos

### PR 1 — Foundation: configuración AWS + modelos internos + key generator
- **Tasks**: T-CFG-001, T-CFG-002, T-CFG-003, T-CFG-004, T-CFG-005, T-CFG-006, T-MOD-001, T-MOD-002, T-MOD-003, T-MOD-004, T-MOD-005
- **Líneas estimadas**: 365 (165 CFG + 200 MOD)
- **Valor**: la app arranca con la nueva config AWS enlazada; `S3KeyGenerator` produce keys `profiles/<uuid>/<uuid>.<ext>` y `publications/<uuid>/<uuid>.<ext>`. No expone endpoints todavía.
- **Verificación**: `AwsS3ConfigurationTest` carga contexto con props completas + verifica `endpointOverride` aplicado en ambos beans; test negativo `missingBucketProfileUrl_failsToStart`; `S3KeyGeneratorTest` cubre formatos de key.
- **Dependencias**: ninguna (foundation). El test `TiendaUniApiApplicationTests` existente se mantiene verde con `@TestPropertySource` que setea las 7 props AWS como placeholder (incluido en T-CFG-006).

### PR 2 — DTOs Request/Response
- **Tasks**: T-DTO-001, T-DTO-002, T-DTO-003, T-DTO-004, T-DTO-005, T-DTO-006, T-DTO-007
- **Líneas estimadas**: 160
- **Valor**: records inmutables listos para que controllers y service operen. Validaciones Jakarta declaradas en cada DTO (anotaciones estándar `@NotBlank`, `@Pattern`, `@Positive`, `@Size`).
- **Verificación**: `./gradlew compileJava` + `./gradlew test` (no hay tests dedicados de records, pero `AwsS3ConfigurationTest` del PR 1 sigue verde y la app arranca). El validator `@ValidFileSize` aún NO está aplicado a DTOs (viene en PR 3).
- **Dependencias**: PR 1 (modelos `BucketTarget`, `PresignedUrl`, `PresignedUrlMimeExtension` están disponibles).

### PR 3 — Validadores custom (`@ValidFileSize`, `@UniqueFileIds`)
- **Tasks**: T-VAL-001, T-VAL-002, T-VAL-003, T-VAL-004, T-VAL-005, T-VAL-006
- **Líneas estimadas**: 250
- **Valor**: anotaciones Jakarta custom funcionales + tests de caps por mime prefix + dedup de ids en `files`. `UniqueFileIdsValidator` puede probarse ya con `PresignedUrlPublicationRequest` del PR 2.
- **Verificación**: `ValidFileSizeValidatorTest` (5 casos), `UniqueFileIdsValidatorTest` (2 casos). `./gradlew test` verde.
- **Dependencias**: PR 2 (los validadores referencian tipos DTO).

### PR 4 — Servicio de pre-firmado
- **Tasks**: T-SVC-001, T-SVC-002, T-SVC-003
- **Líneas estimadas**: 215
- **Valor**: lógica de negocio de firma de URLs operativa, con cobertura unitaria. El servicio se inyecta en PR 5.
- **Verificación**: `PresignedUrlServiceImplTest` cubre happy path de perfil + publicación, orden preservado, excepción envuelta para fallos del SDK.
- **Dependencias**: PR 1 (modelos + properties), PR 3 (validators, vía `@ValidFileSize` ya en DTOs — el servicio los respeta implícitamente, pero no los invoca él mismo).

### PR 5 — Endpoints de perfil + Auth update
- **Tasks**: T-AUTH-001, T-AUTH-002, T-AUTH-003, T-CTL-001, T-CTL-002, T-CTL-004a (sub-task: tests de ProfilePresignedUrlControllerWebMvc + ProfileAvatarControllerWebMvc)
- **Líneas estimadas**: 310 (5 + 15 + 50 + 40 + 40 + 80 + 80)
- **Valor**: tres endpoints nuevos funcionales:
  - `POST /profiles/presigned-url` — devuelve URL pre-firmada para avatar.
  - `PATCH /profiles/me/avatar` — persiste `key` en `users.avatar_url`.
  - `AuthenticationService.updateAvatarKey` operativo y testeado.
- **Verificación**: 2 `@WebMvcTest` cubren happy path, validación, 401 sin cookie y 404 cuando el `userId` del principal no existe en `users`.
- **Dependencias**: PR 4 (servicio).

### PR 6 — Endpoint de publicación + exception handlers
- **Tasks**: T-CTL-003, T-CTL-004b (sub-task: PublicationPresignedUrlControllerWebMvc), T-ADV-001, T-ADV-002, T-ADV-003, T-ADV-004
- **Líneas estimadas**: 285 (40 + 90 + 15 + 50 + 40 + 50)
- **Valor**: `POST /posts/presigned-url` operativo + `PresignedUrlGenerationException` mapeada a 503 con `ProblemDetail` específico + `GlobalExceptionHandler` extendido para `ConstraintViolationException` (usado por `@UniqueFileIds`).
- **Verificación**: `PublicationPresignedUrlControllerWebMvc` cubre happy path con orden preservado, ids duplicados, array vacío, > 10 archivos, 401 sin cookie. `PresignedUrlExceptionHandlerTest` verifica el 503 con `type = /problems/presigned-url-generation-failed`.
- **Dependencias**: PR 5 (controllers previos como referencia + estructura de tests), PR 3 (validator `@UniqueFileIds` ya en producción).

## Lista de tasks

> Cada checkbox termina con `<!-- sdd-owner: implementation -->` (trabajo de código/test) o `<!-- sdd-owner: parent -->` (acciones de lifecycle). Las acciones `parent` se agrupan al final del archivo.

### PR 1 — Foundation

- [x] T-CFG-001 — Crear `AwsS3Properties` record (`@ConfigurationProperties(prefix="app.aws.s3")`) con 7 componentes anotados `@NotBlank`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/configuration/AwsS3Properties.java` (nuevo).
  - **Líneas**: ~25.
  - **ACs**: AC-CFG-1, AC-CFG-5.
  - **Notas**: primer `@ConfigurationProperties` del proyecto; sienta precedente en `configuration/`.

- [x] T-CFG-002 — Crear `AwsS3Configuration` con `@Configuration`, `@EnableConfigurationProperties(AwsS3Properties.class)` y 2 `@Bean S3Presigner` (`profileS3Presigner`, `publicationS3Presigner`) que aplican `endpointOverride` desde `AwsS3Properties.bucketProfileUrl()` / `bucketPublicationUrl()`. Usar factory method estático privado `buildPresigner(properties, bucketUrl)`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/configuration/AwsS3Configuration.java` (nuevo).
  - **Líneas**: ~50.
  - **ACs**: AC-CFG-1, AC-CFG-3, AC-CFG-4.
  - **Notas**: `AwsBasicCredentials.create(accessKeyId, secretAccessKey)` + `StaticCredentialsProvider`. Sin lógica condicional por proveedor (AC-MIG-2, AC-MIG-3).

- [x] T-CFG-003 — Añadir bloque `app.aws.s3.*` en `application.yaml` (base) referenciando las 7 env vars con default vacío. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/resources/application.yaml` (modificado).
  - **Líneas**: ~10 added.
  - **ACs**: AC-CFG-1, AC-CFG-2.

- [x] T-CFG-004 — Añadir bloque dev en `application-dev.yaml` apuntando a LocalStack (`localhost:4566`). <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/resources/application-dev.yaml` (modificado).
  - **Líneas**: ~10 added.
  - **ACs**: AC-CFG-1.

- [x] T-CFG-005 — Añadir bloque prod en `application-prod.yaml` referenciando env vars sin defaults (fail-fast). <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/resources/application-prod.yaml` (modificado).
  - **Líneas**: ~10 added.
  - **ACs**: AC-CFG-2 (fail-fast).

- [x] T-CFG-006 — Crear `AwsS3ConfigurationTest` con `ApplicationContextRunner`. Casos: props completas cargan contexto + ambos beans `S3Presigner` con `endpointOverride` correcto; prop ausente lanza `ConfigurationPropertiesBindException`. Adicionalmente, agregar `@TestPropertySource` a `TiendaUniApiApplicationTests` para mantener el smoke test verde. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/test/java/alberto/cruz/tiendauniapi/configuration/AwsS3ConfigurationTest.java` (nuevo); `src/test/java/alberto/cruz/tiendauniapi/TiendaUniApiApplicationTests.java` (modificado).
  - **Líneas**: ~60 added (test nuevo) + ~5 modified.
  - **ACs**: AC-CFG-1, AC-CFG-2, AC-CFG-3.
  - **Notas**: usar `@MockitoBean` para reemplazar `S3Presigner` en el smoke test si fuera necesario (no debería — la app no invoca el presigner en arranque).

- [x] T-MOD-001 — Crear enum `BucketTarget { PROFILE, PUBLICATION }` con métodos `resolveBucketName(AwsS3Properties)` y `resolveBucketUrl(AwsS3Properties)`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/service/model/BucketTarget.java` (nuevo).
  - **Líneas**: ~40.
  - **ACs**: indirecto (soporte de AC-CFG-3, AC-PROF-2, AC-POST-4).

- [x] T-MOD-002 — Crear record `PresignedUrl(String url, String key)` inmutable. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/service/model/PresignedUrl.java` (nuevo).
  - **Líneas**: ~10.
  - **Notas**: tipo de retorno interno del servicio; no se serializa al cliente (DTOs separados).

- [x] T-MOD-003 — Crear enum `PresignedUrlMimeExtension` con 9 entradas (JPEG, JPG, PNG, GIF, WEBP, SVG_XML, MP4, WEBM, OGG) y `fromMimeType(String): Optional<PresignedUrlMimeExtension>`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/service/model/PresignedUrlMimeExtension.java` (nuevo).
  - **Líneas**: ~60.
  - **ACs**: AC-VAL-4 (sincronizado con whitelist `@Pattern` del DTO; covered indirectly).
  - **Notas**: exhaustividad en compile-time. La whitelist en el DTO se construye con un test cruzado en el slice de DTOs/Validators.

- [x] T-MOD-004 — Crear `@Component S3KeyGenerator` con métodos `generateProfileKey(UUID userId, String mimeType)` y `generatePublicationKey(UUID userId, String mimeType)`. Constantes `PROFILE_FOLDER = "profiles"`, `PUBLICATION_FOLDER = "publications"`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/service/helper/S3KeyGenerator.java` (nuevo).
  - **Líneas**: ~50.
  - **Notas**: lanza `IllegalArgumentException` si `fromMimeType` retorna `Optional.empty()` (servicio lo envuelve en `PresignedUrlGenerationException`).

- [x] T-MOD-005 — Crear `S3KeyGeneratorTest` con 3 casos: profile key format, publication key format, mimeType inválido lanza `IllegalArgumentException`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/test/java/alberto/cruz/tiendauniapi/service/helper/S3KeyGeneratorTest.java` (nuevo).
  - **Líneas**: ~40.
  - **Notas**: `@ExtendWith(MockitoExtension.class)` no necesario (helper sin estado, sin collaborators). JUnit 5 puro.

### PR 2 — DTOs

- [x] T-DTO-001 — Crear record `PresignedUrlItem(String id, String fileName, Long size, String mimeType)` con `@NotBlank`, `@Pattern(regexp="^[a-zA-Z0-9_-]+$")`, `@Size(max=255)`, `@Positive`, `@Pattern(regexp whitelist mime)`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlItem.java` (nuevo).
  - **Líneas**: ~15.
  - **ACs**: AC-POST-1, AC-VAL-4, AC-VAL-5.

- [x] T-DTO-002 — Crear record `PresignedUrlProfileRequest` con anotaciones `@ValidFileSize` (class-level) + jakarta validation por campo (fileName regex + size, mimeType whitelist). <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlProfileRequest.java` (nuevo).
  - **Líneas**: ~30.
  - **ACs**: AC-PROF-1, AC-VAL-1, AC-VAL-2, AC-VAL-3, AC-VAL-4, AC-VAL-5.

- [x] T-DTO-003 — Crear record `PresignedUrlProfileResponse(String url)`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlProfileResponse.java` (nuevo).
  - **Líneas**: ~10.
  - **ACs**: AC-PROF-1, AC-PROF-2.

- [x] T-DTO-004 — Crear record `UpdateAvatarKeyRequest` con `@NotBlank` + `@Pattern(regexp="^profiles/[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}\\.[a-zA-Z0-9]+$", message="...")`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/UpdateAvatarKeyRequest.java` (nuevo).
  - **Líneas**: ~25.
  - **ACs**: AC-AVATAR-1, AC-AVATAR-6, AC-VAL-7.

- [x] T-DTO-005 — Crear record `PresignedUrlPublicationRequest(@UniqueFileIds class-level, List<PresignedUrlItem> files @NotEmpty @Size(max=10) @Valid)`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlPublicationRequest.java` (nuevo).
  - **Líneas**: ~40.
  - **ACs**: AC-POST-1, AC-POST-2, AC-VAL-8, AC-VAL-9.

- [x] T-DTO-006 — Crear record `PresignedUrlPublicationResponse(List<PresignedUrlItemResponse> uris)`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlPublicationResponse.java` (nuevo).
  - **Líneas**: ~15.
  - **ACs**: AC-POST-1.

- [x] T-DTO-007 — Crear record `PresignedUrlItemResponse(String id, String url, String key)` (componente de la respuesta de `/posts/presigned-url`). <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlItemResponse.java` (nuevo).
  - **Líneas**: ~25.
  - **Notas**: nombre final alineado con `design.md` (no `PresignedUrlPublicationFileMeta`).

### PR 3 — Validadores custom

- [x] T-VAL-001 — Crear anotación `@ValidFileSize` (`@Constraint(validatedBy=ValidFileSizeValidator.class)`, `@Target(TYPE)`, `@Retention(RUNTIME)`). <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/validation/ValidFileSize.java` (nuevo).
  - **Líneas**: ~30.
  - **ACs**: AC-VAL-1, AC-VAL-2, AC-VAL-3.

- [x] T-VAL-002 — Crear `ValidFileSizeValidator implements ConstraintValidator<ValidFileSize, Object>`. Constantes `IMAGE_MAX_BYTES = 10L * 1024 * 1024`, `VIDEO_MAX_BYTES = 50L * 1024 * 1024`. Dispatch por `instanceof` (ProfileRequest, Item). <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/validation/ValidFileSizeValidator.java` (nuevo).
  - **Líneas**: ~50.
  - **ACs**: AC-VAL-1, AC-VAL-2, AC-VAL-3.
  - **Notas**: `image/svg+xml` cuenta como imagen (prefijo `image/`).

- [x] T-VAL-003 — Crear `ValidFileSizeValidatorTest` con 5 casos: image dentro de cap pasa, image fuera de cap falla, video dentro de cap pasa, video fuera de cap falla, svg sobre cap de imagen falla. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/test/java/alberto/cruz/tiendauniapi/presentation/validation/ValidFileSizeValidatorTest.java` (nuevo).
  - **Líneas**: ~70.

- [x] T-VAL-004 — Crear anotación `@UniqueFileIds` (`@Constraint(validatedBy=UniqueFileIdsValidator.class)`, `@Target(TYPE)`, `@Retention(RUNTIME)`). <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/validation/UniqueFileIds.java` (nuevo).
  - **Líneas**: ~25.
  - **ACs**: AC-VAL-6.

- [x] T-VAL-005 — Crear `UniqueFileIdsValidator implements ConstraintValidator<UniqueFileIds, PresignedUrlPublicationRequest>`. Construye `Set<String>` con los ids; reporta duplicados en mensaje custom. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/validation/UniqueFileIdsValidator.java` (nuevo).
  - **Líneas**: ~35.
  - **ACs**: AC-VAL-6.

- [x] T-VAL-006 — Crear `UniqueFileIdsValidatorTest` con 2 casos: ids únicos pasa, ids duplicados falla con mensaje listando los conflictivos. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/test/java/alberto/cruz/tiendauniapi/presentation/validation/UniqueFileIdsValidatorTest.java` (nuevo).
  - **Líneas**: ~40.

### PR 4 — Servicio

- [x] T-SVC-001 — Crear interfaz `PresignedUrlService` con 2 métodos: `generateProfilePresignedUrl(PresignedUrlProfileRequest, BucketTarget): PresignedUrl` y `generatePublicationPresignedUrls(PresignedUrlPublicationRequest, BucketTarget): List<PresignedUrl>`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/service/interfaces/PresignedUrlService.java` (nuevo).
  - **Líneas**: ~15.

- [x] T-SVC-002 — Crear `PresignedUrlServiceImpl` con `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly=true)`. Inyectar `S3Presigner profileS3Presigner`, `S3Presigner publicationS3Presigner`, `S3KeyGenerator s3KeyGenerator`, `AwsS3Properties awsS3Properties`. Switch por `BucketTarget` para elegir bean. Capturar `SdkException` y envolver en `PresignedUrlGenerationException` (mensaje enmascarado). <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/service/implementation/PresignedUrlServiceImpl.java` (nuevo).
  - **Líneas**: ~80.
  - **ACs**: AC-PROF-1, AC-PROF-2, AC-POST-1, AC-POST-6, AC-ERR-1.
  - **Notas**: armar `PutObjectRequest` con `bucket`, `key`, `contentType`, `contentLength`. `presignPutObject` retorna `PresignedPutObjectRequest` con `url()`. **NO loggear la URL** (AC-OBS-1).

- [x] T-SVC-003 — Crear `PresignedUrlServiceImplTest` con `@ExtendWith(MockitoExtension.class)`. Casos: happy path perfil (verifica formato de key + `endpointOverride`), happy path publicación (orden preservado, ids coinciden, keys únicas), fallo del SDK lanza `PresignedUrlGenerationException` con `SdkException` como causa. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/test/java/alberto/cruz/tiendauniapi/service/implementation/PresignedUrlServiceImplTest.java` (nuevo).
  - **Líneas**: ~120.
  - **Notas**: helper estático en el test para construir `PresignedPutObjectRequest` con URL sintética (el constructor es privado del SDK).

### PR 5 — Endpoints de perfil

- [x] T-AUTH-001 — Añadir firma `void updateAvatarKey(UUID userId, String key);` a `AuthenticationService`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/service/interfaces/AuthenticationService.java` (modificado).
  - **Líneas**: ~5 added.
  - **Notas**: cierra la decisión cerrada en `proposal.md` (no crear `ProfileService` aún).

- [x] T-AUTH-002 — Implementar `updateAvatarKey(UUID userId, String key)` en `AuthenticationServiceImpl` con `@Transactional`. `userRepository.findById(userId).orElseThrow(UserNotFoundException::new)`; `entity.setAvatarUrl(key)`; `userRepository.save(entity)`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/service/implementation/AuthenticationServiceImpl.java` (modificado).
  - **Líneas**: ~15 added.
  - **ACs**: AC-AVATAR-1, AC-AVATAR-3, AC-AVATAR-4, AC-ERR-4.
  - **Notas**: `UserNotFoundException` la maneja `AuthenticationExceptionHandler` existente (Decisión 12 de `design.md`).

- [x] T-AUTH-003 — Crear test para `updateAvatarKey` en el archivo de tests existente de `AuthenticationServiceImpl` (o nuevo archivo si no existe): caso feliz + caso `userId` no encontrado. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/test/java/alberto/cruz/tiendauniapi/service/implementation/AuthenticationServiceImplTest.java` (nuevo o modificado, según estado actual).
  - **Líneas**: ~50 added.
  - **Notas**: si no existe test del impl, crear archivo nuevo.

- [x] T-CTL-001 — Crear `ProfilePresignedUrlController` (`@RestController @RequestMapping("/profiles/presigned-url") @RequiredArgsConstructor`). Endpoint `POST` que recibe `@Valid @RequestBody PresignedUrlProfileRequest` y delega en `presignedUrlService.generateProfilePresignedUrl(request, BucketTarget.PROFILE)`. Devuelve `200 OK` con `PresignedUrlProfileResponse(presignedUrl.url())`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/controller/ProfilePresignedUrlController.java` (nuevo).
  - **Líneas**: ~40.
  - **ACs**: AC-PROF-1, AC-PROF-3, AC-PROF-4, AC-PROF-5.

- [x] T-CTL-002 — Crear `ProfileAvatarController` (`@RestController @RequestMapping("/profiles/me/avatar") @RequiredArgsConstructor`). Endpoint `PATCH` que recibe `@Valid @RequestBody UpdateAvatarKeyRequest` y `@AuthenticationPrincipal AuthenticatedUser principal`. Delega en `authenticationService.updateAvatarKey(principal.getUserId(), request.key())`. Devuelve `204 No Content`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/controller/ProfileAvatarController.java` (nuevo).
  - **Líneas**: ~40.
  - **ACs**: AC-AVATAR-1, AC-AVATAR-2, AC-AVATAR-3, AC-AVATAR-4, AC-AVATAR-5.

- [x] T-CTL-004a — Crear `ProfilePresignedUrlControllerWebMvcTest` + `ProfileAvatarControllerWebMvcTest`. Casos: happy path perfil, mime inválido → 400, size > cap → 400, sin cookie → 401; happy path avatar, key inválida → 400, userId del principal no existe → 404, sin cookie → 401. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/test/java/alberto/cruz/tiendauniapi/presentation/controller/ProfilePresignedUrlControllerWebMvcTest.java` (nuevo); `src/test/java/alberto/cruz/tiendauniapi/presentation/controller/ProfileAvatarControllerWebMvcTest.java` (nuevo).
  - **Líneas**: ~160 (80 + 80).
  - **ACs**: AC-PROF-3, AC-PROF-4, AC-AVATAR-5, AC-AVATAR-6, AC-ERR-4.
  - **Notas**: `@WebMvcTest(Controller.class)` + `@MockitoBean` para servicio + `@MockitoBean` para beans `S3Presigner` si son requeridos por el contexto del slice (verificar con `@WebMvcTest` qué beans escanea).

### PR 6 — Endpoint de publicación + exception handlers

- [x] T-CTL-003 — Crear `PublicationPresignedUrlController` (`@RestController @RequestMapping("/posts/presigned-url") @RequiredArgsConstructor`). Endpoint `POST` que recibe `@Valid @RequestBody PresignedUrlPublicationRequest` y delega en `presignedUrlService.generatePublicationPresignedUrls(request, BucketTarget.PUBLICATION)`. Mapea `List<PresignedUrl>` a `List<PresignedUrlItemResponse>` preservando orden. Devuelve `200 OK` con `PresignedUrlPublicationResponse`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/controller/PublicationPresignedUrlController.java` (nuevo).
  - **Líneas**: ~40.
  - **ACs**: AC-POST-1, AC-POST-3, AC-POST-4, AC-POST-5.

- [x] T-CTL-004b — Crear `PublicationPresignedUrlControllerWebMvcTest` con 5 casos: 3 elementos con ids mixtos preserva orden, ids duplicados → 400, array vacío → 400, 11 archivos → 400, sin cookie → 401. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/test/java/alberto/cruz/tiendauniapi/presentation/controller/PublicationPresignedUrlControllerWebMvcTest.java` (nuevo).
  - **Líneas**: ~90.
  - **ACs**: AC-POST-2, AC-POST-3, AC-POST-5.

- [x] T-ADV-001 — Crear `PresignedUrlGenerationException extends RuntimeException` con constructor `(String message, Throwable cause)`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/service/exception/PresignedUrlGenerationException.java` (nuevo).
  - **Líneas**: ~15.
  - **ACs**: AC-ERR-1.

- [x] T-ADV-002 — Crear `PresignedUrlExceptionHandler` (`@RestControllerAdvice @Order(20)`) con handler de `PresignedUrlGenerationException` que devuelve `503 Service Unavailable` + `ProblemDetail` (`type = /problems/presigned-url-generation-failed`, `title = "Presigned Url Generation Failed"`, `detail = "No se pudo generar la URL pre-firmada..."`). Loggear `ERROR` con stacktrace (sin URL). <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/advice/PresignedUrlExceptionHandler.java` (nuevo).
  - **Líneas**: ~50.
  - **ACs**: AC-ERR-1.

- [x] T-ADV-003 — Extender `GlobalExceptionHandler` con `@ExceptionHandler(jakarta.validation.ConstraintViolationException.class)` que devuelve `400` + `ProblemDetail` (`type = /problems/validations`, `title = "Validation Failed"`, `detail` con mensaje de la constraint violation). <!-- sdd-owner: implementation -->
  - **Archivos**: `src/main/java/alberto/cruz/tiendauniapi/presentation/advice/GlobalExceptionHandler.java` (modificado).
  - **Líneas**: ~40 added.
  - **ACs**: AC-ERR-3.
  - **Notas**: `@UniqueFileIds` lanza `ConstraintViolationException`, no `MethodArgumentNotValidException`; este handler es el que la captura.

- [x] T-ADV-004 — Crear `PresignedUrlExceptionHandlerTest`. Caso: invocación directa del handler con `PresignedUrlGenerationException` → verifica `status = 503`, `type`, `title`, `detail`. <!-- sdd-owner: implementation -->
  - **Archivos**: `src/test/java/alberto/cruz/tiendauniapi/presentation/advice/PresignedUrlExceptionHandlerTest.java` (nuevo).
  - **Líneas**: ~50.

### Lifecycle (parent-owned)

- [ ] Validar el forecast y `delivery_strategy` cacheados antes de lanzar `sdd-apply`; confirmar `stacked-to-main` como chain strategy o solicitar cambio explícito. <!-- sdd-owner: parent -->
- [ ] Adquirir attempt con `gentle-ai sdd-attempt acquire` antes de cada slice y `settle` después; enrutar sólo por `proceed | blocked | complete`. <!-- sdd-owner: parent -->
- [ ] Lanzar review nativo (`gentle-ai review status --next-transition`) después de cada slice fusionado; autorizar `review.capture-result`/`review.start` sólo desde la transición retornada. <!-- sdd-owner: parent -->
- [ ] Mergear PR 1 → PR 2 → PR 3 → PR 4 → PR 5 → PR 6 a `main` (o tracker si el operador cambia a `feature-branch-chain`). <!-- sdd-owner: parent -->
- [ ] Actualizar README con las 7 env vars AWS requeridas (referencia en `design.md`, fuera de código pero accionable). <!-- sdd-owner: parent -->

## Estimación total

- **Total tasks**: 35
- **Total líneas estimadas**: ~1 585 (additions + modifications; ±20% según juicio del implementador)
- **PR slices**: 6, todas ≤ 400 líneas:
  - PR 1 (Foundation): ~365 líneas
  - PR 2 (DTOs): ~160 líneas
  - PR 3 (Validators): ~250 líneas
  - PR 4 (Service): ~215 líneas
  - PR 5 (Profile endpoints): ~310 líneas
  - PR 6 (Publication + advice): ~285 líneas

## Cadena de dependencias

```
PR 1 (CFG + MOD)
    │
    ├──► PR 2 (DTOs)
    │       │
    │       └──► PR 3 (Validators) ──► PR 4 (Service) ──► PR 5 (Profile endpoints) ──► PR 6 (Publication + advice)
    │
    └──► PR 4 (Service) [paralelo a PR 2/3 desde PR 1; converge en PR 5]
```

- PR 1 es foundation (sin deps).
- PR 2 (DTOs) depende de PR 1 (modelos `BucketTarget`, `PresignedUrl`, `PresignedUrlMimeExtension`).
- PR 3 (Validators) depende de PR 2 (`UniqueFileIdsValidator` necesita `PresignedUrlPublicationRequest`).
- PR 4 (Service) depende de PR 1 (properties + modelos + key generator); puede desarrollarse en paralelo a PR 2/3.
- PR 5 (Profile endpoints) depende de PR 4 (servicio) + PR 3 (validators aplicados en DTOs).
- PR 6 (Publication + advice) depende de PR 5 (estructura de tests web previa como referencia) + PR 3 (validator `@UniqueFileIds` ya activo).

## Notas de aplicación

- **Strict TDD Mode activo** (`openspec/config.yaml → strict_tdd: true`). Cada tarea de código de producción va precedida de su test RED correspondiente. En tasks de "crear anotación" o "crear record" donde el código no tiene lógica, el test es de aceptabilidad (compile + invariants).
- **Test runner**: `./gradlew test` (Gradle test, JUnit 5 + Mockito + Spring Test).
- **Cada PR slice debe**:
  1. `./gradlew compileJava` verde.
  2. `./gradlew test` verde (los tests nuevos del slice + los previos acumulados siguen pasando).
  3. Smoke test `TiendaUniApiApplicationTests.contextLoads()` verde (mantener `@TestPropertySource` actualizado en T-CFG-006 si se agregan nuevos beans que requieren props).
- **Conventional commits en inglés** (sin "Co-Authored-By" ni atribución IA). Formato sugerido:
  - PR 1: `feat(aws-s3): add AWS S3 configuration, presigner beans and key generator`
  - PR 2: `feat(aws-s3): add presigned URL request and response DTOs`
  - PR 3: `feat(aws-s3): add custom file size and unique file ids validators`
  - PR 4: `feat(aws-s3): add presigned URL service with bucket-agnostic key generation`
  - PR 5: `feat(aws-s3): add profile presigned URL and avatar update endpoints`
  - PR 6: `feat(aws-s3): add publication presigned URL endpoint and exception handlers`
- **Después de cada slice mergeado**: tag del PR + actualización del `CHANGELOG` (si existe en el repo; si no, sólo commit message).
- **No loggear URLs exitosas** (AC-OBS-1). El servicio `PresignedUrlServiceImpl` sólo loggea `WARN`/`ERROR` (configurar `logback-spring.xml` con `additivity=false` si hace falta, fuera de alcance de este cambio pero documentado).
- **Mocking**: usar `@MockitoBean` (Spring Framework 6.2+) en `@WebMvcTest`/integration; `@Mock` con `@ExtendWith(MockitoExtension.class)` en unit tests puros. NO usar `@MockBean` (deprecado en Spring Boot 3.4+).
- **Endpoint Override Cloudflare R2**: el cambio de proveedor es 100% env vars. Documentar en README (acción parent en sección Lifecycle).

## Riesgos

- **Estimación de líneas varía ±20%**. Si un PR slice termina > 400 líneas en review, re-cortarlo y aplicar mid-PR (no es fail-fast; el gate lo captura).
- **Primer `@ConfigurationProperties` del proyecto**: los nombres de record components deben calzar exactamente con kebab-case en YAML. Mitigado por `AwsS3ConfigurationTest` que carga el contexto con props válidas y verifica el binding (T-CFG-006).
- **Ordering de `@RestControllerAdvice`**: `GlobalExceptionHandler` está en `@Order(Ordered.HIGHEST_PRECEDENCE)`. `PresignedUrlExceptionHandler` con `@Order(20)` corre después. Riesgo bajo (los tipos son disjuntos) pero documentado en el código.
- **`@ValidFileSize` class-level**: necesita dispatch por `instanceof` (ProfileRequest vs Item). Documentar el patrón en comentario Javadoc del validador (Riesgo de implementación de T-VAL-002).
- **`PresignedUrlItemResponse` (T-DTO-007)** vs `PresignedUrlItem` (T-DTO-001): son records distintos en `presentation/dto/` con shapes diferentes (input vs output). El mapping ocurre en el controller de publicación. Si el operador renombra, ajustar PR 2 antes de mergear.
- **Tests con `PresignedPutObjectRequest`**: el constructor del SDK es privado. Helper estático en `PresignedUrlServiceImplTest` (T-SVC-003) que arma el objeto via builder. Documentar el helper.
- **Drift entre whitelist de `@Pattern` en DTO y `PresignedUrlMimeExtension` enum**: mitigación con test cruzado en slice de DTOs/Validators (PR 2 o PR 3) que valida que el regex coincide con los nombres del enum.
- **Cobertura AWS real fuera de alcance**: no hay Testcontainers LocalStack en este MVP. Si en el futuro aparecen bugs de integración SDK, se introduce como follow-up.
- **Cambio en `application-prod.yaml`**: las env vars AWS_* deben documentarse en README (acción parent, fuera de código). Riesgo de deploy si no se setean vars: la app falla al arrancar (fail-fast intencional, AC-CFG-2).

## Out of scope (referencia)

Referencia completa en `proposal.md` sección "Fuera de alcance (Non-Goals)":

- `POST /posts` (creación efectiva de publicación, persistencia de `publications` y `publication_media`).
- Mapeo JPA de `publications` y `publication_media` (tablas SQL existen; entidades JPA no se crean en este cambio).
- Endpoint `GET` pre-firmado para lectura de assets (bucket público; URL la construye el frontend).
- Limpieza asíncrona de objetos huérfanos en S3 (política leave-it).
- Lifecycle rules, IAM policies, S3 CORS en el bucket (infraestructura).
- Migración efectiva de AWS S3 a Cloudflare R2 (sí se construye el camino: `endpointOverride` + env vars).
- Soporte de `multipart/form-data` en el backend.
- Validación por bytes del archivo subido (delegada a S3 vía `Content-Length` firmado).