package edu.eci.arsw.blueprints.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;


@Repository
@Primary  // ← Esta anotación hace que Spring use esta implementación en lugar de InMemory
public class PostgresBlueprintPersistence implements BlueprintPersistence {


    private final JdbcTemplate jdbc;


    // CONSTRUCTOR CON INYECCIÓN DE DEPENDENCIAS
    // trae la info de application.properties

    public PostgresBlueprintPersistence(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    /**
     * Guarda un nuevo blueprint en la base de datos.
     * 1. Verificar si ya existe (UNIQUE constraint en author+name)
     * 2. Insertar blueprint en tabla blueprints (obtener ID autogenerado)
     * 3. Insertar todos los puntos en tabla points (con orden correcto)
     */
    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        // Verificar si ya existe

        
        String checkSql = "SELECT COUNT(*) FROM blueprints WHERE author = ? AND name = ?";
        Integer count = jdbc.queryForObject(checkSql, Integer.class, bp.getAuthor(), bp.getName());
        
        if (count != null && count > 0) {
            throw new BlueprintPersistenceException(
                "Blueprint already exists: " + bp.getAuthor() + "/" + bp.getName()
            );
        }

        // Insertar blueprint
        String insertBlueprintSql = "INSERT INTO blueprints (author, name) VALUES (?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                insertBlueprintSql, 
                Statement.RETURN_GENERATED_KEYS  // ← Pide devolver el ID generado
            );
            ps.setString(1, bp.getAuthor());
            ps.setString(2, bp.getName());
            return ps;
        }, keyHolder);

        // Obtener el ID autogenerado (tipo Long porque usamos BIGSERIAL)
        Long blueprintId = keyHolder.getKey().longValue();

        // Insertar todos los puntos con su orden correcto
 
        
        if (!bp.getPoints().isEmpty()) {
            String insertPointSql = 
                "INSERT INTO points (x, y, point_order, blueprint_id) VALUES (?, ?, ?, ?)";
            
            List<Point> points = bp.getPoints();
            for (int i = 0; i < points.size(); i++) {
                Point p = points.get(i);
                jdbc.update(insertPointSql, p.x(), p.y(), i, blueprintId);
            }
        }
    }

 
    // Obtiene un blueprint específico por autor y nombre.
 
    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        try {
            // Buscar blueprint 
            String blueprintSql = "SELECT id, author, name FROM blueprints WHERE author = ? AND name = ?";
            
            // queryForObject lanza EmptyResultDataAccessException si no encuentra nada
            Map<String, Object> row = jdbc.queryForMap(blueprintSql, author, name);
            Long blueprintId = ((Number) row.get("id")).longValue();

            // Buscar todos los puntos del blueprint (ORDENADOS)
 
            
            String pointsSql = 
                "SELECT x, y FROM points WHERE blueprint_id = ? ORDER BY point_order";
            
            List<Point> points = jdbc.query(pointsSql, new PointRowMapper(), blueprintId);

            // Reconstruir objeto Blueprint
         
            return new Blueprint(author, name, points);

        } catch (EmptyResultDataAccessException e) {
            // No se encontró el blueprint
            throw new BlueprintNotFoundException(
                "Blueprint not found: " + author + "/" + name
            );
        }
    }

    // OBTENER TODOS LOS BLUEPRINTS DE UN AUTOR
  
    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        //  Buscar todos los blueprints del autor
        // El índice idx_blueprint_author acelera esta consulta
        
        String blueprintsSql = "SELECT id, name FROM blueprints WHERE author = ?";
        List<Map<String, Object>> rows = jdbc.queryForList(blueprintsSql, author);

        if (rows.isEmpty()) {
            throw new BlueprintNotFoundException("No blueprints for author: " + author);
        }

        // : Para cada blueprint, obtener sus puntos
        Set<Blueprint> result = new HashSet<>();
        
        for (Map<String, Object> row : rows) {
            Long blueprintId = ((Number) row.get("id")).longValue();
            String name = (String) row.get("name");

            // Obtener puntos del blueprint actual
            String pointsSql = 
                "SELECT x, y FROM points WHERE blueprint_id = ? ORDER BY point_order";
            List<Point> points = jdbc.query(pointsSql, new PointRowMapper(), blueprintId);

            // Crear Blueprint y agregarlo al Set
            result.add(new Blueprint(author, name, points));
        }

        return result;
    }

    // Obtiene todos los blueprints de todos los autores.

    @Override
    public Set<Blueprint> getAllBlueprints() {
        // Obtener todos los blueprints
        String blueprintsSql = "SELECT id, author, name FROM blueprints";
        List<Map<String, Object>> rows = jdbc.queryForList(blueprintsSql);

        // Para cada uno, obtener sus puntos
    
        Set<Blueprint> result = new HashSet<>();

        for (Map<String, Object> row : rows) {
            Long blueprintId = ((Number) row.get("id")).longValue();
            String author = (String) row.get("author");
            String name = (String) row.get("name");

            // Obtener puntos
            String pointsSql = 
                "SELECT x, y FROM points WHERE blueprint_id = ? ORDER BY point_order";
            List<Point> points = jdbc.query(pointsSql, new PointRowMapper(), blueprintId);

            result.add(new Blueprint(author, name, points));
        }

        return result;
    }

    // AGREGAR UN PUNTO A UN BLUEPRINT EXISTENTE



    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        try {
            // Obtener ID del blueprint (verifica que existe)
         
            String getBlueprintIdSql = 
                "SELECT id FROM blueprints WHERE author = ? AND name = ?";
            Long blueprintId = jdbc.queryForObject(getBlueprintIdSql, Long.class, author, name);

            // Obtener el máximo point_order actual
      
            
            String getMaxOrderSql = 
                "SELECT COALESCE(MAX(point_order), -1) FROM points WHERE blueprint_id = ?";
            Integer maxOrder = jdbc.queryForObject(getMaxOrderSql, Integer.class, blueprintId);

            // Insertar nuevo punto al final
 
            int newOrder = (maxOrder != null ? maxOrder : -1) + 1;
            
            String insertPointSql = 
                "INSERT INTO points (x, y, point_order, blueprint_id) VALUES (?, ?, ?, ?)";
            jdbc.update(insertPointSql, x, y, newOrder, blueprintId);

        } catch (EmptyResultDataAccessException e) {
            throw new BlueprintNotFoundException(
                "Blueprint not found: " + author + "/" + name
            );
        }
    }

    // CLASE AUXILIAR: ROWMAPPER PARA CONVERTIR FILAS SQL → OBJETOS POINT
    
    // ResultSet (SQL) = hoja de cálculo con columnas x, y
    // Point (Java) = objeto con campos x, y
    // RowMapper = traductor que lee la hoja y crea objetos
   
    private static class PointRowMapper implements RowMapper<Point> {
        @Override
        public Point mapRow(ResultSet rs, int rowNum) throws SQLException {
            // ResultSet es como un cursor sobre los datos SQL
            // getInt("x") obtiene el valor de la columna "x" fila actual
            return new Point(rs.getInt("x"), rs.getInt("y"));
        }
    }
}

