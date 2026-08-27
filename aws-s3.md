# Implementar AWS S3 para pre-firmado de URLs

Necesitamos implementar AWS S3 para generar URLs pre-firmadas que permitan a los usuarios desde el frontend subir sus
archivos directamente al bucket evitando tener que subir el/los archivos directamnete al backend.

Esto soluciona 3 problemas:

- Ahorra recursos en la carga del backend en recibir archivos grandes como imagenes y videos.
- Reduce la complejidad de implementar un endpoint de tipo *multipart/form-data* en el backend.
- Permite implementar logica de optimizacion de imagenes y videos en el frontend antes de subirlos al bucket.

## Responsabilidad del backend

El backend solo se encarga de recibir ciertos datos del archivo/archivos que se van a subir al bucket y generar una URL
pre-firmada para que el frontend pueda subir el archivo directamente al bucket.

Las tareas que realizara el backend seran las siguientes:

- Recibir una request mediiante un endpoint REST con los datos del archivo en el body de la request.
- Valida que los datos del archivo sean correctos como: Nombre, Tamaño y MimeType.
- Genera la URL pre-firmada con una key unica para el archivo y un tiempo de expiracion de 5 minutos.
- Retorna la URL pre-firmada al frontend para que este pueda subir el archivo directamente al bucket.

## Alcance de la implementacion

La generacion de las URLs pre-firmadas seran utilizadas para dos propositos:

- Subir imagenes de perfil de usuario.
- Subir imagenes y video de publicaciones de productos.

## Datos de entrada

Los datos que se enviaran desde el frontend al backend seran:

| Campo    | Tipo                 | Descripción                                              | Validaciones                                                                                                           |
|----------|----------------------|----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| fileName | string               | Nombre del archivo a subir al bucket                     | No puede ser nulo o una cadena vacía; solo puede contener letras, números, guiones medios y bajos (a-zA-Z0-9_-).       |
| size     | Long/Bytes[] (bytes) | Tamaño del archivo en bytes                              | Debe ser mayor que 0 y no exceder el máximo permitido (imagenes:  menor o igual a 10MB, videos: menor o igual a 50MB). |
| mimeType | string               | Tipo MIME del archivo (por ejemplo image/png, video/mp4) | Debe pertenecer a la lista blanca de tipos permitidos (image/jpeg, image/png, video/mp4).                              |

> las validaciones deben realizarse mediante un DTO con anotaciones de jakarta validation

## Lista blanca de tipos MIME permitidos

- image/jpeg
- image/jpg
- image/png
- image/gif
- image/webp
- image/svg+xml
- video/mp4
- video/webm
- video/ogg

## Configuracion de AWS S3

### Lectura de propiedades:

La configuracion de AWS S3 se realizara mediante variables de entorno que seran leidas por el backend al iniciar la
`application.yaml` como:

- `AWS_ACCESS_KEY_ID`: Clave de acceso de AWS.
- `AWS_SECRET_ACCESS_KEY`: Clave secreta de AWS.
- `AWS_REGION`: Region de AWS donde se encuentra el bucket.
- `AWS_BUCKET_PROFILE_NAME`: Nombre del bucket de perfil (avatares) de AWS S3.
- `AWS_BUCKET_PUBLICATION_NAME`: Nombre del bucket de publicaciones (imagenes y videos) de AWS S3.
- `AWS_BUCKET_PROFILE_URL`: URL del bucket de perfil (avatares) de AWS S3.
- `AWS_BUCKET_PUBLICATION_URL`: URL del bucket de publicaciones (imagenes y videos) de AWS S3.

Para leer las propiedades del archivo `application.yaml` se utilizara un record con la anotacion
`@ConfigurationProperties` de Spring Boot, que permitira mapear las propiedades del archivo a un objeto Java.

```java
record AwsS3Properties(
        String accessKeyId,
        String secretAccessKey,
        String region,
        String bucketProfileName,
        String bucketPublicationName,
        String bucketProfileUrl,
        String bucketPublicationUrl
) {
}
```

### Clase de configuracion:

La configuracion de AWS S3 se realizara en un archivo de configuracion anotado con `@Configuration` y
`@EnableConfigurationProperties(AwsS3Properties.class)` , que creara un bean de tipo S3Client que sera utilizado para
generar las URLs pre-firmadas. Ejemplo:

```java

@Bean
public S3Presigner s3Presigner() {
    AwsBasicCredentials credentials = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());
    Region region = Region.of(properties.region());
    URI s3Url = URI.create(properties.endpoint());

    return S3Presigner.builder()
            .region(region)
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(s3Url)
            .build();
}
```

### Servicio de generacion de URLs pre-firmadas

Se necesita crear una clase de servicio que se encargue de generar las URLs pre-firmadas para subir archivos al bucket
de AWS S3. Esta clase se recibiran los datos de la request con la cual se generara la URL pre-firmada y se retornara al
controlador para que este la retorne al frontend.

Este servicio debe permitir generar URLs pre-firmadas ya sea si se envia un archivo o varios archivos como es el caso de
las publicaciones de productos que pueden tener varias imagenes y videos ademas dado que solo abran 2 buckets
disponibles mediante un enum el metodo del servicio debe determinar el bucket de destino.

