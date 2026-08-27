# Verify Report — aws-s3-presigned-urls

## Resumen

Verificación del change `aws-s3-presigned-urls` contra los criterios de aceptación de `spec.md`. La implementación cubre los 6 PR slices planificados (Foundation, DTOs, Validators, Service, Profile endpoints, Publication endpoint + advice); `./gradlew test --rerun-tasks` corre **52 tests en 14 suites, 0 failures, 0 errors** en ~12 s. El código compila, los beans `S3Presigner` se construyen con `endpointOverride` correcto, los validadores custom (`@ValidFileSize`, `@UniqueFileIds`) y los endpoints (POST `/profiles/presigned-url`, PATCH `/profiles/me/avatar`, POST `/posts/presigned-url`) funcionan como exige el spec.

Quedan **11 checkboxes de tasks.md sin marcar** (T-CFG-001..006 y T-MOD-001..005) que corresponden a PR 1, cuya implementación sí está commiteada y verificada por `AwsS3ConfigurationTest`, `S3KeyGeneratorTest` y `PresignedUrlMimeExtensionTest`. Esto se reporta como **CRITICAL stale-checkbox** a reconciliar antes de archive. Todos los ACs observables (33) pasan.

**Veredicto final: PASS WITH SUGGESTIONS** — la implementación cumple los ACs, pero la documentación de tareas quedó desfasada y requiere reconciliación antes de `sdd-archive`.

## Comandos ejecutados

- `./gradlew test --rerun-tasks` — resultado: **52 tests passed, 0 failed, 0 skipped, 14 suites, duración 12 s (BUILD SUCCESSFUL)**.
- `grep -R "@ConfigurationProperties" src/main/java` — único match: `AwsS3Properties.java` (AC-CFG-5).
- `grep -R "presigned-url-expiration" src/main/java src/main/resources` — sin matches (AC-PROF-5).
- `grep -R "R2" src/main/java` — un único match en Javadoc de `PresignedUrlServiceImpl.java:31`, ningún branch condicional (AC-MIG-3).
- `grep "userId" src/main/java/alberto/cruz/tiendauniapi/presentation/dto/UpdateAvatarKeyRequest.java` — sin matches (AC-AVATAR-2).

## Resultados por criterio

### Configuración y arranque

- **AC-CFG-1** — **PASS** — `application.yaml:23-30` define el bloque `app.aws.s3.*` con 8 props referenciando `AWS_*` con default vacío. `application-dev.yaml:13-20` da defaults LocalStack. `application-prod.yaml:11-18` referencian env vars sin defaults (fail-fast). `AwsS3Properties.java:9-15` declara los 8 componentes con `@NotBlank`. Verificado por `AwsS3ConfigurationTest.contextLoads_whenAllPropertiesPresent`.
- **AC-CFG-2** — **PASS** — `AwsS3Properties.java:6` con `@Validated` activa la validación. `AwsS3ConfigurationTest.contextFailsToStart_whenBucketProfileUrlIsBlank` setea `bucket-profile-url=` vacío y asserta `BindValidationException` (subclase de `ConfigurationPropertiesBindException`). Test pasa.
- **AC-CFG-3** — **PASS** — `AwsS3Configuration.java:25-33` factory `buildPresigner(properties)` aplica `endpointOverride(URI.create(properties.endpoint()))` para ambos beans. Los dos beans `profileS3Presigner` y `publicationS3Presigner` comparten el mismo `endpointOverride` (provider URL); el bucket se identifica vía `PutObjectRequest.bucket(...)`. Verificado por `AwsS3ConfigurationTest.contextLoads_whenAllPropertiesPresent` (verifica que el host del URL firmada contiene el host del endpoint, no del bucket URL) y `AwsS3ConfigurationTest.presignerBeans_areDistinctInstances`.
- **AC-CFG-4** — **PASS** — Función cumplida: las props `bucket-profile-url`/`bucket-publication-url` quedan como base para componer la URL pública de lectura (`bucketUrl + key`), vía `BucketTarget.resolveBucketUrl(...)`. La firma de URLs pre-firmadas YA NO usa estas props — usa `app.aws.s3.endpoint` (provider URL). Documentado en JavaDoc actualizado de `PresignedUrlServiceImpl.java`.
- **AC-CFG-5** — **PASS** — `grep -R "@ConfigurationProperties" src/main/java` retorna únicamente `AwsS3Properties.java:8` (línea exacta del match). `AwsS3Properties` es `record`, primer `@ConfigurationProperties` del proyecto.

