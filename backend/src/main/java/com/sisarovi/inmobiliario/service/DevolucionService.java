package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.DevolucionPagoRequest;
import com.sisarovi.inmobiliario.dto.DevolucionRequest;
import com.sisarovi.inmobiliario.dto.DevolucionResponse;
import com.sisarovi.inmobiliario.entity.Devolucion;
import com.sisarovi.inmobiliario.entity.DevolucionPago;
import com.sisarovi.inmobiliario.repository.DevolucionPagoRepository;
import com.sisarovi.inmobiliario.repository.DevolucionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DevolucionService {
    private final DevolucionRepository devolucionRepository;
    private final DevolucionPagoRepository devolucionPagoRepository;
    private final RegistroAuditoriaService registroAuditoriaService;

    @Transactional(readOnly = true)
    public List<DevolucionResponse> listar(String estado) {
        List<Devolucion> devoluciones = estado == null || estado.isBlank()
                ? devolucionRepository.findAllByOrderByFechaCreacionDesc()
                : devolucionRepository.findByEstadoIgnoreCaseOrderByFechaCreacionDesc(estado);
        return devoluciones.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DevolucionResponse obtener(Long id) {
        Devolucion devolucion = devolucionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Devolución no encontrada"));
        return toResponse(devolucion);
    }

    @Transactional
    public DevolucionResponse crear(DevolucionRequest request, String usuarioSesion) {
        Devolucion devolucion = Devolucion.builder()
                .loteId(request.getLoteId())
                .loteNumero(request.getLoteNumero())
                .manzana(normalizar(request.getManzana()))
                .parcelaNombre(normalizar(request.getParcelaNombre()))
                .etapaNumero(request.getEtapaNumero())
                .proyectoNombre(normalizar(request.getProyectoNombre()))
                .montoTotal(safeMoney(request.getMontoTotal()))
                .montoPagado(BigDecimal.ZERO)
                .dias(request.getDias())
                .fechaInicio(request.getFechaInicio())
                .fechaFinEstimada(request.getFechaFinEstimada())
                .descripcion(normalizar(request.getDescripcion()))
                .estado("EN_CURSO")
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        Devolucion guardada = devolucionRepository.save(devolucion);
        registroAuditoriaService.registrarAccion(
                usuarioSesion,
                "DEVOLUCION_REGISTRADA",
                construirDescripcionRegistro(guardada),
                "",
                "",
                guardada.getMontoTotal(),
                "",
                guardada.getLoteNumero(),
                guardada.getManzana(),
                guardada.getParcelaNombre(),
                guardada.getEtapaNumero(),
                guardada.getProyectoNombre(),
                guardada.getDescripcion()
        );
        return toResponse(guardada);
    }

    @Transactional
    public DevolucionResponse registrarPago(Long devolucionId, DevolucionPagoRequest request, String usuarioSesion) {
        Devolucion devolucion = devolucionRepository.findById(devolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Devolución no encontrada"));

        DevolucionPago pago = DevolucionPago.builder()
                .devolucion(devolucion)
                .monto(safeMoney(request.getMonto()))
                .fechaPago(request.getFechaPago())
                .descripcion(normalizar(request.getDescripcion()))
                .medioPago(normalizar(request.getMedioPago()))
                .build();
        devolucion.getPagos().add(pago);
        devolucion.setMontoPagado(safeMoney(devolucion.getMontoPagado()).add(safeMoney(pago.getMonto())));
        if (devolucion.getMontoPagado().compareTo(devolucion.getMontoTotal()) >= 0) {
            devolucion.setEstado("COMPLETADA");
            devolucion.setMontoPagado(devolucion.getMontoTotal());
        } else {
            devolucion.setEstado("EN_CURSO");
        }
        devolucion.setFechaActualizacion(LocalDateTime.now());

        devolucionPagoRepository.save(pago);
        Devolucion actualizada = devolucionRepository.save(devolucion);

        registroAuditoriaService.registrarAccion(
                usuarioSesion,
                "DEVOLUCION_PAGO",
                construirDescripcionPago(actualizada, pago),
                "",
                "",
                pago.getMonto(),
                pago.getMedioPago(),
                actualizada.getLoteNumero(),
                actualizada.getManzana(),
                actualizada.getParcelaNombre(),
                actualizada.getEtapaNumero(),
                actualizada.getProyectoNombre(),
                pago.getDescripcion()
        );

        return toResponse(actualizada);
    }

    private DevolucionResponse toResponse(Devolucion devolucion) {
        List<DevolucionResponse.DevolucionPagoItemResponse> pagos = devolucionPagoRepository
                .findByDevolucionIdOrderByFechaRegistroDesc(devolucion.getId())
                .stream()
                .map(pago -> DevolucionResponse.DevolucionPagoItemResponse.builder()
                        .id(pago.getId())
                        .monto(safeMoney(pago.getMonto()))
                        .fechaPago(pago.getFechaPago())
                        .descripcion(pago.getDescripcion())
                        .medioPago(pago.getMedioPago())
                        .fechaRegistro(pago.getFechaRegistro())
                        .build())
                .toList();

        BigDecimal montoTotal = safeMoney(devolucion.getMontoTotal());
        BigDecimal montoPagado = safeMoney(devolucion.getMontoPagado());
        BigDecimal pendiente = montoTotal.subtract(montoPagado);
        if (pendiente.compareTo(BigDecimal.ZERO) < 0) {
            pendiente = BigDecimal.ZERO;
        }
        int progreso = montoTotal.compareTo(BigDecimal.ZERO) == 0
                ? 0
                : montoPagado.multiply(BigDecimal.valueOf(100)).divide(montoTotal, 0, RoundingMode.HALF_UP).intValue();

        return DevolucionResponse.builder()
                .id(devolucion.getId())
                .loteId(devolucion.getLoteId())
                .loteNumero(devolucion.getLoteNumero())
                .manzana(devolucion.getManzana())
                .parcelaNombre(devolucion.getParcelaNombre())
                .etapaNumero(devolucion.getEtapaNumero())
                .proyectoNombre(devolucion.getProyectoNombre())
                .montoTotal(montoTotal)
                .montoPagado(montoPagado)
                .montoPendiente(pendiente)
                .dias(devolucion.getDias())
                .fechaInicio(devolucion.getFechaInicio())
                .fechaFinEstimada(devolucion.getFechaFinEstimada())
                .descripcion(devolucion.getDescripcion())
                .estado(devolucion.getEstado())
                .progreso(progreso)
                .fechaCreacion(devolucion.getFechaCreacion())
                .fechaActualizacion(devolucion.getFechaActualizacion())
                .pagos(pagos)
                .build();
    }

    private String construirDescripcionRegistro(Devolucion devolucion) {
        return String.format(
                "Devolución registrada para el lote %s del proyecto %s. Monto: S/ %s. Plazo: %s días desde %s hasta %s. Descripción: %s",
                devolucion.getLoteNumero(),
                devolucion.getProyectoNombre(),
                safeMoney(devolucion.getMontoTotal()).setScale(2, RoundingMode.HALF_UP),
                devolucion.getDias(),
                devolucion.getFechaInicio(),
                devolucion.getFechaFinEstimada(),
                devolucion.getDescripcion()
        );
    }

    private String construirDescripcionPago(Devolucion devolucion, DevolucionPago pago) {
        return String.format(
                "Pago de devolución registrado para el lote %s del proyecto %s. Monto: S/ %s. Medio: %s. Estado: %s.",
                devolucion.getLoteNumero(),
                devolucion.getProyectoNombre(),
                safeMoney(pago.getMonto()).setScale(2, RoundingMode.HALF_UP),
                pago.getMedioPago(),
                devolucion.getEstado()
        );
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizar(String value) {
        return value == null ? "" : value.trim();
    }
}