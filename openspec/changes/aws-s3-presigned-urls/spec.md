# Especificación — aws-s3-presigned-urls

## Resumen

Esta especificación formaliza el comportamiento observable del cambio `aws-s3-presigned-urls`: la generación de URLs pre-firmadas de AWS S3 (5 minutos de expiración) para que el cliente suba avatares y media de publicaciones **directo al bucket**, sin pasar bytes por el backend. El cambio introduce también el endpoint `PATCH /profiles/me/avatar` que persiste la `key` del avatar en `users.avatar_url`. Quedan fuera de alcance (ver `proposal.md`): la creación efectiva de la publicación (`POST /posts`), el mapeo JPA de `publications` / `publication_media`, la limpieza de objetos huérfanos en el bucket y los endpoints `GET` pre-firmados de lectura.

La propuesta aprobada (ver `proposal.md`) define tres endpoints HTTP, dos validadores custom (`@ValidFileSize`, `@UniqueFileIds`), una excepción dedicada (`PresignedUrlGenerationException`) con handler 503, el primer `@ConfigurationProperties` del proyecto (`AwsS3Properties`), un servicio único `PresignedUrlService` agnóstico al bucket que recibe `BucketTarget` por argumento, y la decisión explícita de **usar `endpointOverride`** en el `S3Presigner` para que migrar a Cloudflare R2 u otro proveedor S3-compatible sea un cambio de variables de entorno sin recompilar.

Todos los criterios aquí listados son verificables mediante `./gradlew test` (con `Mockito` para el `S3Presigner` cuando se necesite) o inspección manual del binario en arranque. No se añade dependencia nueva: `software.amazon.awssdk:s3:2.54.3` ya está declarado en `build.gradle`.

## Criterios de aceptación

Lista numerada, cada criterio MEDIBLE y verificable por test automatizado o inspección manual. Agrupados por endpoint / comportamiento.

### Configuración y arranque

- **AC-CFG-1** — La aplicación SHALL enlazar las siguientes propiedades bajo el prefijo `app.aws.s3.*` desde `application.yaml` / `application-dev.yaml` / `application-prod.yaml` y, en producción, desde las variables de entorno indicadas:

  | Propiedad YAML | Variable de entorno (prod) |
  |---|---|
  | `app.aws.s3.access-key-id` | `AWS_ACCESS_KEY_ID` |
  | `app.aws.s3.secret-access-key` | `AWS_SECRET_ACCESS_KEY` |
  | `app.aws.s3.region` | `AWS_REGION` |
      | `app.aws.s3.endpoint` | `AWS_ENDPOINT` |
  | `app.aws.s3.bucket-profile-name` | `AWS_BUCKET_PROFILE_NAME` |
  | `app.aws.s3.bucket-publication-name` | `AWS_BUCKET_PUBLICATION_NAME` |
  | `app.aws.s3.bucket-profile-url` | `AWS_BUCKET_PROFILE_URL` |
  | `app.aws.s3.bucket-publication-url` | `AWS_BUCKET_PUBLICATION_URL` |

- **AC-CFG-2** — La aplicación SHALL fallar al arrancar (`ApplicationContextException` o equivalente) si cualquiera de las ocho propiedades listadas en AC-CFG-1 está ausente o vacía en el profile activo. Verificable con `./gradlew test` levantando el contexto sin una prop: el test SHALL esperar `ContextLoadException` o equivalente.

- **AC-CFG-3** — Todos los beans `S3Presigner` SHALL aplicar `endpointOverride` con el valor de la propiedad `app.aws.s3.endpoint` (URL del provider — AWS S3 o Cloudflare R2). El bucket se identifica por separado vía `PutObjectRequest.bucket(...)`, no a través del endpoint. Verificable mediante test que intercepta el bean construido y asserta que su `endpointOverride().toString()` coincide con la prop cargada. Si la decisión de `design` produce un único bean, este criterio aplica al bean activo por bucket; si produce dos beans (`profileS3Presigner`, `publicationS3Presigner`), ambos SHALL compartir el mismo `endpointOverride` (el del provider).

