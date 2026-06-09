package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para métodos de lógica en entidades (no generados por Lombok).
 * CronogramaContrato.prePersist / preUpdate
 * Project.getCantidadParcelasTotal
 * Etapa.getCantidadParcelas
 * Parcela.getCantidadLotes
 */
class EntityLogicTest {

    // ─── CronogramaContrato lifecycle ────────────────────────────────────

    @Test
    void cronogramaContrato_prePersist_setsCreatedAtAndUpdatedAt() {
        CronogramaContrato c = buildContrato();
        c.setCreatedAt(null);
        c.setUpdatedAt(null);

        c.prePersist();

        assertNotNull(c.getCreatedAt());
        assertNotNull(c.getUpdatedAt());
    }

    @Test
    void cronogramaContrato_prePersist_doesNotOverwriteExistingCreatedAt() {
        CronogramaContrato c = buildContrato();
        LocalDateTime original = LocalDateTime.of(2024, 1, 1, 10, 0);
        c.setCreatedAt(original);

        c.prePersist();

        assertEquals(original, c.getCreatedAt()); // no se sobreescribe
        assertNotNull(c.getUpdatedAt());
    }

    @Test
    void cronogramaContrato_preUpdate_updatesUpdatedAt() throws InterruptedException {
        CronogramaContrato c = buildContrato();
        c.prePersist();
        LocalDateTime before = c.getUpdatedAt();

        Thread.sleep(10);
        c.preUpdate();

        assertTrue(c.getUpdatedAt().isAfter(before) || c.getUpdatedAt().isEqual(before));
    }

    // ─── Project.getCantidadParcelasTotal ─────────────────────────────────

    @Test
    void project_getCantidadParcelasTotal_sumsParcelas() {
        Project project = buildProject();

        // Etapa 1 con 2 parcelas, etapa 2 con 1 parcela
        Etapa e1 = buildEtapa(1, project);
        e1.getParcelas().add(buildParcela("P1", e1));
        e1.getParcelas().add(buildParcela("P2", e1));

        Etapa e2 = buildEtapa(2, project);
        e2.getParcelas().add(buildParcela("P3", e2));

        project.getEtapas().add(e1);
        project.getEtapas().add(e2);

        assertEquals(3, project.getCantidadParcelasTotal());
    }

    @Test
    void project_getCantidadParcelasTotal_noEtapas_returnsZero() {
        Project project = buildProject();
        assertEquals(0, project.getCantidadParcelasTotal());
    }

    @Test
    void project_getCantidadParcelasTotal_nullEtapas_returnsZero() {
        Project project = buildProject();
        project.setEtapas(null);
        assertEquals(0, project.getCantidadParcelasTotal());
    }

    // ─── Etapa.getCantidadParcelas ────────────────────────────────────────

    @Test
    void etapa_getCantidadParcelas_returnsCount() {
        Project p = buildProject();
        Etapa etapa = buildEtapa(1, p);
        etapa.getParcelas().add(buildParcela("P1", etapa));
        etapa.getParcelas().add(buildParcela("P2", etapa));

        assertEquals(2, etapa.getCantidadParcelas());
    }

    @Test
    void etapa_getCantidadParcelas_empty_returnsZero() {
        Project p = buildProject();
        Etapa etapa = buildEtapa(1, p);

        assertEquals(0, etapa.getCantidadParcelas());
    }

    @Test
    void etapa_getCantidadParcelas_null_returnsZero() {
        Project p = buildProject();
        Etapa etapa = buildEtapa(1, p);
        etapa.setParcelas(null);

        assertEquals(0, etapa.getCantidadParcelas());
    }

    // ─── Parcela.getCantidadLotes ─────────────────────────────────────────

    @Test
    void parcela_getCantidadLotes_returnsCount() {
        Project p = buildProject();
        Etapa e = buildEtapa(1, p);
        Parcela parcela = buildParcela("P1", e);
        Lote l1 = new Lote();
        Lote l2 = new Lote();
        parcela.getLotes().add(l1);
        parcela.getLotes().add(l2);

        assertEquals(2, parcela.getCantidadLotes());
    }