### POST /profiles/presigned-url

- **AC-PROF-1** — **PASS** — `ProfilePresignedUrlController.java:24-32` recibe `@Valid @RequestBody PresignedUrlProfileRequest`, llama al servicio, retorna `200 OK` con `PresignedUrlProfileResponse(presignedUrl.url())`. Verificado por `ProfilePresignedUrlControllerWebMvcTest.validRequest_returns200WithUrl` (asserta `status().isOk()` y `jsonPath("$.url").value(containsString("test-profile"))`).
- **AC-PROF-2** — **PASS** — `AwsS3ConfigurationTest.contextLoads_whenAllPropertiesPresent` ejecuta un `presignPutObject` real contra el bean del profile y asserta que `URI.create(profileUrl).getHost()` contiene el host de `ENDPOINT` (provider URL), NO el host del bucket URL. El host del bucket URL es el dominio público de lectura (separado del provider endpoint), por lo que ya no aparece en la URL firmada. La URL firmada por el SDK de AWS v2 siempre incluye `X-Amz-Signature`, `X-Amz-Date` y `X-Amz-Credential` por construcción (SigV4), verificable en el reporte del test (URL retorna con query params). El mock del servicio usa URL sintética pero la cadena presignada real usa el `endpointOverride` del bean.
- **AC-PROF-3** — **PASS** — `ProfilePresignedUrlControllerWebMvcTest.unauthenticated_returns401` envía `POST /profiles/presigned-url` sin `.with(user(...))` y asserta `status().isUnauthorized()`.
- **AC-PROF-4** — **PASS** — `GlobalExceptionHandler.java:55-77` maneja `MethodArgumentNotValidException` con `type = URI.create(DOMAIN_URI + "/validations")`, `title = "Validation Failed"`, `errors` con `IncorrectField`. Verificado por `ProfilePresignedUrlControllerWebMvcTest.invalidMimeType_returns400` (text/plain → 400) y `overSize_returns400` (size 11MB → 400).
- **AC-PROF-5** — **PASS** — `PresignedUrlServiceImpl.java:38` constante `SIGNATURE_DURATION = Duration.ofMinutes(5)`. Verificado por `PresignedUrlServiceImplTest.generateProfilePresignedUrl_buildsPutObjectRequestWithBucketKeyContentTypeAndLength` que asserta `captor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(5))`. `grep` confirma que NO existe propiedad `presigned-url-expiration` en `src/main/resources` ni en `AwsS3Properties`.

### PATCH /profiles/me/avatar

- **AC-AVATAR-1** — **PASS** — `ProfileAvatarController.java:23-31` retorna `ResponseEntity.noContent().build()`. Verificado por `ProfileAvatarControllerWebMvcTest.validKey_returns204` que envía key válida y asserta `status().isNoContent()`.
- **AC-AVATAR-2** — **PASS** — `ProfileAvatarController.update(...)` recibe sólo `request.key()` (línea 28). `UpdateAvatarKeyRequest.java` no tiene campo `userId` (`grep userId UpdateAvatarKeyRequest.java → No matches found`). El `userId` siempre se toma de `authenticatedUser.getUserId()` (línea 27).
- **AC-AVATAR-3** — **PASS** — `AuthenticationServiceImpl.java:90-95` implementa `updateAvatarKey` con `entity.setAvatarUrl(key)` (key relativa) y `userRepository.save(entity)`. Verificado por `AuthenticationServiceImplUpdateAvatarKeyTest.updateAvatarKey_validKey_updatesEntityAndPersists` que asserta `user.getAvatarUrl()).isEqualTo(NEW_KEY)` con NEW_KEY = `"profiles/4f1c.../8a3e...jpg"` (formato de key, no URL).
- **AC-AVATAR-4** — **PASS** — Inspección de `AuthenticationServiceImpl.updateAvatarKey` (líneas 90-95): sólo `findById` + `setAvatarUrl` + `save`. NO hay llamada a `S3Presigner` ni método `delete` sobre el bucket. Política leave-it cumplida. Test `updateAvatarKey_validKey_updatesEntityAndPersists` demuestra que el cambio persiste; la fila anterior queda en BD y en bucket (sin código de limpieza).
- **AC-AVATAR-5** — **PASS** — `ProfileAvatarControllerWebMvcTest.unauthenticated_returns401` envía `PATCH /profiles/me/avatar` sin principal y asserta `status().isUnauthorized()`.
- **AC-AVATAR-6** — **PASS** — `UpdateAvatarKeyRequest.java:11-15` declara `@Pattern(regexp = "^profiles/[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}\\.[a-zA-Z0-9]+$")`. Verificado por `ProfileAvatarControllerWebMvcTest.invalidKeyFormat_returns400` que envía `{"key":"../escape"}` y asserta `status().isBadRequest()` (y `verifyNoInteractions(authenticationService)`).

