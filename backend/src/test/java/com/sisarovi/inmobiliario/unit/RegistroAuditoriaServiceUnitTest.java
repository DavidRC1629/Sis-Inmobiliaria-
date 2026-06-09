package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.entity.RegistroAuditoria;
import com.sisarovi.inmobiliario.repository.RegistroAuditoriaRepository;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistroAuditoriaServiceUnitTest {

    @Mock RegistroAuditoriaRepository registroAuditoriaRepository;

    @InjectMocks RegistroAuditoriaService registroAuditoriaService;

    @BeforeEach
    void setUp() {
        when(registroAuditoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ─── registrarAccion (3 params) ───────────────────────────────────────

    @Test
    void registrarAccion_3params_savesRecord() {
        when(registroAuditoriaRepository.findTopByUsuarioAndAccionOrderByFechaHoraDesc(any(), any()))
                .thenReturn(Optional.empty());

        registroAuditoriaService.registrarAccion("admin", "CREATE", "Descripción");

        verify(registroAuditoriaRepository).save(argThat(r ->
                "admin".equals(r.getUsuario()) && "CREATE".equals(r.getAccion())));
    }

    @Test
    void registrarAccion_nullUsuario_usesAnonimo() {
        when(registroAuditoriaRepository.findTopByUsuarioAndAccionOrderByFechaHoraDesc(any(), any()))
                .thenReturn(Optional.empty());

        registroAuditoriaService.registrarAccion(null, "UPDATE", "desc");

        verify(registroAuditoriaRepository).save(argThat(r -> "ANONIMO".equals(r.getUsuario())));
    }

    @Test
    void registrarAccion_loginDuplicadoReciente_noGuarda() {
        RegistroAuditoria reciente = RegistroAuditoria.builder()
                .usuario("admin").accion("LOGIN")
                .fechaHora(LocalDateTime.now().minusSeconds(5))
                .descripcion("login").build();

        when(registroAuditoriaRepository.findTopByUsuarioAndAccionOrderByFechaHoraDesc("admin", "LOGIN"))
                .thenReturn(Optional.of(reciente));

        registroAuditoriaService.registrarAccion("admin", "LOGIN", "login");

        verify(registroAuditoriaRepository, never()).save(any());
    }

    @Test
    void registrarAccion_loginAntiguo_siGuarda() {
        RegistroAuditoria antiguo = RegistroAuditoria.builder()
                .usuario("admin").accion("LOGIN")
                .fechaHora(LocalDateTime.now().minusSeconds(30))
                .descripcion("login").build();

        when(registroAuditoriaRepository.findTopByUsuarioAndAccionOrderByFechaHoraDesc("admin", "LOGIN"))
                .thenReturn(Optional.of(antiguo));

        registroAuditoriaService.registrarAccion("admin", "LOGIN", "login");

        verify(registroAuditoriaRepository).save(any());
    }

    @Test
    void registrarAccion_nullDescripcion_usesDefault() {
        when(registroAuditoriaRepository.findTopByUsuarioAndAccionOrderByFechaHoraDesc(any(), any()))
                .thenReturn(Optional.empty());

        registroAuditoriaService.registrarAccion("admin", "DELETE", null);

        verify(registroAuditoriaRepository).save(argThat(r ->
                "Sin descripción".equals(r.getDescripcion())));
    }

    // ─── registrarAccion (7 params) ───────────────────────────────────────

    @Test
    void registrarAccion_7params_savesWithFields() {
        when(registroAuditoriaRepository.findTopByUsuarioAndAccionOrderByFechaHoraDesc(any(), any()))
                .thenReturn(Optional.empty());

        registroAuditoriaService.registrarAccion("admin", "INGRESO", "desc",
                "Juan Perez", "12345678", new BigDecimal("500"), "EFECTIVO");

        verify(registroAuditoriaRepository).save(argThat(r ->
                "Juan Perez".equals(r.getClienteNombre()) &&
                new BigDecimal("500").compareTo(r.getMonto()) == 0));
    }

    // ─── registrarAccion (13 params) ──────────────────────────────────────

    @Test
    void registrarAccion_13params_savesFullRecord() {
        when(registroAuditoriaRepository.findTopByUsuarioAndAccionOrderByFechaHoraDesc(any(), any()))
                .thenReturn(Optional.empty());

        registroAuditoriaService.registrarAccion(
                "admin", "INGRESO", "Pago registrado",
                "Juan Perez", "12345678",
                new BigDecimal("1000"), "EFECTIVO",
                1, "Manzana A", "Parcela 1", 1,
                "Proyecto Alpha", "Pago de Cuota"
        );

        verify(registroAuditoriaRepository).save(argThat(r ->
                "INGRESO".equals(r.getAccion()) &&
                "Pago de Cuota".equals(r.getItem()) &&
                "Proyecto Alpha".equals(r.getProyectoNombre())));
    }

    @Test
    void registrarAccion_itemTooLong_getsTruncated() {
        when(registroAuditoriaRepository.findTopByUsuarioAndAccionOrderByFechaHoraDesc(any(), any()))
                .thenReturn(Optional.empty());

        String longItem = "X".repeat(200);

        registroAuditoriaService.registrarAccion(
                "admin", "CREATE", "desc",
                null, null, null, null,
                null, null, null, null, null, longItem
        );

        verify(registroAuditoriaRepository).save(argThat(r ->
                r.getItem() != null && r.getItem().length() <= 150));
    }

    @Test
    void registrarAccion_emptyItem_returnsNull() {
        when(registroAuditoriaRepository.findTopByUsuarioAndAccionOrderByFechaHoraDesc(any(), any()))
                .thenReturn(Optional.empty());

        registroAuditoriaService.registrarAccion(
                "admin", "CREATE", "desc",
                null, null, null, null,
                null, null, null, null, null, ""
        );

        verify(registroAuditoriaRepository).save(argThat(r -> r.getItem() == null));
    }
}
