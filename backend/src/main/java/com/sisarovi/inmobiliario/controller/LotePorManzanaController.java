package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.LoteResponse;
import com.sisarovi.inmobiliario.entity.Lote;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.LoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manzanas/{manzanaId}/lotes")
@RequiredArgsConstructor
public class LotePorManzanaController {
    private final LoteRepository loteRepository;
    private final ClienteRepository clienteRepository;

    @GetMapping
    public ResponseEntity<List<LoteResponse>> getLotesByManzana(@PathVariable Long manzanaId) {
        List<LoteResponse> response = loteRepository.findByManzanaIdOrderByNumeroAsc(manzanaId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private LoteResponse toResponse(Lote lote) {
        return LoteResponse.builder()
                .id(lote.getId())
                .numero(lote.getNumero())
                .calle(lote.getCalle())
                .perimetro(lote.getPerimetro())
                .areaM2(lote.getAreaM2())
                .medidaFrente(lote.getMedidaFrente())
                .medidaIzquierda(lote.getMedidaIzquierda())
                .medidaDerecha(lote.getMedidaDerecha())
                .medidaFondo(lote.getMedidaFondo())
                .numeroPartida(lote.getNumeroPartida())
                .precioLote(lote.getPrecioLote())
                .manzanaId(lote.getManzana() != null ? lote.getManzana().getId() : null)
                .manzana(lote.getManzana() != null ? lote.getManzana().getNombre() : null)
                .parcelaId(lote.getParcela() != null ? lote.getParcela().getId() : null)
                .parcelaNombre(lote.getParcela() != null ? lote.getParcela().getNombre() : null)
                .etapaNumero(lote.getParcela() != null && lote.getParcela().getEtapa() != null
                        ? lote.getParcela().getEtapa().getNumeroEtapa()
                        : null)
                .projectId(lote.getParcela() != null && lote.getParcela().getEtapa() != null && lote.getParcela().getEtapa().getProject() != null
                        ? lote.getParcela().getEtapa().getProject().getId()
                        : null)
                .projectNombre(lote.getParcela() != null && lote.getParcela().getEtapa() != null && lote.getParcela().getEtapa().getProject() != null
                        ? lote.getParcela().getEtapa().getProject().getNombre()
                        : null)
                .adquirido(clienteRepository.existsByLoteId(lote.getId()))
                .build();
    }
}
