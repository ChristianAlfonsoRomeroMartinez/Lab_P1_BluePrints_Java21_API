# Solucion laboratorio

## Parte 2 - Migración a persistencia en PostgreSQL
Usmaos docker compose para definir el contenedor
[docker-compose.yml](docker-compose.yml)

- Elegi como version de postgres 16 alpine que esta basada en linux alpine y se destaca por ser liguera.

- Guardamos los datos en 'pgdata:/var/lib/postgresql/data'

En el 'pom.xml' agregamos las dependencias para que java hable con postgres
[pom.xml](pom.xml) 

