package com.sisarovi.inmobiliario.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecoveryEmailService {

    private final ObjectMapper objectMapper;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:onboarding@resend.dev}")
    private String resendFromEmail;

    @Value("${resend.enabled:true}")
    private boolean resendEnabled;

    public void sendTemporaryCode(String toEmail, String temporaryCode) {
        if (!resendEnabled) {
            return;
        }

        if (resendApiKey == null || resendApiKey.trim().isEmpty()) {
            throw new RuntimeException("No está configurada la API Key de Resend");
        }

        String html = "<p>Hola,</p>"
                + "<p>Tu código temporal para recuperar tu contraseña es:</p>"
                + "<h2 style='letter-spacing: 2px;'>" + temporaryCode + "</h2>"
                + "<p>Este código es temporal y expirará en <strong>10 minutos</strong>."
                + " Después de ese tiempo no funcionará.</p>"
                + "<p>Si no solicitaste este cambio, ignora este mensaje.</p>";

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", resendFromEmail);
        payload.put("to", toEmail);
        payload.put("subject", "Código temporal de recuperación");
        payload.put("html", html);

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + resendApiKey);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.resend.com/emails",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("No se pudo enviar el correo de recuperación");
            }
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo enviar el correo de recuperación");
        } catch (HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null
                    && responseBody.toLowerCase().contains("you can only send testing emails to your own email address")) {
                throw new RuntimeException(
                        "Resend está en modo de prueba. Verifica un dominio en resend.com/domains, "
                                + "configura RESEND_FROM_EMAIL con ese dominio y vuelve a intentar.");
            }
            throw new RuntimeException("No se pudo enviar el correo de recuperación: " + responseBody);
        } catch (Exception ex) {
            String detail = ex.getMessage() != null ? ex.getMessage() : "sin detalle";
            throw new RuntimeException("No se pudo enviar el correo de recuperación: " + detail);
        }
    }
}
