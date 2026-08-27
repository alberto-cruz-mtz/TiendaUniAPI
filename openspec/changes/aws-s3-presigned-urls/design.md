# Diseño — aws-s3-presigned-urls

## Resumen arquitectónico

El feature introduce un slice vertical nuevo sobre las tres capas existentes (`persistence`, `service`, `presentation`) con un módulo de configuración AWS dedicado. La responsabilidad del backend se reduce a validar metadatos, firmar URLs S3 y persistir la `key` resultante — nunca se reciben bytes. La capa de presentación expone tres endpoints HTTP nuevos, la capa de servicio aporta un único `PresignedUrlService` agnóstico al bucket (que recibe `BucketTarget` por argumento) con su helper de keys, y la capa de configuración aporta dos beans `S3Presigner` que comparten el mismo `endpointOverride` desde `AwsS3Properties.endpoint()` (provider URL — AWS S3 o Cloudflare R2).

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          Cliente (SPA / Frontend)                         │
└──────────────────────────────────────────────────────────────────────────┘
            │  POST /profiles/presigned-url               │
            │  POST /posts/presigned-url                  │
            │  PATCH /profiles/me/avatar                  │
            ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  presentation/                                                           │
│  ┌────────────────────────┐  ┌────────────────────────┐                  │
│  │ ProfilePresignedUrl    │  │ PublicationPresignedUrl│                  │
│  │ Controller             │  │ Controller             │                  │
│  └─────────┬──────────────┘  └─────────┬──────────────┘                  │
│            │                            │                                 │
│            ▼                            ▼                                 │
│  ┌────────────────────────┐  ┌────────────────────────┐                  │
│  │ ProfileAvatar          │  │ PresignedUrlItem       │                  │
│  │ Controller             │  │ PresignedUrlProfile    │                  │
│  └─────────┬──────────────┘  │ Request/Response       │                  │
│            │                 └────────────────────────┘                  │
└────────────┼──────────────────────────────────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  service/                                                                │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  PresignedUrlService (interface)  →  PresignedUrlServiceImpl    │   │
│  │   generateProfilePresignedUrl(request, BucketTarget)             │   │
│  │   generatePublicationPresignedUrls(request, BucketTarget)        │   │
│  └──────────────────────┬───────────────────────────────────────────┘   │
│                         │                                                │
│                         ▼                                                │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  S3KeyGenerator.generateProfileKey(userId, mimeType)             │   │
│  │  S3KeyGenerator.generatePublicationKey(userId, mimeType)         │   │
│  │  BucketTarget (enum) — mapea bucket name + bucket url            │   │
│  └──────────────────────┬───────────────────────────────────────────┘   │
│                         │                                                │
│  ┌──────────────────────┴───────────────────────────────────────────┐   │
│  │  AuthenticationService.updateAvatarKey(userId, key)              │   │
│  │  → UserRepository.findById → entity.setAvatarUrl(key) → save     │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  configuration/                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  AwsS3Properties (@ConfigurationProperties record)               │   │
│  │  AwsS3Configuration (2 @Bean S3Presigner — uno por bucket)       │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────┘
             │
             ▼
        AWS S3 / Cloudflare R2
        (bucket-profile, bucket-publication)
```

El feature mantiene **dos beans `S3Presigner`** (en lugar de uno) por compatibilidad con la implementación inicial y para permitir evolución futura a credenciales o región por bucket sin tocar el wiring. Hoy ambos beans son funcionalmente idénticos: comparten el mismo `endpointOverride` desde `AwsS3Properties.endpoint()`. La decisión histórica y sus tradeoffs se documentan en la Decisión 1.

El servicio único `PresignedUrlService` se apoya en un `S3KeyGenerator` (helper interno, sin estado) para resolver el formato `profiles/<userId>/<uuid>.<ext>` o `publications/<userId>/<uuid>.<ext>` y en `BucketTarget` (enum) para abstraer la elección del bean de presigner correcto. Los dos controladores hardcodean `BucketTarget.PROFILE` o `BucketTarget.PUBLICATION` en su única llamada al servicio.

## Mapa de paquetes / archivos

Árbol exacto con TODOS los archivos nuevos (rutas absolutas desde `src/main/java`):

```
src/main/java/alberto/cruz/tiendauniapi/
├── configuration/
│   ├── AwsS3Properties.java                 [record @ConfigurationProperties(prefix="app.aws.s3")]
│   └── AwsS3Configuration.java              [@Configuration con @EnableConfigurationProperties + 2 @Bean S3Presigner]
├── service/
│   ├── interfaces/
│   │   └── PresignedUrlService.java         [interfaz pública del servicio único; 2 métodos]
│   ├── implementation/
│   │   ├── PresignedUrlServiceImpl.java     [implementación; inyecta 2 S3Presigner + S3KeyGenerator]
│   │   └── AuthenticationServiceImpl.java   [+ método updateAvatarKey(UUID, String)]
│   ├── helper/
│   │   └── S3KeyGenerator.java              [@Component helper sin estado; genera keys folderizadas]
│   ├── model/
│   │   ├── BucketTarget.java                [enum { PROFILE, PUBLICATION }; resolveBucketName / resolveBucketUrl / presignerProvider]
│   │   ├── PresignedUrl.java                [record interno (url, key); inmutable]
│   │   └── PresignedUrlMimeExtension.java   [enum mime→ext; método fromMimeType]
│   └── exception/
│       ├── PresignedUrlGenerationException.java   [extends RuntimeException; mensaje enmascarado]
│       └── (sin nuevos archivos adicionales — UserNotFoundException se reusa)
├── presentation/
│   ├── controller/
│   │   ├── ProfilePresignedUrlController.java        [POST /profiles/presigned-url]
│   │   ├── ProfileAvatarController.java              [PATCH /profiles/me/avatar]
│   │   └── PublicationPresignedUrlController.java    [POST /posts/presigned-url]
│   ├── dto/
│   │   ├── PresignedUrlProfileRequest.java           [record Request profile]
│   │   ├── PresignedUrlProfileResponse.java          [record Response profile (sólo url)]
│   │   ├── UpdateAvatarKeyRequest.java               [record Request PATCH avatar]
│   │   ├── PresignedUrlPublicationRequest.java       [record Request publicación (files: List<PresignedUrlItem>)]
│   │   ├── PresignedUrlPublicationResponse.java      [record Response publicación (uris: List<PresignedUrlItemResponse>)]
│   │   └── PresignedUrlItem.java                     [record componente de files (id, fileName, size, mimeType)]
│   │                                                 [también usado como componente de uris (id, url, key)]
│   ├── validation/
│   │   ├── ValidFileSize.java                        [@Constraint(validatedBy=…); class-level]
│   │   ├── ValidFileSizeValidator.java               [ConstraintValidator<ValidFileSize, PresignedUrlProfileRequest|…>; ConstraintValidator<Object> genérico]
│   │   ├── UniqueFileIds.java                        [@Constraint; class-level]
│   │   └── UniqueFileIdsValidator.java               [ConstraintValidator<UniqueFileIds, PresignedUrlPublicationRequest>]
│   └── advice/
│       ├── GlobalExceptionHandler.java               [+ @ExceptionHandler(ConstraintViolationException.class) → 400]
│       ├── PresignedUrlExceptionHandler.java         [@RestControllerAdvice @Order(20); mapea PresignedUrlGenerationException → 503]
│       └── (sin tocar AuthenticationExceptionHandler — reusa para UserNotFoundException)
└── (modificaciones)
    ├── service/
    │   └── interfaces/
    │       └── AuthenticationService.java            [+ firma updateAvatarKey(UUID userId, String key)]
    └── resources/
        ├── application.yaml                          [+ bloque app.aws.s3.* con valores default-neutrales]
        ├── application-dev.yaml                      [+ bloque dev apuntando a LocalStack/MinIO]
        └── application-prod.yaml                     [+ bloque prod referenciando env vars AWS_*]
