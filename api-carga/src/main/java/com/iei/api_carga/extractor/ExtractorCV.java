package com.iei.api_carga.extractor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

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

    // Caché para evitar consultas repetitivas dentro de la misma ejecución
    private final Map<String, Long> provinciaCache = new HashMap<>();
    private final Map<String, Long> localidadCache = new HashMap<>();

    public void insertar(JsonNode estacionesArray) throws Exception {
        // Usamos try-with-resources para asegurar el cierre de la conexión
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false); // Desactivar auto-commit para transacciones en bloque

            for (JsonNode estacion : estacionesArray) {

                // 1. VALIDACIÓN PREVIA
                // Si la estación no cumple las reglas (incluida la de móvil/agrícola), la saltamos.
                if (!estacionValida(estacion)) {
                    continue;
                }

                try {
                    // 2. GESTIÓN DE PROVINCIA (Busca o Inserta)
                    String provinciaNombre = safeText(estacion.get("provincia_nombre"));
                    // Usamos el nombre como clave si no hay código, para la caché
                    String provinciaKey = (provinciaNombre != null) ? provinciaNombre : "UNKNOWN";

                    long provinciaId = getOrInsertProvincia(conn, provinciaNombre, provinciaKey);

                    // 3. GESTIÓN DE LOCALIDAD (Busca o Inserta)
                    String localidadNombre = safeText(estacion.get("localidad_nombre"));
                    // Si viene nulo (caso estaciones móviles válidas), manejamos un default o saltamos según lógica de negocio.
                    // Asumiremos que para insertar en BD relacional necesitamos localidad,
                    // pero si la validación móvil permite nulos, aquí debemos tener cuidado.
                    if (localidadNombre == null) {
                        localidadNombre = "Desconocida"; // O manejar según tu esquema
                    }
                    String localidadKey = localidadNombre + "_" + provinciaId;

                    long localidadId = getOrInsertLocalidad(conn, localidadNombre, provinciaId, localidadKey);

                    // 4. INSERTAR ESTACIÓN (Evitando duplicados)
                    if (!existeEstacion(conn, estacion.get("nombre").asText(), localidadId)) {
                        insertarEstacion(conn, estacion, localidadId);
                    } else {
                        System.out.println("Estación duplicada omitida: " + estacion.get("nombre").asText());
                    }

                } catch (SQLException e) {
                    // Si falla una estación concreta, hacemos rollback parcial o log y seguimos?
                    // Aquí imprimimos y seguimos con la siguiente para no detener toda la carga.
                    System.err.println("Error procesando estación: " + estacion.get("nombre"));
                    e.printStackTrace();
                }
            }

            conn.commit(); // Confirmar cambios al final
            System.out.println("Proceso de carga finalizado.");

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Valida si la estación cumple los requisitos para ser insertada.
     */
    private boolean estacionValida(JsonNode e) {
        String tipo = safeText(e.get("tipo"));
        if (tipo == null) return false;

        // --- REGLA ESPECÍFICA: ESTACIONES MÓVILES O AGRÍCOLAS ---
        // "Si una estacion movil o agricola tiene datos de codigo_postal o localidad_nombre no se debe insertar"
        if ("Estación móvil".equalsIgnoreCase(tipo) || "Agricola".equalsIgnoreCase(tipo) || "Otros".equalsIgnoreCase(tipo)) {
            boolean tieneCP = !isEmpty(e, "codigo_postal");
            boolean tieneLocalidad = !isEmpty(e, "localidad_nombre");

            // Si TIENE datos de ubicación fija, es INVALIDA para este tipo según la regla.
            if (tieneCP || tieneLocalidad) {
                return false;
            }

            // Si pasa el filtro anterior, validamos que tenga al menos coordenadas y contacto
            if (!isEmpty(e, "latitud") || !isEmpty(e, "longitud")) return false;

            String contacto = safeText(e.get("contacto"));
            // Validación simple de email (debe contener @)
            if (contacto == null || !contacto.contains("@")) return false;

            return true;
        }

        // --- REGLA: ESTACIONES FIJAS ---
        if ("Estación fija".equalsIgnoreCase(tipo)) {
            if (isEmpty(e, "nombre")) return false;
            if (isEmpty(e, "direccion")) return false;
            if (isEmpty(e, "codigo_postal")) return false;
            if (isEmpty(e, "localidad_nombre")) return false;
            if (isEmpty(e, "provincia_nombre")) return false;

            // Validar Horario
            String horario = safeText(e.get("horario"));
            if (!horarioValido(horario)) return false;

            // Validar CP (Comunidad Valenciana: 03, 12, 46)
            String cp = safeText(e.get("codigo_postal"));
            if (cp == null || !cp.matches("\\d{5}")) return false;
            String prefijo = cp.substring(0, 2);
            if (!prefijo.matches("03|12|46")) return false;

            // Validar Coordenadas numéricas
            if (!isNumber(e, "latitud")) return false;
            if (!isNumber(e, "longitud")) return false;

            // Validar Contacto
            String contacto = safeText(e.get("contacto"));
            if (contacto == null || !contacto.contains("@")) return false;

            return true;
        }

        return false; // Tipo desconocido
    }

    // --- MÉTODOS DE SOPORTE DB (SELECT OR INSERT) ---

    private long getOrInsertProvincia(Connection conn, String nombre, String cacheKey) throws SQLException {
        if (provinciaCache.containsKey(cacheKey)) {
            return provinciaCache.get(cacheKey);
        }

        // 1. Intentar buscar
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

        // 2. Si no existe, insertar
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
        if (localidadCache.containsKey(cacheKey)) {
            return localidadCache.get(cacheKey);
        }

        // 1. Intentar buscar (filtrando por provincia para evitar coincidencias de nombre en distintas provincias)
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

        // 2. Si no existe, insertar
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
        // Verificamos duplicados por Nombre + Localidad (ajustar según criterio de unicidad)
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
            stmt.setString(10, safeText(estacion.get("URL"))); // Ojo mayus/minus en JSON
            stmt.setLong(11, localidadId);

            stmt.executeUpdate();
        }
    }

    // --- UTILIDADES ---

    private boolean horarioValido(String h) {
        if (h == null || h.trim().isEmpty()) return false;
        // Regex básica para detectar formato hora HH:MM
        return h.matches(".*([01]?\\d|2[0-3]):[0-5]\\d.*");
    }

    private boolean isEmpty(JsonNode e, String f) {
        return !e.has(f) || e.get(f).isNull() || e.get(f).asText().trim().isEmpty();
    }

    private boolean isNumber(JsonNode e, String f) {
        return e.has(f) && e.get(f).isNumber();
    }

    private String safeText(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText().trim();
    }

    private void setNullableDouble(PreparedStatement stmt, int idx, JsonNode node) throws SQLException {
        if (node == null || node.isNull() || !node.isNumber()) {
            stmt.setNull(idx, Types.DOUBLE);
        } else {
            stmt.setDouble(idx, node.asDouble());
        }
    }
}