- **AC-CFG-4** — Las props `app.aws.s3.bucket-profile-url` y `app.aws.s3.bucket-publication-url` SHALL ser la base para componer la URL pública de lectura (`bucketProfileUrl + key` o `bucketPublicationUrl + key`) que el cliente usará para `<img>` / `<video>`. La firma de URLs pre-firmadas NO usa estas props (usa `app.aws.s3.endpoint`). Verificable por inspección: el `S3Presigner` recibe `properties.endpoint()` como endpoint override, y `BucketTarget.resolveBucketUrl(...)` queda disponible para que un futuro composer de respuestas arme la URL pública.

- **AC-CFG-5** — `AwsS3Properties` SHALL ser un `record` anotado con `@ConfigurationProperties(prefix = "app.aws.s3")` y SHALL ser el primer `@ConfigurationProperties` del proyecto. Verificable por inspección: `grep -R "@ConfigurationProperties" src/main` SHALL retornar exactamente `AwsS3Properties`.

### POST /profiles/presigned-url

- **AC-PROF-1** — `POST /profiles/presigned-url` SHALL devolver `200 OK` con cuerpo `{"url": "<string>"}` cuando el request cumple todas las validaciones (`fileName` no vacío y regex `^[a-zA-Z0-9_-]+$`, `size` dentro del cap según `mimeType`, `mimeType` en whitelist) y el `S3Presigner` genera la URL sin error. Verificable con `@WebMvcTest` + `MockMvc` y `S3Presigner` mockeado.

- **AC-PROF-2** — La `url` devuelta SHALL tener como host un valor que **contenga** el host del `app.aws.s3.endpoint` (provider endpoint — ej. `https://<bucket>.s3.localhost.localstack.cloud` con virtual-hosted style, o `https://s3.localhost.localstack.cloud/<bucket>/<key>` con path-style) y SHALL incluir al menos un parámetro de query que comience con `X-Amz-` (firma AWS). Verificable parseando la URL de la respuesta: `URI.create(url).getHost()` SHALL `.contains(URI.create(endpoint).getHost())`.

- **AC-PROF-3** — El endpoint SHALL rechazar requests sin cookie `access-token` válida con `401 Unauthorized`. Verificable con `MockMvc` sin setear la cookie.

- **AC-PROF-4** — El endpoint SHALL rechazar requests con `Content-Type: application/json` válido pero cuerpo `{}` o ausente con `400 Bad Request` y un `ProblemDetail` cuyo `type` siga la convención existente (`https://tiendauniapi.com/problems/...`) gestionado por `GlobalExceptionHandler`.

- **AC-PROF-5** — La URL generada SHALL tener expiración fija de **5 minutos** (300 000 ms). No SHALL existir propiedad `app.aws.s3.presigned-url-expiration` en este cambio.

### PATCH /profiles/me/avatar

- **AC-AVATAR-1** — `PATCH /profiles/me/avatar` SHALL devolver `204 No Content` (sin cuerpo) cuando la `key` cumple el patrón `profiles/<uuid>/<uuid>.<ext>` y la actualización de `users.avatar_url` persiste correctamente.

- **AC-AVATAR-2** — El endpoint SHALL identificar al usuario objetivo exclusivamente desde `@AuthenticationPrincipal AuthenticatedUser.userId`. No SHALL aceptar `userId` por path, query ni body; cualquier `userId` enviado en el cuerpo SHALL ser ignorado. Verificable por inspección de la firma del controller.

- **AC-AVATAR-3** — El endpoint SHALL persistir la `key` (cadena exacta recibida en el body) en la columna `users.avatar_url` (VARCHAR(300) NOT NULL). SHALL **nunca** almacenar la URL completa ni `null`. Verificable con test de integración que captura la entidad tras el `PATCH` y asserte `entity.getAvatarUrl()`.

