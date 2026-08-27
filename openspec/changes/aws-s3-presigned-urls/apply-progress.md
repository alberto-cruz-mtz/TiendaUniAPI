# Apply Progress — aws-s3-presigned-urls

## Status

PR Slices 1, 2, 3, 4, 5 all committed on `feature/presigned-url`. `./gradlew test --rerun-tasks`
is green (46 tests, 0 failures across 11 suites). No push, no PR, no merge.

PR Slice 5 covers T-CTL-001 (`ProfilePresignedUrlController`), T-CTL-002 (`ProfileAvatarController`),
and T-CTL-004a (both `WebMvcTest` classes). T-AUTH-001/002/003 were committed in a prior delegation
on top of slice 4 (`d199d33`); they precede slice 5 by design and remain green throughout this slice.

## Commits (SHAs)

### PR Slice 1 (already on branch)
- `5999115` feat(aws-s3): add service model types and S3KeyGenerator helper
- `3dbd0e5` feat(aws-s3): add AwsS3Properties and two S3Presigner beans
- `6760ccd` chore(aws-s3): wire app.aws.s3.* into profiles and add context load test

### PR Slice 2 (DTOs)
- `0e79752` feat(aws-s3): add mime whitelist regex and stub validation annotations
- `cf46f80` feat(aws-s3): add pre-signed URL request and response DTOs

### PR Slice 3 (Validators) — implemented in earlier session
- T-VAL-001..T-VAL-006 wired `@ValidFileSize` + `@UniqueFileIds` and their validators/tests.

### PR Slice 4 (Service) — implemented in earlier session
- `0309c3a` feat(aws-s3): add PresignedUrlService with S3Presigner signing per bucket
- `e559c49` feat(aws-s3): add PresignedUrlGenerationException for SDK failures
- `3ceb7d8` feat(aws-s3): add UniqueFileIds for duplicate file id detection
- `049663f` test(aws-s3): cover PresignedUrlService with Mockito

### PR Slice 5 (Auth update) — already merged before this delegation
- `d199d33` feat(auth): add updateAvatarKey to AuthenticationService
- (T-AUTH-003: tests in `AuthenticationServiceImplUpdateAvatarKeyTest`, 2 tests)

### PR Slice 5 (Profile endpoints) — this delegation
- `ac20f30` feat(aws-s3): add profile presigned URL and avatar update endpoints
- `778aa3b` test(aws-s3): cover profile controllers with MockMvc

## Tasks completed — PR 2 (DTOs)

- [x] T-DTO-001 — `PresignedUrlItem` (id, fileName, size, mimeType) with Jakarta validation.
- [x] T-DTO-002 — `PresignedUrlProfileRequest` (`@ValidFileSize` class-level + per-field).
- [x] T-DTO-003 — `PresignedUrlProfileResponse(String url)`.
- [x] T-DTO-004 — `UpdateAvatarKeyRequest` with strict pattern for `profiles/<uuid>/<uuid>.<ext>`.
- [x] T-DTO-005 — `PresignedUrlPublicationRequest` (`@UniqueFileIds`, `@NotEmpty`, `@Size(max=10)`).
- [x] T-DTO-006 — `PresignedUrlPublicationResponse(List<PresignedUrlItemResponse> uris)`.
- [x] T-DTO-007 — `PresignedUrlItemResponse(String id, String url, String key)`.

## Files added

### Source
- `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlItem.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlItemResponse.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlProfileRequest.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlProfileResponse.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlPublicationRequest.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/PresignedUrlPublicationResponse.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/dto/UpdateAvatarKeyRequest.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/validation/ValidFileSize.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/validation/UniqueFileIds.java`

### Tests
- `src/test/java/alberto/cruz/tiendauniapi/presentation/dto/DtoCompilationSmokeTest.java` (9 tests)
- `src/test/java/alberto/cruz/tiendauniapi/service/model/PresignedUrlMimeExtensionTest.java` (5 tests)

## Files modified

- `src/main/java/alberto/cruz/tiendauniapi/service/model/PresignedUrlMimeExtension.java` — added
  `WHITELIST_REGEX` literal constant for use in DTO `@Pattern` annotations. Drift detection is
  enforced by `PresignedUrlMimeExtensionTest`.

## Deviations from design / prompt

- **DTO shape ordering (T-DTO-001 / T-DTO-007)**: the delegation prompt described
  `PresignedUrlItem` as the response shape (`{id, url, key}`) and named the input file
  `PresignedUrlPublicationFileMeta`. tasks.md and design.md instead define
  `PresignedUrlItem` as the input file metadata (`{id, fileName, size, mimeType}`) and the
  response item as `PresignedUrlItemResponse({id, url, key})`. Followed tasks.md/design.md
  because the prompt explicitly labels tasks.md as the source of truth, both canonical
  artifacts agree, and the result matches the controllers planned in PR 5/6.
- **WHITELIST_REGEX is a literal, not a static-block computation**: `@Pattern` requires a
  compile-time-constant expression, so the regex is a hand-written `static final String`
  literal rather than a runtime-computed value. `PresignedUrlMimeExtensionTest` asserts
  the literal matches every enum mimeType, so drift between the literal and the enum
  fails the test before reaching runtime.
- **Stub annotations** (`@ValidFileSize`, `@UniqueFileIds`): placeholder `@interface` with
  no `@Constraint` reference. PR 3 will add the `@Constraint(validatedBy = …)` line and
  the `*Validator` classes. Until then, the annotations compile and can be applied, but
  do nothing.
