package com.iei.api_carga.controller;

import com.iei.api_carga.dto.ResultadoCargaDTO;
import com.iei.api_carga.service.CargaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carga")
@CrossOrigin(origins = "*")
@Tag(name = "API de Carga (ETL)", description = """
                Servicio encargado de ejecutar el proceso ETL (Extract, Transform, Load)
                para la carga de estaciones ITV en la base de datos común.

                La API coordina la obtención de datos desde los wrappers regionales,
                valida y transforma la información recibida y persiste los resultados.
                """)
public class CargaController {

    private final CargaService cargaService;

    public CargaController(CargaService cargaService) {
        this.cargaService = cargaService;
    }

    @Operation(summary = "Ejecutar proceso de carga desde fuentes externas", description = """
                        Ejecuta el proceso ETL completo para una o varias fuentes de datos.

                        El endpoint recibe una lista de identificadores de fuentes
                        (CV, CAT, GAL) y, para cada una de ellas:

                        1. Invoca el wrapper correspondiente mediante una llamada HTTP.
                        2. Obtiene los datos en formato JSON normalizado.
                        3. Valida y transforma los registros mediante extractores específicos.
                        4. Inserta los datos válidos en la base de datos común.
                        5. Registra estadísticas de carga y posibles errores.

                        El endpoint no recibe datos manuales del usuario, sino que
                        actúa como disparador del proceso de carga automatizado.
                        """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proceso de carga ejecutado correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResultadoCargaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Fuente no reconocida o parámetros inválidos", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                                        {
                                          "error": "Fuente desconocida: XXX"
                                        }
                                        """))),
            @ApiResponse(responseCode = "500", description = "Error interno durante el proceso ETL", content = @Content(mediaType = "application/json", schema = @Schema(example = """
                                        {
                                          "error": "Error al procesar los datos de una fuente externa"
                                        }
                                        """)))
    })
    @GetMapping("/cargar")
    public ResponseEntity<ResultadoCargaDTO> cargar(
            @Parameter(description = """
                                        Lista de fuentes de datos a cargar.

                                        Valores admitidos:
                                        - CV  (Comunitat Valenciana)
                                        - CAT (Catalunya)
                                        - GAL (Galicia)
                                        """, example = "[\"CV\", \"CAT\", \"GAL\"]", required = true) @RequestParam List<String> fuentes) {
        cargaService.cargarDatos(fuentes);
        ResultadoCargaDTO resultadoCargaDTO = new ResultadoCargaDTO();
        return ResponseEntity.ok(resultadoCargaDTO);
    }

    @Operation(summary = "Eliminar todos los datos cargados", description = """
                        Elimina completamente todos los registros almacenados en la base de datos
                        relacionados con estaciones, localidades y provincias.

                        Este endpoint se utiliza principalmente con fines de reinicialización
                        del sistema y pruebas controladas.
                        """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos eliminados correctamente", content = @Content(mediaType = "text/plain", schema = @Schema(example = "Datos eliminados correctamente"))),
            @ApiResponse(responseCode = "500", description = "Error interno durante la eliminación de datos")
    })
    @DeleteMapping("/borrar")
    public ResponseEntity<String> borrarTodos() {
        cargaService.eliminarDatos();
        return ResponseEntity.ok("Datos eliminados correctamente");
    }
}

