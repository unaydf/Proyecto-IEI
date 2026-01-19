package com.iei.api_carga.extractor;

import com.iei.api_carga.dto.ResultadoCargaDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.sql.*;
import java.util.ArrayList;
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

    private static final Map<String, String> PROVINCIAS_CP = Map.of(
            "15", "A Coruña",
            "27", "Lugo",
            "32", "Ourense",
            "36", "Pontevedra"
    );

    public ResultadoCargaDTO insertar(JsonNode root) throws Exception {
        provinciaCache.clear();
        localidadCache.clear();

        ResultadoCargaDTO resultado = new ResultadoCargaDTO();
        resultado.setErroresReparados(new ArrayList<>());
        resultado.setErroresRechazados(new ArrayList<>());

        ArrayNode estaciones = (ArrayNode) root.get("estaciones");
        if (estaciones == null) return resultado;

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            for (JsonNode estacion : estaciones) {

                // Copia original para detectar reparaciones
                JsonNode original = estacion.deepCopy();
                String nombreEstacion = estacion.hasNonNull("nombre") ? estacion.get("nombre").asText() : "DESCONOCIDO";
                String localidadNombre = estacion.hasNonNull("localidad_nombre") ? estacion.get("localidad_nombre").asText() : "DESCONOCIDO";

                // Limpieza básica
                limpiarEstacion(estacion);

                // Validación estricta
                String motivoRechazo = validarEstacion(estacion);
                if (motivoRechazo != null) {
                    resultado.setRegistrosRechazados(resultado.getRegistrosRechazados() + 1);
                    resultado.getErroresRechazados().add(
                            new ResultadoCargaDTO.ErrorRechazado("GAL", nombreEstacion, localidadNombre, motivoRechazo)
                    );
                    continue;
                }

                // --------------------------
                // Provincias
                // --------------------------
                String provinciaNombre = estacion.get("provincia_nombre").asText();
                long provinciaId = provinciaCache.computeIfAbsent(
                        provinciaNombre,
                        p -> {
                            try { return getOrInsertProvincia(conn, p); }
                            catch (SQLException ex) { throw new RuntimeException(ex); }
                        }
                );

                // --------------------------
                // Localidades
                // --------------------------
                String keyLoc = localidadNombre + "_" + provinciaId;
                long localidadId = localidadCache.computeIfAbsent(
                        keyLoc,
                        k -> {
                            try { return getOrInsertLocalidad(conn, localidadNombre, provinciaId); }
                            catch (SQLException ex) { throw new RuntimeException(ex); }
                        }
                );

                // --------------------------
                // Estaciones - PRIMERO verificar duplicados
                // --------------------------
                if (!estacionExiste(conn, nombreEstacion, localidadId)) {
                    // No es duplicado, verificar si hubo limpieza
                    boolean reparado = !original.equals(estacion);
                    if (reparado) {
                        resultado.setRegistrosConErroresReparados(resultado.getRegistrosConErroresReparados() + 1);
                        resultado.getErroresReparados().add(
                                new ResultadoCargaDTO.ErrorReparado("GAL", nombreEstacion, localidadNombre, "Valores limpiados", "INSERT")
                        );
                    }

                    insertarEstacion(conn, estacion, localidadId);
                    resultado.setRegistrosCorrectos(resultado.getRegistrosCorrectos() + 1);
                } else {
                    // Registro duplicado - se añade como error reparado (IGNORADO tiene prioridad)
                    resultado.getErroresRechazados().add(
                            new ResultadoCargaDTO.ErrorRechazado(
                                    "GAL",
                                    nombreEstacion,
                                    localidadNombre,
                                    "Registro duplicado"
                            )
                    );
                    resultado.setRegistrosConErroresReparados(resultado.getRegistrosConErroresReparados() + 1);
                }
            }

            conn.commit();
        }

        return resultado;
    }

    private void limpiarEstacion(JsonNode e) {
        // Solo limpiamos strings, dejamos coordenadas como están para validación
        e.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual()) {
                ((ObjectNode) e).put(entry.getKey(), entry.getValue().asText().trim());
            }
        });
    }

    private String validarEstacion(JsonNode e) {
        // Nulos
        if (isNull(e, "nombre") || isNull(e, "tipo") || isNull(e, "direccion")
                || isNull(e, "codigo_postal") || isNull(e, "localidad_nombre")
                || isNull(e, "provincia_nombre") || e.get("latitud").isNull() || e.get("longitud").isNull()) {
            return "Campos obligatorios nulos";
        }

        // CP
        String cp = e.get("codigo_postal").asText();
        if (!cp.matches("\\d{5}")) return "Código postal inválido";

        String prefijo = cp.substring(0, 2);
        if (!PROVINCIAS_CP.containsKey(prefijo)) return "Prefijo postal no reconocido";

        String provincia = e.get("provincia_nombre").asText();
        if (!PROVINCIAS_CP.get(prefijo).equals(provincia)) return "Código postal no coincide con provincia";

        // Coordenadas
        double lat = e.get("latitud").asDouble();
        double lon = e.get("longitud").asDouble();
        if (lat < 39 || lat > 46 || lon < -9 || lon > -4) return "Coordenadas fuera de rango";

        return null; // válido
    }

    private boolean isNull(JsonNode e, String field) {
        return !e.has(field) || e.get(field).isNull() || e.get(field).asText().isBlank();
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

    private boolean estacionExiste(Connection conn, String nombre, long localidadId) throws SQLException {
        String sql = "SELECT cod_estacion FROM estacion WHERE nombre = ? AND localidad_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombre);
            stmt.setLong(2, localidadId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    private void insertarEstacion(Connection conn, JsonNode e, long localidadId) throws SQLException {
        String sql = """
                INSERT INTO estacion (
                    nombre, tipo, direccion, codigo_postal,
                    longitud, latitud, descripcion, horario,
                    contacto, url, localidad_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, e.get("nombre").asText());
            stmt.setString(2, e.get("tipo").asText());
            stmt.setString(3, e.get("direccion").asText());
            stmt.setString(4, e.get("codigo_postal").asText());
            stmt.setDouble(5, e.get("longitud").asDouble());
            stmt.setDouble(6, e.get("latitud").asDouble());
            stmt.setString(7, e.get("descripcion").asText());
            stmt.setString(8, e.get("horario").asText());
            stmt.setString(9, e.get("contacto").asText());
            stmt.setString(10, e.get("URL").asText());
            stmt.setLong(11, localidadId);

            stmt.executeUpdate();
        }
    }
}