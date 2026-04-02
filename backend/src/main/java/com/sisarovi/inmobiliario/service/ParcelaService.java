package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.ParcelaRequest;
import com.sisarovi.inmobiliario.dto.ParcelaResponse;
import com.sisarovi.inmobiliario.entity.Etapa;
import com.sisarovi.inmobiliario.entity.Manzana;
import com.sisarovi.inmobiliario.entity.Parcela;
import com.sisarovi.inmobiliario.repository.EtapaRepository;
import com.sisarovi.inmobiliario.repository.LoteRepository;
import com.sisarovi.inmobiliario.repository.ManzanaRepository;
import com.sisarovi.inmobiliario.repository.ParcelaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParcelaService {
    
    private final ParcelaRepository parcelaRepository;
    private final EtapaRepository etapaRepository;
    private final LoteRepository loteRepository;
    private final ManzanaRepository manzanaRepository;
    
    @Transactional(readOnly = true)
    public List<ParcelaResponse> getParcelasByEtapa(Long etapaId) {
        log.info("Obteniendo parcelas de la etapa ID: {}", etapaId);
        return parcelaRepository.findByEtapaIdOrderByNombreAsc(etapaId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ParcelaResponse getParcelaById(Long parcelaId) {
        log.info("Obteniendo parcela ID: {}", parcelaId);
        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new RuntimeException("Parcela no encontrada con ID: " + parcelaId));
        return toResponse(parcela);
    }
    
    @Transactional(readOnly = true)
    public List<ParcelaResponse> searchByPropietario(String propietario) {
        log.info("Buscando parcelas por propietario: {}", propietario);
        return parcelaRepository.findByPropietarioContainingIgnoreCase(propietario)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ParcelaResponse createParcela(Long etapaId, ParcelaRequest request) {
        log.info("Creando parcela {} para etapa ID: {}", request.getNombre(), etapaId);
        
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> new RuntimeException("Etapa no encontrada con ID: " + etapaId));
        
        Parcela parcela = new Parcela();
        parcela.setNombre(request.getNombre());
        parcela.setNumManzanas(request.getNumManzanas());
        parcela.setPropietario(request.getPropietario());
        parcela.setEtapa(etapa);
        // Asigna valor mínimo válido para numLotes
        parcela.setNumLotes(1);

        Parcela saved = parcelaRepository.save(parcela);
        log.info("Parcela creada con ID: {}", saved.getId());
        ensureManzanas(parcela, request.getNumManzanas());

        return toResponse(saved);
    }
    
    @Transactional
    public ParcelaResponse updateParcela(Long parcelaId, ParcelaRequest request) {
        log.info("Actualizando parcela ID: {}", parcelaId);
        
        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new RuntimeException("Parcela no encontrada con ID: " + parcelaId));
        
        parcela.setNombre(request.getNombre());
        parcela.setNumManzanas(request.getNumManzanas());
        parcela.setPropietario(request.getPropietario());

        ensureManzanas(parcela, request.getNumManzanas());
        
        Parcela updated = parcelaRepository.save(parcela);
        log.info("Parcela actualizada: {}", updated.getId());
        
        return toResponse(updated);
    }
    
    @Transactional
    public void deleteParcela(Long parcelaId) {
        log.info("Eliminando parcela ID: {}", parcelaId);
        
        if (!parcelaRepository.existsById(parcelaId)) {
            throw new RuntimeException("Parcela no encontrada con ID: " + parcelaId);
        }
        
        loteRepository.deleteAllByParcelaId(parcelaId);
        manzanaRepository.deleteAllByParcelaId(parcelaId);
        parcelaRepository.deleteById(parcelaId);
        log.info("Parcela eliminada: {}", parcelaId);
    }
    
    private ParcelaResponse toResponse(Parcela parcela) {
        int cantidadLotesCreados = parcela.getCantidadLotes();
        return ParcelaResponse.builder()
                .id(parcela.getId())
                .nombre(parcela.getNombre())
                .numManzanas(parcela.getNumManzanas())
                .propietario(parcela.getPropietario())
                .cantidadLotes(cantidadLotesCreados)
                .lotesDisponibles(cantidadLotesCreados)
                .etapaId(parcela.getEtapa().getId())
                .build();
    }

    private void ensureManzanas(Parcela parcela, int requiredCount) {
        List<Manzana> existentes = manzanaRepository.findByParcelaIdOrderByNombreAsc(parcela.getId());
        if (existentes.size() >= requiredCount) {
            return;
        }

        for (int index = existentes.size() + 1; index <= requiredCount; index++) {
            Manzana manzana = Manzana.builder()
                    .nombre("Manzana " + toLetters(index))
                    .parcela(parcela)
                    .build();
            manzanaRepository.save(manzana);
        }
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
}