**Enum de buckets**: Ejemplo de enum que determina el bucket de destino para la generacion de las URLs pre-firmadas.

```java
enum BucketTarget {
    PROFILE, PUBLICATION
}

private final AwsS3Properties properties;

public String determinateBucket(BucketTarget target) {
    return switch (target) {
        case PROFILE -> properties.bucketProfileName();
        case PUBLICATION -> properties.bucketPublicationName();
    };
}
```

El metodo de generacion de las URLs pre-firmadas para el caso de los archivos de perfil debera recibir un dato extra, un
ID ya sea numerico o alfanumerico que sera utilizado por el frontend ya que las publicaciones deben mantener un orden
que el usuario desea por ejemplo si el usuario desea subir 3 imagenes y un video en ese orden, el frontend debe mantener
ese orden y para ello se debe enviar un ID unico (se encarga el frontend al backend no le importa el valor) para cada
archivo que sera utilizado para que la respuesta del backend con las URLs mantenga ese ID en cada URL para determinar a
que archivo corresponde cada URL pre-firmada.

**Request Body**: Ejemplo de envio de datos desde el frontend al backend para generar las URLs pre-firmadas para subir
archivos al bucket de AWS S3.

```json
{
  "files": [
    {
      "id": "file1",
      "name": "image1.jpg",
      "size": 102400,
      "mimeType": "image/jpeg"
    },
    {
      "id": "2",
      "name": "image3.png",
      "size": 102400,
      "mimeType": "image/png"
    },
    {
      "id": "7194b889-868c-47c8-8431-4cb4464a15a4",
      "name": "image5.svg",
      "size": 102400,
      "mimeType": "image/svg+xml"
    }
  ]
}
```

El servicio al final termina siendo agnostico independientemente el bucket de destino asi como los archivos a subir, ya
que solo se encarga de generar las URLs pre-firmadas y retornar un objeto con la URL

## Endpoints

**Pefil**:

- `/profiles/presigned-url` (POST) - Genera una URL pre-firmada para subir un archivo de perfil al bucket de AWS S3.
- `/profiles` (PUT/PATCH) - Recibe la URL final del archivo ya subido en el bucket, busca al usuario autenticado y
  actualiza el campo `avatar_url` solo con el recurso o una parte de la URL sin el dominio evitando acoplar el dominio
  del provedor con la BD si se llega a cambiar de provedor.

**Publicacion**:

- `/posts/presigned-url` (POST) - Genera una URL pre-firmada para subir uno o varios archivos de publicacion al bucket
  de AWS S3.
- `/posts` (POST) - Recibe la URL final de los archivos ya subidos en el bucket junto con los datos de la publicacion y
  crea la publicacion en la BD, guardando solo el recurso o una parte de la URL sin el dominio evitando acoplar el
  dominio del provedor con la BD si se llega a cambiar de provedor. Este endpoint aun no sera implementado ya que se
  necesita primero planear el diseño de los endpoints de publicaciones.

## Respuestas de los endpoints

**Perfil**:

- `/profiles/presigned-url` (POST | 200).

```json
{
  "url": "https://bucket-profile.s3.amazonaws.com/unique-key?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ACCESS_KEY%2F20230612%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20230612T000000Z&X-Amz-Expires=300&X-Amz-SignedHeaders=host&X-Amz-Signature=SIGNATURE"
}
```

- `/profiles` (PUT/PATCH | 204).

**Sin cuerpo**

**Publicacion**:

- `/posts/presigned-url` (POST | 200)

```json
{
  "uris": [
    {
      "id": "file1",
      "url": "https://bucket-publication.s3.amazonaws.com/unique-key?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ACCESS_KEY%2F20230612%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20230612T000000Z&X-Amz-Expires=300&X-Amz-SignedHeaders=host&X-Amz-Signature=SIGNATURE",
      "key": "unique-key"
    }
  ]
}
```

## Flujo de trabajo

```plaintext
Cliente                 Endpoint (Controlador)            DTO               Servicio Pre-firmado
     │                              │                        │                         │
     │ 1. Request (Body con datos)  │                        │                         │
     ├─────────────────────────────►│                        │                         │
     │                              │                        │                         │
     │                              │ 2. Valida campos       │                         │
     │                              ├───────────────────────►│                         │
     │                              │                        │                         │
     │                              │  [X] Falla validación  │                         │
     │ ◄┈┈┈┈ Excepción Controlada ┈┈┼┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┤                         │
     │                              │                        │                         │
     │                              │  [✓] Validación OK     │                         │
     │                              │◄┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┤                         │
     │                              │                        │                         │
     │                              │ 3. Envía datos validos │                         │
     │                              ├────────────────────────┼────────────────────────►│
     │                              │                        │                         │
     │                              │                        │ 4. Determina Bucket     │
     │                              │                        │ ↻                       │
     │                              │                        │                         │
     │                              │                        │ 5. Genera URL Prefirmada│
     │                              │                        │ ↻                       │
     │                              │                        │                         │
     │                              │ 5. Devuelve Objeto URL │                         │
     │                              │◄┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┼┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┤
     │                              │                        │                         │
     │ 6. Response (Objeto con URL) │                        │                         │
     ◄──────────────────────────────┤                        │                         │
     │                              │                        │                         │
```