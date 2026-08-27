# Propuesta — aws-s3-presigned-urls

## Resumen ejecutivo

Esta propuesta introduce la generación de **URLs pre-firmadas de S3** en `TiendaUniAPI` para que el frontend suba los assets binarios (avatares de usuario y media de publicaciones) directamente al bucket, sin que el backend reciba nunca los bytes. El backend se reduce a validar metadatos (`fileName`, `size`, `mimeType`), firmar la URL con expiración de 5 minutos y, opcionalmente, persistir la `key` resultante en el usuario.

El cambio cubre dos consumidores: el avatar del usuario autenticado y la media asociada a publicaciones (sólo la generación de URLs, **no** la creación de la publicación). Se reutiliza el SDK AWS v2 ya declarado en `build.gradle` (`software.amazon.awssdk:s3:2.54.3`), se introduce el primer `@ConfigurationProperties` del proyecto (`AwsS3Properties`) y se añade una excepción específica (`PresignedUrlGenerationException`) con handler dedicado para distinguir fallos del proveedor (503) de bugs del cliente (400).

**Decisión clave**: existe un único servicio `PresignedUrlService` agnóstico al bucket. Los dos controladores (`ProfilePresignedUrlController`, `PublicationPresignedUrlController`) hardcodean el `BucketTarget` correspondiente al llamar al servicio. Esto evita duplicación de lógica de firma/validación/keys entre dos servicios. El cambio a otro proveedor S3-compatible (Cloudflare R2, MinIO, etc.) se hace tocando una sola clase de configuración, ya que `endpointOverride` se aplica desde `AwsS3Properties`.

## Problema

Hoy, cuando `TiendaUniAPI` necesite soportar subida de avatares o media de publicaciones, no existe un canal definido: la API no expone endpoints para `multipart/form-data` ni integra AWS. Si la primera implementación se hiciera con upload a través del backend, aparecerían tres problemas reales:

- **Costo de recursos**: recibir binarios (imágenes hasta 10 MB y videos hasta 50 MB) en el backend aumenta latencia, consumo de memoria y ancho de banda, y exige endpoints `multipart` con manejo de streams y limpieza de temporales.
- **Complejidad técnica**: implementar `multipart/form-data` correctamente (parsers, límites, streaming, validaciones de MIME reales en bytes y no sólo en cabecera) es trabajo significativo y propenso a bugs.
- **Imposibilidad de optimizaciones en cliente**: el frontend no puede recomprimir, redimensionar ni convertir formatos antes de subir, porque los bytes llegarían crudos al backend.

Si no se hace nada, el siguiente PR que pida avatares acabará improvisando un endpoint de subida directa, acoplando el backend al `MultipartFile` y arrastrando la deuda.

## Usuarios / situaciones objetivo

- **Estudiante vendedor / comprador autenticado**: en el flujo de edición de perfil necesita cambiar su avatar y, en el flujo de creación de publicación, adjuntar hasta 10 imágenes y/o videos. Hoy no puede hacerlo.
- **Frontend (cliente SPA)**: necesita un endpoint liviano que devuelva una URL pre-firmada para subir directo a S3, sin tener que negociar credenciales AWS ni firmar peticiones él mismo.
- **Equipo backend**: necesita un punto de extensión claro para añadir futuros buckets (por ejemplo, media de chat) sin reescribir la lógica de validación/firma.

## Outcome del producto

Tras este cambio, el frontend puede:

1. Pedir `POST /profiles/presigned-url` con `{ fileName, size, mimeType }` y obtener una URL pre-firmada de 5 minutos para subir un avatar directo al bucket de perfiles.
2. Hacer `PUT` del archivo a esa URL y, una vez subido, llamar a `PATCH /profiles/me/avatar` con `{ key }` para que el backend persista la referencia (sólo `users.avatar_url`).
3. Pedir `POST /posts/presigned-url` con un array de archivos (cada uno con `id` cliente, `fileName`, `size`, `mimeType`) y recibir `uris: [{ id, url, key }]` listos para subir al bucket de publicaciones en paralelo y conservar el orden que el usuario eligió.