```

> **Nota de empaquetado**: AGENTS.md exige separar `service/interfaces/` (interfaz) y `service/implementation/` (clase) para todos los servicios. `BucketTarget`, `PresignedUrl` y `PresignedUrlMimeExtension` viven en `service/model/` porque son modelos del dominio del feature (no entidades ni DTOs), siguiendo la convención de `persistence/model/`.

## Decisiones de arquitectura (con tradeoffs)

### Decisión 1: Cantidad de beans S3Presigner

> **Nota de drift (post-implementación)**: la decisión original era 2 beans con `endpointOverride` desde URLs de bucket. Tras una refactorización posterior (alineada con el modelo "un solo provider URL"), ambos beans comparten el mismo `endpointOverride` desde `AwsS3Properties.endpoint()`. La decisión de mantener dos beans se conserva por compatibilidad con el código en producción y para permitir evolución futura (credenciales o región por bucket). Ver `archive-report.md` y `verify-report.md` para los detalles del cambio.

**Contexto original (al momento de la decisión)**: el SDK AWS v2 (`software.amazon.awssdk:s3:2.54.3`) modela `S3Presigner` como un cliente pesado construido una sola vez con credenciales, región y un único `endpointOverride`. El spec requería dos beans separados para soportar migración a Cloudflare R2 sin recompilar (AC-MIG-1).

**Opción A — Un solo bean `S3Presigner`** con `endpointOverride` neutral (ej. `s3.amazonaws.com`) y selección del bucket vía `bucketName()` en cada `PutObjectRequest`. El SDK firmará siempre contra AWS nativo, la URL pre-firmada apuntará a AWS nativo.

- **Pro**: simpleza (un bean, una configuración).
- **Contra (al momento de la decisión)**: bloqueaba AC-MIG-1. Si en el futuro se migra a Cloudflare R2, ese bean único firmaría con un endpoint incorrecto para R2. Habría que cambiar código.

**Opción B — Dos beans `S3Presigner`** (`profileS3Presigner`, `publicationS3Presigner`).

- **Pro (al momento de la decisión)**: cada bucket podía migrar a un endpoint distinto sin recompilar. `BucketTarget` actuaba como selector del bean correcto.
- **Estado actual**: ambos beans comparten el mismo `endpointOverride` desde `AwsS3Properties.endpoint()`. El bucket se identifica vía `PutObjectRequest.bucket(...)` en cada `presignPutObject(...)`. La migración a R2 sigue siendo 100% por env vars (`AWS_ENDPOINT` + `AWS_REGION`), no requiere recompilar.

**Opción C — Un bean `S3Presigner` por bucket vía `@Bean(name=...)` registrado dinámicamente en un `Map<BucketTarget, S3Presigner>`**.

- **Pro**: extensible a N buckets futuros sin agregar beans.
- **Contra**: introduce indirección sin beneficio real y dificulta el test del servicio.

**Decisión**: opción B — dos beans `S3Presigner` en `AwsS3Configuration`. Hoy son funcionalmente idénticos (mismo provider, misma región, mismas credenciales); se conservan dos beans para (a) no romper el wiring existente, (b) permitir evolución futura sin tocar el servicio.

**Consecuencias (estado actual)**:
- Ambos beans se construyen vía factory method estático `buildPresigner(AwsS3Properties)` en `AwsS3Configuration` — un solo lugar donde se aplica la lógica del SDK.
- `PresignedUrlServiceImpl` recibe ambos beans como dependencias finales vía `@RequiredArgsConstructor` (AGENTS.md), los selecciona con `BucketTarget` (switch en `resolvePresigner(...)`).
- Tests del servicio mockean los dos beans por separado.
- `BucketTarget.resolveBucketUrl(...)` queda como helper para componer la URL pública de lectura (no se usa en este feature).

### Decisión 2: Ubicación de AwsS3Properties

**Contexto**: es el primer `@ConfigurationProperties` del proyecto. AGENTS.md no establece una ubicación para `@ConfigurationProperties`, sólo indica que servicios y repositorios se inyectan por constructor. Spring Boot 3.x recomienda un paquete dedicado (`configuration/` o `properties/`).

**Opción A — `alberto.cruz.tiendauniapi.configuration`** junto a `SecurityConfiguration` y `ApplicationConfiguration`.

- **Pro**: agrupa con los demás `@Configuration` ya existentes. Convención del proyecto. El nombre del paquete ya comunica la intención (configuración de Spring).
- **Contra**: ninguno relevante.

**Opción B — `alberto.cruz.tiendauniapi.properties`** (paquete nuevo).

- **Pro**: separa `@ConfigurationProperties` de `@Configuration` puro.
- **Contra**: introduce un paquete nuevo sin precedente en el proyecto. Una sola clase no justifica un paquete.

**Opción C — `alberto.cruz.tiendauniapi.utils.config`** (sub-paquete de utils).

- **Pro**: reusa `utils/` que ya existe.
- **Contra**: `utils/` está pensado para utilidades sin estado (mappers, jwt), no para beans Spring.

**Decisión**: opción A — `configuration/AwsS3Properties.java`.

**Rationale**: el proyecto ya usa `configuration/` para beans de Spring (`SecurityConfiguration`, `ApplicationConfiguration`). Mantener la convención reduce la carga cognitiva. Como `AwsS3Properties` es el primer `@ConfigurationProperties`, su ubicación sienta precedente.

**Consecuencias**: `AwsS3Configuration` también vive en `configuration/`. Se añade `@EnableConfigurationProperties(AwsS3Properties.class)` sobre `AwsS3Configuration` (forma idiomática Spring Boot 3.x).

### Decisión 3: Ubicación de BucketTarget / PresignedUrl / PresignedUrlMimeExtension

**Contexto**: tres modelos internos del feature (un enum selector, un record de valor, un enum de mapeo mime→ext). Ninguno se serializa hacia el cliente (viven entre `service` y `S3Presigner`).

**Opción A — `service/model/`** junto a la convención `persistence/model/` (que ya existe para `AuthenticatedUser`, `RefreshToken`).

- **Pro**: paralelo con `persistence/model/` (modelos internos no persistentes). El nombre comunica "modelo de la capa service".
- **Contra**: ninguno relevante.

**Opción B — `common/`** (compartido con toda la app).

- **Pro**: reusa paquete existente.
- **Contra**: estos modelos son específicos del feature; vivir en `common/` insinúa reuso que no es real.

**Opción C — `utils/`** (junto a `UserMapper`, `JwtUtil`).

- **Pro**: reusa paquete existente.
- **Contra**: `utils/` es para funciones estáticas puras; `BucketTarget` es un enum de selección de bean y `PresignedUrl` es un record inmutable con semántica de dominio. No encajan.

**Decisión**: opción A — `service/model/`.

**Rationale**: el feature es parte de la capa de servicio (un único servicio nuevo). Los modelos son internos al feature. `service/model/` sigue el patrón de `persistence/model/` y aísla el feature.

**Consecuencias**:
- `BucketTarget` se inyecta como dependencia del servicio (no aparece en DTOs).
- `PresignedUrl` es el return type de los métodos del servicio; los DTOs de respuesta (`PresignedUrlProfileResponse`, `PresignedUrlItem` en respuesta) son records separados en `presentation/dto/`.
- `PresignedUrlMimeExtension` es referenciado por `S3KeyGenerator` (helper) y nunca sale del paquete `service`.

### Decisión 4: Ubicación de los validadores custom

**Contexto**: `@ValidFileSize` y `@UniqueFileIds` son anotaciones Jakarta Validation que validan DTOs de request. Su ciclo de vida es: se declaran en DTOs (`presentation/dto`), se procesan en el binding de Spring MVC (capa `presentation`).

**Opción A — `presentation/validation/`** (paquete nuevo).

- **Pro**: agrupa con los DTOs que los usan. Convención Spring (paquete `validation/` bajo el paquete web).
- **Contra**: introduce un paquete nuevo.

**Opción B — `service/validation/`** (paquete nuevo bajo service).

- **Pro**: el validador encapsula una regla de negocio (caps por tipo MIME).
- **Contra**: contraintuitivo — el validador se ejecuta en la capa de presentación (binding), no en el servicio. Mezclar capas.

**Opción C — Dentro del DTO como clase anidada estática**.

- **Pro**: el validador viaja con el DTO que valida.
- **Contra**: rompe la separación; un validador reusado entre dos DTOs (profile y publication usan `@ValidFileSize`) tendría que duplicarse o referenciarse de forma fea. Además ensucia el DTO.

**Decisión**: opción A — `presentation/validation/`.

**Rationale**: los validadores son restricciones de **binding de input** (qué acepta la API), no reglas internas del servicio. AGENTS.md dice que "los DTOs para manejar la entrada de datos en las solicitudes HTTP deben tener el sufijo 'Request'" — los validadores son extensiones de esos DTOs y deben vivir cerca. Adicionalmente, `@ValidFileSize` se reusa entre dos DTOs Request (perfil y publicación), por lo que necesitan un lugar común.

**Consecuencias**: el paquete `presentation/validation/` se introduce con este cambio; cualquier validador custom futuro vivirá ahí.

### Decisión 5: Ubicación de PresignedUrlGenerationException

**Contexto**: nueva excepción para fallos del proveedor S3. AGENTS.md dice: "Todos los servicios deben manejar las excepciones personalizadas definidas en el proyecto para los casos de error específicos".

**Opción A — `service/exception/`** junto a las demás (`UserNotFoundException`, `EmailAddressAlreadyRegisteredException`, etc.).

- **Pro**: 100% consistente con el patrón existente. La excepción es lanzada por el servicio.
- **Contra**: ninguno.

**Opción B — `common/`** (junto a `UnknownException`, `ResourceNotFoundException`).

- **Pro**: coincide con `UnknownException`, que también es fallback genérico.
- **Contra**: `common/` está reservado para tipos compartidos entre features; `UnknownException` está ahí por la regla AGENTS.md "usar UnknownException como fallback hasta tener específica". Una vez que existe la específica, debe ir a su paquete de feature.

**Decisión**: opción A — `service/exception/PresignedUrlGenerationException.java`.

**Rationale**: consistencia con el resto de excepciones específicas del proyecto. AGENTS.md trata `service/exception/` como el paquete canónico para excepciones de feature.

**Consecuencias**:
- `PresignedUrlExceptionHandler` (advice) vive en `presentation/advice/`.
- La excepción NO extiende `UnknownException` (no es fallback genérico).
- Mensaje enmascarado (no expone causa raíz al cliente); el `handler` arma el `detail` final.

### Decisión 6: Servicio único PresignedUrlService con interfaz + implementación

**Contexto**: AGENTS.md exige "Todos los servicios deben estar conformados de una interfaz (para inyeccion de dependencias) y una clase de implementacion". El proposal (Decisión cerrada) ya eligió un único servicio agnóstico al bucket.

**Opción A — `PresignedUrlService` (interfaz) en `service/interfaces/` + `PresignedUrlServiceImpl` en `service/implementation/`**.

- **Pro**: cumple AGENTS.md literalmente. El resto del proyecto ya sigue este patrón.
- **Contra**: ninguno.

**Opción B — `PresignedUrlService` como clase concreta `@Service`**.

- **Pro**: ahorra un archivo.
- **Contra**: viola AGENTS.md.

**Decisión**: opción A — interfaz + implementación, en sus paquetes canónicos.

**Rationale**: regla AGENTS.md no negociable.

**Consecuencias**:
- `PresignedUrlService` define exactamente dos métodos públicos: `generateProfilePresignedUrl(PresignedUrlProfileRequest, BucketTarget)` y `generatePublicationPresignedUrls(PresignedUrlPublicationRequest, BucketTarget)`.
- `PresignedUrlServiceImpl` usa `@Service` + `@RequiredArgsConstructor` + `@Transactional` (sólo lectura, `readOnly = true`).
- Tests del servicio mockean la interfaz (no la implementación).

### Decisión 7: Forma del updateAvatarKey — método nuevo en AuthenticationServiceImpl

**Contexto**: el spec (AC-AVATAR-1, AC-AVATAR-2, AC-AVATAR-3) requiere persistir la `key` del avatar en `users.avatar_url`. AGENTS.md prohíbe field injection. El usuario cerró la decisión en proposal: agregar `updateAvatarKey` al `AuthenticationService` existente.

**Opción A — `AuthenticationService.updateAvatarKey(UUID userId, String key)` + implementación en `AuthenticationServiceImpl`**.

- **Pro**: el `AuthenticationServiceImpl` ya tiene acceso a `UserRepository` y a las reglas de "user not found" (reusa `UserNotFoundException`). No introduce un nuevo servicio con un solo método.
- **Contra**: el método no es estrictamente de autenticación. Acopla dos responsabilidades.

**Opción B — Crear `ProfileService` (interfaz + impl) nuevo**.

- **Pro**: SRP puro. El nombre del servicio comunica la responsabilidad.
- **Contra**: introduce un servicio con un solo método público hoy. Si en el futuro el perfil crece (bio, display name, redes sociales), se justifica.

**Decisión**: opción A — `updateAvatarKey` en `AuthenticationServiceImpl`.

**Rationale**: el usuario ya cerró la decisión en proposal ("decisión v2 del usuario"). Razones prácticas: reusa `UserRepository`, reusa `UserNotFoundException`, evita proliferación de servicios con un solo método. Si el perfil crece, se refactoriza a `ProfileService` en un cambio futuro.

**Consecuencias**:
- `AuthenticationService` (interfaz) gana la firma `void updateAvatarKey(UUID userId, String key);`.
- `AuthenticationServiceImpl` implementa el método con `@Transactional` (escritura, no readOnly).
- La excepción `UserNotFoundException` se lanza desde `updateAvatarKey` y la maneja el `AuthenticationExceptionHandler` existente (mapea a 404).

### Decisión 8: Forma del S3Presigner en tests

**Contexto**: el servicio llama a `S3Presigner.presignPutObject(...)`. Los tests del servicio no deben tocar AWS real.

**Opción A — `@MockBean` en `@WebMvcTest` o `@SpringBootTest` con el bean registrado**.

- **Pro**: integración real con el contexto Spring; reemplaza el bean por un mock.
- **Contra**: `@MockBean` está deprecado en Spring Boot 3.4+ a favor de `@MockitoBean` (Spring Framework 6.2+). Como el proyecto está en Spring Boot 4.1.1, corresponde usar `@MockitoBean`.

**Opción B — `@Mock S3Presigner profileS3Presigner; @Mock S3Presigner publicationS3Presigner;` con `@ExtendWith(MockitoExtension.class)`** en unit tests del servicio puro.

- **Pro**: tests rápidos, sin contexto Spring. Es la forma idiomática para unit tests de lógica de servicio.
- **Contra**: no verifica el wiring (se cubre por separado en `AwsS3ConfigurationTest`).

**Opción C — WireMock o Testcontainers LocalStack** en integration tests.

- **Pro**: ejercita el SDK real contra un endpoint S3-compatible.
- **Contra**: para unit tests del servicio es overkill; agrega minutos al CI.

**Decisión**: opción B para unit tests del servicio + opción A (vía `@MockitoBean`) para los tests web slice. No se introduce Testcontainers ni WireMock en este MVP.

**Rationale**: AC-ERR-1 y AC-PROF-1 se cubren con mocks del SDK. La integración real con un bucket S3-like (LocalStack/MinIO) se puede añadir en un cambio futuro si se necesita (no requerido por el spec).

**Consecuencias**:
- `PresignedUrlServiceImplTest`: `@ExtendWith(MockitoExtension.class)`, `@Mock` para ambos `S3Presigner`, `@Mock` para `S3KeyGenerator`, inyección manual al servicio bajo test.
- Tests web (`@WebMvcTest`): usan `@MockitoBean` para reemplazar los dos beans `S3Presigner`.
- `AwsS3ConfigurationTest`: usa `ApplicationContextRunner` con `withUserConfiguration` para verificar carga de contexto y `endpointOverride` aplicado.

### Decisión 9: Manejo de ConstraintViolationException

**Contexto**: `@UniqueFileIds` se aplica a nivel de clase sobre `PresignedUrlPublicationRequest` (record). Cuando Spring valida un `@RequestBody @Valid` con una restricción de clase, NO lanza `MethodArgumentNotValidException` — lanza `jakarta.validation.ConstraintViolationException`. El `GlobalExceptionHandler` actual no la maneja.

**Opción A — Agregar `@ExceptionHandler(ConstraintViolationException.class)` al `GlobalExceptionHandler` existente**.

- **Pro**: un solo lugar central para errores de validación Jakarta. Mismo `type` URI (`/problems/validations`), mismo formato de `IncorrectField`. Consistente con AC-VAL-3.
- **Contra**: extiende una clase que no es del feature. Mitigación: la extensión es trivial (un método nuevo) y respeta el orden de precedencia.

**Opción B — Nuevo advice `UniqueFileIdsExceptionHandler` separado**.

- **Pro**: cero cambios al `GlobalExceptionHandler`.
- **Contra**: introduce un advice adicional para una sola restricción. El spec pide que `ConstraintViolationException` se maneje por `GlobalExceptionHandler`.

**Decisión**: opción A — extender `GlobalExceptionHandler` con un handler para `ConstraintViolationException`.

**Rationale**: AC-ERR-3 lo exige explícitamente. La extensión es local (un método) y mantiene todos los errores de validación en un solo lugar.

**Consecuencias**:
- `GlobalExceptionHandler` gana un método `@ExceptionHandler(ConstraintViolationException.class)` que arma un `ProblemDetail` 400 con `type = DOMAIN_URI + "/validations"`, `title = "Validation Failed"` y `detail` que enumera los ids duplicados.
- La propiedad `errors` (mapa de `IncorrectField`) no aplica a `ConstraintViolationException` (no es field-level); el `detail` lleva el mensaje crudo del primer constraint violation.

### Decisión 10: Mapeo de mimeType → extensión (enum vs Map)

**Contexto**: el spec exige que la extensión del archivo subido (parte de la key S3) se infiera del `mimeType`. La whitelist tiene 9 mimeTypes.

**Opción A — `enum PresignedUrlMimeExtension { JPEG("image/jpeg", "jpg"), PNG(...), ...; static Optional<PresignedUrlMimeExtension> fromMimeType(String) }`**.

- **Pro**: exhaustividad en compile-time. Agregar un nuevo mimeType sin mapear su extensión falla el switch. Consistente con la decisión cerrada en proposal.
- **Contra**: enum verboso para 9 entradas (mitigable con `Map.of()` interno).

**Opción B — `Map<String, String>` estático en `S3KeyGenerator`**.

- **Pro**: conciso.
- **Contra**: cualquier string puede llegar al map; no hay chequeo de exhaustividad. Mezcla la lógica de keys con la de mime mapping.

**Decisión**: opción A — enum en `service/model/PresignedUrlMimeExtension.java`.

**Rationale**: ya cerrada en proposal. La exhaustividad y el chequeo en compile-time son valiosos para una lista cerrada de 9 mimeTypes.

**Consecuencias**:
- `S3KeyGenerator` invoca `PresignedUrlMimeExtension.fromMimeType(mimeType)` y lanza `IllegalArgumentException` (envuelta en `PresignedUrlGenerationException` por el servicio) si el mimeType no está en el enum.
- La whitelist de mimeTypes en validación Jakarta (`@Pattern`) y este enum deben estar sincronizados (mitigación: tests cruzados + la validación corre antes que `S3KeyGenerator`, así que un mimeType inválido nunca llega al helper).

### Decisión 11: Persistencia de key vs URL completa en avatar_url

**Contexto**: AC-AVATAR-3 dice: "`avatar_url` SHALL pasar de un valor previo `v_old` a `v_new` cuando el mismo usuario llama al endpoint dos veces con keys distintas". Y el spec exige que sólo se guarde la key relativa.

**Opción A — Persistir la key relativa** (`profiles/<userId>/<uuid>.<ext>`).

- **Pro**: cumple el spec. Permite migrar de proveedor (Cloudflare R2) sin migrar la columna: la URL pública se reconstruye en tiempo de lectura como `bucketProfileUrl + key`.
- **Contra**: el frontend que quiera renderizar el avatar necesita conocer `bucketProfileUrl` (lo recibe vía config del frontend o vía un endpoint futuro). Esto se documenta en el README.

**Opción B — Persistir la URL completa** (`https://bucket.s3.amazonaws.com/profiles/<userId>/<uuid>.<ext>`).

