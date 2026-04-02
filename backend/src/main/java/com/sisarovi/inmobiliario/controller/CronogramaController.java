package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.CronogramaContratoResponse;
import com.sisarovi.inmobiliario.dto.CronogramaFilterRequest;
import com.sisarovi.inmobiliario.dto.RegistrarPagoRequest;
import com.sisarovi.inmobiliario.dto.CronogramaDescuentoRequest;
import com.sisarovi.inmobiliario.service.CronogramaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cronogramas")
@RequiredArgsConstructor
public class CronogramaController {

    private final CronogramaService cronogramaService;

    @GetMapping
    public ResponseEntity<List<CronogramaContratoResponse>> listar(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer etapaNumero,
            @RequestParam(required = false) String parcelaNombre,
            @RequestParam(required = false) String manzana,
            @RequestParam(required = false) Long loteId,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String estado) {

        CronogramaFilterRequest filter = new CronogramaFilterRequest();
        filter.setProjectId(projectId);
        filter.setEtapaNumero(etapaNumero);
        filter.setParcelaNombre(parcelaNombre);
        filter.setManzana(manzana);
        filter.setLoteId(loteId);
        filter.setDni(dni);
        filter.setNombres(nombres);
        filter.setEstado(estado);

        return ResponseEntity.ok(cronogramaService.listar(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CronogramaContratoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cronogramaService.obtenerPorId(id));
    }

    @PostMapping("/{id}/pagos/separacion")
    public ResponseEntity<CronogramaContratoResponse> registrarPagoSeparacion(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarPagoRequest request) {
        return ResponseEntity.ok(cronogramaService.registrarPagoSeparacion(id, request));
    }

    @PostMapping("/cuotas/{cuotaId}/pagos")
    public ResponseEntity<CronogramaContratoResponse> registrarPagoCuota(
            @PathVariable Long cuotaId,
            @Valid @RequestBody RegistrarPagoRequest request) {
        return ResponseEntity.ok(cronogramaService.registrarPagoCuota(cuotaId, request));
    }

    @PatchMapping("/{id}/asesor")
    public ResponseEntity<CronogramaContratoResponse> actualizarAsesor(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String asesor = body.getOrDefault("asesor", "");
        return ResponseEntity.ok(cronogramaService.actualizarAsesor(id, asesor));
    }

    @PostMapping("/aplicar-descuento")
    public ResponseEntity<CronogramaContratoResponse> aplicarDescuento(@Valid @RequestBody CronogramaDescuentoRequest request) {
        CronogramaContratoResponse contrato = cronogramaService.aplicarDescuento(request);
        return ResponseEntity.ok(contrato);
    }
}
