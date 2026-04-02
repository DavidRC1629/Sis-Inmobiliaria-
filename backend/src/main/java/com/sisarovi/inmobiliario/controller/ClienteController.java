package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.ClienteLoteOptionResponse;
import com.sisarovi.inmobiliario.dto.ClienteProjectSummaryResponse;
import com.sisarovi.inmobiliario.dto.ClienteRequest;
import com.sisarovi.inmobiliario.dto.ClienteResponse;
import com.sisarovi.inmobiliario.dto.ClienteAdquisicionRequest;
import com.sisarovi.inmobiliario.service.ClienteService;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteService clienteService;
    private final RegistroAuditoriaService registroAuditoriaService;

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> getAllClientes(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(clienteService.getAllClientes(q));
    }

    @GetMapping("/{clienteId}")
    public ResponseEntity<ClienteResponse> getClienteById(@PathVariable Long clienteId) {
        return ResponseEntity.ok(clienteService.getClienteById(clienteId));
    }

    @GetMapping("/proyectos")
    public ResponseEntity<List<ClienteProjectSummaryResponse>> getProjectSummaries() {
        return ResponseEntity.ok(clienteService.getProjectSummaries());
    }

    @GetMapping("/proyectos/{projectId}")
    public ResponseEntity<List<ClienteResponse>> getClientesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(clienteService.getClientesByProject(projectId));
    }

    @GetMapping("/lotes-disponibles")
    public ResponseEntity<List<ClienteLoteOptionResponse>> getLotesDisponibles(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(clienteService.getLotesDisponibles(projectId, clienteId));
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> createCliente(
            @Valid @RequestBody ClienteRequest request,
            Authentication authentication) {
        ClienteResponse created = clienteService.createCliente(request);

        registroAuditoriaService.registrarAccion(
                authentication != null ? authentication.getName() : "ANONIMO",
                "CREATE",
                String.format(
                        "Se creó el cliente '%s %s' (DNI %s) para el lote %s del proyecto '%s' por %s.",
                        created.getNombres(),
                        created.getApellidos(),
                        created.getDni(),
                        formatLote(created),
                        created.getProjectNombre(),
                        normalizeTipo(created.getTipoRelacion())
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/adquisiciones")
    public ResponseEntity<List<ClienteResponse>> createClientesPorAdquisicion(
            @Valid @RequestBody ClienteAdquisicionRequest request,
            Authentication authentication) {
        List<ClienteResponse> creados = clienteService.createClientesPorAdquisicion(request);

        if (creados == null || creados.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(creados);
        }

        String usuario = authentication != null ? authentication.getName() : "ANONIMO";
        String tipo = normalizeTipoOperacion(request.getTipoOperacion());

        ClienteResponse principal = creados.get(0);
        String propietarios = creados.stream()
            .map(c -> (normalizeEmpty(c.getNombres()) + " " + normalizeEmpty(c.getApellidos())).trim())
            .filter(s -> !s.isBlank())
            .distinct()
            .reduce((a, b) -> a + ", " + b)
            .orElse("Sin nombre");
        String dnis = creados.stream()
            .map(ClienteResponse::getDni)
            .map(this::normalizeEmpty)
            .filter(s -> !s.isBlank())
            .distinct()
            .reduce((a, b) -> a + ", " + b)
            .orElse("-");

        registroAuditoriaService.registrarAccion(
            usuario,
            "ADQUISICION_LOTE",
            String.format(
                "Se registró %s del lote %s del proyecto '%s' para copropietarios: %s (DNI: %s).",
                tipo,
                formatLote(principal),
                principal.getProjectNombre(),
                propietarios,
                dnis
            )
        );

        registrarIngresoAdquisicion(usuario, request, creados);

        return ResponseEntity.status(HttpStatus.CREATED).body(creados);
    }

    @PutMapping("/{clienteId}")
    public ResponseEntity<ClienteResponse> updateCliente(
            @PathVariable Long clienteId,
            @Valid @RequestBody ClienteRequest request,
            Authentication authentication) {
        ClienteResponse before = clienteService.getClienteById(clienteId);
        ClienteResponse updated = clienteService.updateCliente(clienteId, request);

        String descripcionCambios = construirDescripcionCambios(before, updated);
        registroAuditoriaService.registrarAccion(
                authentication != null ? authentication.getName() : "ANONIMO",
                "UPDATE",
                descripcionCambios
        );

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{clienteId}")
    public ResponseEntity<Void> deleteCliente(
            @PathVariable Long clienteId,
            Authentication authentication) {
        ClienteResponse current = clienteService.getClienteById(clienteId);
        clienteService.deleteCliente(clienteId);

        registroAuditoriaService.registrarAccion(
                authentication != null ? authentication.getName() : "ANONIMO",
                "DELETE",
                String.format(
                        "Se eliminó el cliente '%s %s' (DNI %s) del lote %s del proyecto '%s'.",
                        current.getNombres(),
                        current.getApellidos(),
                        current.getDni(),
                        formatLote(current),
                        current.getProjectNombre()
                )
        );

        return ResponseEntity.noContent().build();
    }

    private String construirDescripcionCambios(ClienteResponse before, ClienteResponse after) {
        List<String> cambios = new ArrayList<>();

        if (!Objects.equals(before.getNombres(), after.getNombres())) {
            cambios.add(String.format("se cambió nombres de '%s' a '%s'", before.getNombres(), after.getNombres()));
        }
        if (!Objects.equals(before.getApellidos(), after.getApellidos())) {
            cambios.add(String.format("se cambió apellidos de '%s' a '%s'", before.getApellidos(), after.getApellidos()));
        }
        if (!Objects.equals(before.getDni(), after.getDni())) {
            cambios.add(String.format("se cambió DNI de '%s' a '%s'", before.getDni(), after.getDni()));
        }
        if (!Objects.equals(before.getTelefono(), after.getTelefono())) {
            cambios.add(String.format("se cambió teléfono de '%s' a '%s'", before.getTelefono(), after.getTelefono()));
        }
        if (!Objects.equals(before.getDireccion(), after.getDireccion())) {
            cambios.add("se actualizó dirección");
        }
        if (!Objects.equals(normalizeEmpty(before.getEmail()), normalizeEmpty(after.getEmail()))) {
            cambios.add("se actualizó correo");
        }
        if (!Objects.equals(before.getTipoRelacion(), after.getTipoRelacion())) {
            cambios.add(String.format("se cambió tipo de relación a %s", normalizeTipo(after.getTipoRelacion())));
        }
        if (!Objects.equals(before.getClienteDesde(), after.getClienteDesde())) {
            cambios.add(String.format("se cambió cliente desde a %s", after.getClienteDesde()));
        }
        if (!Objects.equals(before.getLoteId(), after.getLoteId())) {
            cambios.add(String.format("se cambió lote de %s a %s", formatLote(before), formatLote(after)));
        }

        if (cambios.isEmpty()) {
            return String.format("Se actualizó el cliente '%s %s' sin cambios visibles.", after.getNombres(), after.getApellidos());
        }

        return String.format(
                "Se actualizó el cliente '%s %s' del proyecto '%s': %s.",
                after.getNombres(),
                after.getApellidos(),
                after.getProjectNombre(),
                String.join(", ", cambios)
        );
    }

    private String normalizeTipo(String tipoRelacion) {
        return "SEPARACION".equalsIgnoreCase(tipoRelacion) ? "separación" : "adquisición";
    }

    private String normalizeTipoOperacion(String tipoOperacion) {
        String tipo = tipoOperacion == null ? "" : tipoOperacion.trim().toUpperCase();
        return switch (tipo) {
            case "CONTADO" -> "adquisición al contado";
            case "CREDITO" -> "adquisición al crédito";
            case "SEPARACION" -> "separación";
            default -> "adquisición";
        };
    }

    private String formatLote(ClienteResponse cliente) {
        return String.format(
                "Etapa %s / Parcela %s / Manzana %s / Lote %s",
                cliente.getEtapaNumero(),
                cliente.getParcelaNombre(),
                cliente.getManzana(),
                cliente.getLoteNumero()
        );
    }

    private void registrarIngresoAdquisicion(String usuario, ClienteAdquisicionRequest request, List<ClienteResponse> creados) {
        BigDecimal monto = request.getMontoOperacion();
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0 || creados == null || creados.isEmpty()) {
            return;
        }

        ClienteResponse principal = creados.get(0);
        String clienteNombre = creados.stream()
            .map(c -> (normalizeEmpty(c.getNombres()) + " " + normalizeEmpty(c.getApellidos())).trim())
            .filter(s -> !s.isBlank())
            .distinct()
            .reduce((a, b) -> a + ", " + b)
            .orElse((principal.getNombres() + " " + principal.getApellidos()).trim());
        String clienteDni = creados.stream()
            .map(ClienteResponse::getDni)
            .map(this::normalizeEmpty)
            .filter(s -> !s.isBlank())
            .distinct()
            .reduce((a, b) -> a + ", " + b)
            .orElse(principal.getDni());
        String medios = normalizeEmpty(request.getMedios()).trim();
        boolean esSeparacion = "SEPARACION".equalsIgnoreCase(request.getTipoOperacion());
        String item = esSeparacion ? "Pago de Separación" : "Pago Inicial de Adquisición";
        String descripcion = String.format(
                "%s registrado por %s (DNI: %s) para el lote %s del proyecto '%s'.",
                item,
                clienteNombre,
                clienteDni,
                formatLote(principal),
                principal.getProjectNombre()
        );
            if (!medios.isBlank()) {
                descripcion += " - Medios: " + medios;
            }

        registroAuditoriaService.registrarAccion(
                usuario,
                "INGRESO",
                descripcion,
                clienteNombre,
                clienteDni,
                monto,
                medios,
                principal.getLoteNumero(),
                principal.getManzana(),
                principal.getParcelaNombre(),
                principal.getEtapaNumero(),
                principal.getProjectNombre(),
                item
        );
    }

    private String normalizeEmpty(String value) {
        return value == null ? "" : value;
    }
}