- **AC-AVATAR-4** — `users.avatar_url` SHALL pasar de un valor previo `v_old` a `v_new` cuando el mismo usuario llama al endpoint dos veces con keys distintas; SHALL NO borrar ni modificar el objeto `v_old` en S3 (política leave-it, fuera de alcance la limpieza).

- **AC-AVATAR-5** — El endpoint SHALL rechazar requests sin cookie `access-token` válida con `401`.

- **AC-AVATAR-6** — El endpoint SHALL devolver `400` cuando la `key` no cumple el patrón estricto `^profiles/[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}\\.[a-zA-Z0-9]+$`. Verificable con `MockMvc` enviando cada key inválida listada en "Datos de prueba".

### POST /posts/presigned-url

- **AC-POST-1** — `POST /posts/presigned-url` SHALL devolver `200 OK` con cuerpo `{"uris": [...]}` donde cada elemento contiene `id`, `url` y `key`. El array SHALL preservar el orden exacto del array `files` de entrada (id por id).

- **AC-POST-2** — El endpoint SHALL aceptar entre 1 y 10 elementos en `files`. Con 0 elementos SHALL responder `400` (`@NotEmpty`). Con > 10 elementos SHALL responder `400` vía `@Size(max = 10)`.

- **AC-POST-3** — El endpoint SHALL rechazar el request con `400` si dos o más elementos de `files` comparten el mismo `id` (constraint `@UniqueFileIds`). El mensaje del `ProblemDetail` SHALL incluir los ids conflictivos.

- **AC-POST-4** — Las `url` devueltas SHALL tener como host un valor que contenga el host del `app.aws.s3.endpoint` (provider endpoint) y SHALL incluir `X-Amz-*` en los query params. La `key` SHALL comenzar con `publications/` (prefijo distinto al de avatar). Verificable: `URI.create(url).getHost()` SHALL `.contains(URI.create(endpoint).getHost())`.

- **AC-POST-5** — El endpoint SHALL rechazar requests sin cookie `access-token` válida con `401`.

- **AC-POST-6** — El endpoint SHALL poder generar N URLs en una sola llamada (N = `files.length`), cada una con su propia `key` única (UUID independiente). Verificable con test que envía `files.length = 3` y asserta `uris.length = 3` con `key`s distintas.

### Validaciones

- **AC-VAL-1** — La anotación custom `@ValidFileSize` SHALL rechazar con `400` cualquier request cuyo `size` exceda **10 485 760 bytes** (10 MB) cuando `mimeType` empieza con `image/` (incluido `image/svg+xml`).

- **AC-VAL-2** — La anotación `@ValidFileSize` SHALL rechazar con `400` cualquier request cuyo `size` exceda **52 428 800 bytes** (50 MB) cuando `mimeType` empieza con `video/`.

- **AC-VAL-3** — `@ValidFileSize` SHALL aceptar `mimeType = image/svg+xml` bajo el cap de imagen (10 MB), NO bajo el de video. Verificable con test que envía `mimeType = "image/svg+xml"`, `size = 11_000_000` y espera `400`.

- **AC-VAL-4** — El validador de `mimeType` SHALL rechazar con `400` cualquier valor que no esté en la whitelist: `image/jpeg`, `image/jpg`, `image/png`, `image/gif`, `image/webp`, `image/svg+xml`, `video/mp4`, `video/webm`, `video/ogg`. Los mimeTypes `text/plain`, `application/json` e `image/bmp` SHALL ser rechazados.

- **AC-VAL-5** — El campo `fileName` SHALL cumplir regex `^[a-zA-Z0-9_-]+$`, SHALL no ser `null`, no ser vacío y SHALL tener longitud máxima 255. Valores como `""`, `"con espacio"`, `"con.punto"`, `"../path"` SHALL ser rechazados con `400`.

