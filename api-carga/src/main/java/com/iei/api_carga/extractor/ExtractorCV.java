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
public class ExtractorCV {

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String user;
    @Value("${spring.datasource.password}")
    private String password;

    private final Map<String, Long> provinciaCache = new HashMap<>();
    private final Map<String, Long> localidadCache = new HashMap<>();

    public ResultadoCargaDTO insertar(JsonNode estacionesArray) throws Exception {
        ResultadoCargaDTO resultado = new ResultadoCargaDTO();
        resultado.setErroresRechazados(new java.util.ArrayList<>());
        resultado.setErroresReparados(new java.util.ArrayList<>());

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            for (JsonNode estacion : estacionesArray) {

                // Limpiar datos
                limpiarEstacion(estacion);

                // Validar estación
                if (!estacionValida(estacion, resultado)) {
                    continue; // se añade automáticamente a erroresRechazados
                }

                try {
                    // Provincia
                    String provinciaNombre = safeText(estacion.get("provincia_nombre"));
                    String provinciaKey = (provinciaNombre != null) ? provinciaNombre : "UNKNOWN";
                    long provinciaId = getOrInsertProvincia(conn, provinciaNombre, provinciaKey);

                    // Localidad
                    String localidadNombre = safeText(estacion.get("localidad_nombre"));
                    if (localidadNombre == null) localidadNombre = "Desconocida";
                    String localidadKey = localidadNombre + "_" + provinciaId;
                    long localidadId = getOrInsertLocalidad(conn, localidadNombre, provinciaId, localidadKey);

                    // Insertar estación si no existe
                    String nombreEstacion = safeText(estacion.get("nombre"));
                    if (!existeEstacion(conn, nombreEstacion, localidadId)) {
                        insertarEstacion(conn, estacion, localidadId);
                        resultado.setRegistrosCorrectos(resultado.getRegistrosCorrectos() + 1);
                    } else {
                        resultado.getErroresReparados().add(
                                new ResultadoCargaDTO.ErrorReparado(
                                        "CV",
                                        nombreEstacion,
                                        localidadNombre,
                                        "Registro duplicado",
                                        "Ignorado"
                                )
                        );
                        resultado.setRegistrosConErroresReparados(resultado.getRegistrosConErroresReparados() + 1);
                    }

                } catch (SQLException e) {
                    // Error al procesar una estación concreta
                    resultado.getErroresRechazados().add(
                            new ResultadoCargaDTO.ErrorRechazado(
                                    "CV",
                                    safeText(estacion.get("nombre")),
                                    safeText(estacion.get("localidad_nombre")),
                                    "Error SQL: " + e.getMessage()
                            )
                    );
                    resultado.setRegistrosRechazados(resultado.getRegistrosRechazados() + 1);
                }
            }

            conn.commit();
        }