El backend nunca recibe bytes; sólo metadatos validados y, eventualmente, una `key` de S3.

## Alcance (In-Scope)

- `POST /profiles/presigned-url` — genera URL pre-firmada para un único archivo de avatar.
- `PATCH /profiles/me/avatar` — persiste la `key` del nuevo avatar en `users.avatar_url` para el usuario autenticado.
- `POST /posts/presigned-url` — genera URLs pre-firmadas para uno o más archivos de media de publicación (sin creación de la publicación).
- Configuración de AWS S3: `@ConfigurationProperties` (`AwsS3Properties`), bean `S3Presigner` con `StaticCredentialsProvider`, lectura desde `application*.yaml` y variables de entorno en `prod`.
- Validadores custom: `@ValidFileSize` (image ≤ 10 MB, video ≤ 50 MB, SVG cuenta como imagen), `@UniqueFileIds` (sólo en request de publicación).
- Excepción `PresignedUrlGenerationException` + handler específico en `presentation/advice/` que mapea a 503.
- Servicios: separación clara entre perfil y publicación (decisión final de nombres y empaquetado queda para la fase de `design`).
- Pruebas mínimas: `contextLoads()` ampliado, validación rechaza mime/size/name inválidos, servicio firma URL con los parámetros esperados (puede mockearse `S3Presigner`).

## Fuera de alcance (Non-Goals)

- `POST /posts` (creación de publicación, persistencia de `publications` y `publication_media`).
- Mapeo JPA de `publications` y `publication_media` (las tablas existen en SQL pero no como entidades; no se modifican en este cambio).
- Endpoint de `GET` pre-signed URL para lectura de assets (el bucket será público; el cliente compone la URL con `bucket-profile-url` / `bucket-publication-url` + `key`).
- Limpieza asíncrona de archivos huérfanos en S3 (avatar anterior, archivos subidos pero nunca referenciados). Política: dejar el objeto en el bucket.
- Lifecycle rules, IAM policies, S3 CORS en el bucket — son configuración de infraestructura, no código Spring.
- Migración de un proveedor de almacenamiento a otro. Se almacena sólo la `key` para permitirlo en el futuro. **Sí** se construye el camino: `endpointOverride` + variables de entorno permiten migrar a Cloudflare R2 sin tocar código (sólo variables de entorno y región), pero no se incluye la migración en sí misma como trabajo de este cambio.
- Soporte de `multipart/form-data` en el backend.

## Reglas de negocio

- **Whitelist MIME** (hardcodeada en el validador, no configurable):
  `image/jpeg`, `image/jpg`, `image/png`, `image/gif`, `image/webp`, `image/svg+xml`, `video/mp4`, `video/webm`, `video/ogg`.
- **Tamaño máximo por tipo** (decidido por prefijo MIME, vía `@ValidFileSize`):
  - `image/*` → ≤ 10 MB (10 485 760 bytes).
  - `video/*` → ≤ 50 MB (52 428 800 bytes).
  - `image/svg+xml` se considera imagen.
- **File name**: regex `^[a-zA-Z0-9_-]+$`, no nulo, no vacío. Sin espacios, sin puntos, sin extensión en el campo.
- **Cantidad de archivos por publicación**: máximo 10 elementos en el array `files`. Por encima: 400.
- **Ids únicos dentro del mismo request** (publicación): el campo `id` lo provee el cliente y se devuelve en la respuesta para preservar el orden. Si hay duplicados: 400 con detalle del id conflictivo (`@UniqueFileIds`).
- **Expiración URL pre-firmada**: fijo 5 minutos (`Duration.ofMinutes(5)`). No configurable en este cambio.
- **Persistencia de `avatar_url`**: se guarda la **key relativa** del bucket (ej. `profiles/<userId>/<uuid>.jpg`), nunca la URL completa. La URL pública se reconstruye en tiempo de lectura como `bucketProfileUrl + key`.
- **Key naming folderizado**:
  - Avatar: `profiles/<userId>/<uuid>.<ext>`.
  - Publication media: `publications/<userId>/<uuid>.<ext>`.
  - `<ext>` se infiere del `mimeType` (tabla simple).