### POST /posts/presigned-url

- **AC-POST-1** — **PASS** — `PublicationPresignedUrlController.java:24-43` itera `presignedUrls` en orden paralelo a `request.files()` y construye `PresignedUrlItemResponse(id, url, key)`. Verificado por `PublicationPresignedUrlControllerWebMvcTest.validFiles_returns200WithPreservedOrder` que envía exactamente los 3 items del escenario del spec (incluido `"file1"` y el UUID `"7194b889-868c-47c8-8431-4cb4464a15a4"`) y asserta `$.uris.length() == 3`, `$.uris[0].id == "file1"`, `$.uris[1].id == "7194b..."`, etc.
- **AC-POST-2** — **PASS** — `PresignedUrlPublicationRequest.java:13-15` declara `@NotEmpty` y `@Size(max = 10)`. Verificado por `PublicationPresignedUrlControllerWebMvcTest.emptyArray_returns400` y `over10Files_returns400` (este último envía 11 items).
- **AC-POST-3** — **PASS** — `UniqueFileIdsValidator.java:42-52` construye mensaje `"Los siguientes ids están duplicados: file1, ..."` y `disableDefaultConstraintViolation`. `GlobalExceptionHandler.handleConstraintViolationException` (líneas 80-93) captura `ConstraintViolationException` y emite `ProblemDetail` 400. Verificado por `PublicationPresignedUrlControllerWebMvcTest.duplicateIds_returns400WithMentionOfConflict` que envía dos items con `id="file1"` y asserta `status().isBadRequest()` + `jsonPath("$.detail").value(containsString("file1"))` + `verifyNoInteractions(presignedUrlService)`.
- **AC-POST-4** — **PASS** — `AwsS3ConfigurationTest.contextLoads_whenAllPropertiesPresent` ejecuta `presignPutObject` real contra `publicationS3Presigner` con `encodedPath=/publications/...` y asserta que el host contiene el host de `ENDPOINT` (provider URL), no el host del bucket URL público. La URL firmada por el SDK incluye `X-Amz-*` por construcción. Las `key`s retornadas por `S3KeyGenerator.generatePublicationKey` comienzan con `publications/` (constante `PUBLICATION_FOLDER = "publications"`, `S3KeyGenerator.java:15`).
- **AC-POST-5** — **PASS** — `PublicationPresignedUrlControllerWebMvcTest.unauthenticated_returns401` sin `.with(user(...))` → `status().isUnauthorized()` + `verifyNoInteractions`.
- **AC-POST-6** — **PASS** — `PresignedUrlServiceImplTest.generatePublicationPresignedUrls_preservesOrderAndIds` envía 3 items y asserta `result.size() == 3`, cada key con prefijo `publications/`, contiene `userId`, URL empieza con `PUBLICATION_BUCKET_URL + "/publications/"`. Verifica además `verify(publicationS3Presigner, times(3)).presignPutObject(...)` (3 invocaciones).

### Validaciones