- **Pro**: el frontend puede usar el valor directo como `src` de `<img>` sin config adicional.
- **Contra**: ata la columna al proveedor actual. Cambiar de proveedor requiere migrar la columna entera.

**Decisión**: opción A — persistir la key relativa. (Decisión ya cerrada en proposal.)

**Rationale**: AC-AVATAR-3, AC-MIG-1.

**Consecuencias**:
- `AuthenticationServiceImpl.updateAvatarKey(UUID userId, String key)` hace `entity.setAvatarUrl(key)` (setter existente en `UserEntity`).
- La columna `users.avatar_url` (VARCHAR(300) NOT NULL) recibe la key exacta. La URL pública se compone en frontend.

### Decisión 12: Manejo del error cuando el usuario autenticado no existe en PATCH /profiles/me/avatar

**Contexto**: el principal `@AuthenticationPrincipal AuthenticatedUser` puede tener un `userId` que ya no existe en `users` (token válido de un usuario borrado). El endpoint `PATCH /profiles/me/avatar` resuelve el user, lanza `UserNotFoundException` si no existe.

**Opción A — Reusar `AuthenticationExceptionHandler` existente** (ya maneja `UserNotFoundException` → 404 con `type = /problems/user-not-found`).

- **Pro**: cero código nuevo. Consistente con cómo se trata `UserNotFoundException` en el resto de endpoints.
- **Contra**: el handler "se llama Authentication" pero ahora también maneja este caso. Aceptable porque ya cubre otros casos que no son estrictamente de auth (`EmailAddressAlreadyRegisteredException`, etc.).

