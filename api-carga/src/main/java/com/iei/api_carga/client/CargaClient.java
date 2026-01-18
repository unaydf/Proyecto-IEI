package com.iei.api_carga.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CargaClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CargaClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public JsonNode getEstacionesGalicia() throws Exception {
        String json = restTemplate.getForObject("http://localhost:9005/galicia", String.class);
        return objectMapper.readTree(json);
    }

    public JsonNode getEstacionesCatalunya() throws Exception {
        String json = restTemplate.getForObject("http://localhost:9004/catalunya", String.class);
        return objectMapper.readTree(json);
    }

    public JsonNode getEstacionesCV() throws Exception {
        String json = restTemplate.getForObject("http://localhost:9003/valencia", String.class);
        return objectMapper.readTree(json);
    }
}
