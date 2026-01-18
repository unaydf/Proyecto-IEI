package com.iei.api_carga.service;

import com.iei.api_carga.client.CargaClient;
import com.iei.api_carga.dto.ResultadoCargaDTO;
import com.iei.api_carga.extractor.ExtractorCV;
import com.iei.api_carga.extractor.ExtractorCat;
import com.iei.api_carga.extractor.ExtractorGal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
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
    public ResultadoCargaDTO cargarDatos(List<String> fuentes){
        int totalCorrectos = 0;
        int totalErroresReparados = 0;
        int totalRechazados = 0;

        List<ResultadoCargaDTO.ErrorReparado> erroresReparados = new ArrayList<>();
        List<ResultadoCargaDTO.ErrorRechazado> erroresRechazados = new ArrayList<>();

        for (String fuente : fuentes) {
            try {
                ResultadoCargaDTO resultado = switch (fuente.toUpperCase()) {
                    case "GAL" -> {
                        JsonNode jsonGal = cargaClient.getEstacionesGalicia();
                        yield extractorGal.insertar(jsonGal);
                    }
                    case "CAT" -> {
                        JsonNode jsonCat = cargaClient.getEstacionesCatalunya();
                        yield extractorCat.insertar(jsonCat);
                    }
                    case "CV" -> {
                        JsonNode jsonCV = cargaClient.getEstacionesCV();
                        yield extractorCV.insertar(jsonCV);
                    }
                    default -> throw new RuntimeException("Fuente desconocida: " + fuente);
                };

                if (resultado != null) {
                    totalCorrectos += resultado.getRegistrosCorrectos();
                    totalErroresReparados += resultado.getRegistrosConErroresReparados();
                    totalRechazados += resultado.getRegistrosRechazados();

                    if (resultado.getErroresReparados() != null) erroresReparados.addAll(resultado.getErroresReparados());
                    if (resultado.getErroresRechazados() != null) erroresRechazados.addAll(resultado.getErroresRechazados());
                }

            } catch (Exception e) {
                System.err.println("Error cargando fuente " + fuente + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return new ResultadoCargaDTO(
                totalCorrectos,
                totalErroresReparados,
                totalRechazados,
                erroresReparados,
                erroresRechazados
        );
    }

    @Transactional
    public void eliminarDatos() {
        em.createNativeQuery("TRUNCATE TABLE estacion CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE localidad CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE provincia CASCADE").executeUpdate();
    }
}