- **AC-VAL-6** — `@UniqueFileIds` SHALL rechazar con `400` cuando dos o más elementos del array `files` comparten el mismo campo `id`. El `id` SHALL ser cualquier string no vacío (no UUID forzado).

- **AC-VAL-7** — En `PATCH /profiles/me/avatar`, la `key` SHALL validarse con `@Pattern` cuyo regex rechaza `../escape`, `random-string`, strings vacíos y `null`. Sólo SHALL aceptarse `profiles/<uuid>/<uuid>.<ext>`.

- **AC-VAL-8** — El array `files` de `POST /posts/presigned-url` SHALL ser `@NotEmpty` y `@Size(max = 10)`.

- **AC-VAL-9** — Todos los criterios de validación (AC-VAL-1 a AC-VAL-8) SHALL ser rechazados **antes** de invocar al `S3Presigner`. Verificable con un test que mockea `S3Presigner.presignPutObject(...)`, verifica `verifyNoInteractions(presigner)` y aún así recibe `400`.

### Errores y observabilidad

- **AC-ERR-1** — Cuando `S3Presigner.presignPutObject(...)` lance cualquier excepción (`SdkException` o subclase), el servicio SHALL envolverla en `PresignedUrlGenerationException`. El handler dedicado SHALL mapearla a `503 Service Unavailable` con el siguiente `ProblemDetail`:
  - `type`: `https://tiendauniapi.com/problems/presigned-url-generation-failed`
  - `title`: `"Presigned Url Generation Failed"`
  - `detail`: `"No se pudo generar la URL pre-firmada para subir el archivo. Intenta nuevamente en unos momentos."`

- **AC-ERR-2** — `MethodArgumentNotValidException` (validación Jakarta de los DTOs Request) SHALL ser manejada por `GlobalExceptionHandler` existente y SHALL devolver `400` con un `ProblemDetail` cuyo body contenga un mapa de `IncorrectField` con los campos fallidos (no se duplica lógica en un nuevo advice).

- **AC-ERR-3** — `ConstraintViolationException` lanzada por `@UniqueFileIds` SHALL ser manejada por `GlobalExceptionHandler` existente y SHALL devolver `400` con `ProblemDetail` que incluya los ids duplicados en `detail`.

- **AC-ERR-4** — Si el usuario autenticado不存在 (token con `userId` que no existe en `users`) al hacer `PATCH /profiles/me/avatar`, SHALL lanzarse `UserNotFoundException` y SHALL mapearse a `404 Not Found` vía `AuthenticationExceptionHandler` (handler existente) o uno nuevo si se decide en `design`. La política concreta (reusar handler existente vs nuevo) la cierra `design`; el contrato observable es `404` con `ProblemDetail` consistente.

- **AC-ERR-5** — Falla de integridad de datos al persistir el avatar (`DataIntegrityViolationException`) SHALL mapearse a `500` vía `GlobalExceptionHandler` existente, sin handler nuevo.

- **AC-OBS-1** — El log de la aplicación SHALL contener **warnings y errors** relativos a pre-signed URLs (e.g. `PresignedUrlGenerationException` con stacktrace, advertencias de configuración), pero SHALL **NO** loggear URLs exitosas (ni siquiera a nivel DEBUG/TRACE en producción). Verificable con test que captura el output del logger y asserta que ningún mensaje contiene la URL firmada.

- **AC-OBS-2** — Los mensajes de log SHALL redactar cualquier credencial AWS. Si por error se loggea una excepción de `S3Presigner`, el mensaje SHALL pasar por el filtro de redacción existente o, si no existe, SHALL documentarse como follow-up.

### Migración a Cloudflare R2

