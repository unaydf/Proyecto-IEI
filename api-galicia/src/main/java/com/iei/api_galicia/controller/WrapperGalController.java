package com.iei.api_galicia.controller;

import com.iei.api_galicia.wrapper.WrapperGal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.io.IOException;

@RestController
@RequestMapping("/galicia")
@Tag(
        name = "Wrapper Galicia",
        description = "Servicio wrapper encargado de exponer las estaciones ITV de Galicia a partir de un archivo CSV"
)
public class WrapperGalController {

    private final WrapperGal wrapperGal;

    public WrapperGalController(WrapperGal wrapperGal) {
        this.wrapperGal = wrapperGal;
    }

    @Operation(
            summary = "Obtiene las estaciones ITV de Galicia",
            description = """
            Devuelve todas las estaciones de Inspección Técnica de Vehículos (ITV) de la comunidad autónoma de Galicia.
            
            Los datos se obtienen a partir de un archivo CSV local, se transforman a una estructura JSON homogénea
            y se exponen mediante este endpoint para su consumo por el servicio ETL de carga.
            
            Este endpoint forma parte de la fase de extracción (Extract) del proceso ETL.
            """,
            operationId = "getItvGalicia"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de estaciones ITV de Galicia obtenido correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    description = "Estructura JSON que contiene estaciones, localidades y provincias",
                                    implementation = JsonNode.class
                            ),
                            examples = @ExampleObject(
                                    name = "Ejemplo de respuesta",
                                    summary = "Respuesta con una estación ITV",
                                    value = """
                    {
                      "estaciones": [
                        {
                          "nombre": "Estación ITV de Lugo",
                          "tipo": "Estacion_fija",
                          "direccion": "Av. Benigno Rivera, 1",
                          "codigo_postal": "27003",
                          "descripcion": "Av. Benigno Rivera, 1 08:00-20:00",
                          "horario": "08:00-20:00",
                          "contacto": "info@itvgalicia.com 982123456",
                          "URL": "https://itvgalicia.com/cita",
                          "localidad_nombre": "Lugo",
                          "localidad_codigo": "27",
                          "provincia_nombre": "Lugo",
                          "provincia_codigo": "27",
                          "latitud": 43.0123,
                          "longitud": -7.5554
                        }
                      ],
                      "localidades": [
                        {
                          "nombre": "Lugo",
                          "codigo": "27",
                          "provincia_nombre": "Lugo"
                        }
                      ],
                      "provincias": [
                        {
                          "nombre": "Lugo",
                          "codigo": "27"
                        }
                      ]
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno durante la lectura o transformación del archivo CSV",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Error interno",
                                    value = """
                    {
                      "error": "No se pudo procesar el archivo Estacions_ITV.csv"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<JsonNode> getWrapperGal() throws Exception {
        try {
            JsonNode json = wrapperGal.convertirCSVaJSON("src/main/resources/Estacions_ITV.csv");
            return ResponseEntity.ok(json); // 200 OK
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
