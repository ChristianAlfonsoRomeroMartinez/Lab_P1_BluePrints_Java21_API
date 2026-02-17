-- Eliminar tablas existentes (para desarrollo/reinicio)               


DROP TABLE IF EXISTS points CASCADE;
DROP TABLE IF EXISTS blueprints CASCADE;


-- BIGSERIAL: BIGINT con autoincremento automático

CREATE TABLE blueprints (
    id          BIGSERIAL       PRIMARY KEY,
    author      VARCHAR(100)    NOT NULL,
    name        VARCHAR(100)    NOT NULL
);


ALTER TABLE blueprints 
    ADD CONSTRAINT uk_author_name UNIQUE (author, name);

-- Crear ÍNDICE para búsquedas por autor 

CREATE INDEX idx_blueprint_author ON blueprints(author);


-- Almacena los puntos (coordenadas x, y) que componen cada blueprint.
--
-- RELACIÓN: Muchos puntos → Un blueprint (Many-to-One / N:1)
-- - Un blueprint puede tener 0, 1, o muchos puntos
-- - Cada punto pertenece a EXACTAMENTE un blueprint


-- - Los blueprints son secuencias ORDENADAS de puntos (ej: dibujar una línea)
-- - Sin point_order, PostgreSQL podría devolver los puntos en cualquier orden
-- - point_order garantiza que recuperemos los puntos en el orden correcto


CREATE TABLE points (
    id              BIGSERIAL   PRIMARY KEY,
    x               INTEGER     NOT NULL,
    y               INTEGER     NOT NULL,
    point_order     INTEGER     NOT NULL,
    blueprint_id    BIGINT      NOT NULL
);

-- FOREIGN KEY con DELETE CASCADE                              │


ALTER TABLE points
    ADD CONSTRAINT fk_points_blueprint 
    FOREIGN KEY (blueprint_id) 
    REFERENCES blueprints(id) 
    ON DELETE CASCADE;

-- ÍNDICE para búsquedas de puntos por blueprint 

CREATE INDEX idx_point_blueprint ON points(blueprint_id);

-- Evita datos inválidos.

ALTER TABLE points
    ADD CONSTRAINT chk_point_order_positive
    CHECK (point_order >= 0);




