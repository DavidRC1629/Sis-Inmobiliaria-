// ...método se moverá dentro de la clase más abajo...
package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.*;
import com.sisarovi.inmobiliario.entity.*;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.CronogramaContratoRepository;
import com.sisarovi.inmobiliario.repository.CronogramaCuotaRepository;
import com.sisarovi.inmobiliario.repository.CronogramaPagoSeparacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CronogramaService {
        /**
         * Crea un cronograma para un Terreno Propio (fuera de proyectos regulares).
         * @param terreno TerrenoPropio adquirido
         * @param clienteId ID del propietario
         * @param formaPago "CONTADO" o "CREDITO"
         * @param cuotas número de cuotas (si es crédito)
         * @param interes interés anual (si es crédito)
         */
        @Transactional
        public void crearCronogramaParaTerrenoPropio(TerrenoPropio terreno, Long clienteId, String formaPago, int cuotas, double interes) {
            Cliente cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new RuntimeException("Propietario no encontrado para cronograma de terreno propio"));
            // Simular una solicitud de adquisición
            ClienteAdquisicionRequest req = new ClienteAdquisicionRequest();
            req.setPrecioVenta(terreno.getPrecio());
            req.setMontoOperacion(terreno.getPrecio());
            req.setAsesor("");
            req.setPlazoMeses("CREDITO".equalsIgnoreCase(formaPago) ? cuotas : 1);
            req.setInteresPorcentaje("CREDITO".equalsIgnoreCase(formaPago) ? new java.math.BigDecimal(interes) : java.math.BigDecimal.ZERO);
            req.setTipoOperacion(formaPago);
            req.setFechaOperacion(java.time.LocalDate.now());
            this.crearDesdeAdquisicion(cliente, req);
        }
    @Transactional
    public CronogramaContratoResponse aplicarDescuento(CronogramaDescuentoRequest request) {
        Long clienteId = request.getClienteId();
        BigDecimal descuento = normalizeMoney(request.getMontoDescuento());
        if (descuento.compareTo(CERO) <= 0) {
            throw new RuntimeException("El descuento debe ser mayor a cero");
        }

        // Buscar todos los cronogramas activos del cliente
        List<CronogramaContrato> contratos = contratoRepository.findAll().stream()
                .filter(c -> c.getCliente() != null && c.getCliente().getId().equals(clienteId))
                .filter(c -> c.getEstado() != null && !c.getEstado().equalsIgnoreCase("SEPARACION_EN_CURSO"))
                .collect(Collectors.toList());
        if (contratos.isEmpty()) {
            throw new RuntimeException("No se encontraron cronogramas activos para el cliente");
        }

        CronogramaContratoResponse lastResponse = null;
        for (CronogramaContrato contrato : contratos) {
            // Solo cuotas pendientes (no pagadas)
            List<CronogramaCuota> cuotasPendientes = contrato.getCuotas().stream()
                .filter(q -> !"PAGADA".equalsIgnoreCase(q.getEstadoPago()))
                .sorted(Comparator.comparing(CronogramaCuota::getNumeroCuota))
                .collect(Collectors.toList());

            BigDecimal descuentoRestante = descuento;
            for (CronogramaCuota cuota : cuotasPendientes) {
                BigDecimal saldoCuota = normalizeMoney(cuota.getMontoCuota().subtract(cuota.getMontoPagado()));
                if (saldoCuota.compareTo(CERO) <= 0) continue;
                BigDecimal aplicar = descuentoRestante.min(saldoCuota);
                if (aplicar.compareTo(CERO) <= 0) break;

                // Marcar como pagada o parcial
                BigDecimal nuevoPagado = normalizeMoney(cuota.getMontoPagado().add(aplicar));
                cuota.setMontoPagado(nuevoPagado);
                if (nuevoPagado.compareTo(cuota.getMontoCuota()) >= 0) {
                    cuota.setEstadoPago("PAGADA");
                    cuota.setFechaPago(LocalDate.now());
                } else {
                    cuota.setEstadoPago("PARCIAL");
                    cuota.setFechaPago(LocalDate.now());
                }
                if (cuota.getPagos() == null) cuota.setPagos(new ArrayList<>());
                CronogramaPagoCuota pagoDetalle = CronogramaPagoCuota.builder()
                        .cuota(cuota)
                        .fechaPago(LocalDate.now())
                        .monto(aplicar)
                        .tipoPago("DESCUENTO")
                        .estadoPago(cuota.getEstadoPago())
                        .notas("Descuento aplicado: " + (request.getObservacion() != null ? request.getObservacion() : ""))
                        .build();
                cuota.getPagos().add(pagoDetalle);
                cuotaRepository.save(cuota);
                descuentoRestante = descuentoRestante.subtract(aplicar);
                if (descuentoRestante.compareTo(CERO) <= 0) break;
            }
            // Actualizar monto pagado total del contrato
            BigDecimal pagadoEnDescuento = descuento.subtract(descuentoRestante);
            contrato.setMontoPagadoTotal(normalizeMoney(contrato.getMontoPagadoTotal().add(pagadoEnDescuento)));
            contrato.setMontoSeparacionAcumulado(contrato.getMontoPagadoTotal().min(contrato.getMontoSeparacionObjetivo()));
            contrato.setEstado(calcularEstadoContrato(contrato));
            contratoRepository.save(contrato);

            // Registrar en auditoría
            try {
                String usuarioActual = obtenerUsuarioActualSeguro();
                TitularesInfo titulares = obtenerTitulares(contrato);
                String clienteNombre = titulares.nombres;
                String clienteDni = titulares.dnis;
                UbicacionLoteInfo ubicacion = extraerUbicacionLote(contrato);
                String descripcion = "Se aplicó descuento para cronograma de " + clienteNombre + " (DNI: " + clienteDni + ") - Lote: " +
                        "L" + (ubicacion.loteNumero != null ? ubicacion.loteNumero : "") +
                        (ubicacion.manzanaNombre != null && !ubicacion.manzanaNombre.isBlank() ? ", Manzana: " + ubicacion.manzanaNombre : "") +
                        (ubicacion.parcelaNombre != null && !ubicacion.parcelaNombre.isBlank() ? ", Parcela: " + ubicacion.parcelaNombre : "") +
                        (ubicacion.etapaNumero != null ? ", Etapa: " + ubicacion.etapaNumero : "") +
                        (ubicacion.proyectoNombre != null && !ubicacion.proyectoNombre.isBlank() ? ", Proyecto: " + ubicacion.proyectoNombre : "") +
                        ". Monto descontado: S/ " + pagadoEnDescuento.setScale(2, java.math.RoundingMode.HALF_UP);
                registroAuditoriaService.registrarAccion(
                        usuarioActual,
                        "DESCUENTO",
                        descripcion,
                        clienteNombre,
                        clienteDni,
                        pagadoEnDescuento,
                        "DESCUENTO",
                        ubicacion.loteNumero,
                        ubicacion.manzanaNombre,
                        ubicacion.parcelaNombre,
                        ubicacion.etapaNumero,
                        ubicacion.proyectoNombre,
                        request.getObservacion()
                );
            } catch (Exception e) {
                System.err.println("Error registrando DESCUENTO en auditoría: " + e.getMessage());
            }
            // Al final de cada contrato, construir el response actualizado
            lastResponse = obtenerPorId(contrato.getId());
        }
        return lastResponse;
    }

    private static final BigDecimal DEFAULT_SEPARACION_OBJETIVO = new BigDecimal("2000");
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(0, RoundingMode.HALF_UP);

    private final CronogramaContratoRepository contratoRepository;
    private final CronogramaCuotaRepository cuotaRepository;
    private final CronogramaPagoSeparacionRepository pagoSeparacionRepository;
    private final ClienteRepository clienteRepository;
    private final RegistroAuditoriaService registroAuditoriaService;

    @Transactional
    public void crearDesdeAdquisicion(Cliente cliente, ClienteAdquisicionRequest request) {
        Objects.requireNonNull(cliente, "Cliente requerido");

        BigDecimal precioVenta = normalizeMoney(request.getPrecioVenta());
        BigDecimal montoOperacion = normalizeMoney(request.getMontoOperacion());
        String asesor = normalizeString(request.getAsesor());
        BigDecimal montoSeparacionObjetivo = DEFAULT_SEPARACION_OBJETIVO;
        Integer plazoMeses = normalizePlazoMeses(request.getPlazoMeses());
        BigDecimal interesPorcentaje = normalizePercent(request.getInteresPorcentaje(), plazoMeses);

        if (precioVenta.compareTo(CERO) <= 0) {
            throw new RuntimeException("El precio de venta debe ser mayor a 0");
        }
        if (montoOperacion.compareTo(new BigDecimal("100")) <= 0) {
            throw new RuntimeException("El monto de operación debe ser mayor que 100");
        }
        String tipoOperacion = normalizeTipoOperacion(request.getTipoOperacion());

        if ("CONTADO".equals(tipoOperacion) && precioVenta.compareTo(montoOperacion) != 0) {
            throw new RuntimeException("En Pago al Contado, el monto de operación debe ser exactamente igual al precio de venta.");
        }

        BigDecimal montoPagadoTotal = montoOperacion.min(precioVenta);
        BigDecimal montoSeparacionAcumulado = montoPagadoTotal.min(montoSeparacionObjetivo);

        CronogramaContrato contrato = CronogramaContrato.builder()
                .cliente(cliente)
                .tipoOperacion(tipoOperacion)
                .estado("SEPARACION_EN_CURSO")
                .fechaOperacion(request.getFechaOperacion())
                .fechaInicioCronograma(null)
                .precioVenta(precioVenta)
                .asesor(asesor)
                .montoPagadoTotal(montoPagadoTotal)
                .montoSeparacionObjetivo(montoSeparacionObjetivo)
                .montoSeparacionAcumulado(montoSeparacionAcumulado)
                .saldoFinanciarInicial(CERO)
                .plazoMeses(plazoMeses)
                .interesPorcentaje(interesPorcentaje)
                .montoCuotaReferencial(CERO)
                .build();

        if ("CONTADO".equals(tipoOperacion)) {
            BigDecimal saldo = maxZero(precioVenta.subtract(montoPagadoTotal));
            contrato.setEstado(saldo.compareTo(CERO) > 0 ? "DEUDOR" : "AL_DIA");
            contrato.setFechaInicioCronograma(request.getFechaOperacion());
            contrato.setSaldoFinanciarInicial(saldo);
            contrato.setPlazoMeses(saldo.compareTo(CERO) > 0 ? 1 : 0);
            contrato = contratoRepository.save(contrato);
            if (saldo.compareTo(CERO) > 0) {
                generarCuotas(contrato, request.getFechaOperacion(), 1, saldo);
                contrato.setMontoCuotaReferencial(saldo);
            }
            return;
        }

        boolean separacionCompletada = montoSeparacionAcumulado.compareTo(montoSeparacionObjetivo) >= 0;
        contrato = contratoRepository.save(contrato);

        if (separacionCompletada) {
            activarCronogramaDespuesSeparacion(contrato, request.getFechaOperacion());
        }
    }

    @Transactional
    public CronogramaContratoResponse registrarPagoSeparacion(Long contratoId, RegistrarPagoRequest request) {
        CronogramaContrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Cronograma no encontrado"));

        BigDecimal monto = normalizeMoney(request.getMonto());
        if (monto.compareTo(CERO) <= 0) {
            throw new RuntimeException("El monto debe ser mayor a cero");
        }

        if (!"SEPARACION_EN_CURSO".equals(contrato.getEstado())) {
            throw new RuntimeException("Este cronograma ya no está en etapa de separación");
        }

        BigDecimal nuevoPagado = maxZero(contrato.getMontoPagadoTotal().add(monto));
        contrato.setMontoPagadoTotal(nuevoPagado);
        contrato.setMontoSeparacionAcumulado(nuevoPagado.min(contrato.getMontoSeparacionObjetivo()));

        LocalDate fechaPago = request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now();

        if (contrato.getPagosSeparacion() == null) {
            contrato.setPagosSeparacion(new ArrayList<>());
        }

        String estadoPagoSeparacion = contrato.getMontoSeparacionAcumulado().compareTo(contrato.getMontoSeparacionObjetivo()) >= 0
            ? "PAGADA"
            : "PARCIAL";

        CronogramaPagoSeparacion pagoSeparacion = CronogramaPagoSeparacion.builder()
            .contrato(contrato)
            .fechaPago(fechaPago)
            .monto(monto)
            .tipoPago(extraerTipoPagoPrincipal(request))
            .estadoPago(estadoPagoSeparacion)
            .notas(normalizeString(request.getObservacion()))
            .build();

        contrato.getPagosSeparacion().add(pagoSeparacion);

        if (contrato.getMontoSeparacionAcumulado().compareTo(contrato.getMontoSeparacionObjetivo()) >= 0) {
            activarCronogramaDespuesSeparacion(contrato, fechaPago);
        }

        contratoRepository.save(contrato);

        // Registrar transacción de INGRESO en auditoría
        registrarIngresoSeparacion(request, monto, contrato);

        return toResponse(contrato);
    }

    private void registrarIngresoSeparacion(RegistrarPagoRequest request, BigDecimal monto, CronogramaContrato contrato) {
        try {
            String usuarioActual = obtenerUsuarioActualSeguro();
            TitularesInfo titulares = obtenerTitulares(contrato);
            String clienteNombre = titulares.nombres;
            String clienteDni = titulares.dnis;
            UbicacionLoteInfo ubicacion = extraerUbicacionLote(contrato);
            
            String etiquetaIngreso = resolverEtiquetaIngreso(request, true);
            String descripcion = etiquetaIngreso + " registrado por " + clienteNombre + " (DNI: " + clienteDni + ")";
            String mediosStr = extraerMediosComoTexto(request);
            String descripcionPago = extraerDescripcionPago(request);
            
            if (!mediosStr.isBlank()) {
                descripcion += " - Medios: " + mediosStr;
            }
            if (!descripcionPago.isBlank()) {
                descripcion += " - Descripción: " + descripcionPago;
            }
            
            registroAuditoriaService.registrarAccion(
                    usuarioActual,
                    "INGRESO",
                    descripcion,
                    clienteNombre,
                    clienteDni,
                    monto,
                    mediosStr,
                    ubicacion.loteNumero,
                    ubicacion.manzanaNombre,
                    ubicacion.parcelaNombre,
                    ubicacion.etapaNumero,
                    ubicacion.proyectoNombre,
                    descripcionPago
            );
        } catch (Exception e) {
            System.err.println("Error registrando INGRESO en auditoría (separación): " + e.getMessage());
        }
    }

    // Payment registration methods

    @Transactional
    public CronogramaContratoResponse registrarPagoCuota(Long cuotaId, RegistrarPagoRequest request) {
        CronogramaCuota cuota = cuotaRepository.findById(cuotaId)
                .orElseThrow(() -> new RuntimeException("Cuota no encontrada"));
        CronogramaContrato contrato = cuota.getContrato();

        BigDecimal monto = normalizeMoney(request.getMonto());
        if (monto.compareTo(CERO) <= 0) {
            throw new RuntimeException("El monto debe ser mayor a cero");
        }

        validarOrdenPagoCuota(cuota, contrato);

        BigDecimal pagadoAnterior = normalizeMoney(cuota.getMontoPagado());
        BigDecimal montoCuota = normalizeMoney(cuota.getMontoCuota());
        BigDecimal saldoPendiente = maxZero(montoCuota.subtract(pagadoAnterior));
        if (saldoPendiente.compareTo(CERO) <= 0) {
            throw new RuntimeException("La cuota ya se encuentra totalmente pagada");
        }

        // Legacy support: if there is accumulated paid amount but no detail rows, create a historical row
        // so subsequent payments are shown as a true breakdown in "Detalle de Pagos Efectuados".
        backfillPagoHistoricoSiFalta(cuota, pagadoAnterior);

        BigDecimal montoAplicado = monto.min(saldoPendiente);
        BigDecimal nuevoPagado = normalizeMoney(pagadoAnterior.add(montoAplicado));
        cuota.setMontoPagado(nuevoPagado);
        cuota.setObservacion(normalizeString(request.getObservacion()));
        LocalDate fechaPagoOperacion = request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now();

        if (nuevoPagado.compareTo(montoCuota) >= 0) {
            cuota.setEstadoPago("PAGADA");
            cuota.setFechaPago(fechaPagoOperacion);
        } else {
            cuota.setEstadoPago("PARCIAL");
            if (request.getFechaPago() != null) {
                cuota.setFechaPago(request.getFechaPago());
            }
        }

        if (montoAplicado.compareTo(CERO) > 0) {
            if (cuota.getPagos() == null) {
                cuota.setPagos(new ArrayList<>());
            }

            CronogramaPagoCuota pagoDetalle = CronogramaPagoCuota.builder()
                    .cuota(cuota)
                    .fechaPago(fechaPagoOperacion)
                    .monto(montoAplicado)
                    .tipoPago(extraerTipoPagoPrincipal(request))
                    .estadoPago(normalizeString(cuota.getEstadoPago()))
                    .notas(normalizeString(request.getObservacion()))
                    .build();
            cuota.getPagos().add(pagoDetalle);
        }

        cuotaRepository.save(cuota);
        BigDecimal incrementoReal = maxZero(montoAplicado);
        contrato.setMontoPagadoTotal(maxZero(contrato.getMontoPagadoTotal().add(incrementoReal)));
        contrato.setMontoSeparacionAcumulado(contrato.getMontoPagadoTotal().min(contrato.getMontoSeparacionObjetivo()));
        contrato.setEstado(calcularEstadoContrato(contrato));

        contratoRepository.save(contrato);

        boolean cuotaCubiertaCompletaEnEstaOperacion = montoAplicado.compareTo(saldoPendiente) >= 0;

        // Registrar transacción de INGRESO en auditoría
        registrarIngresoEnAuditoria(request, montoAplicado, cuota, contrato, cuotaCubiertaCompletaEnEstaOperacion);

        return toResponse(contrato);
    }

    private void backfillPagoHistoricoSiFalta(CronogramaCuota cuota, BigDecimal pagadoAnterior) {
        BigDecimal pagadoPrevio = normalizeMoney(pagadoAnterior);
        if (pagadoPrevio.compareTo(CERO) <= 0) {
            return;
        }

        if (cuota.getPagos() == null) {
            cuota.setPagos(new ArrayList<>());
        }

        if (!cuota.getPagos().isEmpty()) {
            return;
        }

        CronogramaPagoCuota pagoHistorico = CronogramaPagoCuota.builder()
                .cuota(cuota)
                .fechaPago(cuota.getFechaPago() != null ? cuota.getFechaPago() : cuota.getFechaVencimiento())
                .monto(pagadoPrevio)
                .tipoPago("EFECTIVO")
                .estadoPago(normalizeString(cuota.getEstadoPago()))
                .notas("Pago histórico acumulado")
                .build();

        cuota.getPagos().add(pagoHistorico);
    }

    private void registrarIngresoEnAuditoria(RegistrarPagoRequest request, BigDecimal montoAplicado, CronogramaCuota cuota,
                                             CronogramaContrato contrato, boolean cuotaCubiertaCompletaEnEstaOperacion) {
        try {
            String usuarioActual = obtenerUsuarioActualSeguro();
            TitularesInfo titulares = obtenerTitulares(contrato);
            String clienteNombre = titulares.nombres;
            String clienteDni = titulares.dnis;
            Integer numeroCuota = cuota.getNumeroCuota();
            UbicacionLoteInfo ubicacion = extraerUbicacionLote(contrato);
            BigDecimal montoRegistro = normalizeMoney(montoAplicado);
            
            String etiquetaIngreso = resolverEtiquetaIngreso(request, cuotaCubiertaCompletaEnEstaOperacion);
            String descripcion = etiquetaIngreso + " #" + numeroCuota + " registrado por " + clienteNombre + " (DNI: " + clienteDni + ")";
            String mediosStr = extraerMediosComoTexto(request);
            String descripcionPago = extraerDescripcionPago(request);
            
            if (!mediosStr.isBlank()) {
                descripcion += " - Medios: " + mediosStr;
            }
            if (!descripcionPago.isBlank()) {
                descripcion += " - Descripción: " + descripcionPago;
            }
            
            registroAuditoriaService.registrarAccion(
                    usuarioActual,
                    "INGRESO",
                    descripcion,
                    clienteNombre,
                    clienteDni,
                    montoRegistro,
                    mediosStr,
                    ubicacion.loteNumero,
                    ubicacion.manzanaNombre,
                    ubicacion.parcelaNombre,
                    ubicacion.etapaNumero,
                    ubicacion.proyectoNombre,
                    descripcionPago
            );
        } catch (Exception e) {
            System.err.println("Error registrando INGRESO en auditoría (cuota): " + e.getMessage());
        }
    }

    private UbicacionLoteInfo extraerUbicacionLote(CronogramaContrato contrato) {
        if (contrato == null || contrato.getCliente() == null || contrato.getCliente().getLote() == null) {
            return UbicacionLoteInfo.empty();
        }

        Lote lote = contrato.getCliente().getLote();
        String manzana = lote.getManzana() != null ? normalizeString(lote.getManzana().getNombre()) : "";
        String parcela = lote.getParcela() != null ? normalizeString(lote.getParcela().getNombre()) : "";
        Integer etapaNumero = (lote.getParcela() != null && lote.getParcela().getEtapa() != null)
                ? lote.getParcela().getEtapa().getNumeroEtapa()
                : null;
        String proyecto = (lote.getParcela() != null
                && lote.getParcela().getEtapa() != null
                && lote.getParcela().getEtapa().getProject() != null)
                ? normalizeString(lote.getParcela().getEtapa().getProject().getNombre())
                : "";

        return new UbicacionLoteInfo(lote.getNumero(), manzana, parcela, etapaNumero, proyecto);
    }

    private static class UbicacionLoteInfo {
        private final Integer loteNumero;
        private final String manzanaNombre;
        private final String parcelaNombre;
        private final Integer etapaNumero;
        private final String proyectoNombre;

        private UbicacionLoteInfo(Integer loteNumero, String manzanaNombre, String parcelaNombre,
                                  Integer etapaNumero, String proyectoNombre) {
            this.loteNumero = loteNumero;
            this.manzanaNombre = manzanaNombre;
            this.parcelaNombre = parcelaNombre;
            this.etapaNumero = etapaNumero;
            this.proyectoNombre = proyectoNombre;
        }

        private static UbicacionLoteInfo empty() {
            return new UbicacionLoteInfo(null, "", "", null, "");
        }
    }

    private String obtenerUsuarioActualSeguro() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return "ANONIMO";
        }
        return auth.getName().trim();
    }

    private String extraerMediosComoTexto(RegistrarPagoRequest request) {
        if (request == null || request.getMetadata() == null || request.getMetadata().isEmpty()) {
            return "";
        }

        Map<String, Object> metadata = request.getMetadata();
        Object medios = metadata.get("medios");
        if (medios == null) {
            return "";
        }
        if (medios instanceof List<?> listaMedios && !listaMedios.isEmpty()) {
            String value = listaMedios.stream()
                    .map(this::formatearMedioPago)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(" | "))
                    .trim();
            if (!value.isBlank()) {
                return value.length() > 480 ? value.substring(0, 480) : value;
            }
        }

        String value = String.valueOf(medios).trim();
        return value.length() > 480 ? value.substring(0, 480) : value;
    }

    private String extraerTipoPagoPrincipal(RegistrarPagoRequest request) {
        if (request == null || request.getMetadata() == null || request.getMetadata().isEmpty()) {
            return "EFECTIVO";
        }

        Object mediosObj = request.getMetadata().get("medios");
        if (!(mediosObj instanceof List<?> listaMedios) || listaMedios.isEmpty()) {
            return "EFECTIVO";
        }

        List<String> tipos = listaMedios.stream()
                .map(this::extraerTipoMedio)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        if (tipos.isEmpty()) {
            return "EFECTIVO";
        }

        if (tipos.size() == 1) {
            return tipos.get(0);
        }

        String combinado = String.join(" + ", tipos);
        if (combinado.length() <= 40) {
            return combinado;
        }

        StringBuilder sb = new StringBuilder();
        for (String tipo : tipos) {
            if (tipo == null || tipo.isBlank()) {
                continue;
            }

            String candidate = sb.isEmpty() ? tipo : sb + " + " + tipo;
            if (candidate.length() > 36) {
                break;
            }

            sb.setLength(0);
            sb.append(candidate);
        }

        if (sb.isEmpty()) {
            return combinado.substring(0, 40);
        }

        return sb.length() <= 36 ? sb + " + ..." : sb.substring(0, 40);
    }

    private String extraerTipoMedio(Object medioObj) {
        if (!(medioObj instanceof Map<?, ?> map)) {
            return "";
        }

        Object medio = map.get("medio");
        if (medio == null) {
            return "";
        }

        String tipo = normalizeString(medio.toString()).toUpperCase(Locale.ROOT);
        return tipo.isBlank() ? "" : tipo;
    }

    private String extraerDescripcionPago(RegistrarPagoRequest request) {
        if (request == null || request.getObservacion() == null) {
            return "";
        }
        String value = request.getObservacion().trim();
        if (value.isEmpty()) {
            return "";
        }
        return value.length() > 150 ? value.substring(0, 150) : value;
    }

    private String resolverEtiquetaIngreso(RegistrarPagoRequest request, boolean cuotaCubiertaCompletaEnEstaOperacion) {
        if (request == null || request.getMetadata() == null) {
            return cuotaCubiertaCompletaEnEstaOperacion ? "Pago de Cuota" : "Amortización";
        }
        Object tipoObj = request.getMetadata().get("tipo");
        String tipo = tipoObj == null ? "" : String.valueOf(tipoObj).trim().toLowerCase(Locale.ROOT);
        if (tipo.contains("amort") && !cuotaCubiertaCompletaEnEstaOperacion) {
            return "Amortización";
        }
        return "Pago de Cuota";
    }

    private String formatearMedioPago(Object medio) {
        if (!(medio instanceof Map<?, ?> medioMap)) {
            return "";
        }
        Object medioNombre = medioMap.get("medio");
        Object montoObj = medioMap.get("monto");

        String nombre = medioNombre == null ? "" : String.valueOf(medioNombre).trim();
        BigDecimal monto = parseBigDecimalOrZero(montoObj);

        if (nombre.isBlank() && monto.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        if (nombre.isBlank()) {
            return "S/ " + monto.setScale(2, RoundingMode.HALF_UP);
        }
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            return nombre;
        }
        return nombre + " S/ " + monto.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseBigDecimalOrZero(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private void validarOrdenPagoCuota(CronogramaCuota cuota, CronogramaContrato contrato) {
        Integer numeroCuotaActual = cuota.getNumeroCuota();
        if (numeroCuotaActual == null || numeroCuotaActual <= 1) {
            return;
        }

        boolean hayCuotaAnteriorPendiente = (contrato.getCuotas() == null ? List.<CronogramaCuota>of() : contrato.getCuotas())
                .stream()
                .filter(c -> c.getNumeroCuota() != null && c.getNumeroCuota() < numeroCuotaActual)
                .anyMatch(c -> !"PAGADA".equalsIgnoreCase(normalizeString(c.getEstadoPago())));

        if (hayCuotaAnteriorPendiente) {
            throw new RuntimeException("No se puede pagar la cuota " + numeroCuotaActual + " sin pagar antes las cuotas anteriores");
        }
    }

    @Transactional
    public List<CronogramaContratoResponse> listar(CronogramaFilterRequest filter) {
        List<CronogramaContrato> contratos = contratoRepository.findAllDetailed();

        List<CronogramaContrato> filtrados = contratos.stream()
                .filter(c -> filter == null || coincideFiltro(c, filter))
                .collect(Collectors.toList());

        filtrados.forEach(contrato -> {
            if (repararCuotasPendientesLegacy(contrato)) {
                contratoRepository.save(contrato);
            }
        });

        return filtrados.stream()
                .map(this::toResponse)
            .sorted(Comparator
                .comparing((CronogramaContratoResponse r) -> r.getFechaOperacion() == null ? LocalDate.MIN : r.getFechaOperacion())
                .reversed()
                .thenComparing(CronogramaContratoResponse::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CronogramaContratoResponse obtenerPorId(Long id) {
        CronogramaContrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cronograma no encontrado"));
        if (repararCuotasPendientesLegacy(contrato)) {
            contrato = contratoRepository.save(contrato);
        }
        return toResponse(contrato);
    }

    @Transactional
    public CronogramaContratoResponse actualizarAsesor(Long id, String asesor) {
        CronogramaContrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cronograma no encontrado"));
        contrato.setAsesor(normalizeString(asesor));
        contrato = contratoRepository.save(contrato);
        return toResponse(contrato);
    }

    private boolean coincideFiltro(CronogramaContrato contrato, CronogramaFilterRequest filter) {
        Cliente cliente = contrato.getCliente();
        Lote lote = cliente.getLote();
        List<Cliente> titularesLote = obtenerTitularesLote(lote);
        if (titularesLote.isEmpty()) {
            titularesLote = List.of(cliente);
        }

        if (filter.getProjectId() != null && !Objects.equals(lote.getParcela().getEtapa().getProject().getId(), filter.getProjectId())) {
            return false;
        }
        if (filter.getEtapaNumero() != null && !Objects.equals(lote.getParcela().getEtapa().getNumeroEtapa(), filter.getEtapaNumero())) {
            return false;
        }

        String parcela = normalizeString(filter.getParcelaNombre());
        if (!parcela.isEmpty() && !normalizeString(lote.getParcela().getNombre()).toUpperCase(Locale.ROOT).contains(parcela.toUpperCase(Locale.ROOT))) {
            return false;
        }

        String manzana = normalizeString(filter.getManzana());
        if (!manzana.isEmpty()) {
            String m = lote.getManzana() != null ? normalizeString(lote.getManzana().getNombre()) : "";
            if (!m.toUpperCase(Locale.ROOT).contains(manzana.toUpperCase(Locale.ROOT))) {
                return false;
            }
        }

        if (filter.getLoteId() != null && !Objects.equals(lote.getId(), filter.getLoteId())) {
            return false;
        }

        String dni = normalizeString(filter.getDni());
        if (!dni.isEmpty()) {
            boolean dniCoincide = titularesLote.stream()
                    .map(Cliente::getDni)
                    .map(this::normalizeString)
                    .anyMatch(d -> d.contains(dni));
            if (!dniCoincide) {
                return false;
            }
        }

        String nombres = normalizeString(filter.getNombres()).toUpperCase(Locale.ROOT);
        if (!nombres.isEmpty()) {
            boolean nombreCoincide = titularesLote.stream()
                    .map(c -> (normalizeString(c.getNombres()) + " " + normalizeString(c.getApellidos())).toUpperCase(Locale.ROOT))
                    .anyMatch(fullName -> fullName.contains(nombres));
            if (!nombreCoincide) {
                return false;
            }
        }

        String estado = normalizeString(filter.getEstado()).toUpperCase(Locale.ROOT);
        return estado.isEmpty() || calcularEstadoContrato(contrato).equals(estado);
    }

    private void activarCronogramaDespuesSeparacion(CronogramaContrato contrato, LocalDate fechaBase) {
        BigDecimal saldo = maxZero(contrato.getPrecioVenta().subtract(contrato.getMontoPagadoTotal()));
        contrato.setSaldoFinanciarInicial(saldo);
        contrato.setFechaInicioCronograma(fechaBase);

        if (saldo.compareTo(CERO) <= 0) {
            contrato.setPlazoMeses(0);
            contrato.setMontoCuotaReferencial(CERO);
            contrato.setEstado("AL_DIA");
            contrato.getCuotas().clear();
            return;
        }

        int plazoMeses = Math.max(1, contrato.getPlazoMeses() == null ? 1 : contrato.getPlazoMeses());
        BigDecimal factorInteres = BigDecimal.ONE.add(contrato.getInteresPorcentaje().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        // Apply interest to remaining balance after separation is complete
        BigDecimal saldoConInteres = normalizeMoney(saldo.multiply(factorInteres));

        contrato.getCuotas().clear();
        generarCuotas(contrato, fechaBase, plazoMeses, saldoConInteres);
        BigDecimal cuotaReferencial = saldoConInteres.divide(new BigDecimal(plazoMeses), 2, RoundingMode.HALF_UP);
        BigDecimal cuotaBase = roundToNearestTen(cuotaReferencial);
        if (cuotaBase.compareTo(CERO) <= 0) {
            cuotaBase = normalizeMoney(cuotaReferencial);
        }
        if (plazoMeses > 1) {
            BigDecimal maxCuotaPermitida = saldoConInteres.divide(new BigDecimal(plazoMeses - 1), 0, RoundingMode.DOWN);
            if (cuotaBase.compareTo(maxCuotaPermitida) > 0) {
                cuotaBase = maxCuotaPermitida.max(BigDecimal.ONE);
            }
        }
        contrato.setMontoCuotaReferencial(cuotaBase);
        contrato.setEstado(calcularEstadoContrato(contrato));

        registrarConfirmacionCronograma(contrato, saldoConInteres, plazoMeses);
    }

    private void registrarConfirmacionCronograma(CronogramaContrato contrato, BigDecimal totalCronograma, int plazoMeses) {
        try {
            String usuarioActual = obtenerUsuarioActualSeguro();
            TitularesInfo titulares = obtenerTitulares(contrato);
            String clienteNombre = titulares.nombres;
            String clienteDni = titulares.dnis;

            String descripcion = "Cronograma confirmado al completar separación para " + clienteNombre
                    + " (DNI: " + clienteDni + ")"
                    + " - Plazo: " + plazoMeses + " meses"
                    + " - Total financiado: S/ " + totalCronograma.setScale(2, RoundingMode.HALF_UP);

            registroAuditoriaService.registrarAccion(
                    usuarioActual,
                    "CRONOGRAMA",
                    descripcion,
                    clienteNombre,
                    clienteDni,
                    totalCronograma,
                    "",
                    null,
                    "",
                    "",
                    null,
                    "",
                    ""
            );
        } catch (Exception e) {
            System.err.println("Error registrando confirmación de cronograma en auditoría: " + e.getMessage());
        }
    }

    private void generarCuotas(CronogramaContrato contrato, LocalDate fechaBase, int plazoMeses, BigDecimal total) {
        if (plazoMeses <= 0) {
            return;
        }

        BigDecimal cuotaReferencial = total.divide(new BigDecimal(plazoMeses), 2, RoundingMode.HALF_UP);
        BigDecimal cuotaBase = roundToNearestTen(cuotaReferencial);
        if (cuotaBase.compareTo(CERO) <= 0) {
            cuotaBase = normalizeMoney(cuotaReferencial);
        }

        BigDecimal maxCuotaPermitida = total;
        if (plazoMeses > 1) {
            maxCuotaPermitida = total.divide(new BigDecimal(plazoMeses - 1), 0, RoundingMode.DOWN);
        }
        if (plazoMeses > 1 && cuotaBase.compareTo(maxCuotaPermitida) > 0) {
            cuotaBase = maxCuotaPermitida.max(BigDecimal.ONE);
        }

        BigDecimal acumulado = CERO;
        List<CronogramaCuota> cuotas = new ArrayList<>();

        for (int i = 1; i <= plazoMeses; i++) {
            BigDecimal montoCuota = (i == plazoMeses) ? normalizeMoney(total.subtract(acumulado)) : cuotaBase;
            acumulado = normalizeMoney(acumulado.add(montoCuota));

            CronogramaCuota cuota = CronogramaCuota.builder()
                    .contrato(contrato)
                    .numeroCuota(i)
                    .fechaVencimiento(fechaBase.plusMonths(i))
                    .montoCuota(montoCuota)
                    .montoPagado(CERO)
                    .estadoPago("PENDIENTE")
                    .build();
            cuotas.add(cuota);
        }

        contrato.getCuotas().addAll(cuotas);
    }

    private CronogramaContratoResponse toResponse(CronogramaContrato contrato) {
        Cliente cliente = contrato.getCliente();
        Lote lote = cliente.getLote();

        List<CronogramaCuotaResponse> cuotas = contrato.getCuotas() == null
                ? List.of()
                : contrato.getCuotas().stream()
                .sorted(Comparator.comparing(CronogramaCuota::getNumeroCuota))
                .map(this::toCuotaResponse)
                .collect(Collectors.toList());

        cuotas = normalizarCuotasPendientesConRedondeoProforma(contrato, cuotas);

        BigDecimal saldoPendienteActual = cuotas.stream()
                .map(CronogramaCuotaResponse::getSaldoPendiente)
                .reduce(CERO, this::sum);

        BigDecimal cuotaReferencial = normalizeMoney(contrato.getMontoCuotaReferencial());
        if (cuotas != null && !cuotas.isEmpty()) {
            cuotaReferencial = cuotas.stream()
                    .sorted(Comparator.comparing(CronogramaCuotaResponse::getNumeroCuota))
                    .findFirst()
                    .map(CronogramaCuotaResponse::getMontoCuota)
                    .map(this::normalizeMoney)
                    .orElse(cuotaReferencial);
        }

        List<CronogramaPagoSeparacion> pagosSeparacionPersistidos = pagoSeparacionRepository
            .findByContratoIdOrderByIdAsc(contrato.getId());

        List<CronogramaPagoCuotaResponse> pagosSeparacion = pagosSeparacionPersistidos.stream()
            .sorted(Comparator.comparing(CronogramaPagoSeparacion::getId, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(pago -> CronogramaPagoCuotaResponse.builder()
                .id(pago.getId())
                .fechaPago(pago.getFechaPago())
                .monto(normalizeMoney(pago.getMonto()))
                .tipoPago(normalizeString(pago.getTipoPago()))
                .estadoPago(normalizeString(pago.getEstadoPago()))
                .notas(normalizeString(pago.getNotas()))
                .build())
            .collect(Collectors.toList());

        if (pagosSeparacion.isEmpty() && normalizeMoney(contrato.getMontoSeparacionAcumulado()).compareTo(CERO) > 0) {
            String estadoSeparacion = normalizeMoney(contrato.getMontoSeparacionAcumulado())
                .compareTo(normalizeMoney(contrato.getMontoSeparacionObjetivo())) >= 0
                ? "PAGADA"
                : "PARCIAL";

            pagosSeparacion = List.of(
                CronogramaPagoCuotaResponse.builder()
                    .id(null)
                    .fechaPago(contrato.getFechaOperacion())
                    .monto(normalizeMoney(contrato.getMontoSeparacionAcumulado()))
                    .tipoPago("EFECTIVO")
                    .estadoPago(estadoSeparacion)
                    .notas("Pago de separación histórico")
                    .build()
            );
        }

        String estadoActual = calcularEstadoContrato(contrato);
        TitularesInfo titulares = obtenerTitulares(contrato);

        return CronogramaContratoResponse.builder()
                .id(contrato.getId())
                .clienteId(cliente.getId())
            .clienteNombre(titulares.nombres)
            .clienteDni(titulares.dnis)
                .asesor(normalizeString(contrato.getAsesor()))
                .projectId(lote.getParcela().getEtapa().getProject().getId())
                .projectNombre(lote.getParcela().getEtapa().getProject().getNombre())
                .etapaNumero(lote.getParcela().getEtapa().getNumeroEtapa())
                .parcelaNombre(lote.getParcela().getNombre())
                .manzana(lote.getManzana() != null ? lote.getManzana().getNombre() : "")
                .loteId(lote.getId())
                .loteNumero(lote.getNumero())
                .tipoOperacion(contrato.getTipoOperacion())
                .estado(estadoActual)
                .fechaOperacion(contrato.getFechaOperacion())
                .fechaInicioCronograma(contrato.getFechaInicioCronograma())
                .precioVenta(normalizeMoney(contrato.getPrecioVenta()))
                .montoPagadoTotal(normalizeMoney(contrato.getMontoPagadoTotal()))
                .montoSeparacionObjetivo(normalizeMoney(contrato.getMontoSeparacionObjetivo()))
                .montoSeparacionAcumulado(normalizeMoney(contrato.getMontoSeparacionAcumulado()))
                .saldoFinanciarInicial(normalizeMoney(contrato.getSaldoFinanciarInicial()))
                .saldoPendienteActual(saldoPendienteActual)
                .plazoMeses(contrato.getPlazoMeses())
                .interesPorcentaje(normalizeMoney(contrato.getInteresPorcentaje()))
                .montoCuotaReferencial(cuotaReferencial)
                .pagosSeparacion(pagosSeparacion)
                .cuotas(cuotas)
                .build();
    }

    private CronogramaCuotaResponse toCuotaResponse(CronogramaCuota cuota) {
        BigDecimal montoCuota = normalizeMoney(cuota.getMontoCuota());
        BigDecimal montoPagado = normalizeMoney(cuota.getMontoPagado());
        BigDecimal saldo = maxZero(montoCuota.subtract(montoPagado));

        LocalDate hoy = LocalDate.now();
        Integer diasRetraso = 0;
        if (saldo.compareTo(CERO) > 0 && hoy.isAfter(cuota.getFechaVencimiento())) {
            diasRetraso = (int) ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), hoy);
        }

        List<CronogramaPagoCuotaResponse> pagos = cuota.getPagos() == null
            ? List.of()
            : cuota.getPagos().stream()
            .sorted(Comparator.comparing(CronogramaPagoCuota::getId, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(pago -> CronogramaPagoCuotaResponse.builder()
                .id(pago.getId())
                .fechaPago(pago.getFechaPago())
                .monto(normalizeMoney(pago.getMonto()))
                .tipoPago(normalizeString(pago.getTipoPago()))
                .estadoPago(normalizeString(pago.getEstadoPago()))
                .notas(normalizeString(pago.getNotas()))
                .build())
            .collect(Collectors.toList());

        return CronogramaCuotaResponse.builder()
                .id(cuota.getId())
                .numeroCuota(cuota.getNumeroCuota())
                .fechaVencimiento(cuota.getFechaVencimiento())
                .montoCuota(montoCuota)
                .montoPagado(montoPagado)
                .saldoPendiente(saldo)
                .estadoPago(normalizeEstadoCuota(cuota, saldo))
                .diasRetraso(diasRetraso)
                .fechaPago(cuota.getFechaPago())
                .pagos(pagos)
                .build();
    }

    private List<CronogramaCuotaResponse> normalizarCuotasPendientesConRedondeoProforma(CronogramaContrato contrato, List<CronogramaCuotaResponse> cuotas) {
        if (cuotas == null || cuotas.isEmpty()) {
            return cuotas;
        }

        boolean todasPendientesSinPago = cuotas.stream().allMatch(c ->
                normalizeMoney(c.getMontoPagado()).compareTo(CERO) == 0
                        && "PENDIENTE".equalsIgnoreCase(normalizeString(c.getEstadoPago())));

        if (!todasPendientesSinPago) {
            return cuotas;
        }

        int plazo = cuotas.size();
        if (plazo <= 1) {
            return cuotas;
        }

        BigDecimal total = calcularTotalPlanEsperado(contrato);

        if (total.compareTo(CERO) <= 0) {
            return cuotas;
        }

        BigDecimal cuotaReferencial = total.divide(new BigDecimal(plazo), 2, RoundingMode.HALF_UP);
        BigDecimal cuotaBase = roundToNearestTen(cuotaReferencial);
        if (cuotaBase.compareTo(CERO) <= 0) {
            cuotaBase = normalizeMoney(cuotaReferencial);
        }

        BigDecimal maxCuotaPermitida = total.divide(new BigDecimal(plazo - 1), 0, RoundingMode.DOWN);
        if (cuotaBase.compareTo(maxCuotaPermitida) > 0) {
            cuotaBase = maxCuotaPermitida.max(BigDecimal.ONE);
        }

        BigDecimal acumulado = CERO;
        for (int i = 0; i < plazo; i++) {
            BigDecimal montoCuota = (i == plazo - 1) ? maxZero(total.subtract(acumulado)) : cuotaBase;
            acumulado = normalizeMoney(acumulado.add(montoCuota));

            CronogramaCuotaResponse cuota = cuotas.get(i);
            cuota.setMontoCuota(montoCuota);
            cuota.setSaldoPendiente(montoCuota);
        }

        return cuotas;
    }

    private BigDecimal calcularTotalPlanEsperado(CronogramaContrato contrato) {
        if (contrato == null) {
            return CERO;
        }

        BigDecimal precioVenta = normalizeMoney(contrato.getPrecioVenta());
        BigDecimal montoPagadoTotal = normalizeMoney(contrato.getMontoPagadoTotal());
        BigDecimal montoSeparacionObjetivo = normalizeMoney(contrato.getMontoSeparacionObjetivo());

        BigDecimal pagadoAlCompletarSeparacion = montoPagadoTotal.max(montoSeparacionObjetivo);
        BigDecimal saldoBase = maxZero(precioVenta.subtract(pagadoAlCompletarSeparacion));
        if (saldoBase.compareTo(CERO) <= 0) {
            return CERO;
        }

        BigDecimal factorInteres = BigDecimal.ONE.add(normalizeMoney(contrato.getInteresPorcentaje())
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return normalizeMoney(saldoBase.multiply(factorInteres));
    }

    private boolean repararCuotasPendientesLegacy(CronogramaContrato contrato) {
        if (contrato == null || contrato.getCuotas() == null || contrato.getCuotas().isEmpty()) {
            return false;
        }

        List<CronogramaCuota> cuotas = contrato.getCuotas().stream()
                .sorted(Comparator.comparing(CronogramaCuota::getNumeroCuota))
                .collect(Collectors.toList());

        int plazo = cuotas.size();
        if (plazo <= 1) {
            return false;
        }

        boolean todasPendientesSinPago = cuotas.stream().allMatch(c ->
                normalizeMoney(c.getMontoPagado()).compareTo(CERO) == 0
                        && "PENDIENTE".equalsIgnoreCase(normalizeString(c.getEstadoPago())));
        if (!todasPendientesSinPago) {
            return false;
        }

        BigDecimal total = calcularTotalPlanEsperado(contrato);
        if (total.compareTo(CERO) <= 0) {
            return false;
        }

        BigDecimal cuotaReferencial = total.divide(new BigDecimal(plazo), 2, RoundingMode.HALF_UP);
        BigDecimal cuotaBase = roundToNearestTen(cuotaReferencial);
        if (cuotaBase.compareTo(CERO) <= 0) {
            cuotaBase = normalizeMoney(cuotaReferencial);
        }

        BigDecimal maxCuotaPermitida = total.divide(new BigDecimal(plazo - 1), 0, RoundingMode.DOWN);
        if (cuotaBase.compareTo(maxCuotaPermitida) > 0) {
            cuotaBase = maxCuotaPermitida.max(BigDecimal.ONE);
        }

        boolean changed = false;
        BigDecimal acumulado = CERO;
        for (int i = 0; i < plazo; i++) {
            BigDecimal montoCuotaNuevo = (i == plazo - 1) ? maxZero(total.subtract(acumulado)) : cuotaBase;
            acumulado = normalizeMoney(acumulado.add(montoCuotaNuevo));

            CronogramaCuota cuota = cuotas.get(i);
            BigDecimal montoActual = normalizeMoney(cuota.getMontoCuota());
            if (montoActual.compareTo(montoCuotaNuevo) != 0) {
                cuota.setMontoCuota(montoCuotaNuevo);
                changed = true;
            }
        }

        if (changed) {
            contrato.setMontoCuotaReferencial(cuotaBase);
        }

        return changed;
    }

    private String calcularEstadoContrato(CronogramaContrato contrato) {
        if (contrato.getMontoSeparacionAcumulado().compareTo(contrato.getMontoSeparacionObjetivo()) < 0
                && (contrato.getCuotas() == null || contrato.getCuotas().isEmpty())) {
            return "SEPARACION_EN_CURSO";
        }

        if (contrato.getCuotas() == null || contrato.getCuotas().isEmpty()) {
            BigDecimal saldo = maxZero(contrato.getPrecioVenta().subtract(contrato.getMontoPagadoTotal()));
            return saldo.compareTo(CERO) > 0 ? "DEUDOR" : "AL_DIA";
        }

        LocalDate hoy = LocalDate.now();
        boolean deudaVencida = contrato.getCuotas().stream()
                .anyMatch(cuota -> maxZero(normalizeMoney(cuota.getMontoCuota()).subtract(normalizeMoney(cuota.getMontoPagado()))).compareTo(CERO) > 0
                        && hoy.isAfter(cuota.getFechaVencimiento()));

        BigDecimal saldoPendiente = contrato.getCuotas().stream()
                .map(cuota -> maxZero(normalizeMoney(cuota.getMontoCuota()).subtract(normalizeMoney(cuota.getMontoPagado()))))
                .reduce(CERO, this::sum);

        if (saldoPendiente.compareTo(CERO) <= 0) {
            return "AL_DIA";
        }

        return deudaVencida ? "DEUDOR" : "AL_DIA";
    }

    private String normalizeEstadoCuota(CronogramaCuota cuota, BigDecimal saldo) {
        if (saldo.compareTo(CERO) <= 0) {
            return "PAGADA";
        }
        if (normalizeMoney(cuota.getMontoPagado()).compareTo(CERO) > 0) {
            return "PARCIAL";
        }
        return "PENDIENTE";
    }

    private List<Cliente> obtenerTitularesLote(Lote lote) {
        if (lote == null || lote.getId() == null) {
            return List.of();
        }

        List<Cliente> titulares = clienteRepository.findAllByLoteId(lote.getId()).stream()
                .sorted(Comparator.comparing(Cliente::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        return titulares;
    }

    private TitularesInfo obtenerTitulares(CronogramaContrato contrato) {
        if (contrato == null || contrato.getCliente() == null || contrato.getCliente().getLote() == null) {
            return new TitularesInfo("Desconocido", "");
        }

        List<Cliente> titulares = obtenerTitularesLote(contrato.getCliente().getLote());
        if (titulares.isEmpty()) {
            Cliente c = contrato.getCliente();
            String nombre = (normalizeString(c.getNombres()) + " " + normalizeString(c.getApellidos())).trim();
            return new TitularesInfo(nombre.isBlank() ? "Desconocido" : nombre, normalizeString(c.getDni()));
        }

        String nombres = titulares.stream()
                .map(c -> (normalizeString(c.getNombres()) + " " + normalizeString(c.getApellidos())).trim())
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        String dnis = titulares.stream()
                .map(Cliente::getDni)
                .map(this::normalizeString)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        return new TitularesInfo(nombres.isBlank() ? "Desconocido" : nombres, dnis);
    }

    private record TitularesInfo(String nombres, String dnis) {}

    private String normalizeTipoOperacion(String tipoOperacion) {
        String tipo = normalizeString(tipoOperacion).toUpperCase(Locale.ROOT);
        if (!"CONTADO".equals(tipo) && !"CREDITO".equals(tipo) && !"SEPARACION".equals(tipo)) {
            throw new RuntimeException("Tipo de operación inválido para cronograma");
        }
        return tipo;
    }

    private Integer normalizePlazoMeses(Integer plazoMeses) {
        int value = plazoMeses == null ? 24 : plazoMeses;
        return Math.max(1, Math.min(360, value));
    }

    private BigDecimal normalizePercent(BigDecimal provided, int plazoMeses) {
        if (provided != null) {
            return provided.setScale(2, RoundingMode.HALF_UP).min(new BigDecimal("100.00"));
        }

        if (plazoMeses <= 12) {
            return CERO;
        }
        if (plazoMeses <= 24) {
            return new BigDecimal("10.00");
        }
        return new BigDecimal("20.00");
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return CERO;
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal roundToNearestTen(BigDecimal value) {
        if (value == null) {
            return CERO;
        }
        return value.divide(new BigDecimal("10"), 0, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("10"))
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal maxZero(BigDecimal value) {
        return normalizeMoney(value).max(CERO);
    }

    private BigDecimal sum(BigDecimal a, BigDecimal b) {
        return normalizeMoney(a.add(b));
    }

    private String normalizeString(String value) {
        return value == null ? "" : value.trim();
    }
}

// ...existing code...
