package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.entity.Parcela;
import com.sisarovi.inmobiliario.entity.Manzana;
import com.sisarovi.inmobiliario.repository.ManzanaRepository;
import com.sisarovi.inmobiliario.repository.ParcelaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/parcelas/{parcelaId}/manzanas")
@RequiredArgsConstructor
public class ManzanaController {
    private final ManzanaRepository manzanaRepository;
    private final ParcelaRepository parcelaRepository;

    @GetMapping
    public ResponseEntity<List<ManzanaOptionDto>> getManzanasByParcela(@PathVariable Long parcelaId) {
        List<Manzana> manzanas = manzanaRepository.findByParcelaIdOrderByNombreAsc(parcelaId);
        if (!manzanas.isEmpty()) {
            return ResponseEntity.ok(toDtoList(manzanas));
        }

        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new RuntimeException("Parcela no encontrada con ID: " + parcelaId));

        int requiredCount = parcela.getNumManzanas() != null ? parcela.getNumManzanas() : 0;
        for (int index = 1; index <= requiredCount; index++) {
            manzanaRepository.save(Manzana.builder()
                    .nombre("Manzana " + toLetters(index))
                    .parcela(parcela)
                    .build());
        }

        return ResponseEntity.ok(toDtoList(manzanaRepository.findByParcelaIdOrderByNombreAsc(parcelaId)));
    }

    private List<ManzanaOptionDto> toDtoList(List<Manzana> manzanas) {
        return manzanas.stream()
                .map(m -> new ManzanaOptionDto(m.getId(), m.getNombre()))
                .collect(Collectors.toList());
    }

    private String toLetters(int index) {
        StringBuilder value = new StringBuilder();
        int n = index;
        while (n > 0) {
            int remainder = (n - 1) % 26;
            value.insert(0, (char) ('A' + remainder));
            n = (n - 1) / 26;
        }
        return value.toString();
    }

    private record ManzanaOptionDto(Long id, String nombre) {}
}
