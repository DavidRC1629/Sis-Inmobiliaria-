package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.entity.RegistroAuditoria;
import com.sisarovi.inmobiliario.repository.RegistroAuditoriaRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registro-auditoria")
@RequiredArgsConstructor
public class RegistroAuditoriaController {
    private final RegistroAuditoriaRepository registroAuditoriaRepository;
    private final UserRepository userRepository;

    @GetMapping
    public List<RegistroAuditoriaResponse> getAllRegistros(
            @RequestParam(name = "fechaDesde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(name = "fechaHasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(name = "tipo", required = false) String tipo) {
        List<RegistroAuditoria> registros;

        if (fechaDesde != null && fechaHasta != null) {
            registros = registroAuditoriaRepository.findByFechaHoraBetween(
                    fechaDesde.atStartOfDay(),
                    fechaHasta.plusDays(1).atStartOfDay().minusSeconds(1));
        } else if (fechaDesde != null) {
            registros = registroAuditoriaRepository.findByFechaHoraGreaterThanEqualOrderByFechaHoraDesc(
                    fechaDesde.atStartOfDay());
        } else if (fechaHasta != null) {
            registros = registroAuditoriaRepository.findByFechaHoraLessThanEqualOrderByFechaHoraDesc(
                    fechaHasta.plusDays(1).atStartOfDay().minusSeconds(1));
        } else {
            registros = registroAuditoriaRepository.findAllByOrderByFechaHoraDesc();
        }

        // Filtrar por tipo si se proporciona
        if (tipo != null && !tipo.isEmpty()) {
            registros = registros.stream()
                    .filter(r -> tipo.equalsIgnoreCase(r.getAccion()))
                    .toList();
        }

        // Los inicios de sesión no se muestran en la vista de registro.
        registros = registros.stream()
            .filter(r -> !"LOGIN".equalsIgnoreCase(r.getAccion()))
            .toList();

        Map<String, String> cacheNombresPorDni = new HashMap<>();
        return registros.stream()
                .map(registro -> toResponse(registro, cacheNombresPorDni))
                .toList();
    }

    @PutMapping("/{id}/item")
    public RegistroAuditoriaResponse actualizarItem(@PathVariable Long id, @RequestBody ItemUpdateRequest request) {
        RegistroAuditoria registro = registroAuditoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado"));

        String item = request != null && request.item() != null ? request.item().trim() : "";
        if (item.length() > 150) {
            item = item.substring(0, 150);
        }
        registro.setItem(item.isBlank() ? null : item);
        RegistroAuditoria actualizado = registroAuditoriaRepository.save(registro);

        return toResponse(actualizado, new HashMap<>());
    }

    private RegistroAuditoriaResponse toResponse(RegistroAuditoria registro, Map<String, String> cacheNombresPorDni) {
        String usuarioRaw = registro.getUsuario() != null ? registro.getUsuario().trim() : "";
        String dni = extraerDni(usuarioRaw);
        String nombre = "";

        if (!dni.isEmpty()) {
            nombre = cacheNombresPorDni.computeIfAbsent(dni, this::buscarNombreCompletoPorDni);
        }

        if (nombre.isEmpty()) {
            if ("ANONIMO".equalsIgnoreCase(usuarioRaw)) {
                nombre = "ANONIMO";
            } else if (!usuarioRaw.isEmpty() && !usuarioRaw.equals(dni)) {
                nombre = usuarioRaw;
            }
        }

        return new RegistroAuditoriaResponse(
                registro.getId(),
                registro.getUsuario(),
                nombre,
                dni,
                registro.getAccion(),
                registro.getDescripcion(),
                registro.getFechaHora(),
                registro.getClienteNombre(),
                registro.getClienteDni(),
                registro.getMonto(),
            registro.getMedios(),
                registro.getItem(),
            registro.getLoteNumero(),
            registro.getManzanaNombre(),
            registro.getParcelaNombre(),
            registro.getEtapaNumero(),
            registro.getProyectoNombre()
        );
    }

    private String buscarNombreCompletoPorDni(String dni) {
        return userRepository.findByDni(dni)
                .map(user -> user.getNombres() != null ? user.getNombres().trim() : "")
                .orElse("");
    }

    private String extraerDni(String usuarioRaw) {
        if (usuarioRaw == null || usuarioRaw.isBlank()) {
            return "";
        }
        String trimmed = usuarioRaw.trim();
        if (trimmed.matches("\\d{8}")) {
            return trimmed;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(\\d{8})\\b").matcher(trimmed);
        return matcher.find() ? matcher.group(1) : "";
    }

    public record RegistroAuditoriaResponse(
            Long id,
            String usuario,
            String usuarioNombre,
            String usuarioDni,
            String accion,
            String descripcion,
            LocalDateTime fechaHora,
            String clienteNombre,
            String clienteDni,
            BigDecimal monto,
                String medios,
            String item,
                Integer loteNumero,
                String manzanaNombre,
                String parcelaNombre,
                Integer etapaNumero,
                String proyectoNombre
    ) {}

        public record ItemUpdateRequest(String item) {}

}