**Opción B — Nuevo advice `ProfileExceptionHandler` para este caso específico**.

- **Pro**: nombre más preciso.
- **Contra**: introduce un advice con un solo handler para una sola excepción que ya tiene handler.

**Decisión**: opción A — reusar `AuthenticationExceptionHandler`.

**Rationale**: AC-ERR-4 lo permite ("reusar handler existente vs nuevo si se decide en design"). La consistencia con el resto del proyecto pesa más que la precisión del nombre del advice.

**Consecuencias**: el advice `AuthenticationExceptionHandler` no se modifica. La excepción `UserNotFoundException` lanzada desde `updateAvatarKey` la maneja automáticamente.

### Decisión 13: Estructura del test del servicio de pre-firmado

**Contexto**: ¿unit test con Mockito o integration test con Testcontainers LocalStack?

**Opción A — Sólo unit tests con Mockito**.

- **Pro**: rápidos (ms), aislados, no requieren infraestructura. Cubren la lógica del servicio: orden de los `uris`, manejo de excepciones del SDK, armado correcto del `PutObjectRequest`.
- **Contra**: no verifica la interacción real con el SDK contra S3-like. Para este MVP (sin cliente real todavía), aceptable.

**Opción B — Unit + integration con Testcontainers LocalStack**.

- **Pro**: prueba el SDK real contra un S3-compatible local.
- **Contra**: agrega minutos al CI, requiere Docker disponible, complejidad no justificada por el alcance actual.