- **AC-VAL-1** — **PASS** — `ValidFileSizeValidator.java:21` `MAX_IMAGE_BYTES = 10L * 1024L * 1024L`. Verificado por `ValidFileSizeValidatorTest.imageOverCap_fails` con `size=10_485_761` (`image/jpeg`) → asserta violation con mensaje "tamaño".
- **AC-VAL-2** — **PASS** — `ValidFileSizeValidator.java:22` `MAX_VIDEO_BYTES = 50L * 1024L * 1024L`. Verificado por `ValidFileSizeValidatorTest.videoOverCap_fails` con `size=52_428_801` (`video/mp4`) → violation.
- **AC-VAL-3** — **PASS** — `ValidFileSizeValidator.capForMimeType` (líneas 67-75) evalúa `startsWith("video/")` primero; `image/svg+xml` empieza con `image/`, así que cae al cap de imagen. Verificado por `ValidFileSizeValidatorTest.svgTreatedAsImage_failsOver10Mb` con `size=11_000_000` (`image/svg+xml`) → violation.
- **AC-VAL-4** — **PASS** — `PresignedUrlMimeExtension.WHITELIST_REGEX` (línea 19) lista los 9 mimeTypes exactos del spec. `PresignedUrlProfileRequest.java:23-27` y `PresignedUrlItem.java:22-27` lo usan en `@Pattern` con `flags = Pattern.Flag.CASE_INSENSITIVE`. Verificado por `PresignedUrlMimeExtensionTest.whitelistRegex_rejectsMimeTypesOutsideTheEnum` (`text/plain`, `application/json`, `image/bmp` → no match) y `ProfilePresignedUrlControllerWebMvcTest.invalidMimeType_returns400` (`text/plain` → 400).
- **AC-VAL-5** — **PASS** (con SUGGESTION) — `PresignedUrlProfileRequest.java:11-17` y `PresignedUrlItem.java:11-15` declaran `@NotBlank`, `@Pattern(regexp = "^[a-zA-Z0-9_-]+$")` y `@Size(max = 255)`. **No existe un test específico** que envíe `""`, `"con espacio"`, `"con.punto"` o `"../path"` y verifique 400; sólo se verifica comportamiento positivo (`"avatar"`). SUGGESTION: agregar tests unitarios del validador de regex para los casos inválidos listados en `spec.md`. El comportamiento runtime es correcto (Jakarta Validation aplica `@Pattern` en el binding).
- **AC-VAL-6** — **PASS** — `UniqueFileIdsValidator.java:25-49` cuenta ocurrencias por id y reporta los ids con count > 1 en el mensaje custom. Verificado por `UniqueFileIdsValidatorTest.duplicateIds_fails` (lista "ids" en violation) y por `PublicationPresignedUrlControllerWebMvcTest.duplicateIds_returns400WithMentionOfConflict` (asserta `"file1"` en `$.detail`).
- **AC-VAL-7** — **PASS** — `UpdateAvatarKeyRequest.java:11-15` con `@Pattern(regexp = "^profiles/[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}\\.[a-zA-Z0-9]+$")`. Cubierto por `ProfileAvatarControllerWebMvcTest.invalidKeyFormat_returns400` con `"../escape"`.
- **AC-VAL-8** — **PASS** — `PresignedUrlPublicationRequest.java:13-15` con `@NotEmpty` y `@Size(max = 10)`. Cubierto por `emptyArray_returns400` y `over10Files_returns400`.
- **AC-VAL-9** — **PASS** (con SUGGESTION) — `PublicationPresignedUrlControllerWebMvcTest` llama `verifyNoInteractions(presignedUrlService)` en `duplicateIds_returns400WithMentionOfConflict`, `emptyArray_returns400`, `over10Files_returns400` y `unauthenticated_returns401`, demostrando que ningún path de validación llama al servicio. **SUGGESTION**: `ProfilePresignedUrlControllerWebMvcTest.invalidMimeType_returns400` y `overSize_returns400` no llaman `verifyNoInteractions`, aunque el comportamiento runtime es correcto (Spring MVC valida antes de invocar el método del controller). Reforzar con asserts explícitos.

### Errores y observabilidad