- **Bucket con lectura pública habilitada**. La URL final se construye como `bucketProfileUrl + key` / `bucketPublicationUrl + key`.
- **Autorización del cambio de avatar**: sólo el usuario autenticado puede cambiar SU avatar. La `userId` se toma de `@AuthenticationPrincipal AuthenticatedUser`; no se acepta `userId` por path ni por body. Por diseño es imposible cambiar el avatar de otro usuario (no hay punto de inyección).
- **Idempotencia de keys**: NO entre requests distintos. Distintos requests producen keys distintas aunque el `fileName` coincida (la key lleva UUID). Unicidad de ids sólo dentro del mismo request, y sólo para publicaciones.
- **Migración a Cloudflare R2 (u otro S3-compatible)**: soportada sin cambio de código. Se modifica la variable de entorno `AWS_BUCKET_*_URL` y opcionalmente `AWS_REGION`. La clase de configuración ya consume la prop y la pasa como `endpointOverride` al `S3Presigner`.
- **Logging**: sólo errores y warnings. URLs firmadas con éxito NO se loggean (podrían filtrar credenciales firmadas en logs persistentes).

## Endpoints (contratos exactos)

### `POST /profiles/presigned-url`

**Request body**
```json
{ "fileName": "avatar", "size": 102400, "mimeType": "image/jpeg" }
```

**Response 200**
```json
{ "url": "https://bucket-profile.s3.amazonaws.com/profiles/<uuid>/<uuid>.jpg?X-Amz-..." }
```

**Errores**
- `400` — validación Jakarta (mime no permitido, size fuera de rango, `fileName` vacío o con caracteres inválidos).
- `401` — autenticación ausente o inválida.
- `500` — bug inesperado o falla de DB (no aplica aquí, pero queda cubierto por el advice global).
- `503` — `PresignedUrlGenerationException` (AWS caído, red, config S3 ausente).

### `PATCH /profiles/me/avatar`

**Request body**
```json
{ "key": "profiles/4f1c.../8a3e....jpg" }
```

**Response 204** — sin cuerpo.

**Errores**
- `400` — `key` nulo, vacío o con formato inválido (no empieza con `profiles/<uuid>/<uuid>.<ext>`).
- `401` — autenticación ausente o inválida.
- `500` — falla de DB al actualizar `users.avatar_url`.

### `POST /posts/presigned-url`

**Request body**
```json
{
  "files": [
    { "id": "file1", "fileName": "front", "size": 102400, "mimeType": "image/jpeg" },
    { "id": "7194b889-868c-47c8-8431-4cb4464a15a4", "fileName": "demo", "size": 204800, "mimeType": "video/mp4" }
  ]
}
```

**Response 200**
```json
{
  "uris": [
    { "id": "file1", "url": "https://bucket-publication.s3.amazonaws.com/publications/<uuid>/<uuid>.jpg?X-Amz-...", "key": "publications/<uuid>/<uuid>.jpg" },
    { "id": "7194b889-868c-47c8-8431-4cb4464a15a4", "url": "https://...", "key": "publications/<uuid>/<uuid>.mp4" }
  ]
}
```

**Errores**
- `400` — validación (mime, size, fileName, ids duplicados, > 10 archivos, array vacío).
- `401` — autenticación ausente o inválida.
- `503` — `PresignedUrlGenerationException`.

## Decisiones de arquitectura (resumen)