- **AC-MIG-1** — Cambiar `AWS_ENDPOINT` y `AWS_REGION` en variables de entorno SHALL hacer que el `S3Presigner` firme contra el nuevo endpoint **sin recompilar** el código Java. Verificable manualmente: configurar las dos env vars apuntando a un endpoint R2 (`https://<accountid>.r2.cloudflarestorage.com`), levantar la app, llamar a `POST /profiles/presigned-url` y observar que la `url` retornada tiene como host el endpoint R2. Las URLs de bucket (`AWS_BUCKET_PROFILE_URL`, `AWS_BUCKET_PUBLICATION_URL`) NO afectan la firma — sólo se usan para componer la URL pública de lectura.

- **AC-MIG-2** — La firma SHALL usar las credenciales provistas por `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` independientemente del endpoint (no SHALL haber lógica condicional "si endpoint es R2 entonces…"). Verificable por inspección: una sola rama de construcción del `S3Presigner` con `AwsBasicCredentials.create(...)` + `endpointOverride(...)`.

- **AC-MIG-3** — El cambio SHALL NO introducir ningún `if (provider == R2)` en código de producción. El camino de migración es 100% variables de entorno + `application-prod.yaml`.

## Escenarios (Given/When/Then)

Uno por criterio crítico. Mínimo: 1 escenario por endpoint (feliz) + 1 escenario por edge case crítico (validación fallida, AWS fail, ids duplicados, key mal formato).

### Escenario AC-PROF-1: generación exitosa de URL para avatar
**Dado** un usuario autenticado (cookie `access-token` válida) con `userId = 4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c` y el bean `S3Presigner` mockeado que devuelve una `PresignedPutObjectRequest` cualquiera.
**Cuando** el cliente envía `POST /profiles/presigned-url` con body `{"fileName": "avatar", "size": 1024, "mimeType": "image/jpeg"}`.
**Entonces** el backend responde `200 OK` con `{"url": "<string cuyo host contiene el host del endpoint del provider y contiene X-Amz-* en el query>"}`.
**Y** el body no contiene `key` (la key se devuelve sólo en `POST /posts/presigned-url`).

### Escenario AC-PROF-3: rechazo sin autenticación
**Dado** que no existe cookie `access-token` en la request.
**Cuando** el cliente envía `POST /profiles/presigned-url` con body válido.
**Entonces** el backend responde `401 Unauthorized` antes de tocar el `S3Presigner`.
**Y** `S3Presigner.presignPutObject(...)` no se invoca (verificable con `verifyNoInteractions`).

### Escenario AC-AVATAR-1: persistencia exitosa de key de avatar
**Dado** un usuario autenticado con `userId = 4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c` cuyo `users.avatar_url` actual es `profiles/old-user/old-uuid.jpg`.
**Cuando** el cliente envía `PATCH /profiles/me/avatar` con body `{"key": "profiles/4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c/8a3e2c11-1111-2222-3333-444455556666.jpg"}`.
**Entonces** el backend responde `204 No Content` sin cuerpo.
**Y** la fila en `users` para ese `userId` queda con `avatar_url = "profiles/4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c/8a3e2c11-1111-2222-3333-444455556666.jpg"`.

### Escenario AC-AVATAR-6: rechazo de key con formato inválido
**Dado** un usuario autenticado.
**Cuando** el cliente envía `PATCH /profiles/me/avatar` con body `{"key": "../escape"}`.
**Entonces** el backend responde `400 Bad Request` con `ProblemDetail` cuyo `detail` menciona que el formato es inválido.
**Y** `users.avatar_url` no se modifica.

### Escenario AC-AVATAR-2: rechazo de userId externo en body
**Dado** un usuario autenticado con `userId = 4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c`.
**Cuando** el cliente envía `PATCH /profiles/me/avatar` con body `{"userId": "otro-uuid", "key": "profiles/4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c/8a3e2c11-1111-2222-3333-444455556666.jpg"}`.
**Entonces** el backend responde `204` y la fila actualizada corresponde al `userId` del principal (`4f1c…`), no al del body.
**Y** el campo `userId` del body es ignorado silenciosamente (no produce error).