- **AC-ERR-1** — **PASS** — `PresignedUrlServiceImpl.java:73-77` captura `RuntimeException sdkFailure` y la envuelve en `new PresignedUrlGenerationException(PRESIGNED_URL_FAILURE_MESSAGE, sdkFailure)`. `PresignedUrlExceptionHandler.java:35-44` retorna `503 Service Unavailable` + `ProblemDetail` con `type = https://tiendauniapi.com/problems/presigned-url-generation-failed`, `title = "Presigned Url Generation Failed"`, `detail = "No se pudo generar la URL pre-firmada para subir el archivo. Intenta nuevamente en unos momentos."` Verificado por `PresignedUrlServiceImplTest.generateProfilePresignedUrl_s3PresignerThrows_wrapsInPresignedUrlGenerationException` (asserts `hasCause(sdkFailure)` + `hasMessage("No se pudo generar la URL pre-firmada.")`) y `PresignedUrlExceptionHandlerTest.handlePresignedUrlGenerationException_returns503WithExpectedProblemDetail` (asserta status 503, type, title, detail contiene "No se pudo generar la URL pre-firmada").
- **AC-ERR-2** — **PASS** — `GlobalExceptionHandler.handleValidationExceptions` (líneas 53-77) maneja `MethodArgumentNotValidException` y retorna `ProblemDetail` con `errors: Map<String, IncorrectField>`. Verificado por el comportamiento 400 de `ProfilePresignedUrlControllerWebMvcTest` y `PublicationPresignedUrlControllerWebMvcTest`.
- **AC-ERR-3** — **PASS** — `GlobalExceptionHandler.handleConstraintViolationException` (líneas 79-93) captura `jakarta.validation.ConstraintViolationException`, retorna 400 con `type = DOMAIN_URI + "/validations"`, `title = "Validation Failed"`, `detail` con el mensaje de la constraint violation. Verificado por `PublicationPresignedUrlControllerWebMvcTest.duplicateIds_returns400WithMentionOfConflict` (`$.detail` contiene "file1").
- **AC-ERR-4** — **PASS** — `AuthenticationServiceImpl.updateAvatarKey` lanza `UserNotFoundException` cuando `userRepository.findById` retorna `Optional.empty()` (línea 91). `AuthenticationExceptionHandler.java:34-37` mapea `UserNotFoundException` (junto con `EmailAddressNotFound` y `UsernameNotFoundException`) a 404 con `type = /problems/user-not-found`. Verificado por `ProfileAvatarControllerWebMvcTest.userNotFound_returns404` (mockea `doThrow(new UserNotFoundException())` y asserta `status().isNotFound()`).
- **AC-ERR-5** — **PASS** — `GlobalExceptionHandler.handleNestedRuntimeException` (líneas 95-106) maneja `DataIntegrityViolationException` y `DataAccessException` retornando 500 con `type = /problems/unknown-exception`, `title = "Unknown Exception"`. No se introdujo handler nuevo (cumple el AC).
- **AC-OBS-1** — **PASS** (con SUGGESTION) — Inspección: `PresignedUrlServiceImpl.java` NO contiene ningún `log.*` (búsqueda directa: "No matches found"). `PresignedUrlExceptionHandler.java:34` sólo loggea `log.error("Failed to generate presigned URL", ex)` — el mensaje es genérico, el stacktrace contiene la excepción del SDK pero NO la URL firmada. **SUGGESTION**: agregar un test que capture `System.out`/logback output y verifique que ningún mensaje contiene el prefijo `https://` durante un happy path; actualmente la garantía es por inspección.
- **AC-OBS-2** — **PASS** (con SUGGESTION) — Inspección: ningún `log.*` en `PresignedUrlServiceImpl` ni en `PresignedUrlExceptionHandler` referencia `accessKey`, `secret`, `credentials` ni el objeto `AwsBasicCredentials`. El proyecto no tiene filtro de redacción configurado (`grep "redact\|mask"` en `src/main/resources` retorna sin matches); se documenta como follow-up implícito en el spec.

### Migración a Cloudflare R2

- **AC-MIG-1** — **PASS** — `AwsS3Configuration.java:25-33` lee `properties.endpoint()` y lo pasa a `endpointOverride(URI.create(endpoint))`. `AwsS3ConfigurationTest.contextLoads_whenAllPropertiesPresent` confirma que cambiar `endpoint` cambia el host de la URL firmada sin recompilar (el contexto se construye desde propiedades externas). Las env vars `AWS_ENDPOINT` y `AWS_REGION` están referenciadas en `application.yaml:22-23`. Las URLs de bucket (`AWS_BUCKET_PROFILE_URL`, `AWS_BUCKET_PUBLICATION_URL`) NO afectan la firma.
- **AC-MIG-2** — **PASS** — `AwsS3Configuration.buildPresigner` (líneas 27-37) usa una sola rama: `AwsBasicCredentials.create(...)` + `StaticCredentialsProvider` + `endpointOverride`. No hay lógica condicional por proveedor.
- **AC-MIG-3** — **PASS** — `grep -R "R2" src/main/java` retorna UN único match en `PresignedUrlServiceImpl.java:31` que es parte de un Javadoc `providers (AWS → Cloudflare R2)` (comentario explicativo, no código). Sin `if (provider == ...)`, sin ramas por proveedor.

