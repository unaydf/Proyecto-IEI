package com.iei.api_busqueda.dto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "Estacion",
        description = "Representación de una estación ITV almacenada en el sistema"
)
public class EstacionDTO {

    @Schema(description = "Identificador único de la estación", example = "12")
    private Long id;

    @Schema(description = "Nombre de la estación", example = "Estación ITV de Valencia")
    private String nombre;

    @Schema(description = "Tipo de estación", example = "FIJA")
    private String tipo;

    @Schema(description = "Dirección postal completa", example = "Av. del Puerto, 123")
    private String direccion;

    @Schema(description = "Código postal", example = "46001")
    private String codigoPostal;

    @Schema(description = "Longitud geográfica", example = "-0.376288")
    private Double longitud;

    @Schema(description = "Latitud geográfica", example = "39.469907")
    private Double latitud;

    @Schema(description = "Descripción adicional de la estación")
    private String descripcion;

    @Schema(description = "Horario de atención", example = "L-V 8:00-20:00")
    private String horario;

    @Schema(description = "Información de contacto", example = "info@itv.es / 961234567")
    private String contacto;

    @Schema(description = "URL de información o cita previa", example = "https://www.itv.es")
    private String url;

    @Schema(description = "Nombre de la localidad", example = "Valencia")
    private String localidadNombre;

    @Schema(description = "Nombre de la provincia", example = "Valencia")
    private String provinciaNombre;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getLocalidadNombre() { return localidadNombre; }
    public void setLocalidadNombre(String localidadNombre) { this.localidadNombre = localidadNombre; }

    public String getProvinciaNombre() { return provinciaNombre; }
    public void setProvinciaNombre(String provinciaNombre) { this.provinciaNombre = provinciaNombre; }
}