- **`AwsS3Properties`** como `record` anotado con `@ConfigurationProperties` (primer `@ConfigurationProperties` del proyecto). Vive en `configuration/` con `@EnableConfigurationProperties(AwsS3Properties.class)` en una nueva clase `AwsS3Configuration`.
- **Bean `S3Presigner`**: construido con `AwsBasicCredentials.create(accessKey, secretKey)` + `StaticCredentialsProvider`. **Usa `endpointOverride`** leyendo la URL del bucket desde `AwsS3Properties` (mismo valor que se usa como base para URLs públicas de lectura). Esto unifica la fuente de verdad de la URL del bucket y es **obligatorio para la migración futura a Cloudflare R2 u otro proveedor S3-compatible**.
- **Doble función de las props `bucket-profile-url`/`bucket-publication-url`**:
  1. **`endpointOverride` del `S3Presigner`** — la URL que aparece en la URL pre-firmada de subida.
  2. **Base para construir URLs públicas de lectura** — concatenada con la `key` cuando el frontend necesita mostrar el archivo.
- **Origen de la URL del bucket (para migrar fácil)**: variable de entorno (`AWS_BUCKET_PROFILE_URL`, `AWS_BUCKET_PUBLICATION_URL`) → propiedad en `application.yaml` (`app.aws.s3.bucket-profile-url`, `app.aws.s3.bucket-publication-url`) → sobreescritura por la clase `AwsS3Configuration`. Cambiar de AWS S3 a Cloudflare R2 = cambiar la variable de entorno + región; nada de código.
- **`enum BucketTarget { PROFILE, PUBLICATION }`** en `utils/` o `common/` (decisión de paquete en `design`). Mapea a `bucketProfileName` / `bucketPublicationName` Y a la URL pública del bucket correspondiente.
- **Validadores custom**:
  - `@ValidFileSize` — recibe el `mimeType` y el `size`, aplica el cap según prefijo `image/*` o `video/*`. Constantes en bytes internos al validador.
  - `@UniqueFileIds` — sólo sobre la lista `files` del request de publicación; rechaza duplicados con 400 vía `ConstraintViolationException`.
- **Excepción `PresignedUrlGenerationException`**: nueva, en `service/exception/`. Handler propio (`@RestControllerAdvice`, orden después de `GlobalExceptionHandler`) que devuelve 503 con `ProblemDetail` siguiendo la convención existente (`type` bajo `https://tiendauniapi.com/problems/presigned-url-generation-failed`).
- **Servicio único `PresignedUrlService`** (agnóstico al bucket): expone métodos que reciben `BucketTarget` como argumento y operan sobre cualquier bucket. Métodos previstos (forma final en `design`):
  - `generateProfilePresignedUrl(PresignedUrlProfileRequest request, BucketTarget target)` — perfil.
  - `generatePublicationPresignedUrls(PresignedUrlPublicationRequest request, BucketTarget target)` — publication (lista).
  El helper `S3KeyGenerator` (paquete interno o `utils/`) encapsula la lógica de folderizado y UUIDs.
- **Update de avatar**: se añade un método `updateAvatarKey(UUID userId, String key)` en el servicio existente que gestione la entidad `UserEntity` (probablemente `UserService` o donde ya viva `findById` + save). Si en `design` se decide aislar, se introduce `ProfileService` con esa única responsabilidad. La ruta `/profiles/me/avatar` no expone el `userId`.
- **No se introduce `MultipartFile`** ni `multipart/form-data`. Todos los endpoints reciben JSON (`@RequestBody` con `@Valid`).

## Edge cases

