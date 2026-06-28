package com.sisarovi.inmobiliario.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.service.RecoveryEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests extendidos de RecoveryEmailService.
 * Cubre todos los branches: disabled, apiKey vacía/nula, y envío real con error de red.
 */
class RecoveryEmailServiceExtendedTest {

    private RecoveryEmailService service;

    @BeforeEach
    void setUp() {
        service = new RecoveryEmailService(new ObjectMapper());
        ReflectionTestUtils.setField(service, "resendFromEmail", "onboarding@resend.dev");
    }

    // ─── resendEnabled = false → no hace nada ────────────────────────────────

    @Test
    void sendTemporaryCode_disabled_doesNothing() {
        ReflectionTestUtils.setField(service, "resendEnabled", false);
        ReflectionTestUtils.setField(service, "resendApiKey", "");

        assertDoesNotThrow(() -> service.sendTemporaryCode("user@test.com", "ABCD1234"));
    }

    @Test
    void sendTemporaryCode_disabledWithKey_stillDoesNothing() {
        ReflectionTestUtils.setField(service, "resendEnabled", false);
        ReflectionTestUtils.setField(service, "resendApiKey", "re_real_key_123");

        assertDoesNotThrow(() -> service.sendTemporaryCode("user@test.com", "WXYZ5678"));
    }

    // ─── resendEnabled = true, apiKey vacía/nula → lanza excepción ───────────

    @Test
    void sendTemporaryCode_enabledEmptyApiKey_throws() {
        ReflectionTestUtils.setField(service, "resendEnabled", true);
        ReflectionTestUtils.setField(service, "resendApiKey", "");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.sendTemporaryCode("user@test.com", "ABCD1234"));

        assertTrue(ex.getMessage().contains("API Key") || ex.getMessage().contains("api"));
    }

    @Test
    void sendTemporaryCode_enabledNullApiKey_throws() {
        ReflectionTestUtils.setField(service, "resendEnabled", true);
        ReflectionTestUtils.setField(service, "resendApiKey", null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.sendTemporaryCode("user@test.com", "ABCD1234"));

        assertNotNull(ex.getMessage());
    }

    @Test
    void sendTemporaryCode_enabledWhitespaceApiKey_throws() {
        ReflectionTestUtils.setField(service, "resendEnabled", true);
        ReflectionTestUtils.setField(service, "resendApiKey", "   ");

        assertThrows(RuntimeException.class,
                () -> service.sendTemporaryCode("user@test.com", "ABCD1234"));
    }

    // ─── resendEnabled = true, apiKey presente → intenta enviar (falla en red) ─

    @Test
    void sendTemporaryCode_enabledWithKey_failsOnNetwork() {
        // Con apiKey presente pero sin red real, lanza RuntimeException (no NPE)
        ReflectionTestUtils.setField(service, "resendEnabled", true);
        ReflectionTestUtils.setField(service, "resendApiKey", "re_test_fake_key");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.sendTemporaryCode("user@test.com", "ABCD1234"));

        // El mensaje debe ser sobre no poder enviar el correo, no sobre apiKey
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().contains("API Key"));
    }

    @Test
    void sendTemporaryCode_enabledWithKey_differentEmail_failsOnNetwork() {
        ReflectionTestUtils.setField(service, "resendEnabled", true);
        ReflectionTestUtils.setField(service, "resendApiKey", "re_another_fake");

        assertThrows(RuntimeException.class,
                () -> service.sendTemporaryCode("otro@dominio.com", "ZXCV9876"));
    }

    @Test
    void sendTemporaryCode_enabledWithKey_differentCode_failsOnNetwork() {
        ReflectionTestUtils.setField(service, "resendEnabled", true);
        ReflectionTestUtils.setField(service, "resendApiKey", "re_yet_another");

        // Cambia el código para cubrir el branch de generación del HTML
        assertThrows(RuntimeException.class,
                () -> service.sendTemporaryCode("test@email.com", "12345678"));
    }
}
