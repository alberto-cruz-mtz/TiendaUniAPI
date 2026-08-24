# TiendaUniAPI

Este proyecto es una API REST desarrollada para la gestion de ventas de estudiantes universitarios eliminando el uso de grupos de whatsapp y otras redes sociales para la divulgacion de sus productos. TiendaUni permite a los estudiantes crear y postear publicaciones tomando como referencia el marketplace de Facebook, pero con la diferencia de que esta API esta enfocada en la gestion de ventas, publicaciones, productos, usuarios, universidades y categorias de productos.

## Stack Tecnologico

- Spring Boot '4.1.1'
- Spring Actuator
- Spring Security
- Spring Data JPA
- Spring Validation
- Lombok
- PostgreSQL
- Gradle

## Estructura del proyecto

El proeycto esta estructurado con una arquitectura de capas (persistencia, servicios y controladores). A continuación se describe la estructura de carpetas y archivos principales:

- persistence: Contiene las clases de entidad, repositorios y modelos de datos que interactúan con la base de datos.
- services: Contiene la logica de negocio de la aplicacion como interfaces para inyeccion de dependencia, clases de implementacion de interfaces y excepciones personalizadas para los casos de error especificos.
- presentation: Contiene los controladores que manejan las solicitudes HTTP y devuelven las respuestas correspondientes, controladores de excepcion o advice que se encargan de responder con codigos de error ya controlados apartir de las excepciones personalizadas, asi como los DTOs (Data Transfer Objects) para la transferencia de datos entre capas.

## Reglas de mapeo de entidades

el mapeo de entidades se realiza tomando como referencia el schema de la base de datos en SQL si no es proporcionado negar la peticion y solicitarlo, ademas durante el mapeo de entidades se deben seguir las siguientes reglas:

- Campos de fechas: Todos los campos de fecha como 'createdAt' y 'updatedAt' deben ser mapeados como tipo de dato 'Instant' en Java.
- propiedades de columna con @Column: Se deben agregar las caracteristicas de la columna como 'name', 'nullable', 'lenght' y 'unique' en la anotacion @Column para reflejar las restricciones de la base de datos siempre y cuando tambien esten presentes en el SQL del schema la unica excepcion es el campo 'id' si el campo ID tiene un nombre diferente como 'user_id' o cualquier otro si se debera agregar la anotacion @Column con sus respectivas propiedades.
- propiedad 'columnDefinition' en @Column: Se debe agregar la propiedad 'columnDefinition' en la anotacion @Column para reflejar el tipo de dato de la columna en la base de datos si es un dato que no se pueda representar correctamente con un tipo/clase de java, por ejemplo 'NUMERIC/DECIMAL(10,2)' | 'UUID' @Column(..., columnDefinition = 'NUMERIC(10,2)').
- manejo de auditing: Usar el Jpa Autiditing de Spring Data para el manejo de auditoria de las entidades, agregando las anotaciones @CreatedDate y @LastModifiedDate en los campos 'createAt' y 'updatedAt' o de preferencia con la clase 'AuditableEntity' en caso de existir.
- mapeo de relaciones: Se deben mapear las relaciones entre entidades usando las anotaciones de JPA como @OneToMany, @ManyToOne, @OneToOne y @ManyToMany, especificando correctamente los atributos 'mappedBy', 'cascade' y 'fetch' según corresponda.
- uso de @JoinColumn: Se debe usar la anotacion @JoinColumn para especificar la columna que se usara como clave foranea en las relaciones entre entidades, indicando el nombre de la columna y si es nullable o no.
- uso de @JoinTable: Se debe usar la anotacion @JoinTable para especificar la tabla intermedia que se usara en las relaciones @ManyToMany, indicando el nombre de la tabla y las columnas que se usaran como claves foraneas con la anotacion @JoinColumn.
- **Siempre** agregar el nombre de la tabla en la anotacion @Table de la clase de entidad, y el nombre de la columna en la anotacion @Column de cada campo de la clase de entidad, para reflejar correctamente los nombres de las tablas y columnas en la base de datos.
- Los IDs deben ser marcados con @GeneratedValue(strategy = GenerationType.IDENTITY) si son de tipo autoincrementales en la base de datos, y deben ser de tipo Long (BIGSERIAL o BIGINT) o Integer (SERIAL o INT/INTEGER) si son UUIDs deben ser marcados como @GeneratedValue(strategy = GenerationType.UUID).
- lombok: **Siempre** agregar las anotaciones de lombok @Getter, @Setter, @NoArgsConstructor y @AllArgsConstructor en las clases de entidad para generar automaticamente los metodos getter, setter y constructores.