- **Subida sin update posterior**: si el usuario obtiene una URL pre-firmada y sube el archivo pero nunca llama a `PATCH /profiles/me/avatar` (o nunca crea la publicación), el objeto queda huérfano en el bucket. Política: leave-it (aceptable por costo).
- **Avatar anterior queda en el bucket al actualizar**: al recibir una nueva `key` para el usuario, la `key` anterior sigue siendo un objeto válido en S3. Política: leave-it.
- **`fileName` idéntico en dos requests distintos**: se generan `keys` distintas (UUID aleatorio + timestamp del bucket). No hay idempotencia entre requests.
- **`id`s duplicados en `files`**: 400 vía `@UniqueFileIds`, mensaje enumera los ids conflictivos.
- **`mimeType` no permitido**: 400 vía validación estándar, mensaje lista los mimeTypes válidos.
- **`size` excedido**: 400, el mensaje indica el cap aplicable (10 MB o 50 MB) según el prefijo MIME enviado.
- **AWS caído / credenciales inválidas / red**: `S3Presigner` lanza, el servicio lo envuelve en `PresignedUrlGenerationException`, el handler responde 503.
- **Variables de entorno AWS ausentes al arranque**: `AwsS3Properties` no se puede enlazar → la aplicación falla al arrancar (preferible a fallar en runtime, queda documentado en README).
- **Body de `PATCH /profiles/me/avatar` con `key` que no respeta el patrón `profiles/<uuid>/<uuid>.<ext>`**: 400. No se acepta cualquier string como key (defensa contra inyección de paths).
- **`files` vacío en `POST /posts/presigned-url`**: 400 (`@NotEmpty`).

## Métricas de éxito

- El frontend puede completar el flujo de subida de avatar y de media de publicación **sin** pasar binarios por el backend (verificable manualmente).
- **Cero endpoints `multipart/form-data`** nuevos (verificable por ausencia de `MultipartFile` en el código).
- Tamaño medio de respuesta de los endpoints de pre-signed URL **< 1 KB** (sólo `{url}` o `{uris: [...]}`).
- p95 de los endpoints de pre-signed URL **< 200 ms** en entorno local (medible con logs/timers).
- 100% de las requests con metadatos inválidos rechazadas con 400 antes de llamar a AWS (medible por ausencia de errores `S3Exception` por validación).
- `application-prod.yaml` documenta las 7 variables AWS requeridas y la app falla rápido si falta alguna al arranque.

## Riesgos (producto)

- **Configuración AWS ausente al deploy**: si no se setean las 7 env vars en `prod`, la app no arranca. Mitigación: documentar en README y fallar al arranque (fail-fast) en lugar de 500 en runtime.
- **Cambio futuro del formato de key**: si en algún momento se decide cambiar el patrón `profiles/<userId>/<uuid>.<ext>`, los registros existentes en `users.avatar_url` quedan apuntando a keys inexistentes. Mitigación: el patrón queda explícito en la propuesta y en comentarios de la clase generadora de keys; cualquier migración futura requerirá script de transformación.
- **Keys firmadas en logs**: si en el futuro se loggea la respuesta por error, las URLs pre-firmadas quedan en logs. Mitigación acordada: no loggear respuestas exitosas; sólo warnings y errores. La política debe quedar explícita en la clase de servicio.
- **Tamaño real del archivo no validado en backend**: la validación de `size` se hace contra el valor declarado por el cliente. S3 rechazará archivos más grandes en el `PUT` con un error que el cliente verá. Esto es aceptable porque el `Content-Length` firmado por la URL pre-firmada limita lo que S3 acepta; un archivo mayor no subirá.
- **SVG malicioso**: SVG puede contener JavaScript. El bucket es público, así que un usuario podría subir un SVG con `<script>`. Mitigación fuera de alcance (sanitización en cliente o CloudFront); se documenta como riesgo conocido para que el equipo de frontend decida.
- **Costo de objetos huérfanos**: la política leave-it puede acumular archivos. Se asume aceptable; si en el futuro se vuelve problema, se introduce un job de limpieza (explícitamente fuera de alcance).

## Preguntas abiertas que se cierran en design

