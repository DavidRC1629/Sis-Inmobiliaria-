package com.sisarovi.inmobiliario.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests directos de entidades Lombok para que JaCoCo las registre como cubiertas.
 * Con lombok.addLombokGeneratedAnnotation=true, los getters/setters generados
 * se anotan con @Generated y JaCoCo los ignora — solo medimos la lógica real.
 */
class EntidadesBasicasTest {

    // ─── CronogramaCuota ─────────────────────────────────────────────────────

    @Test
    void testCronogramaCuota_noArgsConstructor() {
        CronogramaCuota c = new CronogramaCuota();
        c.setNumeroCuota(1);
        c.setMontoCuota(new BigDecimal("1500"));
        c.setMontoPagado(BigDecimal.ZERO);
        c.setEstadoPago("PENDIENTE");
        c.setFechaVencimiento(LocalDate.now().plusMonths(1));
        c.setPagos(new ArrayList<>());

        assertEquals(1, c.getNumeroCuota());
        assertEquals("PENDIENTE", c.getEstadoPago());
        assertEquals(0, BigDecimal.ZERO.compareTo(c.getMontoPagado()));
    }

    @Test
    void testCronogramaCuota_builder() {
        CronogramaCuota c = CronogramaCuota.builder()
                .numeroCuota(2)
                .montoCuota(new BigDecimal("2000"))
                .montoPagado(new BigDecimal("1000"))
                .estadoPago("PARCIAL")
                .fechaVencimiento(LocalDate.now())
                .pagos(new ArrayList<>())
                .build();

        assertEquals(2, c.getNumeroCuota());
        assertEquals("PARCIAL", c.getEstadoPago());
    }

    @Test
    void testCronogramaCuota_setId() {
        CronogramaCuota c = new CronogramaCuota();
        // id es @GeneratedValue — solo verificamos que no lanza NPE
        assertNull(c.getId()); // antes de persistir es null
    }

    // ─── CronogramaPagoCuota ─────────────────────────────────────────────────

    @Test
    void testCronogramaPagoCuota_noArgsConstructor() {
        CronogramaPagoCuota p = new CronogramaPagoCuota();
        p.setFechaPago(LocalDate.now());
        p.setMonto(new BigDecimal("500"));
        p.setTipoPago("EFECTIVO");
        p.setEstadoPago("PAGADA");
        p.setNotas("Test");

        assertEquals("EFECTIVO", p.getTipoPago());
        assertEquals("PAGADA", p.getEstadoPago());
        assertEquals(0, new BigDecimal("500").compareTo(p.getMonto()));
    }

    @Test
    void testCronogramaPagoCuota_builder() {
        CronogramaPagoCuota p = CronogramaPagoCuota.builder()
                .fechaPago(LocalDate.now())
                .monto(new BigDecimal("750"))
                .tipoPago("TRANSFERENCIA")
                .estadoPago("PARCIAL")
                .notas("Pago parcial")
                .build();

        assertEquals("TRANSFERENCIA", p.getTipoPago());
        assertNotNull(p.getFechaPago());
    }

    @Test
    void testCronogramaPagoCuota_nullId() {
        CronogramaPagoCuota p = new CronogramaPagoCuota();
        assertNull(p.getId());
    }

    // ─── CronogramaPagoSeparacion ────────────────────────────────────────────

    @Test
    void testCronogramaPagoSeparacion_noArgsConstructor() {
        CronogramaPagoSeparacion p = new CronogramaPagoSeparacion();
        p.setFechaPago(LocalDate.now());
        p.setMonto(new BigDecimal("2000"));
        p.setTipoPago("DEPOSITO");
        p.setEstadoPago("PAGADA");
        p.setNotas("Separación completada");

        assertEquals("DEPOSITO", p.getTipoPago());
        assertEquals("PAGADA", p.getEstadoPago());
    }

    @Test
    void testCronogramaPagoSeparacion_builder() {
        CronogramaPagoSeparacion p = CronogramaPagoSeparacion.builder()
                .fechaPago(LocalDate.now())
                .monto(new BigDecimal("1000"))
                .tipoPago("YAPE")
                .estadoPago("PARCIAL")
                .notas("Primer pago de separación")
                .build();

        assertEquals("YAPE", p.getTipoPago());
        assertNotNull(p.getMonto());
    }

    @Test
    void testCronogramaPagoSeparacion_nullId() {
        CronogramaPagoSeparacion p = new CronogramaPagoSeparacion();
        assertNull(p.getId());
    }

    // ─── PasswordRecoveryCode ────────────────────────────────────────────────

    @Test
    void testPasswordRecoveryCode_builder() {
        PasswordRecoveryCode code = PasswordRecoveryCode.builder()
                .code("ABCD1234")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        assertEquals("ABCD1234", code.getCode());
        assertFalse(code.isUsed());
        assertNotNull(code.getExpiresAt());
    }

    @Test
    void testPasswordRecoveryCode_setUsed() {
        PasswordRecoveryCode code = new PasswordRecoveryCode();
        code.setUsed(true);
        assertTrue(code.isUsed());
    }

    // ─── AppSetting ──────────────────────────────────────────────────────────

    @Test
    void testAppSetting_builder() {
        AppSetting setting = AppSetting.builder()
                .key("logo.arovi")
                .value("http://logo.png")
                .build();

        assertEquals("logo.arovi", setting.getKey());
        assertEquals("http://logo.png", setting.getValue());
    }

    @Test
    void testAppSetting_noArgsAndSetters() {
        AppSetting s = new AppSetting();
        s.setKey("test.key");
        s.setValue("test-value");

        assertEquals("test.key", s.getKey());
        assertEquals("test-value", s.getValue());
    }
}