## Reglas de mapeo de DTOs

el mapeo de DTOs se realiza tomando como referencia las entidades y los requerimientos de la API, ademas durante el mapeo de DTOs se deben seguir las siguientes reglas:

- class o record: Se debe usar de preferencia 'record' para los DTOs, solo se usara 'class' en caso de que haya mas de un record con los mismos campos o si se requiera herencia.
- manejo de fechas: Todos los campos de fecha que haya en un DTO deben ser mapeados como tipo de dato 'OffsetDateTime' en Java.
- sufijo en DTOs: Los DTOs para manejar la entrada de datos en las solucitudes HTTP deben tener el sufijo 'Request' y los DTOs para manejar la salida de datos en las respuestas HTTP deben tener el sufijo 'Response'.
- validaciones de campos: Se deben agregar anotaciones de validacion de 'Jakarta Validation' en los campos de los DTOs para asegurar que los datos recibidos cumplan con los requerimientos de la API, como @NotNull, @Size, @Email, @Pattern, entre otras. Antes de Agregar las validaciones se deben solicitar las validaciones requeridas para cada campo al equipo de desarrollo o al cliente.

## Reglas de mapeo entidades a DTOs

La conversion de entidades a DTOs y viceversa se realiza tomando como referencia las entidades y los requerimientos de la API, ademas durante el mapeo se deben seguir las siguientes reglas:

- Crear mapeadores manuales/personalizados: Se deben crear mapeadores manuales para convertir entidades a DTOs y viceversa dado que no se adoptara el uso de librerias externas como MapStruct o ModelMapper en este momento, pero se podra evaluar su uso en el futuro si se requiere.
- Metodos de mapeo: Los metodos de mapeo deben ser nombrados como 'toDto' para convertir una entidad a un DTO y 'toEntity' para convertir un DTO a una entidad, ademas se deben crear metodos de mapeo para listas de entidades y listas de DTOs nombrados como 'toDtoList' y 'toEntityList' respectivamente.
- Metodos de mapeo en servicios: Los metodos de mapeo deben ser utilizados en los servicios para convertir las entidades obtenidas de la base de datos a DTOs antes de ser devueltos en las respuestas HTTP, y para convertir los DTOs recibidos en las solicitudes HTTP a entidades antes de ser guardados en la base de datos.
- Metodos estaticos: Los metodos de mapeo deben ser declarados como 'static' para poder ser utilizados sin necesidad de instanciar la clase del mapeador ya que no se depende de un bean para esta tarea.

## Repositorios

Al generar repositorios se deben seguir las siguientes reglas:

- tipo de repositorio: Se debe usar de preferencia 'JpaRepository' para todos los casos.
- anotacion @Repository: **No agregar** la anotacion @Repository en los repositorios, ya que Spring Data JPA la agrega automaticamente al heredar de las interfaces 'repository'.
- metodos de busqueda: Cualquier metodo de busqueda que se agregue en el repositorio debe ser declarado como 'Optional' para evitar el retorno de valores nulos y manejar correctamente los casos de no encontrado la unica excepcion a la regla es en el caso de tablas con datos ya predefinidos donde se espera que siempre sean encontrados pero donde se utilize debe marcase con un 'assert' por seguridad.

## Servicios

Todos los servicios deben estar conformados de una interfaz (para inyeccion de dependencias) y una clase de implementacion (aquel que implementa la interfaz), ademas se deben seguir las siguientes reglas:

- Parametros de entrada: Todos los metodos de los servicios deben recibir como parametros de entrada los DTOs correspondientes a la solicitud HTTP, solo recibiran entidades/objetos o tipos primitivos si son solo entre 2 y 3 campos en el DTO o no se requiera un DTO para ello.
- Inyeccion de dependencias: **Jamas** utilizar la anotacion @Autowired para inyectar dependencias, siempre usar inyeccion por constructor mediante la anotacion @RequiredArgsConstructor de lombok ademas siempre marcar las dependencias de la siguiente manera 'private final Class dependency'.
- inyeccion de valores mediante @Value: Se permite el uso de la anotacion @Value para inyectar valores provenientes de archivos de configuracion como 'application.properties' o 'application.yml', pero solo para valores que no sean entidades, repositorios o servicios.
- constructores: Solo se permitira el uso de constructores manuales en los servicios si se requiere realizar alguna logica adicional en el constructor, de lo contrario se debe usar la anotacion @RequiredArgsConstructor de lombok para generar automaticamente el constructor con las dependencias finales.
- control de excepciones: Todos los servicios deben manejar las excepciones personalizadas definidas en el proyecto para los casos de error especificos, y lanzar las excepciones correspondientes cuando sea necesario. No se deben manejar excepciones genericas como 'Exception' o 'RuntimeException', solo se deben manejar las excepciones personalizadas definidas en el proyecto.
- excepciones genericas: En caso de no contar aun con una excepcion personalizada para un caso de error especifico, se debe utilizar la excepcion personalizada 'UnknownException' la cual sera manejada por el controlador de excepciones global para retornar un mensaje generico de error al cliente, ademas se debe crear una tarea para la creacion de la excepcion personalizada correspondiente.