### Escenario AC-POST-1: generación exitosa de N URLs para publicación
**Dado** un usuario autenticado y `S3Presigner` mockeado.
**Cuando** el cliente envía `POST /posts/presigned-url` con `files` de 3 elementos: `[{"id": "file1", "fileName": "demo-front", "size": 1024, "mimeType": "image/jpeg"}, {"id": "7194b889-868c-47c8-8431-4cb4464a15a4", "fileName": "demo", "size": 5242880, "mimeType": "video/mp4"}, {"id": "file3", "fileName": "test_123", "size": 2048, "mimeType": "image/png"}]`.
**Entonces** el backend responde `200 OK` con `{"uris": [...]}` de exactamente 3 elementos.
**Y** el orden de `uris` coincide 1-a-1 con el orden de `files` por `id`.
**Y** cada `key` es única (UUID distinto por elemento) y empieza con `publications/`.

### Escenario AC-POST-3: rechazo por ids duplicados
**Dado** un usuario autenticado.
**Cuando** el cliente envía `POST /posts/presigned-url` con `files` que contiene dos elementos con `id = "file1"`.
**Entonces** el backend responde `400 Bad Request` con `ProblemDetail` cuyo `detail` lista `"file1"` como id conflictivo.
**Y** `S3Presigner` no se invoca.

### Escenario AC-POST-2: rechazo por más de 10 archivos
**Dado** un usuario autenticado.
**Cuando** el cliente envía `POST /posts/presigned-url` con `files.length = 11`.
**Entonces** el backend responde `400 Bad Request` con detalle mencionando el máximo de 10.

### Escenario AC-VAL-1: rechazo por size > 10 MB en imagen
**Dado** un usuario autenticado.
**Cuando** el cliente envía `POST /profiles/presigned-url` con `{"fileName": "big", "size": 10485761, "mimeType": "image/jpeg"}`.
**Entonces** el backend responde `400 Bad Request` con detalle indicando que el máximo para `image/*` es 10 MB.
**Y** `S3Presigner` no se invoca.

### Escenario AC-VAL-4: rechazo por mimeType no permitido
**Dado** un usuario autenticado.
**Cuando** el cliente envía `POST /profiles/presigned-url` con `{"fileName": "doc", "size": 1024, "mimeType": "text/plain"}`.
**Entonces** el backend responde `400 Bad Request` con detalle listando los mimeTypes válidos.
**Y** `S3Presigner` no se invoca.

### Escenario AC-ERR-1: fallo de AWS → 503
**Dado** un usuario autenticado y `S3Presigner` mockeado que lanza `SdkException("AWS caído")` al llamar `presignPutObject(...)`.
**Cuando** el cliente envía `POST /profiles/presigned-url` con body válido.
**Entonces** el backend responde `503 Service Unavailable` con `ProblemDetail` cuyo `type = "https://tiendauniapi.com/problems/presigned-url-generation-failed"` y `title = "Presigned Url Generation Failed"`.
**Y** se loggea un `ERROR` con la stacktrace (no la URL generada).

### Escenario AC-CFG-2: arranque falla si falta variable AWS
**Dado** un profile activo sin `AWS_BUCKET_PROFILE_URL` configurada.
**Cuando** se levanta el contexto de Spring.
**Entonces** el arranque SHALL lanzar una excepción de tipo `ConfigurationPropertiesBindException` (o equivalente) y la app SHALL NO quedar en estado "ready".
**Y** el log SHALL indicar qué propiedad falta.

### Escenario AC-MIG-1: migración a Cloudflare R2 sin recompilar
**Dado** un entorno con `AWS_ENDPOINT = "https://accountid.r2.cloudflarestorage.com"`, `AWS_REGION = "auto"` y credenciales R2.
**Cuando** el cliente envía `POST /profiles/presigned-url` con body válido.
**Entonces** la `url` retornada tiene como host un valor que contiene `accountid.r2.cloudflarestorage.com` (no `s3.amazonaws.com`).
**Y** ningún cambio en código fuente fue necesario entre esta ejecución y la anterior con AWS nativo.

