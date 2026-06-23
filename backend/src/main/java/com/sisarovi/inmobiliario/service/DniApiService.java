
package com.sisarovi.inmobiliario.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.*;
import org.json.JSONObject;
import com.sisarovi.inmobiliario.exception.ReniecServiceUnavailableException;

@Service
@Slf4j
public class DniApiService {
    private static final String API_URL = "https://api.consultasperu.com/api/v1/query";
    private static final String TOKEN = "db36cac925a708ea8c44d4fd0753455b769ec30106fb2d8d08a60f622ca1770f";

    @Value("${reniec.enabled:true}")
    private boolean reniecEnabled;
    

    // Nuevo método para devolver el JSON completo recibido de la API externa
    public org.json.JSONObject consultarPorDniJson(String dni) {
        JSONObject json = executeRequest(dni);
        if (json == null) {
            return null;
        }

        if (json.has("data") && !json.isNull("data")) {
            return json.getJSONObject("data");
        }

        if (json.has("message")) {
            return json;
        }

        return null;
    }

    public DniApiResponse consultarPorDni(String dni) {
        JSONObject json = executeRequest(dni);
        if (json == null || !json.has("data") || json.isNull("data")) {
            return null;
        }

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

    private JSONObject executeRequest(String dni) {
        if (!reniecEnabled) {
            String detail = "RENIEC deshabilitado para pruebas en el perfil activo";
            log.warn(detail);
            throw new ReniecServiceUnavailableException("Servicio de RENIEC no disponible", detail);
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(TOKEN);
        headers.set("Authorization", "Bearer " + TOKEN);
        JSONObject body = new JSONObject();
        body.put("token", TOKEN);
        body.put("type_document", "dni");
        body.put("document_number", dni);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);
            log.info("\uD83D\uDD0E RAW RESPONSE: {}", response.getBody());
            if (response.getStatusCode() == HttpStatus.OK) {
                return new JSONObject(response.getBody());
            }
            throw new ReniecServiceUnavailableException(
                    "Servicio de RENIEC no disponible",
                    "La API externa respondió con estado " + response.getStatusCode().value()
            );
        } catch (HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            String detail = buildReniecDetail(ex.getStatusCode().value(), responseBody, ex.getMessage());
            log.error("Error consultando API DNI: {} - {}", ex.getStatusCode().value(), detail);
            throw new ReniecServiceUnavailableException("Servicio de RENIEC no disponible", detail);
        } catch (ResourceAccessException ex) {
            String detail = "No se pudo conectar con la API externa de RENIEC: " + ex.getMessage();
            log.error("Error consultando API DNI: {}", detail);
            throw new ReniecServiceUnavailableException("Servicio de RENIEC no disponible", detail);
        } catch (ReniecServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            String detail = "Error inesperado consultando RENIEC: " + ex.getMessage();
            log.error(detail, ex);
            throw new ReniecServiceUnavailableException("Servicio de RENIEC no disponible", detail);
        }
    }

    private String buildReniecDetail(int statusCode, String responseBody, String fallbackMessage) {
        String lowerBody = responseBody == null ? "" : responseBody.toLowerCase();
        if (statusCode == 401 || statusCode == 403 || lowerBody.contains("token") || lowerBody.contains("authorization")) {
            return "Error de token de RENIEC o servicio no habilitado: " + safeSnippet(responseBody, fallbackMessage);
        }
        if (statusCode >= 500) {
            return "El servicio externo de RENIEC respondió con error " + statusCode + ": " + safeSnippet(responseBody, fallbackMessage);
        }
        return safeSnippet(responseBody, fallbackMessage);
    }

    private String safeSnippet(String responseBody, String fallbackMessage) {
        String value = responseBody != null && !responseBody.isBlank() ? responseBody : fallbackMessage;
        if (value == null) {
            return "Sin detalle adicional";
        }
        value = value.replaceAll("[\r\n]+", " ").trim();
        return value.length() > 300 ? value.substring(0, 300) + "..." : value;
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