**Decisión**: opción A — sólo unit tests con Mockito en este MVP. Se documenta como follow-up posible un test de integración con Testcontainers LocalStack si el proyecto empieza a tener problemas de SDK.

**Rationale**: el SDK AWS v2 es estable; mockear `S3Presigner.presignPutObject` y verificar los argumentos es suficiente para cubrir la lógica. AC-PROF-2 (URL contiene `X-Amz-*`) se cubre con un mock que devuelve una URL prefirmada sintética y un assert que parsea esa URL.

**Consecuencias**:
- `PresignedUrlServiceImplTest` con `@ExtendWith(MockitoExtension.class)`, `@Mock` para los 2 `S3Presigner` y `S3KeyGenerator`.
- `when(profileS3Presigner.presignPutObject(any())).thenReturn(mockRequestConUrl("https://bucket.profile.url/profiles/.../...?X-Amz-Signature=abc"))`.

## Flujos críticos (secuencia)

### Flujo: POST /profiles/presigned-url (feliz)

1. Cliente envía `POST /profiles/presigned-url` con body `{"fileName": "avatar", "size": 1024, "mimeType": "image/jpeg"}` y cookie `access-token`.
2. `JwtAuthFilter` (existente) valida la cookie y carga el `SecurityContext` con `AuthenticatedUser` (que tiene `userId` UUID).
3. Spring resuelve el body al record `PresignedUrlProfileRequest` → la fase de bean validation corre `@NotBlank`, `@Pattern(regexp="^[a-zA-Z0-9_-]+$")`, `@Size(max=255)` sobre `fileName`, `@Positive` y `@ValidFileSize` (class-level) sobre el record, `@NotBlank` y `@Pattern(regexp whitelist mime)` sobre `mimeType`.
4. Si la validación falla → `MethodArgumentNotValidException` → manejada por `GlobalExceptionHandler.handleValidationExceptions` → 400 con `ProblemDetail` y mapa de `IncorrectField`.
5. El controller invoca `presignedUrlService.generateProfilePresignedUrl(request, BucketTarget.PROFILE)`.
6. El servicio resuelve el bean correcto vía `BucketTarget.PROFILE.presignerProvider(awsS3Properties, profileS3Presigner, publicationS3Presigner)` (o vía inyección directa de los dos beans y switch interno — ver firma de `BucketTarget`). En la práctica, `PresignedUrlServiceImpl` ya tiene ambos beans inyectados; el `BucketTarget` selecciona cuál usar.
7. `PresignedUrlServiceImpl` llama a `S3KeyGenerator.generateProfileKey(authenticatedUser.getUserId(), request.mimeType())` → devuelve `String key = "profiles/<userId>/<uuid>.jpg"`.
8. `PresignedUrlServiceImpl` arma `PutObjectRequest.builder().bucket(BucketTarget.PROFILE.resolveBucketName(awsS3Properties)).key(key).contentType(mimeType).contentLength(size).build()`.
9. `PresignedUrlServiceImpl` invoca `presigner.presignPutObject(req)` → el SDK devuelve un `PresignedPutObjectRequest` con `url()` y `expiration()`.
10. `PresignedUrlServiceImpl` envuelve el resultado en `new PresignedUrl(url, key)` y lo devuelve.
11. El controller mapea a `new PresignedUrlProfileResponse(presignedUrl.url())` y devuelve `200 OK`.
12. **NO se loggea la URL** (AC-OBS-1). El servicio no tiene `@Slf4j` o sólo lo usa para `WARN`/`ERROR`.

### Flujo: PATCH /profiles/me/avatar (feliz)

1. Cliente envía `PATCH /profiles/me/avatar` con body `{"key": "profiles/4f1c.../8a3e....jpg"}` y cookie `access-token`.
2. `JwtAuthFilter` valida la cookie.
3. Spring resuelve el body al record `UpdateAvatarKeyRequest` → `@Pattern(regexp="^profiles/[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}\\.[a-zA-Z0-9]+$")` valida la `key`.
4. Si inválida → `MethodArgumentNotValidException` → 400 vía `GlobalExceptionHandler`.
5. El controller invoca `authenticationService.updateAvatarKey(authenticatedUser.getUserId(), request.key())`.
6. `AuthenticationServiceImpl.updateAvatarKey` hace `userRepository.findById(userId)`:
   - Si no existe → `UserNotFoundException` (constructor sin args) → 404 vía `AuthenticationExceptionHandler.handleUserNotFoundException`.
   - Si existe → continúa.
7. `AuthenticationServiceImpl` hace `entity.setAvatarUrl(key)` y `userRepository.save(entity)` dentro de la transacción.
8. El controller devuelve `204 No Content` sin cuerpo.

### Flujo: fallo AWS → 503

1. Pasos 1-7 idénticos al flujo feliz de `POST /profiles/presigned-url`.
2. `presigner.presignPutObject(req)` lanza `SdkException` (red caída, credenciales inválidas, región mal configurada).
3. `PresignedUrlServiceImpl` captura `SdkException` (captura amplia: cualquier `RuntimeException` que no sea del propio feature) y la envuelve en `PresignedUrlGenerationException("No se pudo generar la URL pre-firmada", cause)`. **El mensaje enmascarado no expone detalles del SDK al cliente**.
4. El controller NO captura (AGENTS.md: "Ningun controlador debe manejar excepciones").
5. `PresignedUrlExceptionHandler.handlePresignedUrlGenerationException(exception)` captura la excepción → devuelve `503 Service Unavailable` con:
   - `type`: `https://tiendauniapi.com/problems/presigned-url-generation-failed`
   - `title`: `"Presigned Url Generation Failed"`
   - `detail`: `"No se pudo generar la URL pre-firmada para subir el archivo. Intenta nuevamente en unos momentos."`
6. El advice loggea `ERROR` con la `PresignedUrlGenerationException` (stacktrace incluido, **sin la URL**).

### Flujo: ids duplicados en POST /posts/presigned-url

1. Cliente envía `POST /posts/presigned-url` con `files` que contiene dos elementos con el mismo `id`.
2. Spring valida el record `PresignedUrlPublicationRequest` → `@UniqueFileIds` (class-level) detecta duplicados y lanza `ConstraintViolationException` con mensaje `"Los siguientes ids están duplicados: file1, file3"`.
3. Como `ConstraintViolationException` no es `MethodArgumentNotValidException`, NO la maneja el método existente de `GlobalExceptionHandler`. El nuevo `@ExceptionHandler(ConstraintViolationException.class)` la captura.
4. Devuelve `400 Bad Request` con `ProblemDetail`:
   - `type`: `https://tiendauniapi.com/problems/validations`
   - `title`: `"Validation Failed"`
   - `detail`: el mensaje de la constraint violation.
5. `S3Presigner` NO se invoca.

## Estructura de cada archivo nuevo (firmas)

### `AwsS3Properties.java`

```java
package alberto.cruz.tiendauniapi.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aws.s3")
public record AwsS3Properties(
    @NotBlank String accessKeyId,
    @NotBlank String secretAccessKey,
    @NotBlank String region,
    @NotBlank String endpoint,
    @NotBlank String bucketProfileName,
    @NotBlank String bucketPublicationName,
    @NotBlank String bucketProfileUrl,
    @NotBlank String bucketPublicationUrl
) {}
```

### `AwsS3Configuration.java`

```java
package alberto.cruz.tiendauniapi.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(AwsS3Properties.class)
public class AwsS3Configuration {

    @Bean
    public S3Presigner profileS3Presigner(AwsS3Properties properties) { /* ... */ }

    @Bean
    public S3Presigner publicationS3Presigner(AwsS3Properties properties) { /* ... */ }

    private static S3Presigner buildPresigner(AwsS3Properties properties, String bucketUrl) { /* factory */ }
}
```

