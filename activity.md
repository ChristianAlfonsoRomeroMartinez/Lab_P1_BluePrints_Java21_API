# Solucion laboratorio

## Parte 2 - Migración a persistencia en PostgreSQL

### Configuración de Docker Compose

Usamos Docker Compose para levantar el contenedor de PostgreSQL

**Archivo:** [docker-compose.yml](docker-compose.yml)


- **PostgreSQL 18.2:** puesto que es la versión más reciente y estable
- **Puerto 5441:** el puerto 5432 ya se encontraba reservado en la PC por que ya tenia instalado postgresql por lo que lo cambie
- **Scripts automáticos:** Los archivos en `/docker-entrypoint-initdb.d/` se ejecutan automáticamente al crear el contenedor por primera vez

---

### Diseño de la base de datos

### **blueprints**

| Campo   | Tipo      |
| ------- | --------- |
| id (PK) | BIGSERIAL |
| author  | VARCHAR   |
| name    | VARCHAR   |

### **points**

| Campo              | Tipo      |
| ------------------ | --------- |
| id (PK)            | BIGSERIAL |
| x                  | INTEGER   |
| y                  | INTEGER   |
| point\_order       | INTEGER   |
| blueprint\_id (FK) | BIGINT    |

#### Tabla `blueprints`
Almacena la información principal de cada plano:
- `id`: Clave primaria autogenerada con `BIGSERIAL`
- `author`: Autor del blueprint
- `name`: Nombre del blueprint

#### Tabla `points`
Almacena los puntos:
- `id`: Clave primaria autogenerada
- `x`, `y`: Coordenadas del punto
- `point_order`: mantiene el orden correcto de los puntos
- `blueprint_id`: Referencia al blueprint padre (Foreign Key)

---

### Constraints e índices implementados

**Constraints:**
- `blueprints.id` → PRIMARY KEY (unicidad)
- `blueprints(author, name)` → **UNIQUE** (no permite duplicados con mismo autor+nombre)
- `points.blueprint_id` → **FOREIGN KEY con ON DELETE CASCADE** (si borras un blueprint, automáticamente borra sus puntos)
- `points.point_order` → CHECK (point_order >= 0) (solo valores positivos)

**Índices para mejorar rendimiento:**
- `idx_blueprint_author` → Acelera búsquedas por autor: `WHERE author = 'john'`
- `idx_point_blueprint` → Acelera búsquedas de puntos: `WHERE blueprint_id = 1`

- **ON DELETE CASCADE** 
---

#### schema.sql - Creación de tablas
**Archivo:** [postgres_data/schema.sql](postgres_data/schema.sql)

#### data.sql - Datos iniciales
**Archivo:** [postgres_data/data.sql](postgres_data/data.sql)

Inserta 3 blueprints de prueba

**Total:** 3 blueprints, 10 puntos

---

### Configuración de Spring Boot

**Archivo:** [src/main/resources/application.properties](src/main/resources/application.properties)

Los scripts SQL se ejecutan automáticamente desde Docker, configuré `spring.sql.init.mode=never` para evitar ejecución duplicada.

---

### Dependencias Maven

**Archivo:** [pom.xml](pom.xml)

Agregué las dependencias necesarias para conectar Java con PostgreSQL:

---

### Implementación PostgresBlueprintPersistence

**Archivo:** [src/main/java/.../persistence/PostgresBlueprintPersistence.java](src/main/java/edu/eci/arsw/blueprints/persistence/PostgresBlueprintPersistence.java)

Esta clase reemplaza a `InMemoryBlueprintPersistence` usando PostgreSQL.

**1. saveBlueprint(Blueprint bp):**
- Verifica que no exista blueprint duplicado
- Inserta el blueprint y captura el ID autogenerado con `KeyHolder`
- Inserta todos los puntos con su `point_order` correcto

**2. getBlueprint(String author, String name):**
- Busca el blueprint por author+name
- Obtiene todos sus puntos **ordenados por point_order** (crucial)
- Reconstruye el objeto Blueprint

**3. getBlueprintsByAuthor(String author):**
- Obtiene todos los blueprints de un autor
- Para cada uno, carga sus puntos ordenados
- Devuelve un Set de Blueprints completos

**4. getAllBlueprints():**
- Similar al anterior pero sin filtro de autor

**5. addPoint(String author, String name, int x, int y):**
- Obtiene el `point_order` máximo actual
- Inserta el nuevo punto con `point_order = max + 1`


### Pruebas de funcionamiento

#### 1. Iniciar PostgreSQL:
```bash
docker-compose up -d
```

#### 2. Verificar que las tablas se crearon:
```bash
docker exec -it blueprints-postgres psql -U blueprints -d blueprints_db -c "\dt"
```

#### 3. Ver datos insertados:
```bash
docker exec -it blueprints-postgres psql -U blueprints -d blueprints_db -c "SELECT * FROM blueprints;"
```

#### 4. Verificar puntos de un blueprint:
```bash
docker exec -it blueprints-postgres psql -U blueprints -d blueprints_db \
  -c "SELECT x, y, point_order FROM points WHERE blueprint_id = 1 ORDER BY point_order;"
```


#### 5. Compilar y ejecutar Spring Boot:
```bash
mvn clean install
mvn spring-boot:run
```

### Capturas de funcionamiento

![1](img/2/1.png)
![2](img/2/2.png)
![3](img/2/3.png)
![4](img/2/4.png)
![5](img/2/5.png)


### Comandos

**Reiniciar base de datos desde cero:**
```bash
docker-compose down -v    # -v elimina volúmenes (datos)
docker-compose up -d      # Los scripts se ejecutan de nuevo
```

**Ver logs de PostgreSQL:**
```bash
docker logs blueprints-postgres
```

**Conectar manualmente a PostgreSQL:**
```bash
docker exec -it blueprints-postgres psql -U blueprints -d blueprints_db
```

**Ver logs SQL de Spring (opcional):**
Agregar a `application.properties`:
```properties
logging.level.org.springframework.jdbc.core=DEBUG
```

---