## Escritura de codigo

En la escritura de codigo se debe seguir el siguiente principio, generar codigo que funcione correctamente y cumpla con el objetivo sin importar la cantidad de lineas de codigo, el desorden, etc. y despues refactorizarlo para que sea mas legible, mantenible y optimizado, siempre y cuando no se rompa la funcionalidad del codigo. Este sigue el principio 'Primero haz que funcione y despues mejoralo'.

Para las refactorizaciones se deben seguir las siguientes reglas:

- Early returns para reducir nesting
- Extracción de funciones pequeñas y reutilizables
- Nombres descriptivos para variables, constantes y metodos
- Regla de 3 si un codigo se repite 3 veces o mas, se debe abstraer a una clase reutilizable solo si el codigo es en clases distintas no aplica si el codigo es solo propio de una misma clase es sus metodos.
- Funciones de creacion nombradas como 'createX' si solo se encargan de crear un objeto y no realizar ninguna logica adicional, de lo contrario usar 'buildX'
- No usar comentarios para explicar el codigo: si algun fragmento de codigo sirve para explicar su funcionamiento hay que refactorizarlo de manera que el comentario ahora se codigo, como en el ejemplo de abajo, solo se permiten comentarios si explica una decision tecnica o de negocio que permita perservar el conocimiento del porque.
- Usar constantes para valores magicos y literales, como numeros, strings, etc. y nombrarlas de manera descriptiva, si solo son dentro de un scope pequeño como un metodo hacerlo de forma interna caso contrario en el scope de la clase.

```java
// Con comentarios

// Si el usuario es premium y supera el monto minimo
// tiene envio gratis.
if(user.getPlan().equals("premium") && total > 1000){
  //.... logic
}

// Sin comentarios

boolean isPremium = user.getPlan().equals("premium");
boolean qualifiesForFreeShipping = total > 1000;

if(isPremium && qualifiesForFreeShipping){
  //.... logic
}

```

## Controladores

Las clases de controladores deben manejar las solicitudes HTTP y devolver las respuestas correspondientes, ademas se deben seguir las siguientes reglas:

- Siempre usar @RestController + @RequestMapping para definir la ruta base del controlador.
- Siempre usar ResponseEntity como tipo de retorno de los metodos del controlador para poder manejar correctamente los codigos de estado HTTP y las respuestas.
- Usar @PathVariable para obtener los parametros de la ruta y @RequestParam para obtener los parametros de consulta.
- Usar la anotacion @Valid en los parametros de entrada de los metodos del controlador para validar los DTOs recibidos en las solicitudes HTTP.
- Usar la anotacion @AuthenticationPrincipal para obtener datos que provengan del usuario autenticado, como el ID del usuario o el rol del usuario, siempre y cuando se requiera para la logica del metodo.
- Jamas usar la anotacion @Autowired para inyectar dependencias, siempre usar inyeccion por constructor mediante la anotacion @RequiredArgsConstructor de lombok ademas siempre marcar las dependencias de la siguiente manera 'private final Class dependency'.
- Control de excepciones: Ningun controlador debe manejar excepciones, todas las excepciones deben ser manejadas por el controlador de excepciones global o especifico para retornar un mensaje generico de error al cliente.
- Los controladores pueden devolver diferentes codigos de respuesta HTTP siempre y cuando sean codigos de exito 2xx no se permiten codigos de error 4xx o 5xx, ya que estos seran manejados por el controlador de excepciones global o especifico para retornar un mensaje generico de error al cliente.
- Los codigos de respuesta HTTP deben ser indicados para poder proceder, en caso de no estar seguro se debe consultar con el equipo de desarrollo o con el cliente para definir el codigo de respuesta HTTP correcto.