### `PresignedUrlService.java` (interfaz)

```java
package alberto.cruz.tiendauniapi.service.interfaces;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
import alberto.cruz.tiendauniapi.service.model.PresignedUrl;

import java.util.List;

public interface PresignedUrlService {
    PresignedUrl generateProfilePresignedUrl(PresignedUrlProfileRequest request, BucketTarget target);
    List<PresignedUrl> generatePublicationPresignedUrls(PresignedUrlPublicationRequest request, BucketTarget target);
}
```

### `PresignedUrlServiceImpl.java`

```java
package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.service.helper.S3KeyGenerator;
import alberto.cruz.tiendauniapi.service.interfaces.PresignedUrlService;
import alberto.cruz.tiendauniapi.service.model.BucketTarget;
import alberto.cruz.tiendauniapi.service.model.PresignedUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Service
@RequiredArgsConstructor
public class PresignedUrlServiceImpl implements PresignedUrlService {

    private final S3Presigner profileS3Presigner;
    private final S3Presigner publicationS3Presigner;
    private final S3KeyGenerator s3KeyGenerator;
    private final AwsS3Properties awsS3Properties;

    @Override
    @Transactional(readOnly = true)
    public PresignedUrl generateProfilePresignedUrl(PresignedUrlProfileRequest request, BucketTarget target) { /* ... */ }

    @Override
    @Transactional(readOnly = true)
    public List<PresignedUrl> generatePublicationPresignedUrls(PresignedUrlPublicationRequest request, BucketTarget target) { /* ... */ }
}
```

### `S3KeyGenerator.java`

```java
package alberto.cruz.tiendauniapi.service.helper;

import alberto.cruz.tiendauniapi.service.model.BucketTarget;
import alberto.cruz.tiendauniapi.service.model.PresignedUrlMimeExtension;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class S3KeyGenerator {

    private static final String PROFILE_FOLDER = "profiles";
    private static final String PUBLICATION_FOLDER = "publications";

    public String generateProfileKey(UUID userId, String mimeType) { /* profiles/<userId>/<uuid>.<ext> */ }

    public String generatePublicationKey(UUID userId, String mimeType) { /* publications/<userId>/<uuid>.<ext> */ }
}
```

### `BucketTarget.java`

```java
package alberto.cruz.tiendauniapi.service.model;

import alberto.cruz.tiendauniapi.configuration.AwsS3Properties;

public enum BucketTarget {
    PROFILE, PUBLICATION;

    public String resolveBucketName(AwsS3Properties properties) { /* PROFILE → bucketProfileName, etc. */ }

    public String resolveBucketUrl(AwsS3Properties properties) { /* PROFILE → bucketProfileUrl, etc. */ }
}
```

### `PresignedUrl.java`

```java
package alberto.cruz.tiendauniapi.service.model;

public record PresignedUrl(String url, String key) {}
```

### `PresignedUrlMimeExtension.java`

```java
package alberto.cruz.tiendauniapi.service.model;

import java.util.Optional;

public enum PresignedUrlMimeExtension {
    JPEG("image/jpeg", "jpg"),
    JPG("image/jpg", "jpg"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp"),
    SVG_XML("image/svg+xml", "svg"),
    MP4("video/mp4", "mp4"),
    WEBM("video/webm", "webm"),
    OGG("video/ogg", "ogg");

    public static Optional<PresignedUrlMimeExtension> fromMimeType(String mimeType) { /* ... */ }
    public String extension() { /* getter */ }
}
```

### `ValidFileSize.java` + `ValidFileSizeValidator.java`

```java
// ValidFileSize.java
package alberto.cruz.tiendauniapi.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ValidFileSizeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFileSize {
    String message() default "El tamaño del archivo excede el máximo permitido para este tipo de contenido.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// ValidFileSizeValidator.java
package alberto.cruz.tiendauniapi.presentation.validation;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlItem;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidFileSizeValidator implements ConstraintValidator<ValidFileSize, Object> {

    private static final long IMAGE_MAX_BYTES = 10L * 1024L * 1024L;   // 10 MB
    private static final long VIDEO_MAX_BYTES = 50L * 1024L * 1024L;   // 50 MB

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) { /* dispatch por tipo */ }

    private boolean validateFile(String mimeType, long size) { /* image/* <= IMAGE_MAX, video/* <= VIDEO_MAX */ }
}
```

### `UniqueFileIds.java` + `UniqueFileIdsValidator.java`

```java
// UniqueFileIds.java
package alberto.cruz.tiendauniapi.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = UniqueFileIdsValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueFileIds {
    String message() default "Los siguientes ids están duplicados: ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// UniqueFileIdsValidator.java
package alberto.cruz.tiendauniapi.presentation.validation;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UniqueFileIdsValidator implements ConstraintValidator<UniqueFileIds, PresignedUrlPublicationRequest> {

    @Override
    public boolean isValid(PresignedUrlPublicationRequest value, ConstraintValidatorContext context) { /* ... */ }
}
```

### `PresignedUrlGenerationException.java`

```java
package alberto.cruz.tiendauniapi.service.exception;

public class PresignedUrlGenerationException extends RuntimeException {
    public PresignedUrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### `PresignedUrlExceptionHandler.java`

```java
package alberto.cruz.tiendauniapi.presentation.advice;

import alberto.cruz.tiendauniapi.service.exception.PresignedUrlGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@Slf4j
@Order(20)
@RestControllerAdvice
public class PresignedUrlExceptionHandler {

    @ExceptionHandler(PresignedUrlGenerationException.class)
    public ResponseEntity<ProblemDetail> handlePresignedUrlGenerationException(PresignedUrlGenerationException exception) { /* 503 + ProblemDetail */ }
}
```

### DTOs Request/Response (records)

```java
// PresignedUrlItem.java — usado en body Request files[] y en Response uris[]
package alberto.cruz.tiendauniapi.presentation.dto;

public record PresignedUrlItem(
    @NotBlank String id,
    @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_-]+$") @Size(max = 255) String fileName,
    @Positive Long size,
    @NotBlank @Pattern(regexp = "<whitelist regex>") String mimeType
) {}
```

```java
// PresignedUrlItemResponse.java — sólo usado en la respuesta de /posts/presigned-url
package alberto.cruz.tiendauniapi.presentation.dto;

public record PresignedUrlItemResponse(String id, String url, String key) {}
```

```java
// PresignedUrlProfileRequest.java
package alberto.cruz.tiendauniapi.presentation.dto;

import alberto.cruz.tiendauniapi.presentation.validation.ValidFileSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@ValidFileSize
public record PresignedUrlProfileRequest(
    @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_-]+$") @Size(max = 255) String fileName,
    @Positive Long size,
    @NotBlank @Pattern(regexp = "<whitelist mime regex>") String mimeType
) {}
```

```java
// PresignedUrlProfileResponse.java
package alberto.cruz.tiendauniapi.presentation.dto;

public record PresignedUrlProfileResponse(String url) {}
```

```java
// PresignedUrlPublicationRequest.java
package alberto.cruz.tiendauniapi.presentation.dto;

import alberto.cruz.tiendauniapi.presentation.validation.UniqueFileIds;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@UniqueFileIds
public record PresignedUrlPublicationRequest(
    @NotEmpty @Size(max = 10) @Valid List<PresignedUrlItem> files
) {}
```

```java
// PresignedUrlPublicationResponse.java
package alberto.cruz.tiendauniapi.presentation.dto;

import java.util.List;

public record PresignedUrlPublicationResponse(List<PresignedUrlItemResponse> uris) {}
```

```java
// UpdateAvatarKeyRequest.java
package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAvatarKeyRequest(
    @NotBlank
    @Pattern(regexp = "^profiles/[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}\\.[a-zA-Z0-9]+$",
             message = "La key debe tener el formato profiles/<uuid>/<uuid>.<ext>.")
    String key
) {}
```

### Controllers

```java
// ProfilePresignedUrlController.java
package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileResponse;
import alberto.cruz.tiendauniapi.service.interfaces.PresignedUrlService;
import alberto.cruz.tiendauniapi.service.model.BucketTarget;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles/presigned-url")
@RequiredArgsConstructor
public class ProfilePresignedUrlController {

