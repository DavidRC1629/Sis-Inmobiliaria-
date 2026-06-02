package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.LiberacionLoteResponse;
import com.sisarovi.inmobiliario.entity.Cliente;
import com.sisarovi.inmobiliario.entity.CronogramaContrato;
import com.sisarovi.inmobiliario.entity.CronogramaCuota;
import com.sisarovi.inmobiliario.entity.Lote;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.CronogramaContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiberacionService {

    private final ClienteRepository clienteRepository;
    private final CronogramaContratoRepository cronogramaContratoRepository;
    private final AdminService adminService;
    private final RegistroAuditoriaService registroAuditoriaService;

    @Transactional(readOnly = true)
    public List<LiberacionLoteResponse> listarLotesAdquiridosPorProyecto(Long projectId) {
        List<Cliente> clientes = clienteRepository.findByProjectIdOrdered(projectId);
        if (clientes.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Cliente>> titularesPorLote = clientes.stream()
                .filter(c -> c.getLote() != null && c.getLote().getId() != null)
                .collect(Collectors.groupingBy(c -> c.getLote().getId(), LinkedHashMap::new, Collectors.toList()));

        if (titularesPorLote.isEmpty()) {
            log.warn("No se encontraron lotes válidos para projectId={}, clientes encontrados={}", projectId, clientes.size());
            return List.of();
        }

        List<Long> loteIds = new ArrayList<>(titularesPorLote.keySet());
        List<CronogramaContrato> contratos = cronogramaContratoRepository.findDetailedByLoteIds(loteIds);
        Map<Long, CronogramaContrato> contratoActivoPorLote = contratos.stream()
                .filter(c -> c.getCliente() != null
                        && c.getCliente().getLote() != null
                        && c.getCliente().getLote().getId() != null)
                .collect(Collectors.toMap(
                        c -> c.getCliente().getLote().getId(),
                        c -> c,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return titularesPorLote.values().stream()
                .filter(titulares -> titulares != null && !titulares.isEmpty())
                .filter(titulares -> titulares.get(0) != null && titulares.get(0).getLote() != null)
                .map(titulares -> toResponse(titulares, contratoActivoPorLote.get(titulares.get(0).getLote().getId())))
                .sorted(Comparator
                        .comparing((LiberacionLoteResponse r) -> normalize(r.getProjectNombre()))
                        .thenComparing(r -> safeLong(r.getEtapaNumero()))
                        .thenComparing((LiberacionLoteResponse r) -> normalize(r.getParcelaNombre()))
                        .thenComparing((LiberacionLoteResponse r) -> normalize(r.getManzana()))
                        .thenComparing(LiberacionLoteResponse::getLoteNumero, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void liberarLote(Long loteId, String descripcionDevolucion, String adminPassword, String usuarioSesion) {
        List<Cliente> titulares = clienteRepository.findAllByLoteId(loteId);
        if (titulares == null || titulares.isEmpty()) {
            throw new RuntimeException("El lote ya no tiene titulares asociados.");
        }

        List<Long> clienteIds = titulares.stream()
                .map(Cliente::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<CronogramaContrato> contratos = cronogramaContratoRepository.findByClienteIdIn(clienteIds);

        BigDecimal totalPagado = contratos.stream()
                .map(CronogramaContrato::getMontoPagadoTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPagado.compareTo(BigDecimal.ZERO) > 0) {
            adminService.validarPasswordAdmin(usuarioSesion, adminPassword);
        }

        cronogramaContratoRepository.deleteByClienteIdIn(clienteIds);
        clienteRepository.deleteAll(titulares);

        Cliente principal = titulares.get(0);
        Lote lote = principal.getLote();
        String titularesNombre = titulares.stream()
                .map(c -> (normalize(c.getNombres()) + " " + normalize(c.getApellidos())).trim())
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
        String titularesDni = titulares.stream()
                .map(Cliente::getDni)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        String descripcion = "Liberación de Lote realizada. "
                + "Lote " + safeInt(lote != null ? lote.getNumero() : null)
                + " - Titulares liberados: " + (titularesNombre.isBlank() ? "Sin nombre" : titularesNombre)
                + " (DNI: " + (titularesDni.isBlank() ? "-" : titularesDni) + ")"
                + ". Motivo/devolución: " + normalize(descripcionDevolucion)
                + ". Total pagado histórico: S/ " + totalPagado.setScale(2, java.math.RoundingMode.HALF_UP);

        registroAuditoriaService.registrarAccion(
                usuarioSesion,
                "LIBERACION_LOTE",
                descripcion,
                titularesNombre,
                titularesDni,
                totalPagado,
                "",
                lote != null ? lote.getNumero() : null,
                lote != null && lote.getManzana() != null ? normalize(lote.getManzana().getNombre()) : "",
                lote != null && lote.getParcela() != null ? normalize(lote.getParcela().getNombre()) : "",
                lote != null && lote.getParcela() != null && lote.getParcela().getEtapa() != null
                        ? lote.getParcela().getEtapa().getNumeroEtapa()
                        : null,
                lote != null && lote.getParcela() != null && lote.getParcela().getEtapa() != null
                        && lote.getParcela().getEtapa().getProject() != null
                        ? normalize(lote.getParcela().getEtapa().getProject().getNombre())
                        : "",
                normalize(descripcionDevolucion)
        );
    }

    private LiberacionLoteResponse toResponse(List<Cliente> titulares, CronogramaContrato contrato) {
        Cliente principal = titulares.get(0);
        Lote lote = principal.getLote();

        String titularesNombre = titulares.stream()
                .map(c -> (normalize(c.getNombres()) + " " + normalize(c.getApellidos())).trim())
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
        String titularesDni = titulares.stream()
                .map(Cliente::getDni)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        BigDecimal montoPagado = contrato != null && contrato.getMontoPagadoTotal() != null
                ? contrato.getMontoPagadoTotal()
                : BigDecimal.ZERO;
        boolean moroso = contrato != null && isMoroso(contrato);

        return LiberacionLoteResponse.builder()
                .loteId(lote.getId())
                .loteNumero(lote.getNumero())
                .manzana(lote.getManzana() != null ? normalize(lote.getManzana().getNombre()) : "")
                .parcelaNombre(lote.getParcela() != null ? normalize(lote.getParcela().getNombre()) : "")
                .parcelaId(lote.getParcela() != null ? lote.getParcela().getId() : null)
                .etapaNumero(lote.getParcela() != null && lote.getParcela().getEtapa() != null
                        ? lote.getParcela().getEtapa().getNumeroEtapa()
                        : null)
                .etapaId(lote.getParcela() != null && lote.getParcela().getEtapa() != null
                        ? lote.getParcela().getEtapa().getId()
                        : null)
                .projectId(lote.getParcela() != null && lote.getParcela().getEtapa() != null
                        && lote.getParcela().getEtapa().getProject() != null
                        ? lote.getParcela().getEtapa().getProject().getId()
                        : null)
                .projectNombre(lote.getParcela() != null && lote.getParcela().getEtapa() != null
                        && lote.getParcela().getEtapa().getProject() != null
                        ? normalize(lote.getParcela().getEtapa().getProject().getNombre())
                        : "")
                .titulares(titularesNombre)
                .titularesDni(titularesDni)
                .cantidadTitulares(titulares.size())
                .contratoId(contrato != null ? contrato.getId() : null)
                .tipoOperacion(contrato != null ? normalize(contrato.getTipoOperacion()) : "")
                .estadoCronograma(contrato != null ? normalize(contrato.getEstado()) : "")
                .estadoVisual(resolveEstadoVisual(contrato))
                .moroso(moroso)
                .montoPagadoTotal(montoPagado)
                .requierePasswordAdmin(montoPagado.compareTo(BigDecimal.ZERO) > 0)
                .build();
    }

    private String resolveEstadoVisual(CronogramaContrato contrato) {
        if (contrato == null) {
            return "Sin cronograma";
        }

        BigDecimal sepAcum = contrato.getMontoSeparacionAcumulado() == null
                ? BigDecimal.ZERO
                : contrato.getMontoSeparacionAcumulado();
        BigDecimal sepObj = contrato.getMontoSeparacionObjetivo() == null
                ? BigDecimal.ZERO
                : contrato.getMontoSeparacionObjetivo();
        if ("SEPARACION_EN_CURSO".equalsIgnoreCase(normalize(contrato.getEstado())) && sepAcum.compareTo(sepObj) < 0) {
            return "Separación en curso";
        }

        List<CronogramaCuota> cuotas = contrato.getCuotas() == null ? List.of() : contrato.getCuotas();

        BigDecimal saldoCuotas = cuotas.stream()
                .map(c -> safeMoney(c.getMontoCuota()).subtract(safeMoney(c.getMontoPagado())))
                .map(this::maxZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!cuotas.isEmpty() && saldoCuotas.compareTo(BigDecimal.ZERO) > 0) {
            return "Pago de cuotas en curso";
        }

        if ("AL_DIA".equalsIgnoreCase(normalize(contrato.getEstado()))) {
            return "Al día";
        }

        return "DEUDOR".equalsIgnoreCase(normalize(contrato.getEstado())) ? "Pago de cuotas en curso" : normalize(contrato.getEstado());
    }

    private boolean isMoroso(CronogramaContrato contrato) {
        if ("DEUDOR".equalsIgnoreCase(normalize(contrato.getEstado()))) {
            return true;
        }

        LocalDate hoy = LocalDate.now();
        return (contrato.getCuotas() == null ? List.<CronogramaCuota>of() : contrato.getCuotas()).stream()
                .anyMatch(c -> maxZero(safeMoney(c.getMontoCuota()).subtract(safeMoney(c.getMontoPagado()))).compareTo(BigDecimal.ZERO) > 0
                        && c.getFechaVencimiento() != null
                        && hoy.isAfter(c.getFechaVencimiento()));
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer safeInt(Integer value) {
        return value == null ? 0 : value;
    }

        private Long safeLong(Integer value) {
                return value == null ? Long.MAX_VALUE : value.longValue();
        }
}