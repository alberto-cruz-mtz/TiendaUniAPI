INSERT INTO universities(name, acronym, logo_url, brand_color)
VALUES ('Universidad Tecnológica del Sureste de Veracruz', 'UTSV',
        'https://lh5.googleusercontent.com/proxy/TXbH0sLBa0j0d7TQNP7QO8l_Sca2u5P4SwiVzrxGCtbvSW5oAd9Y20D6LHeu0fwTKO92A_33zbpt3GJlI4V-EA',
        '#237F45'),
       ('Universidad Nacional Autónoma de México', 'UNAM',
        'https://lh5.googleusercontent.com/proxy/TXbH0sLBa0j0d7TQNP7QO8l_Sca2u5P4SwiVzrxGCtbvSW5oAd9Y20D6LHeu0fwTKO92A_33zbpt3GJlI4V-EA',
        '#0000FF');

INSERT INTO email_domains(domain, university_id)
VALUES ('alumnos.utsv.edu.mx', (SELECT id FROM universities WHERE acronym = 'UTSV')),
       ('docentes.utsv.edu.mx', (SELECT id FROM universities WHERE acronym = 'UTSV')),
       ('administrativos.utsv.edu.mx', (SELECT id FROM universities WHERE acronym = 'UTSV')),
       ('alumnos.unam.mx', (SELECT id FROM universities WHERE acronym = 'UNAM')),
       ('docentes.unam.mx', (SELECT id FROM universities WHERE acronym = 'UNAM')),
       ('administrativos.unam.mx', (SELECT id FROM universities WHERE acronym = 'UNAM'));