    private final PresignedUrlService presignedUrlService;

    @PostMapping
    public ResponseEntity<PresignedUrlProfileResponse> generate(@Valid @RequestBody PresignedUrlProfileRequest request) { /* ... */ }
}
```

```java
// ProfileAvatarController.java
package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.presentation.dto.UpdateAvatarKeyRequest;
import alberto.cruz.tiendauniapi.service.interfaces.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;

@RestController
@RequestMapping("/profiles/me/avatar")
@RequiredArgsConstructor
public class ProfileAvatarController {

    private final AuthenticationService authenticationService;

    @PatchMapping
    public ResponseEntity<Void> updateAvatar(@Valid @RequestBody UpdateAvatarKeyRequest request,
                                              @AuthenticationPrincipal AuthenticatedUser principal) { /* ... */ }
}
```

```java
// PublicationPresignedUrlController.java
package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationResponse;
import alberto.cruz.tiendauniapi.service.interfaces.PresignedUrlService;
import alberto.cruz.tiendauniapi.service.model.BucketTarget;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts/presigned-url")
@RequiredArgsConstructor
public class PublicationPresignedUrlController {

    private final PresignedUrlService presignedUrlService;

    @PostMapping
    public ResponseEntity<PresignedUrlPublicationResponse> generate(@Valid @RequestBody PresignedUrlPublicationRequest request) { /* ... */ }
}
```

### Modificaciones a `AuthenticationService` y `AuthenticationServiceImpl`

```java
// AuthenticationService.java — delta
void updateAvatarKey(UUID userId, String key);
```

```java
// AuthenticationServiceImpl.java — delta (dentro de la clase)
@Override
@Transactional
public void updateAvatarKey(UUID userId, String key) {
    UserEntity user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
    user.setAvatarUrl(key);
    userRepository.save(user);
}
```

### Modificación a `GlobalExceptionHandler` (+ ConstraintViolationException)

```java
// GlobalExceptionHandler.java — delta (nuevo método)
@ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
public ResponseEntity<ProblemDetail> handleConstraintViolationException(jakarta.validation.ConstraintViolationException ex) { /* 400 + ProblemDetail */ }
```

### Bloque yaml en application*.yaml

Ver sección "Configuración" más abajo.

## Configuración

### `application.yaml` (base)

```yaml
app:
  aws:
    s3:
      access-key-id: ${AWS_ACCESS_KEY_ID:}
      secret-access-key: ${AWS_SECRET_ACCESS_KEY:}
      region: ${AWS_REGION:}
      endpoint: ${AWS_ENDPOINT:}
      bucket-profile-name: ${AWS_BUCKET_PROFILE_NAME:}
      bucket-publication-name: ${AWS_BUCKET_PUBLICATION_NAME:}
      bucket-profile-url: ${AWS_BUCKET_PROFILE_URL:}
      bucket-publication-url: ${AWS_BUCKET_PUBLICATION_URL:}