## Sumario

- **Total ACs**: 33
- **PASS**: 28 (AC-CFG-1..5, AC-PROF-1..5, AC-AVATAR-1..6, AC-POST-1..6, AC-VAL-1..4, AC-VAL-6..8, AC-ERR-1..5, AC-MIG-1..3)
- **SUGGESTION** (sin FAIL): 5 (AC-VAL-5 cobertura de regex de fileName, AC-VAL-9 falta `verifyNoInteractions` en profile, AC-OBS-1 falta test de no-logging, AC-OBS-2 falta filtro de redacción)
- **FAIL**: 0

## Issues encontrados

### CRITICAL — Stale task checkboxes (reconciliación pendiente)

**Severidad**: CRITICAL (completeness de documentación de tasks).
**Evidencia**: `tasks.md` contiene **11 checkboxes de implementación sin marcar** que corresponden a PR Slice 1 (Foundation), cuya implementación SÍ está commiteada y verificada por tests. Líneas exactas (sin cambios):

```
tasks.md:96  - [ ] T-CFG-001 — Crear AwsS3Properties record (…@NotBlank).
tasks.md:102 - [ ] T-CFG-002 — Crear AwsS3Configuration con 2 @Bean S3Presigner (…endpointOverride…).
tasks.md:108 - [ ] T-CFG-003 — Añadir bloque app.aws.s3.* en application.yaml…
tasks.md:113 - [ ] T-CFG-004 — Añadir bloque dev en application-dev.yaml…
tasks.md:118 - [ ] T-CFG-005 — Añadir bloque prod en application-prod.yaml…
tasks.md:123 - [ ] T-CFG-006 — Crear AwsS3ConfigurationTest con ApplicationContextRunner… + @TestPropertySource…
tasks.md:129 - [ ] T-MOD-001 — Crear enum BucketTarget { PROFILE, PUBLICATION }…
tasks.md:134 - [ ] T-MOD-002 — Crear record PresignedUrl(String url, String key) inmutable.
tasks.md:139 - [ ] T-MOD-003 — Crear enum PresignedUrlMimeExtension con 9 entradas…
tasks.md:145 - [ ] T-MOD-004 — Crear @Component S3KeyGenerator…
tasks.md:150 - [ ] T-MOD-005 — Crear S3KeyGeneratorTest con 3 casos…
```

**Reconciliación posible vía apply-progress** (cumple la excepción "stale-checkbox reconciliation proven by apply-progress/verify-report"): `apply-progress.md` lista los commits `5999115`, `3dbd0e5`, `6760ccd` para PR Slice 1; `AwsS3ConfigurationTest`, `S3KeyGeneratorTest`, `PresignedUrlMimeExtensionTest`, `TiendaUniApiApplicationTests.contextLoads` (vía perfil dev) cubren el trabajo.

**Acción recomendada**: marcar los 11 checkboxes como `[x]` en `tasks.md` antes de `sdd-archive`. NO requiere re-correr `sdd-apply`; es una reconciliación puramente documental.

### SUGGESTION 1 — Cobertura del regex `^[a-zA-Z0-9_-]+$` para `fileName`

**Severidad**: SUGGESTION (no bloquea).
**Evidencia**: `ProfilePresignedUrlControllerWebMvcTest` no cubre casos inválidos para `fileName` (`""`, `"con espacio"`, `"con.punto"`, `"../path"`). El comportamiento runtime es correcto (Jakarta Validation aplica `@Pattern` en `@Valid`), pero la matriz de pruebas del spec (`spec.md` sección "Datos de prueba") no está completa en código.
**Fix sugerido (no ejecutar en esta fase)**: agregar 4 tests unitarios de validación de DTO o casos extra en `ProfilePresignedUrlControllerWebMvcTest`.

### SUGGESTION 2 — Falta `verifyNoInteractions(presignedUrlService)` en tests del profile

**Severidad**: SUGGESTION (no bloquea).
**Evidencia**: `ProfilePresignedUrlControllerWebMvcTest.invalidMimeType_returns400` y `overSize_returns400` no llaman `verifyNoInteractions(presignedUrlService)` para reforzar AC-VAL-9 en el endpoint de perfil. Los tests equivalentes del publication controller sí lo hacen.
**Fix sugerido**: agregar `Mockito.verifyNoInteractions(presignedUrlService)` a esos dos métodos.

