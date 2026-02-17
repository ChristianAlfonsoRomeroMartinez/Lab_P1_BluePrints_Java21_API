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
**Archivo:** [schema.sql](postgres_data/schema.sql)

#### data.sql - Datos iniciales
**Archivo:** [data.sql](postgres_data/data.sql)

Inserta 3 blueprints de prueba

**Total:** 3 blueprints, 10 puntos

---

### Configuración de Spring Boot

**Archivo:** [application.properties](src/main/resources/application.properties)

Los scripts SQL se ejecutan automáticamente desde Docker, configuré `spring.sql.init.mode=never` para evitar ejecución duplicada.

---

### Dependencias Maven

**Archivo:** [pom.xml](pom.xml)

Agregué las dependencias necesarias para conectar Java con PostgreSQL:

---

### Implementación PostgresBlueprintPersistence

**Archivo:** [PostgresBlueprintPersistence.java](src/main/java/edu/eci/arsw/blueprints/persistence/PostgresBlueprintPersistence.java)

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

## Parte 3 - Buenas prácticas de API REST



### 1. Versionamiento de API - Path base `/api/v1/blueprints`

**Cambio realizado en:** [BlueprintsAPIController.java](src/main/java/edu/eci/arsw/blueprints/controllers/BlueprintsAPIController.java)

```java

@RequestMapping("/api/v1/blueprints")  

```

**Antes:** `http://localhost:8080/blueprints`  
**Ahora:** `http://localhost:8080/api/v1/blueprints`

- **`/api/`**: Separa endpoints REST de páginas web estáticas o frontend

  - Permite mantener múltiples versiones en paralelo durante migraciones

- Tambien se modificaron los metodos con para usar la clase ApiResponse en los end points

---

### 2. Clase `ApiResponse<T>` - Respuestas uniformes

**Archivo:** [ApiResponse.java](src/main/java/edu/eci/arsw/blueprints/dto/ApiResponse.java)



#### `record` 
- **Inmutabilidad**
- **Getters automáticos**: 
- **JSON limpio**: Serializa directamente a `{"code":200, "message":"...", "data":{...}}`

#### Factory methods implementados:

| Método | Código HTTP | Uso |
|--------|-------------|-----|
| `success(T data)` | 200 | Consultas exitosas (GET) |
| `created(T data)` | 201 | Recurso creado (POST) |
| `accepted()` | 202 | Actualización aceptada (PUT) |
| `badRequest(String msg)` | 400 | Validación fallida |
| `notFound(String msg)` | 404 | Recurso no existe |
| `Error internal server(String msg)` | 500 | Error en el servidor |
| `error(int code, String msg)` | Personalizado | Errores genéricos |

