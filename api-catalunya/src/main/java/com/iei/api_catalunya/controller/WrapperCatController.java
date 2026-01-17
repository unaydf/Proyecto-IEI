package com.iei.api_catalunya.controller;

import com.iei.api_catalunya.wrapper.WrapperCat;
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
@RequestMapping("/catalunya")
@Tag(
        name = "Wrapper Catalunya",
        description = "Servicio wrapper encargado de exponer las estaciones ITV de Catalunya a partir de un fichero XML oficial transformado a JSON"
)
public class WrapperCatController {

    private final WrapperCat wrapperCat;

    public WrapperCatController(WrapperCat wrapperCat) {
        this.wrapperCat = wrapperCat;
    }

    @Operation(
            summary = "Obtiene las estaciones ITV de Catalunya",
            description = """
            Devuelve el listado de estaciones de Inspección Técnica de Vehículos (ITV) de Catalunya.
            
            Los datos se obtienen a partir de un fichero XML oficial proporcionado por la administración pública,
            que es procesado mediante XPath para extraer la información relevante de cada estación.
            
            Durante el proceso de transformación, los datos XML se normalizan y convierten a una estructura
            JSON homogénea, compatible con el resto de wrappers del sistema.
            
            Este endpoint forma parte de la fase de extracción (Extract) del proceso ETL del sistema.
            """,
            operationId = "getItvCatalunya"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de estaciones ITV de Catalunya obtenido correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    description = "Array JSON con las estaciones ITV de Catalunya",
                                    implementation = JsonNode.class
                            ),
                            examples = @ExampleObject(
                                    name = "Ejemplo de respuesta",
                                    summary = "Respuesta con una estación ITV fija",
                                    value = """
                    [
                      {
                        "cod_estacion": 1,
                        "nombre": "Estació de ITV de Barcelona",
                        "tipo": "Estación_fija",
                        "direccion": "Carrer de la Mecànica, 12",
                        "codigo_postal": "08038",
                        "descripcion": "Carrer de la Mecànica, 12 / 08:00-20:00",
                        "horario": "08:00-20:00",
                        "contacto": "info@itv.cat / 934000000",
                        "URL": "https://itv.gencat.cat",
                        "localidad_codigo": 1,
                        "localidad_nombre": "Barcelona",
                        "provincia_codigo": "08",
                        "provincia_nombre": "Barcelona",
                        "latitud": 41.3546,
                        "longitud": 2.1380
                      }
                    ]
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno durante la lectura o transformación del fichero XML",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Error interno",
                                    value = """
                    {
                      "error": "No se pudo procesar el archivo XML de estaciones de Catalunya"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<JsonNode> getWrapperCat() {
        try {
            JsonNode json = wrapperCat.convertirXMLaJSON("src/main/resources/ITV-CAT.xml");
            return ResponseEntity.ok(json); // 200 OK
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
