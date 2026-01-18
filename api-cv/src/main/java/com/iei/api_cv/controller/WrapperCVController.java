package com.iei.api_cv.controller;

import com.iei.api_cv.wrapper.WrapperCV;
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
@RequestMapping("/valencia")
@Tag(
        name = "Wrapper Comunitat Valenciana",
        description = "Servicio wrapper encargado de exponer las estaciones ITV de la Comunitat Valenciana a partir de un archivo JSON enriquecido mediante scraping"
)
public class WrapperCVController {

    private final WrapperCV wrapperCV;

    public WrapperCVController(WrapperCV wrapperCV) {
        this.wrapperCV = wrapperCV;
    }

    @Operation(
            summary = "Obtiene las estaciones ITV de la Comunitat Valenciana",
            description = """
            Devuelve el listado de estaciones de Inspección Técnica de Vehículos (ITV) de la Comunitat Valenciana.
            
            Los datos se obtienen a partir de un archivo JSON preprocesado, que contiene información estructurada
            sobre las estaciones. Para las estaciones fijas, el sistema obtiene dinámicamente las coordenadas
            geográficas mediante un servicio auxiliar de scraping basado en Selenium.
            
            La información se transforma a una estructura JSON homogénea y se expone a través de este endpoint
            para su posterior consumo por el servicio ETL de carga.
            
            Este endpoint forma parte de la fase de extracción (Extract) del proceso ETL del sistema.
            """,
            operationId = "getItvComunitatValenciana"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de estaciones ITV de la Comunitat Valenciana obtenido correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    description = "Array JSON con las estaciones ITV de la Comunitat Valenciana",
                                    implementation = JsonNode.class
                            ),
                            examples = @ExampleObject(
                                    name = "Ejemplo de respuesta",
                                    summary = "Respuesta con una estación ITV fija",
                                    value = """
                    [
                      {
                        "cod_estacion": 1,
                        "nombre": "Estación ITV de Valencia",
                        "tipo": "Estacion_fija",
                        "direccion": "Av. del Cid, 152",
                        "codigo_postal": "46014",
                        "descripcion": "Av. del Cid, 152 / 08:00-20:00",
                        "horario": "08:00-20:00",
                        "contacto": "info@sitval.com",
                        "URL": "www.sitval.com",
                        "localidad_nombre": "Valencia",
                        "provincia_nombre": "Valencia",
                        "provincia_codigo": "46",
                        "latitud": 39.4699,
                        "longitud": -0.3763
                      }
                    ]
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno durante la lectura, transformación o enriquecimiento de los datos",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Error interno",
                                    value = """
                    {
                      "error": "No se pudo procesar el archivo de estaciones de la Comunitat Valenciana"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<JsonNode> getWrapperCV() {
        try {
            JsonNode json = wrapperCV.convertirAJSON("src/main/resources/estaciones.json");
            return ResponseEntity.ok(json); // 200 OK
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
