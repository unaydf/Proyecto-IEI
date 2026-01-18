package com.iei.api_busqueda.service;

import com.iei.api_busqueda.model.Estacion;
import com.iei.api_busqueda.model.Tipo;
import com.iei.api_busqueda.repository.EstacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstacionService {

    private final EstacionRepository estacionRepository;

    public EstacionService(EstacionRepository estacionRepository) {
        this.estacionRepository = estacionRepository;
    }

    public List<Estacion> buscarPorFiltros(String localidad, String codigoPostal, String provincia, String tipoStr) {
        Tipo tipo = null;

        if (tipoStr != null && !tipoStr.isBlank()) {
            try {
                tipo = Tipo.valueOf(tipoStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("El tipo de estación indicado no es válido");
            }
        }

        return estacionRepository.buscarPorFiltros(
                localidad,
                codigoPostal,
                provincia,
                tipo
        );
    }
}