- **No controller-side validation tests in this slice**: T-DTO-002/T-DTO-005 use Jakarta
  Validation annotations, but the actual `@RequestBody @Valid` wiring lives in PR 5/6
  controllers and their WebMvc tests. The DTO smoke test pins shapes only.

## TDD evidence

- Records with no behavior: written and verified by `DtoCompilationSmokeTest` (9 tests
  pinning component count, field accessors, and class loadability).
- `PresignedUrlMimeExtension.WHITELIST_REGEX` cross-checked by
  `PresignedUrlMimeExtensionTest` (5 tests): whitelist acceptance, rejection of
  `text/plain`/`application/json`/`image/bmp`, case-insensitive matching, escape of the
  `+` in `svg+xml`, and `whitelist()` set stays in sync with enum values.

## Verification

- `./gradlew compileJava` — green.
- `./gradlew test` — green, 21 tests, 0 failures (14 new in this slice, 7 from PR 1).
- `git status` — only user's pre-existing uncommitted changes
  (`build.gradle`, `SecurityConfiguration.java`) and user's untracked files
  (`aws-s3.md`, `openspec/changes/`) remain outside the commits.

## Untouched (per user instructions)

- `build.gradle` (user-added AWS SDK dep).
- `src/main/java/alberto/cruz/tiendauniapi/configuration/SecurityConfiguration.java` (user EOF newline).
- `aws-s3.md`, `openspec/changes/` (user-managed).

## Tasks completed — PR Slice 5 (this slice)

- [x] T-CTL-001 — `ProfilePresignedUrlController` (`@PostMapping /profiles/presigned-url`).
- [x] T-CTL-002 — `ProfileAvatarController` (`@PatchMapping /profiles/me/avatar`).
- [x] T-CTL-004a — `ProfilePresignedUrlControllerWebMvcTest` (4 cases) + `ProfileAvatarControllerWebMvcTest` (4 cases).

## Files added (PR Slice 5)

### Source
- `src/main/java/alberto/cruz/tiendauniapi/presentation/controller/ProfilePresignedUrlController.java`
- `src/main/java/alberto/cruz/tiendauniapi/presentation/controller/ProfileAvatarController.java`

### Tests
- `src/test/java/alberto/cruz/tiendauniapi/presentation/controller/ProfilePresignedUrlControllerWebMvcTest.java` (4 tests)
- `src/test/java/alberto/cruz/tiendauniapi/presentation/controller/ProfileAvatarControllerWebMvcTest.java` (4 tests)

## TDD cycle evidence (strict mode)

| Cycle | RED command | GREEN command | Evidence |
|---|---|---|---|
| T-CTL-001 + T-CTL-004a (profile presigned URL) | `./gradlew test --tests "*ProfilePresignedUrlControllerWebMvcTest"` fails with `cannot find symbol: class ProfilePresignedUrlController` | added `ProfilePresignedUrlController.java`; same command green | 4/4 tests pass |
| T-CTL-002 (profile avatar) | `./gradlew test --tests "*ProfileAvatarControllerWebMvcTest"` fails with `cannot find symbol: class ProfileAvatarController` | added `ProfileAvatarController.java`; same command green | 4/4 tests pass |
| Full suite | n/a | `./gradlew test --rerun-tasks` green | 46 tests, 0 failures across 11 suites |

## Deviations from prompt / design

- **`@WebMvcTest` package path**: Spring Boot 4.1.1 moved `@WebMvcTest` from
  `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` to
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`. Used the new path.
- **ObjectMapper package**: Spring Boot 4 ships Jackson 3 under `tools.jackson.databind`,
  not `com.fasterxml.jackson.databind`. Used `tools.jackson.databind.ObjectMapper`
  for request body serialization in tests.
- **Security wiring in test**: `@WebMvcTest` slice excludes production security beans
  (`UserDetailsService`, CORS source). Instead of importing the full `SecurityConfiguration`
  (which fails on missing dependencies), each test class embeds a `@TestConfiguration`
  with a `SecurityFilterChain` bean annotated `@EnableWebSecurity` that mirrors the
  production authorization rules and adds `.httpBasic(...)` so the default entry
  point returns 401 for unauthenticated requests (matching the prompt's expectation).
  This satisfies AC-PROF-5 and AC-AVATAR-5 without making the test depend on the DB.
- **No MockitoBean for `S3Presigner` beans**: despite the `tasks.md` note, the slice
  does not require `S3Presigner` because `PresignedUrlService` is mocked at the
  controller boundary (`@MockitoBean PresignedUrlService`). Confirmed by green test.

## Verification

- `./gradlew compileJava` — green.
- `./gradlew test --tests "*ProfilePresignedUrlControllerWebMvcTest"` — green.
- `./gradlew test --tests "*ProfileAvatarControllerWebMvcTest"` — green.
- `./gradlew test --rerun-tasks` — green, 46 tests across 11 suites, 0 failures.
- `git status` — only user's pre-existing uncommitted changes
  (`build.gradle`, `SecurityConfiguration.java`) and user's untracked files
  (`aws-s3.md`, `openspec/changes/`) remain outside the commits. No new untracked files
  from this slice.

## Untouched (per user instructions)

- `build.gradle` (user-added AWS SDK dep).
- `src/main/java/alberto/cruz/tiendauniapi/configuration/SecurityConfiguration.java` (user EOF newline).
- `aws-s3.md`, `openspec/changes/` (user-managed except for tasks checkboxes + apply-progress).

## Next recommended

Parent-lifecycle: route PR slice 5 to its own commit chain or merge to base, then
continue with PR Slice 6 (publication endpoint + exception handlers).