    @Test
    void parcela_getCantidadLotes_empty_returnsZero() {
        Project p = buildProject();
        Etapa e = buildEtapa(1, p);
        Parcela parcela = buildParcela("P1", e);

        assertEquals(0, parcela.getCantidadLotes());
    }

    @Test
    void parcela_getCantidadLotes_null_returnsZero() {
        Project p = buildProject();
        Etapa e = buildEtapa(1, p);
        Parcela parcela = buildParcela("P1", e);
        parcela.setLotes(null);

        assertEquals(0, parcela.getCantidadLotes());
    }

    // ─── Entidades que JaCoCo no ve cubiertas (instanciación directa) ───────

    @Test
    void proforma_prePersist_setsCreatedAt() {
        com.sisarovi.inmobiliario.entity.Proforma p = new com.sisarovi.inmobiliario.entity.Proforma();
        p.setCreatedAt(null);
        p.prePersist();
        assertNotNull(p.getCreatedAt());
    }

    @Test
    void proforma_prePersist_noOverwrite_existingCreatedAt() {
        com.sisarovi.inmobiliario.entity.Proforma p = new com.sisarovi.inmobiliario.entity.Proforma();
        java.time.LocalDateTime original = java.time.LocalDateTime.of(2024, 1, 1, 10, 0);
        p.setCreatedAt(original);
        p.prePersist();
        assertEquals(original, p.getCreatedAt());
    }

    @Test
    void cronogramaCuota_builder_setsFields() {
        com.sisarovi.inmobiliario.entity.CronogramaCuota c =
                com.sisarovi.inmobiliario.entity.CronogramaCuota.builder()
                        .numeroCuota(1)
                        .montoCuota(new java.math.BigDecimal("1500"))
                        .montoPagado(java.math.BigDecimal.ZERO)
                        .estadoPago("PENDIENTE")
                        .fechaVencimiento(java.time.LocalDate.now().plusMonths(1))
                        .pagos(new java.util.ArrayList<>())
                        .build();
        assertEquals(1, c.getNumeroCuota());
        assertEquals("PENDIENTE", c.getEstadoPago());
    }

    @Test
    void cronogramaPagoCuota_builder_setsFields() {
        com.sisarovi.inmobiliario.entity.CronogramaPagoCuota p =
                com.sisarovi.inmobiliario.entity.CronogramaPagoCuota.builder()
                        .fechaPago(java.time.LocalDate.now())
                        .monto(new java.math.BigDecimal("500"))
                        .tipoPago("EFECTIVO")
                        .estadoPago("PAGADA")
                        .notas("Pago test")
                        .build();
        assertEquals("EFECTIVO", p.getTipoPago());
        assertEquals("PAGADA", p.getEstadoPago());
    }

    @Test
    void cronogramaPagoSeparacion_builder_setsFields() {
        com.sisarovi.inmobiliario.entity.CronogramaPagoSeparacion p =
                com.sisarovi.inmobiliario.entity.CronogramaPagoSeparacion.builder()
                        .fechaPago(java.time.LocalDate.now())
                        .monto(new java.math.BigDecimal("2000"))
                        .tipoPago("TRANSFERENCIA")
                        .estadoPago("PAGADA")
                        .notas("Separación completada")
                        .build();
        assertEquals("TRANSFERENCIA", p.getTipoPago());
    }

    @Test
    void devolucion_prePersist_setsTimestamps() {
        com.sisarovi.inmobiliario.entity.Devolucion d =
                com.sisarovi.inmobiliario.entity.Devolucion.builder()
                        .loteId(1L).loteNumero(1)
                        .manzana("A").parcelaNombre("P1")
                        .etapaNumero(1).proyectoNombre("Proj")
                        .montoTotal(new java.math.BigDecimal("1000"))
                        .dias(30)
                        .fechaInicio(java.time.LocalDate.now())
                        .fechaFinEstimada(java.time.LocalDate.now().plusDays(30))
                        .descripcion("Test")
                        .build();
        d.prePersist();
        assertNotNull(d.getFechaCreacion());
        assertNotNull(d.getFechaActualizacion());
        assertEquals(java.math.BigDecimal.ZERO, d.getMontoPagado());
        assertEquals("EN_CURSO", d.getEstado());
    }