        return resultado;
    }

    // =========================================
    // VALIDACIÓN
    // =========================================
    private boolean estacionValida(JsonNode e, ResultadoCargaDTO resultado) {
        String tipo = safeText(e.get("tipo"));
        String nombre = safeText(e.get("nombre"));
        String localidad = safeText(e.get("localidad_nombre"));
        String provincia = safeText(e.get("provincia_nombre"));
        String cp = safeText(e.get("codigo_postal"));
        String contacto = safeText(e.get("contacto"));

        Double lat = safeCoordinateLat(e.get("latitud"));
        Double lon = safeCoordinateLong(e.get("longitud"));

        boolean valido = true; // Asumimos que es válido al principio
        StringBuilder motivo = new StringBuilder(); // Motivo para rechazo

        if (tipo == null) {
            motivo.append("Tipo de estación nulo. ");
            valido = false;
        } else {
            // Validar móviles, agrícolas y "Otros"
            if ("Estacion movil".equalsIgnoreCase(tipo) ||
                    "Agricola".equalsIgnoreCase(tipo) ||
                    "Otros".equalsIgnoreCase(tipo)) {

                // Verificar que esos campos estén marcados explícitamente como "null"
                if (!"null".equals(localidad)) {
                    motivo.append("Localidad debe ser 'null' para estaciones móviles/agrícolas/otros. ");
                    valido = false;
                }
                if (!"null".equals(provincia)) {
                    motivo.append("Provincia debe ser 'null' para estaciones móviles/agrícolas/otros. ");
                    valido = false;
                }
                if (!"null".equals(cp)) {
                    motivo.append("Código postal debe ser 'null' para estaciones móviles/agrícolas/otros. ");
                    valido = false;
                }
                if (!"null".equals(e.get("latitud").asText()) || !"null".equals(e.get("longitud").asText())) {
                    motivo.append("Coordenadas deben ser 'null' para estaciones móviles/agrícolas/otros. ");
                    valido = false;
                }
            }

            // Validar estaciones fijas
            else if ("Estacion fija".equalsIgnoreCase(tipo)) {
                if (nombre == null || nombre.isEmpty()) {
                    motivo.append("Nombre de estación vacío. ");
                    valido = false;
                }
                if (localidad == null || localidad.isEmpty()) {
                    motivo.append("Localidad vacía para estación fija. ");
                    valido = false;
                }
                if (provincia == null || provincia.isEmpty()) {
                    motivo.append("Provincia vacía para estación fija. ");
                    valido = false;
                }
                if (cp == null || !cp.matches("\\d{5}")) {
                    motivo.append("Código postal inválido o ausente: ").append(cp).append(". ");
                    valido = false;
                } else {
                    // Validar prefijo CP
                    String prefijo = cp.substring(0, 2);
                    if (!prefijo.matches("03|12|46")) {
                        motivo.append("CP no pertenece a la Comunidad Valenciana: ").append(cp).append(". ");
                        valido = false;
                    }
                }
                if (lat == null || lon == null) {
                    motivo.append("Coordenadas inválidas o fuera de rango (lat: ").append(lat).append(", lon: ").append(lon).append("). ");
                    valido = false;
                }
            } else {
                motivo.append("Tipo desconocido: ").append(tipo).append(". ");
                valido = false;
            }
        }

        if (!valido) {
            resultado.getErroresRechazados().add(
                    new ResultadoCargaDTO.ErrorRechazado(
                            "CV",
                            nombre,
                            localidad,
                            motivo.toString()
                    )
            );
            resultado.setRegistrosRechazados(resultado.getRegistrosRechazados() + 1);
        }

        return valido;
    }

    // =========================================
    // UTILIDADES DE LIMPIEZA Y COORDENADAS
    // =========================================
    private void limpiarEstacion(JsonNode estacion) {
        if (!(estacion instanceof ObjectNode)) return;
        ObjectNode e = (ObjectNode) estacion;

        String tipo = safeText(estacion.get("tipo"));
        if (tipo == null) tipo = "Otros"; // Si es nulo, asumir "Otros" para evitar problemas en validación

        // Móviles, Agrícolas, Otros: Asignar campos como "null"
        if ("Estacion movil".equalsIgnoreCase(tipo) ||
                "Agricola".equalsIgnoreCase(tipo) ||
                "Otros".equalsIgnoreCase(tipo)) {
            e.put("localidad_nombre", "null");
            e.put("provincia_nombre", "null");
            e.put("codigo_postal", "null");
            e.put("latitud", "null");
            e.put("longitud", "null");
        }

        // Limpieza común para todos los campos
        e.put("nombre", clean(safeText(estacion.get("nombre"))));
        e.put("direccion", clean(safeText(estacion.get("direccion"))));
        e.put("codigo_postal", clean(safeText(estacion.get("codigo_postal"))));
        e.put("descripcion", clean(safeText(estacion.get("descripcion"))));
        e.put("horario", clean(safeText(estacion.get("horario"))));
        e.put("contacto", clean(safeText(estacion.get("contacto"))));
        e.put("URL", clean(safeText(estacion.get("URL"))));

        // Coordenadas para estaciones fijas o verificar rango
        if (!"Estacion movil".equalsIgnoreCase(tipo) && !"Agricola".equalsIgnoreCase(tipo) && !"Otros".equalsIgnoreCase(tipo)) {
            e.put("latitud", safeCoordinateLat(estacion.get("latitud")));
            e.put("longitud", safeCoordinateLong(estacion.get("longitud")));
        }
    }

    private String clean(String s) {
        if (s == null) return null;
        return s.replaceAll("[^\\p{L}\\p{N}\\s.,@-]", "").trim();
    }

    private String safeText(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText().trim();
    }

    private Double safeCoordinateLat(JsonNode node) {
        if (node == null || !node.isNumber()) return null;
        double v = node.asDouble();
        return (v >= 37.7 && v <= 42) ? v : null; // Comunidad valenciana aproximada
    }

    private Double safeCoordinateLong(JsonNode node) {
        if (node == null || !node.isNumber()) return null;
        double v = node.asDouble();
        return (v >= -1 && v <= 1) ? v : null; // Comunidad valenciana aproximada
    }

    // =========================================
    // MÉTODOS DE DB
    // =========================================
    private long getOrInsertProvincia(Connection conn, String nombre, String cacheKey) throws SQLException {
        if (provinciaCache.containsKey(cacheKey)) return provinciaCache.get(cacheKey);

        String selectSql = "SELECT codigo FROM provincia WHERE nombre = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("codigo");
                provinciaCache.put(cacheKey, id);
                return id;
            }
        }

        String insertSql = "INSERT INTO provincia(nombre) VALUES (?) RETURNING codigo";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("codigo");
                provinciaCache.put(cacheKey, id);
                return id;
            }
        }

        throw new SQLException("No se pudo insertar ni recuperar la provincia: " + nombre);
    }

    private long getOrInsertLocalidad(Connection conn, String nombre, long provinciaId, String cacheKey) throws SQLException {
        if (localidadCache.containsKey(cacheKey)) return localidadCache.get(cacheKey);

        String selectSql = "SELECT codigo FROM localidad WHERE nombre = ? AND provincia_codigo = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, nombre);
            stmt.setLong(2, provinciaId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("codigo");
                localidadCache.put(cacheKey, id);
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
                localidadCache.put(cacheKey, id);
                return id;
            }
        }

        throw new SQLException("No se pudo insertar ni recuperar la localidad: " + nombre);
    }

    private boolean existeEstacion(Connection conn, String nombre, long localidadId) throws SQLException {
        String sql = "SELECT 1 FROM estacion WHERE nombre = ? AND localidad_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombre);
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
            stmt.setString(1, safeText(estacion.get("nombre")));
            stmt.setString(2, safeText(estacion.get("tipo")));
            stmt.setString(3, safeText(estacion.get("direccion")));
            stmt.setString(4, safeText(estacion.get("codigo_postal")));
            setNullableDouble(stmt, 5, estacion.get("longitud"));
            setNullableDouble(stmt, 6, estacion.get("latitud"));
            stmt.setString(7, safeText(estacion.get("descripcion")));
            stmt.setString(8, safeText(estacion.get("horario")));
            stmt.setString(9, safeText(estacion.get("contacto")));
            stmt.setString(10, safeText(estacion.get("URL")));
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
}