1. **Empaquetado**: ubicación exacta de `AwsS3Properties`, `AwsS3Configuration`, `BucketTarget`, validadores custom, excepción y handler. Candidatos: `configuration/`, `service/`, `utils/`, `presentation/advice/`. Se decide en `design` siguiendo el principio "cosas de la misma capa juntas".
2. **Forma del servicio**: **decidido por el usuario** — un único servicio `PresignedUrlService` agnóstico al bucket. Métodos reciben `BucketTarget` por argumento. Helpers internos (`S3KeyGenerator` y la dependencia del `S3Presigner`) encapsulan detalles. Los controladores (`ProfilePresignedUrlController`, `PublicationPresignedUrlController`) hardcodean el `BucketTarget` correspondiente en la llamada al servicio.
3. **Update de avatar**: **decidido por el usuario** — agregar `updateAvatarKey(UUID userId, String key)` al `UserService` existente. Si en el futuro el perfil crece (bio, display name, redes sociales), se migra todo el conjunto a `ProfileService` con sus métodos juntos. Hoy, evitar proliferación de servicios con un solo método.
4. **Tabla de extensiones por MIME**: si se hace `enum MimeExtension` con mapeo directo, o un `Map<String, String>` en `S3KeyGenerator`. Decisión: enum para que sea exhaustivo y compile-time safe.
5. **Validación del formato de `key` en `PATCH /profiles/me/avatar`**: regex exacta y dónde vive (anotación `@Pattern` en DTO vs validador custom). Decisión: `@Pattern` en DTO si la regex es simple.
6. **Handler de `PresignedUrlGenerationException`**: si convive en una nueva clase advice o se añade al `GlobalExceptionHandler`. Decisión: nueva clase `PresignedUrlExceptionHandler` con orden explícito para mantener el `GlobalExceptionHandler` enfocado.
7. **Mensaje del `ProblemDetail`** (decidido): `type` = `https://tiendauniapi.com/problems/presigned-url-generation-failed`, `title` = `"Presigned Url Generation Failed"`, `detail` = `"No se pudo generar la URL pre-firmada para subir el archivo. Intenta nuevamente en unos momentos."`
8. **`AwsS3Configuration` y doble función de las props**: confirmar que el bean `S3Presigner` se construye UNA sola vez para toda la app (es un cliente compartido) y que el `endpointOverride` se aplica desde `AwsS3Properties.bucketProfileUrl()` o `bucketPublicationUrl()` según el bucket activo. Si el SDK permite un único `endpointOverride` por instancia, evaluar si se necesitan dos beans (`profileS3Presigner`, `publicationS3Presigner`) o uno solo configurado con la URL "neutral" (ej. el dominio del proveedor sin bucket específico). Decisión final: dos beans, uno por bucket, cada uno con su propio `endpointOverride`.

> **Nota post-implementación (drift documentado)**: la decisión anterior quedó **parcialmente superada** durante la implementación. Se conservan los dos beans (`profileS3Presigner`, `publicationS3Presigner`) tal como dice la propuesta, pero el `endpointOverride` ya NO se aplica desde `bucketProfileUrl()`/`bucketPublicationUrl()`. Hoy ambos beans comparten el mismo `endpointOverride` desde `AwsS3Properties.endpoint()` (una nueva propiedad introducida durante la implementación). Las props `bucket-profile-url` / `bucket-publication-url` siguen vivas y se usan exclusivamente para componer la URL pública de lectura (`bucketUrl + key`), accesible vía `BucketTarget.resolveBucketUrl(...)` para un composer futuro. Detalles en `spec.md` (AC-CFG-3, AC-CFG-4, AC-MIG-1), `design.md` (Decisión 1 con nota de drift) y `archive-report.md`.

---

**Listo para revisión.** Esta propuesta refleja las decisiones de producto acordadas en `openspec/changes/aws-s3-presigned-urls/explore.md` (sección "Open questions") y el contrato registrado en memoria como `aws-s3/decisions`. No se introduce scope nuevo; las preguntas abiertas son decisiones de empaquetado/organización que pertenecen a `design`.
