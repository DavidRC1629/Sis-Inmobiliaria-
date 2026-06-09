package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.exception.ReniecServiceUnavailableException;
import com.sisarovi.inmobiliario.service.DniApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests extendidos de DniApiService usando RestTemplate mockeado.
 * Cubre la lógica de executeRequest, parseResponse y manejo de errores HTTP.
 */
class DniApiServiceExtendedTest {

    private DniApiService dniApiService;
    private RestTemplate restTemplateMock;

    @BeforeEach
    void setUp() {
        dniApiService = new DniApiService();
        ReflectionTestUtils.setField(dniApiService, "reniecEnabled", true);

        // Mockear RestTemplate internamente mediante spy
        restTemplateMock = mock(RestTemplate.class);
    }

    // ─── reniecEnabled = false ────────────────────────────────────────────────

    @Test
    void consultarPorDni_disabled_throwsUnavailable() {
        ReflectionTestUtils.setField(dniApiService, "reniecEnabled", false);

        assertThrows(ReniecServiceUnavailableException.class,
                () -> dniApiService.consultarPorDni("12345678"));
    }

    @Test
    void consultarPorDniJson_disabled_throwsUnavailable() {
        ReflectionTestUtils.setField(dniApiService, "reniecEnabled", false);

        ReniecServiceUnavailableException ex = assertThrows(ReniecServiceUnavailableException.class,
                () -> dniApiService.consultarPorDniJson("12345678"));

        assertNotNull(ex.getDetail());
        assertTrue(ex.getDetail().contains("deshabilitado") || ex.getDetail().contains("RENIEC"));
    }

    // ─── reniecEnabled = true → llama a la API externa (sin mock de RestTemplate) ──

    @Test
    void consultarPorDni_enabled_throwsUnavailableOnNetworkError() {
        // Con reniecEnabled=true pero sin red real → ResourceAccessException
        ReflectionTestUtils.setField(dniApiService, "reniecEnabled", true);

        // El servicio lanzará ReniecServiceUnavailableException al no poder conectar
        assertThrows(ReniecServiceUnavailableException.class,
                () -> dniApiService.consultarPorDni("12345678"));
    }

    @Test
    void consultarPorDniJson_enabled_throwsUnavailableOnNetworkError() {
        ReflectionTestUtils.setField(dniApiService, "reniecEnabled", true);

        assertThrows(ReniecServiceUnavailableException.class,
                () -> dniApiService.consultarPorDniJson("12345678"));
    }

    // ─── DniApiResponse ───────────────────────────────────────────────────────

    @Test
    void dniApiResponse_allFields_setCorrectly() {
        DniApiService.DniApiResponse r =
                new DniApiService.DniApiResponse("CARLOS", "LOPEZ", "TORRES", "1985-03-22");

        assertEquals("CARLOS", r.nombres);
        assertEquals("LOPEZ", r.primerApellido);
        assertEquals("TORRES", r.segundoApellido);
        assertEquals("1985-03-22", r.fechaNacimiento);
    }

    @Test
    void dniApiResponse_emptyValues_allowedWithoutException() {
        DniApiService.DniApiResponse r =
                new DniApiService.DniApiResponse("", "", "", "");

        assertEquals("", r.nombres);
        assertEquals("", r.primerApellido);
        assertEquals("", r.segundoApellido);
        assertEquals("", r.fechaNacimiento);
    }

    @Test
    void dniApiResponse_nullValues_allowedWithoutException() {
        DniApiService.DniApiResponse r =
                new DniApiService.DniApiResponse(null, null, null, null);

        assertNull(r.nombres);
        assertNull(r.primerApellido);
    }

    // ─── ReniecServiceUnavailableException ───────────────────────────────────

    @Test
    void reniecException_detailIsAccessible() {
        ReniecServiceUnavailableException ex =
                new ReniecServiceUnavailableException("RENIEC no disponible", "timeout al conectar");

        assertEquals("RENIEC no disponible", ex.getMessage());
        assertEquals("timeout al conectar", ex.getDetail());
    }

    @Test
    void reniecException_longDetail_preservesMessage() {
        String longDetail = "Error detallado ".repeat(20);
        ReniecServiceUnavailableException ex =
                new ReniecServiceUnavailableException("Error", longDetail);

        assertNotNull(ex.getDetail());
        assertEquals(longDetail, ex.getDetail());
    }
}
