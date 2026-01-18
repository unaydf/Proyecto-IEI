package com.iei.api_busqueda.mapper;

import com.iei.api_busqueda.dto.EstacionDTO;
import com.iei.api_busqueda.model.Estacion;

public class EstacionMapper {

    public static EstacionDTO toDTO(Estacion estacion) {
        if (estacion == null) {
            return null;
        }

        EstacionDTO dto = new EstacionDTO();
        dto.setId(estacion.getId());
        dto.setNombre(estacion.getNombre());
        dto.setTipo(estacion.getTipo() != null ? estacion.getTipo().name() : null);
        dto.setDireccion(estacion.getDireccion());
        dto.setCodigoPostal(estacion.getCodigoPostal());
        dto.setLongitud(estacion.getLongitud());
        dto.setLatitud(estacion.getLatitud());
        dto.setDescripcion(estacion.getDescripcion());
        dto.setHorario(estacion.getHorario());
        dto.setContacto(estacion.getContacto());
        dto.setUrl(estacion.getUrl());

        if (estacion.getLocalidad() != null) {
            dto.setLocalidadNombre(estacion.getLocalidad().getNombre());
            if (estacion.getLocalidad().getProvincia() != null) {
                dto.setProvinciaNombre(estacion.getLocalidad().getProvincia().getNombre());
            }
        }

        return dto;
    }
}
