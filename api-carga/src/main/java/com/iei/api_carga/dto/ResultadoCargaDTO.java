package com.iei.api_carga.dto;

import java.util.List;

public class ResultadoCargaDTO {

    private int registrosCorrectos;
    private int registrosConErroresReparados;
    private int registrosRechazados;

    // Listado de errores reparados
    private List<ErrorReparado> erroresReparados;

    // Listado de errores rechazados
    private List<ErrorRechazado> erroresRechazados;

    // Constructor vacío (para Jackson)
    public ResultadoCargaDTO() {}

    // Constructor completo
    public ResultadoCargaDTO(int registrosCorrectos,
                              int registrosConErroresReparados,
                              int registrosRechazados,
                              List<ErrorReparado> erroresReparados,
                              List<ErrorRechazado> erroresRechazados) {
        this.registrosCorrectos = registrosCorrectos;
        this.registrosConErroresReparados = registrosConErroresReparados;
        this.registrosRechazados = registrosRechazados;
        this.erroresReparados = erroresReparados;
        this.erroresRechazados = erroresRechazados;
    }

    // Getters y setters
    public int getRegistrosCorrectos() {
        return registrosCorrectos;
    }
    public void setRegistrosCorrectos(int registrosCorrectos) {
        this.registrosCorrectos = registrosCorrectos;
    }

    public int getRegistrosConErroresReparados() {
        return registrosConErroresReparados;
    }
    public void setRegistrosConErroresReparados(int registrosConErroresReparados) {
        this.registrosConErroresReparados = registrosConErroresReparados;
    }

    public int getRegistrosRechazados() {
        return registrosRechazados;
    }
    public void setRegistrosRechazados(int registrosRechazados) {
        this.registrosRechazados = registrosRechazados;
    }

    public List<ErrorReparado> getErroresReparados() {
        return erroresReparados;
    }
    public void setErroresReparados(List<ErrorReparado> erroresReparados) {
        this.erroresReparados = erroresReparados;
    }

    public List<ErrorRechazado> getErroresRechazados() {
        return erroresRechazados;
    }
    public void setErroresRechazados(List<ErrorRechazado> erroresRechazados) {
        this.erroresRechazados = erroresRechazados;
    }

    // Clases internas para los errores
    public static class ErrorReparado {
        private String fuente;
        private String nombre;
        private String localidad;
        private String motivo;
        private String operacion;

        public ErrorReparado() {}

        public ErrorReparado(String fuente, String nombre, String localidad, String motivo, String operacion) {
            this.fuente = fuente;
            this.nombre = nombre;
            this.localidad = localidad;
            this.motivo = motivo;
            this.operacion = operacion;
        }

        // Getters y setters
        public String getFuente() { return fuente; }
        public void setFuente(String fuente) { this.fuente = fuente; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getLocalidad() { return localidad; }
        public void setLocalidad(String localidad) { this.localidad = localidad; }

        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }

        public String getOperacion() { return operacion; }
        public void setOperacion(String operacion) { this.operacion = operacion; }
    }

    public static class ErrorRechazado {
        private String fuente;
        private String nombre;
        private String localidad;
        private String motivo;

        public ErrorRechazado() {}

        public ErrorRechazado(String fuente, String nombre, String localidad, String motivo) {
            this.fuente = fuente;
            this.nombre = nombre;
            this.localidad = localidad;
            this.motivo = motivo;
        }

        // Getters y setters
        public String getFuente() { return fuente; }
        public void setFuente(String fuente) { this.fuente = fuente; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getLocalidad() { return localidad; }
        public void setLocalidad(String localidad) { this.localidad = localidad; }

        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
    }
}