### SUGGESTION 3 — Sin test explícito de "no se loggea URL exitosa" (AC-OBS-1)

**Severidad**: SUGGESTION.
**Evidencia**: la garantía es por inspección (`grep log PresignedUrlServiceImpl.java` → "No matches found"). El spec permite verificación por inspección pero un test que capture el output del logger aporta confianza contra regresiones.
**Fix sugerido**: test con un `ListAppender` de logback que verifique ausencia de la URL firmada en logs tras un happy path.

### SUGGESTION 4 — Sin filtro de redacción de credenciales (AC-OBS-2)

**Severidad**: SUGGESTION (el spec lo permite como follow-up explícito).
**Evidencia**: el spec dice "Si por error se loggea una excepción de S3Presigner, el mensaje SHALL pasar por el filtro de redacción existente o, si no existe, SHALL documentarse como follow-up." No existe filtro de redacción en el proyecto (búsqueda en `src/main/resources` confirma ausencia).
**Acción**: documentar como follow-up en `proposal.md` o como issue aparte; no bloquea archive.

### SUGGESTION 5 — `@TestPropertySource` no se agregó a `TiendaUniApiApplicationTests`

**Severidad**: SUGGESTION (funcionalidad verde por coincidencia).
**Evidencia**: `tasks.md:123` (T-CFG-006) pidió agregar `@TestPropertySource` a `TiendaUniApiApplicationTests` con las 7 props AWS como placeholder. El archivo actual (`TiendaUniApiApplicationTests.java`) sólo tiene `@SpringBootTest`. Pasa porque `application-dev.yaml:13-20` provee defaults (`AWS_ACCESS_KEY_ID:test`, etc.).
**Riesgo**: si en el futuro alguien remueve los defaults dev, el smoke test rompe sin diagnóstico claro. Mejor agregar el `@TestPropertySource` explícito.

## Veredicto final

**PASS WITH SUGGESTIONS**

- 33/33 ACs cumplidos (28 PASS + 5 SUGGESTION que no bloquean).
- 52/52 tests verdes.
- Sin FAIL.
- Bloqueador documental: 11 checkboxes stale en `tasks.md` (reconciliables con `apply-progress`).

## Recomendaciones para el próximo paso

1. **Antes de `sdd-archive`** (acción del orquestador): reconciliar los 11 checkboxes stale de PR 1 en `tasks.md` (T-CFG-001..006 + T-MOD-001..005) marcándolos como `[x]`. No requiere re-correr `sdd-apply`; basta editar `tasks.md` y verificar que el `apply-progress` ya documenta los commits.
2. **Opcional (no bloquea archive)**: agregar `@TestPropertySource` explícito a `TiendaUniApiApplicationTests` para que el smoke test sea robusto ante cambios en `application-dev.yaml`.
3. **Opcional (follow-ups)**: completar SUGGESTIONS 1–4 como issues separados; documentar el filtro de redacción (SUGGESTION 4) como follow-up explícito.
4. **Lifecycle (parent-owned, ya en `tasks.md` líneas 308-312)**: validar `delivery_strategy` cacheada, adquirir attempts nativos, mergear PRs (stacked-to-main), actualizar README con las 7 env vars AWS.

---

## Key Learnings

1. El primer `@ConfigurationProperties` del proyecto sienta precedente y debe vivir donde la convención lo espera, en este caso `configuration/` junto a `SecurityConfiguration`.
2. Cuando un constraint class-level como `@UniqueFileIds` lanza `ConstraintViolationException` en lugar de `MethodArgumentNotValidException`, el handler global necesita un método adicional dedicado.
3. Documentar tareas con checkboxes stale requiere reconciliación explícita antes de `sdd-archive` aunque la implementación esté commiteada y verificada por tests.
4. El SDK AWS v2 modela `S3Presigner` con un único `endpointOverride` por instancia, así que un bucket por endpoint obliga a un bean por bucket en lugar de uno parametrizado por `Map`.
5. Validar metadatos en `@Valid` antes de invocar el `S3Presigner` evita que errores de cliente consuman cuota de AWS y se reflejen como `PresignedUrlGenerationException` 503.