```

> Nota: en base no se setean defaults reales (sería un anti-patrón sobreescribir credenciales). Se referencian env vars con default vacío; `AwsS3Properties` con `@NotBlank` falla el binding si llegan vacías al contexto.

### `application-dev.yaml`

```yaml
app:
  aws:
    s3:
      access-key-id: ${AWS_ACCESS_KEY_ID:test}
      secret-access-key: ${AWS_SECRET_ACCESS_KEY:test}
      region: ${AWS_REGION:us-east-1}
      endpoint: ${AWS_ENDPOINT:http://localhost:4566}
      bucket-profile-name: ${AWS_BUCKET_PROFILE_NAME:tiendauni-profile-dev}
      bucket-publication-name: ${AWS_BUCKET_PUBLICATION_NAME:tiendauni-publication-dev}
      bucket-profile-url: ${AWS_BUCKET_PROFILE_URL:http://localhost:4566/tiendauni-profile-dev}
      bucket-publication-url: ${AWS_BUCKET_PUBLICATION_URL:http://localhost:4566/tiendauni-publication-dev}
```

> Default apuntando a LocalStack (`localhost:4566`). Tests pueden usar MinIO con la misma URL cambiando sólo el puerto.

### `application-prod.yaml`

```yaml
app:
  aws:
    s3:
      access-key-id: ${AWS_ACCESS_KEY_ID}
      secret-access-key: ${AWS_SECRET_ACCESS_KEY}
      region: ${AWS_REGION}
      endpoint: ${AWS_ENDPOINT}
      bucket-profile-name: ${AWS_BUCKET_PROFILE_NAME}
      bucket-publication-name: ${AWS_BUCKET_PUBLICATION_NAME}
      bucket-profile-url: ${AWS_BUCKET_PROFILE_URL}
      bucket-publication-url: ${AWS_BUCKET_PUBLICATION_URL}
```

> Sin defaults: si falta cualquiera, `@NotBlank` + Spring Boot hacen fallar el contexto (fail-fast, AC-CFG-2).

## Tests mínimos

Lista de tests a escribir:

- **`PresignedUrlServiceImplTest`** (`@ExtendWith(MockitoExtension.class)`):
  - `generateProfilePresignedUrl_validRequest_returnsPresignedUrl` — happy path; verifica que la key resultante tiene formato `profiles/<userId>/<uuid>.<ext>`. (El `endpointOverride` del presigner mockeado coincide con el provider endpoint, no con el bucket URL público — el bucket se identifica vía `PutObjectRequest.bucket(...)`.)
  - `generateProfilePresignedUrl_s3PresignerThrows_wrapsInPresignedUrlGenerationException` — mockea `profileS3Presigner.presignPutObject` para que lance `SdkException`; verifica que el servicio lanza `PresignedUrlGenerationException` con la causa envuelta.
  - `generatePublicationPresignedUrls_preservesOrderAndIds` — 3 elementos de entrada; verifica que la lista de salida preserva orden 1-a-1 y que los ids coinciden.
  - `generatePublicationPresignedUrls_emptyList_throwsValidationBeforeService` — se cubre por validación `@NotEmpty`; test documenta que el servicio no se invoca.

- **`S3KeyGeneratorTest`**:
  - `generateProfileKey_returnsProfilesUuidUuidExt`
  - `generatePublicationKey_returnsPublicationsUuidUuidExt`
  - `generateKey_unknownMimeType_throwsIllegalArgument` — mimeType `"application/octet-stream"`; verifica que el helper lanza `IllegalArgumentException` (el servicio la envuelve en `PresignedUrlGenerationException`).

- **`ValidFileSizeValidatorTest`**:
  - `imageWithinCap_passes` — `image/jpeg`, 10 MB exactos → válido.
  - `imageOverCap_fails` — `image/jpeg`, 10 MB + 1 byte → inválido.
  - `videoWithinCap_passes` — `video/mp4`, 50 MB exactos → válido.
  - `videoOverCap_fails` — `video/mp4`, 50 MB + 1 byte → inválido.
  - `svgTreatedAsImage_failsOver10Mb` — `image/svg+xml`, 10.5 MB → inválido (cap de imagen, NO de video).

- **`UniqueFileIdsValidatorTest`**:
  - `uniqueIds_passes` — 3 ids distintos → válido.
  - `duplicateIds_fails` — 2 elementos con `id = "file1"` → inválido.

- **`ProfilePresignedUrlControllerWebMvcTest`** (`@WebMvcTest(ProfilePresignedUrlController.class)` con `@MockitoBean` para `PresignedUrlService`):
  - `validRequest_returns200WithUrl` — body válido; `MockMvc` recibe `200` y el JSON tiene `{"url": "..."}`.
  - `invalidMimeType_returns400` — `mimeType: "text/plain"`; recibe `400` con `type = /problems/validations`.
  - `overSize_returns400` — `image/jpeg`, 11 MB; recibe `400`.
  - `unauthenticated_returns401` — sin cookie `access-token`; recibe `401`.

- **`ProfileAvatarControllerWebMvcTest`** (`@WebMvcTest`):
  - `validKey_returns204` — `key` válida; recibe `204`.
  - `invalidKeyFormat_returns400` — `key: "../escape"`; recibe `400`.
  - `userNotFound_returns404` — mockea `AuthenticationService` para lanzar `UserNotFoundException`; recibe `404` con `type = /problems/user-not-found`.
  - `unauthenticated_returns401` — sin cookie; recibe `401`.

- **`PublicationPresignedUrlControllerWebMvcTest`** (`@WebMvcTest`):
  - `validFiles_returns200WithPreservedOrder` — 3 elementos con ids mixtos; verifica orden 1-a-1.
  - `duplicateIds_returns400` — 2 elementos con mismo `id`; recibe `400` con detalle listando los ids.
  - `emptyArray_returns400` — `files: []`; recibe `400`.
  - `over10Files_returns400` — 11 elementos; recibe `400`.
  - `unauthenticated_returns401` — sin cookie; recibe `401`.

- **`PresignedUrlExceptionHandlerTest`**:
  - `presignedUrlGenerationException_returns503WithProblemDetail` — invocación directa del handler; verifica `status = 503`, `type = /problems/presigned-url-generation-failed`, `title = "Presigned Url Generation Failed"`.

- **`AwsS3ConfigurationTest`** (`ApplicationContextRunner`):
  - `contextLoad_succeedsWithValidProps` — props completas; el contexto carga y hay dos beans `S3Presigner` que comparten el mismo `endpointOverride` desde `app.aws.s3.endpoint`.
  - `missingBucketProfileUrl_failsToStart` — prop `app.aws.s3.bucket-profile-url` removida; `ApplicationContextRunner` espera excepción de binding.

- **`AuthenticationServiceImpl_updateAvatarKey_Test`** (unit test del método nuevo):
  - `validKey_updatesEntityAndPersists` — `findById` devuelve entidad; verifica `entity.setAvatarUrl(key)` llamado y `save` invocado.
  - `userNotFound_throwsUserNotFoundException` — `findById` devuelve `Optional.empty()`; verifica lanzamiento de `UserNotFoundException`.

## Plan de migración (orden de implementación)

Orden recomendado para `sdd-apply` (secuencia lógica, no es `tasks.md`):

1. **`AwsS3Properties`** + **`AwsS3Configuration`** + bloque yaml en `application*.yaml`. Sin esto no se puede arrancar el contexto.
2. **`BucketTarget`** + **`PresignedUrlMimeExtension`** + **`S3KeyGenerator`**. Modelos internos sin dependencias externas.
3. **`@ValidFileSize`** + **`ValidFileSizeValidator`** + tests. Validador reusable por los DTOs siguientes.
4. **DTOs Request/Response** (`PresignedUrlItem`, `PresignedUrlProfileRequest`, `PresignedUrlProfileResponse`, `PresignedUrlPublicationRequest`, `PresignedUrlPublicationResponse`, `UpdateAvatarKeyRequest`).
5. **`PresignedUrlService`** (interfaz) + **`PresignedUrlServiceImpl`** + tests con Mockito.
6. **`ProfilePresignedUrlController`** + tests `@WebMvcTest`.
7. **`ProfileAvatarController`** + tests `@WebMvcTest`.
8. **`PresignedUrlGenerationException`** + **`PresignedUrlExceptionHandler`** + tests + extensión de **`GlobalExceptionHandler`** para `ConstraintViolationException`.
9. **`@UniqueFileIds`** + **`UniqueFileIdsValidator`** + tests.
10. **`PublicationPresignedUrlController`** + tests `@WebMvcTest`.
11. **`AuthenticationService.updateAvatarKey`** (interfaz + impl) + tests.

## Riesgos de implementación

- **El primer `@ConfigurationProperties` del proyecto puede romper el binding si los nombres no calzan exactamente entre YAML y record components.** Mitigación: tests de carga de contexto (`AwsS3ConfigurationTest`) + propiedad explícita `prefix = "app.aws.s3"` + los nombres de los record components ya alineados con kebab-case en el YAML (Spring Boot 3.x hace el mapeo automático).
- **`@ValidFileSize` es class-level** y necesita acceder al `mimeType` y `size` del mismo record. Hibernate Validator lo soporta: `@Constraint(validatedBy = ValidFileSizeValidator.class)` con `Target = TYPE`, y `ConstraintValidator<ValidFileSize, Object>` para que sirva tanto para `PresignedUrlProfileRequest` como para cualquier otro record que lo use. El validador hace `instanceof` y dispatch. Documentar este patrón en el comentario de la clase.
- **El orden de los `@RestControllerAdvice` matters.** `GlobalExceptionHandler` está en `@Order(Ordered.HIGHEST_PRECEDENCE)`. El nuevo `PresignedUrlExceptionHandler` usa `@Order(20)` (valor explícito más bajo = mayor precedencia) para no comerse excepciones del `GlobalExceptionHandler` pero sí capturar `PresignedUrlGenerationException` antes de que caiga al default (que devolvería 500). Documentar el `@Order` y por qué.
- **El test `missingBucketProfileUrl_failsToStart`** requiere un `ApplicationContextRunner` con `withUserConfiguration(AwsS3Configuration.class).withPropertyValues(...)` o un `@SpringBootTest(properties = "...")` con la prop removida. Se opta por `ApplicationContextRunner` por velocidad y aislamiento. Documentar el patrón.
- **Cambios en `application-prod.yaml`**: las env vars AWS_* deben quedar documentadas en README (out of scope del código, pero importante). El task de apply que toca `application-prod.yaml` debe incluir un reminder para actualizar el README.
- **El `S3Presigner` mockeado para los tests web** debe devolver `PresignedPutObjectRequest` con `url()` no nulo. Como el constructor es privado, se usa `PresignedPutObjectRequest.builder().expiration(...).signedHeaders(...).signature(...).identity(...).request(PutObjectRequest.builder()...).build()` o se crea un helper estático en el test que arma el objeto. Documentar el helper.
- **`PresignedUrlMimeExtension` enum y la whitelist de `@Pattern` en el DTO** son dos fuentes de verdad. Si alguien agrega un mimeType en uno pero no en el otro, hay drift. Mitigación: el regex del `@Pattern` se construye a partir de los nombres del enum en un test (`PresignedUrlMimeExtension_whitelistMatchesDtoPattern`).

## Out of scope del diseño (referencia)

Recordatorio del out of scope del `proposal.md` para evitar que se cuele scope nuevo en `apply`:

- `POST /posts` (creación efectiva de publicación, persistencia de `publications` y `publication_media`).
- Mapeo JPA de `publications` y `publication_media` (las tablas SQL existen; las entidades JPA no se crean en este cambio).
- Endpoint `GET` pre-firmado para lectura de assets (el bucket es público).
- Limpieza asíncrona de objetos huérfanos en S3 (política leave-it).
- Lifecycle rules, IAM policies, S3 CORS en el bucket (infraestructura, no código).
- Migración efectiva de AWS S3 a Cloudflare R2 (sí se construye el camino: `endpointOverride` + env vars; no se ejecuta la migración en este cambio).
- Soporte de `multipart/form-data` en el backend.
- Cualquier tipo de validación por bytes del archivo subido (la validación real de tamaño se delega a S3 vía el `Content-Length` firmado en la URL pre-firmada).
- Endpoints `GET` pre-firmados para mostrar assets (la URL pública la construye el frontend con `bucketProfileUrl + key` / `bucketPublicationUrl + key`).
