package com.iei.api_cv.wrapper;

import com.iei.api_cv.service.CoordenadasService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

@Component
public class WrapperCV {

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode convertirAJSON(String filePath) throws IOException {

        JsonNode rootNode = mapper.readTree(new File(filePath));
        ArrayNode estacionesArray = mapper.createArrayNode();

        if (!rootNode.isArray()) return estacionesArray;

        for (JsonNode raw : rootNode) {

            String tipoRaw = get(raw, "TIPO ESTACIÓN").toLowerCase();
            String municipio = get(raw, "MUNICIPIO");
            String direccion = get(raw, "DIRECCIÓN");
            String cp = get(raw, "C.POSTAL");
            String provincia = get(raw, "PROVINCIA");
            String horario = get(raw, "HORARIOS");
            String correo = get(raw, "CORREO");

            if (!correo.contains("@")) continue;

            ObjectNode estacion = mapper.createObjectNode();

            if (tipoRaw.contains("fija")) {

                if (municipio.isEmpty()) continue;
                if (direccion.isEmpty()) continue;
                if (!cp.matches("\\d{5}")) continue;

                String prefijo = cp.substring(0, 2);
                if (!prefijo.matches("03|12|46")) continue;

                double[] coords = CoordenadasService.obtenerLatLon(direccion);

                estacion.put("nombre", "Estación ITV de " + municipio);
                estacion.put("tipo", "Estacion_fija");
                estacion.put("direccion", direccion);
                estacion.put("codigo_postal", cp);
                estacion.put("provincia_nombre", provincia);
                estacion.put("localidad_nombre", municipio);
                estacion.put("latitud", coords[0]);
                estacion.put("longitud", coords[1]);
            }

            else if (tipoRaw.contains("móvil")) {

                estacion.put("nombre", "Estación " + direccion);
                estacion.put("tipo", "Estacion_movil");

                estacion.putNull("direccion");
                estacion.putNull("codigo_postal");
                estacion.putNull("provincia_nombre");
                estacion.putNull("localidad_nombre");
                estacion.putNull("latitud");
                estacion.putNull("longitud");
            }

            else if (tipoRaw.contains("agrícola")) {

                estacion.put("nombre", "Estación ITV Agrícola " + direccion);
                estacion.put("tipo", "Otros");

                estacion.putNull("direccion");
                estacion.putNull("codigo_postal");
                estacion.putNull("provincia_nombre");
                estacion.putNull("localidad_nombre");
                estacion.putNull("latitud");
                estacion.putNull("longitud");
            }

            else continue;

            estacion.put("horario", horario);
            estacion.put("descripcion", direccion + " / " + horario);
            estacion.put("contacto", correo);
            estacion.put("URL", "www.sitval.com");

            estacionesArray.add(estacion);
        }

        return estacionesArray;
    }

    private String get(JsonNode n, String f) {
        return n.has(f) && !n.get(f).isNull() ? n.get(f).asText().trim() : "";
    }
}

