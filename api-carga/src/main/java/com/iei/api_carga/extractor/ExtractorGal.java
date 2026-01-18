package com.iei.api_carga.extractor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class ExtractorGal {

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String user;
    @Value("${spring.datasource.password}")
    private String password;

    private final Map<String, Long> provinciaCache = new HashMap<>();
    private final Map<String, Long> localidadCache = new HashMap<>();

    private static final Map<String, String> PROVINCIA_POR_CP = Map.of(
            "15", "A Coruña",
            "27", "Lugo",
            "32", "Ourense",
            "36", "Pontevedra"
    );

    public void insertar(JsonNode jsonMultiEntidad) throws Exception {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            ArrayNode estaciones = (ArrayNode) jsonMultiEntidad.get("estaciones");

            for (JsonNode estacion : estaciones) {

                // Copia original para detectar reparaciones
                JsonNode original = estacion.deepCopy();

                // 1. Limpieza
                limpiarEstacion(estacion);

                // 3. Correcciones automáticas
                corregirProvinciaSegunCP(estacion);

                boolean reparado = !original.equals(estacion);

                // 4. Provincia
                String provinciaNombre = estacion.get("provincia_nombre").asText();
                long provinciaId = provinciaCache.computeIfAbsent(
                        provinciaNombre,
                        p -> {
                            try {
                                return getOrInsertProvincia(conn, p);
                            } catch (SQLException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                );

                // 5. Localidad
                String localidadNombre = estacion.get("localidad_nombre").asText();
                String keyLoc = localidadNombre + "_" + provinciaId;

                long localidadId = localidadCache.computeIfAbsent(
                        keyLoc,
                        k -> {
                            try {
                                return getOrInsertLocalidad(conn, localidadNombre, provinciaId);
                            } catch (SQLException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                );

                // 6. Estación
                if (!estacionExiste(conn, estacion.get("nombre").asText(), localidadId)) {
                    insertarEstacion(conn, estacion, localidadId);
                }
            }

            conn.commit();
        }
    }

    private void limpiarEstacion(JsonNode estacion) {
        ObjectNode e = (ObjectNode) estacion;

        e.put("nombre", clean(safeText(e.get("nombre"))));
        e.put("tipo", clean(safeText(e.get("tipo"))));
        e.put("direccion", clean(safeText(e.get("direccion"))));
        e.put("codigo_postal", clean(safeText(e.get("codigo_postal"))));
        e.put("descripcion", clean(safeText(e.get("descripcion"))));
        e.put("horario", clean(safeText(e.get("horario"))));
        e.put("contacto", clean(safeText(e.get("contacto"))));
        e.put("URL", clean(safeText(e.get("URL"))));
        e.put("localidad_nombre", clean(safeText(e.get("localidad_nombre"))));
        e.put("provincia_nombre", clean(safeText(e.get("provincia_nombre"))));

        Double lat = safeDoubleLat(e.get("latitud"));
        Double lon = safeDoubleLong(e.get("longitud"));

        e.put("latitud", lat);
        e.put("longitud", lon);
    }

    private Double safeDoubleLat(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            double v = node.asDouble();
            if (v < 40 || v > 45) return null;
            return v;
        } catch (Exception ex) {
            return null;
        }
    }

    private Double safeDoubleLong(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            double v = node.asDouble();
            if (v < -9 || v > -4) return null;
            return v;
        } catch (Exception ex) {
            return null;
        }
    }

    private String safeText(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String t = node.asText().trim();
        return t.isEmpty() ? null : t;
    }

    private String clean(String s) {
        if (s == null) return null;
        return s.replaceAll("[^\\p{L}\\p{N}\\s.,-]", "").trim();
    }

    private boolean codigoPostalValido(String cp) {
        if (cp == null) return false;

        if (!cp.matches("\\d{5}")) return false;

        int prefijo = Integer.parseInt(cp.substring(0, 2));
        return prefijo == 15 || prefijo == 27 || prefijo == 32 || prefijo == 36;
    }

    private boolean estacionValida(JsonNode e) {

        if (safeText(e.get("nombre")) == null) return false;
        if (safeText(e.get("direccion")) == null) return false;
        String cp = safeText(e.get("codigo_postal"));
        if (!codigoPostalValido(cp)) return false;
        if (safeText(e.get("localidad_nombre")) == null) return false;
        if (safeText(e.get("provincia_nombre")) == null) return false;

        Double lat = safeDoubleLat(e.get("latitud"));
        Double lon = safeDoubleLong(e.get("longitud"));

        if (lat == null || lon == null) return false;

        return true;
    }

    private void corregirProvinciaSegunCP(JsonNode estacion) {
        ObjectNode e = (ObjectNode) estacion;

        String cp = safeText(e.get("codigo_postal"));
        if (!codigoPostalValido(cp)) return;

        String prefijo = cp.substring(0, 2);
        String provinciaCorrecta = PROVINCIA_POR_CP.get(prefijo);

        if (provinciaCorrecta != null) {
            e.put("provincia_nombre", provinciaCorrecta);
        }
    }

    private long getOrInsertProvincia(Connection conn, String nombre) throws SQLException {
        String selectSql = "SELECT codigo FROM provincia WHERE nombre = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("codigo");
        }

        String insertSql = "INSERT INTO provincia(nombre) VALUES (?) RETURNING codigo";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("codigo");
        }

        throw new SQLException("Error insertando provincia " + nombre);
    }

    private long getOrInsertLocalidad(Connection conn, String nombre, long provinciaId) throws SQLException {
        String selectSql = "SELECT codigo FROM localidad WHERE nombre = ? AND provincia_codigo = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, nombre);
            stmt.setLong(2, provinciaId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("codigo");
        }

        String insertSql = "INSERT INTO localidad(nombre, provincia_codigo) VALUES (?, ?) RETURNING codigo";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, nombre);
            stmt.setLong(2, provinciaId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("codigo");
        }

        throw new SQLException("Error insertando localidad " + nombre);
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

    private void insertarEstacion(Connection conn, JsonNode estacion, long localidadId) throws SQLException {
        String sql = """
                INSERT INTO estacion(
                    nombre, tipo, direccion, codigo_postal,
                    longitud, latitud, descripcion, horario,
                    contacto, url, localidad_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, estacion.get("nombre").asText());
            stmt.setString(2, estacion.get("tipo").asText());
            stmt.setString(3, estacion.get("direccion").asText());
            stmt.setString(4, estacion.get("codigo_postal").asText());
            stmt.setDouble(5, estacion.get("longitud").asDouble());
            stmt.setDouble(6, estacion.get("latitud").asDouble());
            stmt.setString(7, estacion.get("descripcion").asText());
            stmt.setString(8, estacion.get("horario").asText());
            stmt.setString(9, estacion.get("contacto").asText());
            stmt.setString(10, estacion.get("URL").asText());
            stmt.setLong(11, localidadId);

            stmt.executeUpdate();
        }
    }
}
