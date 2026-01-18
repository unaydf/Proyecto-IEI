package com.iei.api_carga.service;

import com.iei.api_carga.client.CargaClient;
import com.iei.api_carga.extractor.ExtractorCV;
import com.iei.api_carga.extractor.ExtractorCat;
import com.iei.api_carga.extractor.ExtractorGal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Service
public class CargaService {

    @PersistenceContext
    private EntityManager em;

    private final CargaClient cargaClient;
    private final ExtractorGal extractorGal;
    private final ExtractorCat extractorCat;
    private final ExtractorCV extractorCV;


    public CargaService(CargaClient cargaClient,
                        ExtractorGal extractorGal,
                        ExtractorCat extractorCat,
                        ExtractorCV extractorCV) {
        this.cargaClient = cargaClient;
        this.extractorGal = extractorGal;
        this.extractorCat = extractorCat;
        this.extractorCV = extractorCV;
    }

    @Transactional
    public void cargarDatos(List<String> fuentes){
        for (String fuente : fuentes) {
            try {
                switch (fuente.toUpperCase()) {
                    case "GAL":
                        JsonNode jsonGal = cargaClient.getEstacionesGalicia();
                        extractorGal.insertar(jsonGal);
                        break;
                    case "CAT":
                        JsonNode jsonCat = cargaClient.getEstacionesCatalunya();
                        extractorCat.insertar(jsonCat);
                        break;

                    case "CV":
                        JsonNode jsonCV = cargaClient.getEstacionesCV();
                        extractorCV.insertar(jsonCV);
                        break;
                    default:
                        throw new RuntimeException("Fuente desconocida: " + fuente);
                }
            } catch (Exception e) {
                System.err.println("Error cargando fuente " + fuente + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Transactional
    public void eliminarDatos() {
        em.createNativeQuery("TRUNCATE TABLE estacion CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE localidad CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE provincia CASCADE").executeUpdate();
    }
}
