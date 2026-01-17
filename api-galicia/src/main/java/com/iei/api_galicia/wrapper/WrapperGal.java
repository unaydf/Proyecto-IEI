package com.iei.api_galicia.wrapper;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class WrapperGal {
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode convertirCSVaJSON(String filePath) throws IOException {
        ArrayNode estacionesArray = mapper.createArrayNode();
        Set<String> uniqueEstacion = new HashSet<>();
        Set<String> uniqueLocalidad = new HashSet<>();
        Set<String> uniqueProvincia = new HashSet<>();
        ArrayNode localidadesArray = mapper.createArrayNode();
        ArrayNode provinciasArray = mapper.createArrayNode();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) return mapper.createObjectNode();
            String[] headers = headerLine.split(";");

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                if (values.length < 10) continue;

                // --- Inputs de CSV
                String nombre = getValue(headers, values, "NOME DA ESTACIÓN");
                String direccion = getValue(headers, values, "ENDEREZO");
                String concello = getValue(headers, values, "CONCELLO");
                String codigoPostal = getValue(headers, values, "CÓDIGO POSTAL");
                String provincia = getValue(headers, values, "PROVINCIA");
                String telefono = getValue(headers, values, "TELÉFONO");
                String horario = getValue(headers, values, "HORARIO");
                String urlCita = getValue(headers, values, "SOLICITUDE DE CITA PREVIA");
                String correo = getValue(headers, values, "CORREO ELECTRÓNICO");
                String coordGmaps = getValue(headers, values, "COORDENADAS GMAPS");

                String provinciaNombre = limpiarNombreProvincia(provincia);
                String provinciaCodigo = obtenerCodigoProvincia(codigoPostal);

                String localidadNombre = limpiarNombreLocalidad(nombre);
                String localidadCodigo = provinciaCodigo;

                String keyEstacion = nombre.trim() + direccion.trim() + concello.trim() + codigoPostal.trim();
                if (uniqueEstacion.contains(keyEstacion)) continue;
                uniqueEstacion.add(keyEstacion);

                String keyLocalidad = localidadNombre + "_" + localidadCodigo;
                if (!uniqueLocalidad.contains(keyLocalidad)) {
                    ObjectNode localidadNode = mapper.createObjectNode();
                    localidadNode.put("nombre", localidadNombre);
                    localidadNode.put("codigo", localidadCodigo);
                    localidadNode.put("provincia_nombre", provinciaNombre);
                    localidadesArray.add(localidadNode);
                    uniqueLocalidad.add(keyLocalidad);
                }

                if (!uniqueProvincia.contains(provinciaNombre)) {
                    ObjectNode provinciaNode = mapper.createObjectNode();
                    provinciaNode.put("nombre", provinciaNombre);
                    provinciaNode.put("codigo", provinciaCodigo);
                    provinciasArray.add(provinciaNode);
                    uniqueProvincia.add(provinciaNombre);
                }

                ObjectNode estacion = mapper.createObjectNode();
                estacion.put("nombre", nombre);

                boolean esFija = !isNullOrEmpty(codigoPostal) && !isNullOrEmpty(direccion);
                estacion.put("tipo", esFija ? "Estacion_fija" : "Otros");
                estacion.put("direccion", direccion);
                estacion.put("codigo_postal", codigoPostal);

                estacion.put("descripcion", direccion + " " + horario);

                estacion.put("horario", horario);

                estacion.put("contacto", (isNullOrEmpty(correo) ? "" : correo) + " " + (isNullOrEmpty(telefono) ? "" : telefono));

                estacion.put("URL", isNullOrEmpty(urlCita) ? "" : urlCita);

                estacion.put("localidad_nombre", localidadNombre);
                estacion.put("localidad_codigo", localidadCodigo);
                estacion.put("provincia_nombre", provinciaNombre);
                estacion.put("provincia_codigo", provinciaCodigo);

                Double[] coords = parseCoordenadas(coordGmaps);
                estacion.put("latitud", coords[0]);
                estacion.put("longitud", coords[1]);

                estacionesArray.add(estacion);
            }
        }
        ObjectNode root = mapper.createObjectNode();
        root.set("estaciones", estacionesArray);
        root.set("localidades", localidadesArray);
        root.set("provincias", provinciasArray);
        return root;
    }

    private String limpiarNombreProvincia(String provincia) {
        String resultado = isNullOrEmpty(provincia) ? "Desconocida" : provincia.trim();
        if (resultado.equals("Coruña")) return "A Coruña";
        return resultado;
    }

    private String limpiarNombreLocalidad(String nombreEstacion) {
        if (nombreEstacion == null) return "Desconocido";
        String n = nombreEstacion.trim();
        n = n.replaceFirst("(?i)Estación ITV d[aeo] ", "").trim();
        n = n.replaceFirst("(?i)Estación ITV de ", "").trim();
        n = n.replaceFirst("(?i)Estación ITV do ", "").trim();
        return n.isEmpty() ? "Desconocido" : n;
    }

    private String obtenerCodigoProvincia(String codigoPostal) {
        return (codigoPostal == null || codigoPostal.length() < 2) ? "00" : codigoPostal.trim().substring(0, 2);
    }

    private String getValue(String[] headers, String[] values, String key) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(key) && i < values.length) {
                return values[i].trim();
            }
        }
        return "";
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Double[] parseCoordenadas(String coordStr) {
        if (coordStr == null || coordStr.trim().isEmpty()) {
            return new Double[]{null, null};
        }

        String[] parts = coordStr.split(",");
        if (parts.length != 2) {
            return new Double[]{null, null};
        }

        Double lat = parseCoordenada(parts[0].trim());
        if (lat != null && lat > 100.0) {
            lat = lat / 10.0;
        }

        Double lon = parseCoordenada(parts[1].trim());
        if (lon != null && lon > 100.0) {
            lon = lon / 10.0;
        }

        return new Double[]{lat, lon};
    }

    private Double parseCoordenada(String input) {
        input = input.replace("°", "°").replace("'", "'").trim();
        try {
            if (input.contains("°")) {
                int idxDeg = input.indexOf("°");
                int idxMin = input.indexOf("'");
                double grados = Double.parseDouble(input.substring(0, idxDeg).trim());
                double minutos = (idxMin > idxDeg) ? Double.parseDouble(input.substring(idxDeg + 1, idxMin).trim()) : 0.0;
                return grados + (minutos / 60.0);
            } else {
                return Double.parseDouble(input);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
