package edu.eci.arsw.blueprints.controllers;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
// /api/ es el separador de endpoints
// /v1/ corresponde al versionamiento
@RequestMapping("/api/v1/blueprints")
@Tag(name = "Blueprints", description = "API REST para gestión de planos (blueprints)")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    // GET /blueprints
    @Operation(
        summary = "Obtener todos los blueprints",
        description = "Retorna la lista completa de blueprints almacenados en el sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de blueprints obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.dto.ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<edu.eci.arsw.blueprints.dto.ApiResponse<Set<Blueprint>>> getAll() {
        Set<Blueprint> blueprints = services.getAllBlueprints();
        return ResponseEntity.ok(edu.eci.arsw.blueprints.dto.ApiResponse.success(blueprints));
    }

    // GET /blueprints/{author}
    @Operation(
        summary = "Obtener blueprints por autor",
        description = "Retorna todos los blueprints creados por un autor específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Blueprints del autor obtenidos exitosamente"),
        @ApiResponse(responseCode = "404", description = "No se encontraron blueprints para el autor especificado")
    })
    @GetMapping("/{author}")
    public ResponseEntity<edu.eci.arsw.blueprints.dto.ApiResponse<?>> byAuthor(@PathVariable String author) {
        try {
            Set<Blueprint> blueprints = services.getBlueprintsByAuthor(author);
            return ResponseEntity.ok(edu.eci.arsw.blueprints.dto.ApiResponse.success(blueprints));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(edu.eci.arsw.blueprints.dto.ApiResponse.notFound(e.getMessage()));
        }
    }

    // GET /blueprints/{author}/{bpname}
    @Operation(
        summary = "Obtener un blueprint específico",
        description = "Retorna un blueprint identificado por su autor y nombre"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Blueprint encontrado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Blueprint no encontrado")
    })
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<edu.eci.arsw.blueprints.dto.ApiResponse<?>> byAuthorAndName(@PathVariable String author, @PathVariable String bpname) {
        try {
            Blueprint bp = services.getBlueprint(author, bpname);
            return ResponseEntity.ok(edu.eci.arsw.blueprints.dto.ApiResponse.success(bp));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(edu.eci.arsw.blueprints.dto.ApiResponse.notFound(e.getMessage()));
        }
    }

    // POST /blueprints
    @Operation(
        summary = "Crear un nuevo blueprint",
        description = "Crea y almacena un nuevo blueprint en el sistema. El autor y nombre deben ser únicos."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Blueprint creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o blueprint duplicado")
    })
    @PostMapping
    public ResponseEntity<edu.eci.arsw.blueprints.dto.ApiResponse<?>> add(@Valid @RequestBody NewBlueprintRequest req) {
        try {
            Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
            services.addNewBlueprint(bp);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(edu.eci.arsw.blueprints.dto.ApiResponse.created(bp));
        } catch (BlueprintPersistenceException e) {
            // 400 Bad Request: datos inválidos (blueprint duplicado)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(edu.eci.arsw.blueprints.dto.ApiResponse.badRequest(e.getMessage()));
        }
    }

    // PUT /blueprints/{author}/{bpname}/points
    @Operation(
        summary = "Agregar un punto a un blueprint",
        description = "Agrega un nuevo punto (coordenadas x, y) a un blueprint existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Punto agregado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Blueprint no encontrado")
    })
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<edu.eci.arsw.blueprints.dto.ApiResponse<?>> addPoint(@PathVariable String author, @PathVariable String bpname,
                                      @RequestBody Point p) {
        try {
            services.addPoint(author, bpname, p.x(), p.y());
            // 202 Accepted: solicitud de actualización aceptada para procesamiento
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(edu.eci.arsw.blueprints.dto.ApiResponse.accepted());
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(edu.eci.arsw.blueprints.dto.ApiResponse.notFound(e.getMessage()));
        }
    }

    
    //Validaciones
    public record NewBlueprintRequest(
            @NotBlank(message = "Author cannot be empty") String author,
            @NotBlank(message = "Name cannot be empty") String name,
            @Valid java.util.List<Point> points
    ) { }
}
