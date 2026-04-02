package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.entity.RegistroAuditoria;
import com.sisarovi.inmobiliario.repository.RegistroAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RegistroAuditoriaService {
    private final RegistroAuditoriaRepository registroAuditoriaRepository;

    public void registrarAccion(String usuario, String accion, String descripcion) {
        String usuarioNormalizado = normalizarValor(usuario, "ANONIMO");
        String accionNormalizada = normalizarValor(accion, "OTHER").toUpperCase();

        if ("LOGIN".equals(accionNormalizada)) {
            boolean loginDuplicadoReciente = registroAuditoriaRepository
                    .findTopByUsuarioAndAccionOrderByFechaHoraDesc(usuarioNormalizado, accionNormalizada)
                    .map(ultimo -> ChronoUnit.SECONDS.between(ultimo.getFechaHora(), LocalDateTime.now()) < 15)
                    .orElse(false);

            if (loginDuplicadoReciente) {
                return;
            }
        }

        RegistroAuditoria registro = RegistroAuditoria.builder()
                .usuario(usuarioNormalizado)
                .accion(accionNormalizada)
                .descripcion(normalizarValor(descripcion, "Sin descripción"))
                .fechaHora(LocalDateTime.now())
                .build();
        registroAuditoriaRepository.save(registro);
    }

    public void registrarAccion(String usuario, String accion, String descripcion, String clienteNombre, String clienteDni, BigDecimal monto, String medios) {
        registrarAccion(usuario, accion, descripcion, clienteNombre, clienteDni, monto, medios, null, null, null, null, null, null);
    }

    public void registrarAccion(String usuario, String accion, String descripcion, String clienteNombre, String clienteDni,
                                BigDecimal monto, String medios, Integer loteNumero, String manzanaNombre,
                                String parcelaNombre, Integer etapaNumero, String proyectoNombre, String item) {
        String usuarioNormalizado = normalizarValor(usuario, "ANONIMO");
        String accionNormalizada = normalizarValor(accion, "OTHER").toUpperCase();

        if ("LOGIN".equals(accionNormalizada)) {
            boolean loginDuplicadoReciente = registroAuditoriaRepository
                    .findTopByUsuarioAndAccionOrderByFechaHoraDesc(usuarioNormalizado, accionNormalizada)
                    .map(ultimo -> ChronoUnit.SECONDS.between(ultimo.getFechaHora(), LocalDateTime.now()) < 15)
                    .orElse(false);

            if (loginDuplicadoReciente) {
                return;
            }
        }

        RegistroAuditoria registro = RegistroAuditoria.builder()
                .usuario(usuarioNormalizado)
                .accion(accionNormalizada)
                .descripcion(normalizarValor(descripcion, "Sin descripción"))
                .fechaHora(LocalDateTime.now())
                .clienteNombre(clienteNombre)
                .clienteDni(clienteDni)
                .monto(monto)
                .medios(medios)
                .item(normalizarItem(item))
            .loteNumero(loteNumero)
            .manzanaNombre(normalizarValor(manzanaNombre, ""))
            .parcelaNombre(normalizarValor(parcelaNombre, ""))
            .etapaNumero(etapaNumero)
            .proyectoNombre(normalizarValor(proyectoNombre, ""))
                .build();
        registroAuditoriaRepository.save(registro);
    }

    private String normalizarItem(String item) {
        String normalized = normalizarValor(item, "");
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > 150 ? normalized.substring(0, 150) : normalized;
    }

    private String normalizarValor(String valor, String valorDefecto) {
        if (valor == null || valor.trim().isEmpty()) {
            return valorDefecto;
        }
        return valor.trim();
    }
}
