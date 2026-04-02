
package com.sisarovi.inmobiliario.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.json.JSONObject;

@Service
@Slf4j
public class DniApiService {
    private static final String API_URL = "https://api.consultasperu.com/api/v1/query";
    private static final String TOKEN = "57e38cbb595a20e55fa1cee64e1647fd2cc2f0ecd78f52b9dd9f7fab9c1d2e87";
    

    // Nuevo método para devolver el JSON completo recibido de la API externa
    public org.json.JSONObject consultarPorDniJson(String dni) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject();
        body.put("token", TOKEN);
        body.put("type_document", "dni");
        body.put("document_number", dni);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);
            log.info("\uD83D\uDD0E RAW RESPONSE: {}", response.getBody());
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject json = new JSONObject(response.getBody());
                if (json.has("data") && !json.isNull("data")) {
                    return json.getJSONObject("data");
                } else if (json.has("message")) {
                    // Si la API devuelve un mensaje de error
                    return json;
                }
            }
        } catch (Exception e) {
            log.error("Error consultando API DNI: {}", e.getMessage());
        }
        return null;
    }

    public DniApiResponse consultarPorDni(String dni) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject();
        body.put("token", TOKEN);
        body.put("type_document", "dni");
        body.put("document_number", dni);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);
            log.info("\uD83D\uDD0E RAW RESPONSE: {}", response.getBody());
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject json = new JSONObject(response.getBody());
                // Ajustar el mapeo según la estructura real
                if (json.has("data") && !json.isNull("data")) {
                    JSONObject data = json.getJSONObject("data");
                        String nombres = data.optString("name", "");
                        String surname = data.optString("surname", "");
                        String primerApellido = "";
                        String segundoApellido = "";
                        if (surname != null && !surname.isEmpty()) {
                            String[] apellidos = surname.trim().split(" ");
                            primerApellido = apellidos.length > 0 ? apellidos[0] : "";
                            segundoApellido = apellidos.length > 1 ? apellidos[1] : "";
                        }
                        String fechaNacimiento = data.optString("date_of_birth", "");
                        return new DniApiResponse(nombres, primerApellido, segundoApellido, fechaNacimiento);
                }
            }
        } catch (Exception e) {
            log.error("Error consultando API DNI: {}", e.getMessage());
        }
        return null;
    }

    public static class DniApiResponse {
        public final String nombres;
        public final String primerApellido;
        public final String segundoApellido;
        public final String fechaNacimiento;
        public DniApiResponse(String nombres, String primerApellido, String segundoApellido, String fechaNacimiento) {
            this.nombres = nombres;
            this.primerApellido = primerApellido;
            this.segundoApellido = segundoApellido;
            this.fechaNacimiento = fechaNacimiento;
        }
    }
}