## Contratos (referencia)

Resumen de request/response de los 3 endpoints. El detalle completo está en `proposal.md` sección "Endpoints (contratos exactos)".

### POST /profiles/presigned-url

Request:
```json
{ "fileName": "avatar", "size": 102400, "mimeType": "image/jpeg" }
```

Response 200:
```json
{ "url": "https://<bucket>.<endpoint-host>/profiles/<uuid>/<uuid>.jpg?X-Amz-..." }
```

Errores: `400` validación · `401` no autenticado · `503` fallo AWS.

### PATCH /profiles/me/avatar

Request:
```json
{ "key": "profiles/4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c/8a3e2c11-1111-2222-3333-444455556666.jpg" }
```

Response: `204 No Content` (sin cuerpo).

Errores: `400` key con formato inválido · `401` no autenticado · `404` usuario no existe · `500` fallo de DB.

### POST /posts/presigned-url

Request:
```json
{
  "files": [
    { "id": "file1", "fileName": "demo-front", "size": 102400, "mimeType": "image/jpeg" },
    { "id": "7194b889-868c-47c8-8431-4cb4464a15a4", "fileName": "demo", "size": 5242880, "mimeType": "video/mp4" }
  ]
}
```

Response 200:
```json
{
  "uris": [
    { "id": "file1", "url": "https://<bucket>.<endpoint-host>/publications/<uuid>/<uuid>.jpg?X-Amz-...", "key": "publications/<uuid>/<uuid>.jpg" },
    { "id": "7194b889-868c-47c8-8431-4cb4464a15a4", "url": "https://...", "key": "publications/<uuid>/<uuid>.mp4" }
  ]
}
```

Errores: `400` validación (mime, size, fileName, ids duplicados, >10 archivos, array vacío) · `401` no autenticado · `503` fallo AWS.

## Datos de prueba

Valores concretos que los tests usarán:

### fileName

| Caso | Valor | Resultado esperado |
|---|---|---|
| válido | `"avatar"` | aceptado |
| válido | `"demo-front"` | aceptado |
| válido | `"test_123"` | aceptado |
| inválido | `""` (vacío) | `400` |
| inválido | `"con espacio"` | `400` |
| inválido | `"con.punto"` | `400` |
| inválido | `"../path"` | `400` |
| inválido | `null` | `400` |

### mimeType

| Caso | Valor | Resultado esperado |
|---|---|---|
| válido | `image/jpeg` | aceptado (cap imagen) |
| válido | `image/png`, `image/gif`, `image/webp` | aceptado |
| válido | `image/svg+xml` | aceptado (cap imagen, NO video) |
| válido | `video/mp4`, `video/webm`, `video/ogg` | aceptado (cap video) |
| inválido | `text/plain` | `400` |
| inválido | `application/json` | `400` |
| inválido | `image/bmp` | `400` (no está en whitelist) |

### size (en bytes)

| Caso | mimeType | size | Resultado esperado |
|---|---|---|---|
| válido imagen | `image/jpeg` | `1024` (1 KB) | aceptado |
| válido video | `video/mp4` | `5242880` (5 MB) | aceptado |
| inválido imagen | `image/jpeg` | `10485761` (10 MB + 1) | `400` |
| inválido video | `video/mp4` | `52428801` (50 MB + 1) | `400` |
| borde imagen | `image/jpeg` | `10485760` (10 MB exacto) | aceptado |
| borde video | `video/mp4` | `52428800` (50 MB exacto) | aceptado |
| svg sobre cap imagen | `image/svg+xml` | `11000000` (~10.5 MB) | `400` (cap imagen) |

### Identificadores

- **userId (UUID)**: `4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c`
- **id de publication**: `"file1"`, `"7194b889-868c-47c8-8431-4cb4464a15a4"` (cualquier string no vacío, no se fuerza UUID)

