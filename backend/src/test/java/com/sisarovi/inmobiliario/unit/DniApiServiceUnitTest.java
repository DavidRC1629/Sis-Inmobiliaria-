package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.exception.ReniecServiceUnavailableException;
import com.sisarovi.inmobiliario.service.DniApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para DniApiService.
 * No llama a la API externa — prueba la lógica de control de flujo.
 */
class DniApiServiceUnitTest {

    private DniApiService dniApiService;

    @BeforeEach
    void setUp() {
        dniApiService = new DniApiService();
    }

    // ─── reniecEnabled = false ────────────────────────────────────────────

    @Test
    void consultarPorDni_reniecDisabled_throwsUnavailable() {
        ReflectionTestUtils.setField(dniApiService, "reniecEnabled", false);

        assertThrows(ReniecServiceUnavailableException.class,
                () -> dniApiService.consultarPorDni("12345678"));
    }

    @Test
    void consultarPorDniJson_reniecDisabled_throwsUnavailable() {
        ReflectionTestUtils.setField(dniApiService, "reniecEnabled", false);

        assertThrows(ReniecServiceUnavailableException.class,
                () -> dniApiService.consultarPorDniJson("12345678"));
    }

    // ─── DniApiResponse constructor ───────────────────────────────────────

    @Test
    void dniApiResponse_constructor_setsFields() {
        DniApiService.DniApiResponse resp =
                new DniApiService.DniApiResponse("JUAN", "PEREZ", "LOPEZ", "1990-01-15");

        assertEquals("JUAN", resp.nombres);
        assertEquals("PEREZ", resp.primerApellido);
        assertEquals("LOPEZ", resp.segundoApellido);
        assertEquals("1990-01-15", resp.fechaNacimiento);
    }

    @Test
    void dniApiResponse_emptyFields_allowed() {
        DniApiService.DniApiResponse resp =
                new DniApiService.DniApiResponse("", "", "", "");

        assertEquals("", resp.nombres);
        assertEquals("", resp.primerApellido);
    }
}
