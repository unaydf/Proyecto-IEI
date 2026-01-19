package com.iei.api_carga.extractor;

import com.iei.api_carga.dto.ResultadoCargaDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class ExtractorCat {

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String user;
    @Value("${spring.datasource.password}")
    private String password;

    private final Map<String, Long> provinciaCache = new HashMap<>();
    private final Map<String, Long> localidadCache = new HashMap<>();

    private static final Map<String, String> PROVINCIA_POR_CP = Map.of(
            "08", "Barcelona",
            "17", "Girona",
            "25", "Lleida",
            "43", "Tarragona"
    );

    public ResultadoCargaDTO insertar(JsonNode estacionesArray) throws Exception {
        provinciaCache.clear();
        localidadCache.clear();

        ResultadoCargaDTO resultado = new ResultadoCargaDTO();
        resultado.setErroresRechazados(new java.util.ArrayList<>());
        resultado.setErroresReparados(new java.util.ArrayList<>());

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            for (JsonNode estacion : estacionesArray) {

                // Limpiamos los strings
                limpiarEstacion(estacion);

                if (!estacionValida(estacion, resultado)) {
                    continue; // se añade al listado de errores rechazados dentro de estacionValida
                }

                String provinciaNombre = safeText(estacion, "provincia_nombre");
                String provinciaCodigoKey = safeText(estacion, "provincia_codigo");

                long provinciaId = provinciaCache.containsKey(provinciaCodigoKey)
                        ? provinciaCache.get(provinciaCodigoKey)
                        : getOrInsertProvincia(conn, provinciaNombre, provinciaCodigoKey);

                String localidadNombre = safeText(estacion, "localidad_nombre");
                String localidadKey = localidadNombre + "_" + provinciaId;

                long localidadId = localidadCache.containsKey(localidadKey)
                        ? localidadCache.get(localidadKey)
                        : getOrInsertLocalidad(conn, localidadNombre, provinciaId, localidadKey);

                if (!estacionExiste(conn, safeText(estacion, "nombre"), localidadId)) {
                    insertarEstacion(conn, estacion, localidadId);
                    resultado.setRegistrosCorrectos(resultado.getRegistrosCorrectos() + 1);
                } else {
                    // Registro duplicado, consideramos que se actualiza o se ignora
                    resultado.getErroresRechazados().add(
                            new ResultadoCargaDTO.ErrorRechazado(
                                    "CAT",
                                    safeText(estacion, "nombre"),
                                    localidadNombre,
                                    "Registro duplicado"
                            )
                    );
                    resultado.setRegistrosRechazados(resultado.getRegistrosRechazados() + 1);
                }
            }

            conn.commit();
        }

        return resultado;
    }

    private boolean estacionValida(JsonNode e, ResultadoCargaDTO resultado) {
        String nombre = safeText(e, "nombre");
        String direccion = safeText(e, "direccion");
        String cp = safeText(e, "codigo_postal");
        String localidad = safeText(e, "localidad_nombre");
        String provincia = safeText(e, "provincia_nombre");
        Double lat = safeCoordinateLat(e, "latitud");
        Double lon = safeCoordinateLong(e, "longitud");

        boolean valido = true;
        String motivo = "";

        if (nombre == null || direccion == null || cp == null || localidad == null || provincia == null) {
            motivo = "Campo obligatorio nulo";
            valido = false;
        } else if (cp.length() != 5) {
            motivo = "Código postal inválido";
            valido = false;
        } else if (PROVINCIA_POR_CP.containsKey(cp.substring(0, 2)) &&
                !PROVINCIA_POR_CP.get(cp.substring(0, 2)).equalsIgnoreCase(provincia)) {
            motivo = "Código postal no coincide con provincia";
            valido = false;
        } else if (lat == null || lon == null) {
            motivo = "Coordenadas fuera de rango";
            valido = false;
        }

        if (!valido) {
            resultado.getErroresRechazados().add(
                    new ResultadoCargaDTO.ErrorRechazado("CAT", nombre, localidad, motivo)
            );
            resultado.setRegistrosRechazados(resultado.getRegistrosRechazados() + 1);
        }

        return valido;
    }

    private void limpiarEstacion(JsonNode estacion) {
        ObjectNode e = (ObjectNode) estacion;

        e.put("nombre", clean(safeText(estacion, "nombre")));
        e.put("direccion", clean(safeText(estacion, "direccion")));
        e.put("codigo_postal", clean(safeText(estacion, "codigo_postal")));
        e.put("descripcion", clean(safeText(estacion, "descripcion")));
        e.put("horario", clean(safeText(estacion, "horario")));
        e.put("contacto", clean(safeText(estacion, "contacto")));
        e.put("URL", clean(safeText(estacion, "URL")));

        Double lat = safeCoordinateLat(estacion, "latitud");
        Double lon = safeCoordinateLong(estacion, "longitud");

        e.put("latitud", lat);
        e.put("longitud", lon);
    }

    private String clean(String s) {
        if (s == null) return null;
        return s.replaceAll("[^\\p{L}\\p{N}\\s.,-]", "").trim();
    }

    private String safeText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private Double safeCoordinateLat(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) return null;
        double v = value.asDouble();
        return (v >= 40 && v <= 43) ? v : null;
    }

    private Double safeCoordinateLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) return null;
        double v = value.asDouble();
        return (v >= 0 && v <= 4) ? v : null;
    }

    private void insertarEstacion(Connection conn, JsonNode estacion, long localidadId) throws SQLException {
        String sql = """
                INSERT INTO estacion(
                    nombre, tipo, direccion,
                    codigo_postal, longitud, latitud,
                    descripcion, horario, contacto, url,
                    localidad_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, safeText(estacion, "nombre"));
            stmt.setString(2, safeText(estacion, "tipo"));
            stmt.setString(3, safeText(estacion, "direccion"));
            stmt.setString(4, safeText(estacion, "codigo_postal"));

            setNullableDouble(stmt, 5, estacion.get("longitud"));
            setNullableDouble(stmt, 6, estacion.get("latitud"));

            stmt.setString(7, safeText(estacion, "descripcion"));
            stmt.setString(8, safeText(estacion, "horario"));
            stmt.setString(9, safeText(estacion, "contacto"));
            stmt.setString(10, safeText(estacion, "URL"));

            stmt.setLong(11, localidadId);

            stmt.executeUpdate();
        }
    }

    private void setNullableDouble(PreparedStatement stmt, int idx, JsonNode node) throws SQLException {
        if (node == null || node.isNull() || !node.isNumber()) {
            stmt.setNull(idx, Types.DOUBLE);
        } else {
            stmt.setDouble(idx, node.asDouble());
        }
    }

    private long getOrInsertProvincia(Connection conn, String nombre, String codigoKey) throws SQLException {
        String selectSql = "SELECT codigo FROM provincia WHERE nombre = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("codigo");
                provinciaCache.put(codigoKey, id);
                return id;
            }
        }

        String insertSql = "INSERT INTO provincia(nombre) VALUES (?) RETURNING codigo";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("codigo");
                provinciaCache.put(codigoKey, id);
                return id;
            }
        }
        throw new SQLException("Error insertando Provincia");
    }

    private long getOrInsertLocalidad(Connection conn, String nombre, long provinciaId, String localidadKey)
            throws SQLException {
        String selectSql = "SELECT codigo FROM localidad WHERE nombre = ? AND provincia_codigo = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, nombre);
            stmt.setLong(2, provinciaId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("codigo");
                localidadCache.put(localidadKey, id);
                return id;
            }
        }

        String insertSql = "INSERT INTO localidad(nombre, provincia_codigo) VALUES (?, ?) RETURNING codigo";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, nombre);
            stmt.setLong(2, provinciaId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("codigo");
                localidadCache.put(localidadKey, id);
                return id;
            }
        }

        throw new SQLException("Error insertando Localidad");
    }

    private boolean estacionExiste(Connection conn, String nombreEstacion, long localidadId) throws SQLException {
        String sql = "SELECT cod_estacion FROM estacion WHERE nombre = ? AND localidad_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombreEstacion);
            stmt.setLong(2, localidadId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }
}