### Keys para PATCH /profiles/me/avatar

| Caso | Valor | Resultado esperado |
|---|---|---|
| válida | `"profiles/4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c/8a3e2c11-1111-2222-3333-444455556666.jpg"` | aceptado |
| inválida | `"../escape"` | `400` |
| inválida | `"random-string"` | `400` |
| inválida | `""` | `400` |
| inválida | `null` | `400` |
| inválida | `"posts/uuid/uuid.jpg"` (prefijo incorrecto) | `400` |

## Restricciones y reglas (referencia)

Lista corta de las reglas de negocio más críticas; referencia completa en `proposal.md` sección "Reglas de negocio".

- Whitelist MIME hardcodeada (no configurable en este cambio): `image/jpeg`, `image/jpg`, `image/png`, `image/gif`, `image/webp`, `image/svg+xml`, `video/mp4`, `video/webm`, `video/ogg`.
- `image/svg+xml` cuenta como imagen para el cap de tamaño.
- Caps: `image/*` ≤ 10 MB · `video/*` ≤ 50 MB.
- `fileName` regex `^[a-zA-Z0-9_-]+$`, longitud máx 255.
- Máximo 10 archivos por publicación.
- Expiración fija de 5 minutos (no configurable).
- `users.avatar_url` siempre recibe la `key` relativa, nunca la URL completa ni `null`.
- Key naming folderizado: `profiles/<userId>/<uuid>.<ext>` para avatar, `publications/<userId>/<uuid>.<ext>` para media.
- `<ext>` derivado de `mimeType` (mapeo en `enum` interno).
- Bucket público para lectura; el frontend compone URL pública como `bucketUrl + key`.
- Cambio de avatar: sólo el propio usuario, `userId` siempre desde `@AuthenticationPrincipal`.
- No se introduce `multipart/form-data`.
- No se loggean URLs exitosas.

## Dependencias entre criterios

- AC-PROF-1, AC-PROF-2 dependen de **AC-CFG-1 y AC-CFG-3** (sin config válida no se puede generar URL).
- AC-AVATAR-1, AC-AVATAR-3 dependen de **AC-CFG-1** (la persistencia requiere el contexto Spring cargado).
- AC-POST-1 depende de **AC-CFG-1, AC-CFG-3**.
- AC-VAL-1, AC-VAL-2, AC-VAL-3, AC-VAL-4, AC-VAL-5, AC-VAL-6, AC-VAL-7, AC-VAL-8 dependen del validador custom registrado en el contexto Spring (parte de AC-CFG-1).
- AC-ERR-1 depende de **AC-PROF-1** (necesita que `S3Presigner` se haya intentado invocar para que lance).
- AC-MIG-1 depende de **AC-CFG-3** (`endpointOverride` aplicado).
- AC-OBS-1 depende de la implementación del logger configurado en `application*.yaml` (no se verifica como parte de este cambio pero debe quedar explícito en código).

## Out of scope (referencia)

Referencia completa en `proposal.md` sección "Fuera de alcance (Non-Goals)". Resumen:

- `POST /posts` (creación efectiva de publicación, persistencia de `publications` y `publication_media`).
- Mapeo JPA de `publications` y `publication_media` (las tablas SQL existen; las entidades JPA no se crean en este cambio).
- Endpoint `GET` pre-firmado para lectura de assets (el bucket es público).
- Limpieza asíncrona de objetos huérfanos en S3 (política leave-it).
- Lifecycle rules, IAM policies, S3 CORS en el bucket (infraestructura, no código).
- Migración efectiva de AWS S3 a Cloudflare R2 (sí se construye el camino: `endpointOverride` + env vars; no se ejecuta la migración en este cambio).
- Soporte de `multipart/form-data` en el backend.
- Cualquier tipo de validación por bytes del archivo subido (la validación real de tamaño se delega a S3 vía el `Content-Length` firmado en la URL pre-firmada).
