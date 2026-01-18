package com.iei.api_carga.model;

import jakarta.persistence.*;

@Entity
@Table(name = "estacion")
public class Estacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_estacion")
    private Long id;

    private String nombre;

    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    private String direccion;

    @Column(name = "codigo_postal")
    private String codigoPostal;

    private Double longitud;

    private Double latitud;

    private String descripcion;

    private String horario;

    private String contacto;

    private String url;

    @ManyToOne
    @JoinColumn(name = "localidad_id", nullable = false)
    private Localidad localidad;

    public Estacion() {}

    public Estacion(Long id,
                    String nombre,
                    Tipo tipo,
                    String direccion,
                    String codigoPostal,
                    Double longitud,
                    Double latitud,
                    String descripcion,
                    String horario,
                    String contacto,
                    String url,
                    Localidad localidad) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.direccion = direccion;
        this.codigoPostal = codigoPostal;
        this.longitud = longitud;
        this.latitud = latitud;
        this.descripcion = descripcion;
        this.horario = horario;
        this.contacto = contacto;
        this.url = url;
        this.localidad = localidad;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

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

    public Localidad getLocalidad() { return localidad; }
    public void setLocalidad(Localidad localidad) { this.localidad = localidad; }
}
