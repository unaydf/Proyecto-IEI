package com.iei.api_carga.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

@Component
public class CargaClient {

    private final RestTemplate restTemplate;

    public CargaClient() {
        this.restTemplate = new RestTemplate();
    }

    public JsonNode getEstacionesGalicia() {
        return restTemplate.getForObject("http://localhost:9005/galicia", JsonNode.class);
    }

    public JsonNode getEstacionesCatalunya() {
        return restTemplate.getForObject("http://localhost:9004/catalunya", JsonNode.class);
    }

    public JsonNode getEstacionesCV() {
        return restTemplate.getForObject("http://localhost:9003/valencia", JsonNode.class);
    }
}