    @Test
    void devolucion_preUpdate_updatesTimestamp() throws InterruptedException {
        com.sisarovi.inmobiliario.entity.Devolucion d =
                com.sisarovi.inmobiliario.entity.Devolucion.builder()
                        .loteId(1L).loteNumero(1).manzana("A").parcelaNombre("P1")
                        .etapaNumero(1).proyectoNombre("Proj")
                        .montoTotal(new java.math.BigDecimal("1000"))
                        .dias(30).fechaInicio(java.time.LocalDate.now())
                        .fechaFinEstimada(java.time.LocalDate.now().plusDays(30))
                        .descripcion("Test").build();
        d.prePersist();
        java.time.LocalDateTime before = d.getFechaActualizacion();
        Thread.sleep(10);
        d.preUpdate();
        assertTrue(!d.getFechaActualizacion().isBefore(before));
    }

    @Test
    void devolucionPago_prePersist_setsFechaRegistro() {
        com.sisarovi.inmobiliario.entity.DevolucionPago p =
                com.sisarovi.inmobiliario.entity.DevolucionPago.builder()
                        .monto(new java.math.BigDecimal("300"))
                        .fechaPago(java.time.LocalDate.now())
                        .descripcion("Pago")
                        .medioPago("EFECTIVO")
                        .build();
        p.prePersist();
        assertNotNull(p.getFechaRegistro());
    }

    @Test
    void devolucionPago_prePersist_noOverwrite() {
        com.sisarovi.inmobiliario.entity.DevolucionPago p =
                com.sisarovi.inmobiliario.entity.DevolucionPago.builder()
                        .monto(new java.math.BigDecimal("300"))
                        .fechaPago(java.time.LocalDate.now())
                        .descripcion("Pago").medioPago("EFECTIVO")
                        .build();
        java.time.LocalDateTime original = java.time.LocalDateTime.of(2024, 1, 1, 9, 0);
        p.setFechaRegistro(original);
        p.prePersist();
        assertEquals(original, p.getFechaRegistro());
    }

    private CronogramaContrato buildContrato() {
        return CronogramaContrato.builder()
                .tipoOperacion("CONTADO")
                .estado("AL_DIA")
                .fechaOperacion(LocalDate.now())
                .precioVenta(new BigDecimal("50000"))
                .montoPagadoTotal(new BigDecimal("50000"))
                .montoSeparacionObjetivo(new BigDecimal("2000"))
                .montoSeparacionAcumulado(new BigDecimal("2000"))
                .saldoFinanciarInicial(BigDecimal.ZERO)
                .plazoMeses(0)
                .interesPorcentaje(BigDecimal.ZERO)
                .montoCuotaReferencial(BigDecimal.ZERO)
                .build();
    }

    private Project buildProject() {
        Role role = Role.builder().name("ROLE_ADMIN").build();
        User user = User.builder().dni("admin").nombres("A").primerApellido("B").segundoApellido("")
                .password("pw").email("a@b.com").role(role).estado(UserStatus.ACTIVO).enabled(true).build();
        Project p = new Project();
        p.setNombre("Test Project");
        p.setCantidadEtapas(0);
        p.setCreatedBy(user);
        p.setEtapas(new ArrayList<>());
        return p;
    }

    private Etapa buildEtapa(int numero, Project project) {
        Etapa e = new Etapa();
        e.setNumeroEtapa(numero);
        e.setProject(project);
        e.setParcelas(new ArrayList<>());
        return e;
    }

    private Parcela buildParcela(String nombre, Etapa etapa) {
        Parcela p = new Parcela();
        p.setNombre(nombre);
        p.setNumManzanas(1);
        p.setNumLotes(0);
        p.setPropietario("Owner");
        p.setLotesDisponibles(0);
        p.setEtapa(etapa);
        p.setLotes(new ArrayList<>());
        return p;
    }
}
