package com.iei.api_busqueda.controller;

import com.iei.api_busqueda.dto.EstacionDTO;
import com.iei.api_busqueda.mapper.EstacionMapper;
import com.iei.api_busqueda.service.EstacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/busqueda")
@Tag(
        name = "API de Búsqueda de Estaciones ITV",
        description = """
        Servicio encargado de realizar búsquedas sobre las estaciones ITV
        previamente cargadas en la base de datos común.

        El endpoint permite filtrar estaciones por distintos criterios,
        devolviendo únicamente aquellas que cumplen las condiciones indicadas.
        """
)
public class BusquedaController {

    private final EstacionService estacionService;

    public BusquedaController(EstacionService estacionService) {
        this.estacionService = estacionService;
    }

    @Operation(
            summary = "Búsqueda de estaciones ITV por filtros",
            description = """
        Realiza una búsqueda de estaciones ITV aplicando uno o varios filtros opcionales
        mediante parámetros de consulta (query parameters).

        Cualquier parámetro puede omitirse, en cuyo caso no se aplicará
        dicho criterio en la búsqueda.

        Filtros disponibles:
        - localidad
        - codigoPostal
        - provincia
        - tipo (FIJA, MOVIL, OTROS)

        El resultado es una lista de estaciones que cumplen todos los filtros proporcionados.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de estaciones que cumplen los criterios de búsqueda",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EstacionDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetros de búsqueda inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                    {
                      "error": "El tipo de estación indicado no es válido"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno durante el proceso de búsqueda",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                    {
                      "error": "Error interno al acceder a la base de datos"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/estaciones")
    public ResponseEntity<List<EstacionDTO>> buscar(
            @RequestParam(required = false)
            @Schema(description = "Localidad de la estación")
            String localidad,

            @RequestParam(required = false)
            @Schema(description = "Código postal de la estación")
            String codigoPostal,

            @RequestParam(required = false)
            @Schema(description = "Provincia de la estación")
            String provincia,

            @RequestParam(required = false)
            @Schema(description = "Tipo de estación (FIJA, MOVIL, OTROS)")
            String tipo
    ) {
        List<EstacionDTO> estaciones = estacionService.buscarPorFiltros(
                        localidad,
                        codigoPostal,
                        provincia,
                        tipo
                ).stream()
                .map(EstacionMapper::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(estaciones);
    }
}