#### Formato JSON de respuesta:
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "author": "john",
    "name": "house",
    "points": [{"x":0,"y":0}, {"x":10,"y":0}]
  }
}
```


---

#### Tabla de códigos implementados:

| Endpoint | Método | Éxito | Error |
|----------|--------|-------|-------|
| `/api/v1/blueprints` | **GET** | `200 OK` | - |
| `/api/v1/blueprints/{author}` | **GET** | `200 OK` | `404 Not Found` |
| `/api/v1/blueprints/{author}/{name}` | **GET** | `200 OK` | `404 Not Found` |
| `/api/v1/blueprints` | **POST** | **`201 Created`** | `400 Bad Request` |
| `/api/v1/blueprints/{author}/{name}/points` | **PUT** | **`202 Accepted`** | `404 Not Found` |


### Capturas de funcionamiento

![1](img/3/1.png)
![2](img/3/2.png)
![3](img/3/3.png)

---


## Parte 4 - OpenAPI / Swagger

### Configuración de springdoc-openapi

Ya contaba con la dependencia de `springdoc-openapi` en `pom.xml`:


Esta dependencia proporciona:
- OpenAPI 3.0
- **Swagger UI
- Generación automática

---

### Implementación

#### 1. Configuración mejorada de OpenAPI

**Archivo:** [OpenApiConfig.java](src/main/java/edu/eci/arsw/blueprints/config/OpenApiConfig.java)

#### 2. Anotaciones en el controlador

Agregamos anotaciones OpenAPI a cada endpoint:

**Archivo:** [BlueprintsAPIController.java](src/main/java/edu/eci/arsw/blueprints/controllers/BlueprintsAPIController.java)

**Anotaciones utilizadas:**
- `@Tag` - Agrupa endpoints relacionados
- `@Operation` - Describe el endpoint
- `@ApiResponses` - Documenta las respuestas HTTP posibles
- `@ApiResponse` - Define cada código de respuesta
- `@Content` - Especifica el tipo de contenido en la respuesta
- `@Schema` - Referencia el modelo de datos

---

#### 3. Tabla de endpoints documentados

| Endpoint | Método | Summary | Respuestas HTTP |
|----------|--------|---------|-----------------|
| `/api/v1/blueprints` | **GET** | Obtener todos los blueprints | `200` |
| `/api/v1/blueprints/{author}` | **GET** | Obtener blueprints por autor | `200`, `404` |
| `/api/v1/blueprints/{author}/{bpname}` | **GET** | Obtener un blueprint específico | `200`, `404` |
| `/api/v1/blueprints` | **POST** | Crear un nuevo blueprint | `201`, `400` |
| `/api/v1/blueprints/{author}/{bpname}/points` | **PUT** | Agregar un punto a un blueprint | `202`, `404` |

---

### Acceso a la documentación

**Acceso Swagger UI:**

 http://localhost:8081/swagger-ui.html

#### OpenAPI JSON
http://localhost:8081/v3/api-docs

![1](img/4/1.png)
![2](img/4/2.png)

---

## Parte 5 - Filtros de Blueprints

### Implementación de filtros

Los filtros ya los habiamos implementado desde puntos anteriores como lo fue:
- IdentityFilter 
- RedundancyFilter 
- UndersamplingFilter 

**Interfaz base:** [BlueprintsFilter.java](src/main/java/edu/eci/arsw/blueprints/filters/BlueprintsFilter.java)


### Filtros implementados

#### 1. IdentityFilter (por defecto)

**Archivo:** [IdentityFilter.java](src/main/java/edu/eci/arsw/blueprints/filters/IdentityFilter.java)

- No modifica el blueprint, lo retorna tal cual.

- Activo cuando no se especifica ningún perfil de Spring.


---

#### 2. RedundancyFilter

**Archivo:** [RedundancyFilter.java](src/main/java/edu/eci/arsw/blueprints/filters/RedundancyFilter.java)

- Elimina puntos consecutivos duplicados (misma coordenada x,y).

- `redundancy`


**Se utiliza cundo**
- Datos capturados con puntos repetidos por error de sensores
- Dibujos con trazos que permanecen en la misma posición
- Optimización de almacenamiento eliminando redundancias

---

#### 3. UndersamplingFilter

**Archivo:** [UndersamplingFilter.java](src/main/java/edu/eci/arsw/blueprints/filters/UndersamplingFilter.java)

- Reduce la densidad conservando 1 de cada 2 puntos (índices pares: 0, 2, 4...).

- `undersampling`

**Se utiliza cunado:**
1. Recorre los puntos del blueprint
2. Solo conserva puntos en índices pares (i % 2 == 0)
3. Si hay ≤ 2 puntos, retorna sin modificar

- Reducir ancho de banda en transmisión de datos
- Visualización de previews con menor nivel de detalle
- Procesamiento más rápido con menos puntos

---

### Integración con el servicio

**Archivo:** [BlueprintsServices.java](src/main/java/edu/eci/arsw/blueprints/services/BlueprintsServices.java)


- El filtro solo se aplica en `getBlueprint()` individual, no en `getAllBlueprints()` ni `getBlueprintsByAuthor()`.

![1](img/5/1.png)

