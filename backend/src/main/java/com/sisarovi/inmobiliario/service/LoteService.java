package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.LoteRequest;
import com.sisarovi.inmobiliario.dto.LoteResponse;
import com.sisarovi.inmobiliario.entity.Lote;
import com.sisarovi.inmobiliario.entity.Parcela;
import com.sisarovi.inmobiliario.entity.Manzana;
import com.sisarovi.inmobiliario.repository.LoteRepository;
import com.sisarovi.inmobiliario.repository.ParcelaRepository;
import com.sisarovi.inmobiliario.repository.ManzanaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoteService {
    
    private final LoteRepository loteRepository;
    private final ParcelaRepository parcelaRepository;
    private final ManzanaRepository manzanaRepository;
    
    @Transactional(readOnly = true)
    public List<LoteResponse> getLotesByParcela(Long parcelaId) {
        log.info("Obteniendo lotes de la parcela ID: {}", parcelaId);
        return loteRepository.findByParcelaIdOrderByNumeroAsc(parcelaId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<LoteResponse> getLotesByParcelaAndManzana(Long parcelaId, String manzana) {
        log.info("Obteniendo lotes de la parcela ID: {} y manzanaId: {}", parcelaId, manzana);
        Long manzanaId = null;
        try {
            manzanaId = Long.valueOf(manzana);
        } catch (NumberFormatException e) {
            throw new RuntimeException("El id de manzana debe ser un número válido");
        }
        return loteRepository.findByParcelaIdAndManzanaIdOrderByNumeroAsc(parcelaId, manzanaId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public LoteResponse getLoteById(Long loteId) {
        log.info("Obteniendo lote ID: {}", loteId);
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + loteId));
        return toResponse(lote);
    }
    
    @Transactional
    public LoteResponse createLote(Long parcelaId, LoteRequest request) {
        log.info("Creando lote {} para parcela ID: {}", request.getNumero(), parcelaId);
        
        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new RuntimeException("Parcela no encontrada con ID: " + parcelaId));
        
        Manzana manzana = resolveManzana(parcelaId, request)
            .orElseThrow(() -> new RuntimeException("La manzana es obligatoria"));

        // Validar que el número de lote sea único dentro de la manzana
        if (loteRepository.findByParcelaIdAndManzana_IdAndNumero(parcelaId, manzana.getId(), request.getNumero()).isPresent()) {
            throw new RuntimeException("Ya existe un lote con el número " + request.getNumero() + " en la manzana " + manzana.getId());
        }
        
        // Validar que el número de partida sea único globalmente
        String numeroPartida = normalizeNumeroPartida(request.getNumeroPartida());
        if (numeroPartida.isEmpty()) {
            throw new RuntimeException("El número de partida es obligatorio.");
        }
        Optional<Lote> loteConPartida = loteRepository.findByNumeroPartidaIgnoreCase(numeroPartida);
        if (loteConPartida.isPresent()) {
            throw new RuntimeException("El número de partida " + numeroPartida + " ya existe y no se puede repetir.");
        }

        BigDecimal precioLote = normalizePrecioLote(request.getPrecioLote());
        
        Lote lote = new Lote();
        lote.setNumero(request.getNumero());
        lote.setCalle(request.getCalle());
        lote.setPerimetro(request.getPerimetro());
        lote.setAreaM2(request.getAreaM2());
        lote.setMedidaFrente(request.getMedidaFrente());
        lote.setMedidaIzquierda(request.getMedidaIzquierda());
        lote.setMedidaDerecha(request.getMedidaDerecha());
        lote.setMedidaFondo(request.getMedidaFondo());
        lote.setNumeroPartida(numeroPartida);
        lote.setPrecioLote(precioLote);
        lote.setManzana(manzana);
        lote.setParcela(parcela);
        
        Lote saved;
        try {
            saved = loteRepository.saveAndFlush(lote);
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException(resolveConstraintMessage(ex, request.getNumero(), numeroPartida));
        }
        log.info("Lote creado con ID: {}", saved.getId());
        
        return toResponse(saved);
    }
    
    @Transactional
    public LoteResponse updateLote(Long loteId, LoteRequest request) {
        log.info("Actualizando lote ID: {}", loteId);
        
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + loteId));
        
        Manzana manzana = resolveManzana(lote.getParcela().getId(), request)
                .orElseThrow(() -> new RuntimeException("La manzana es obligatoria"));

        // Validar que el número de lote sea único dentro de la manzana (si cambió)
        if (!lote.getNumero().equals(request.getNumero()) ||
            (lote.getManzana() == null || !lote.getManzana().getId().equals(manzana.getId()))) {
            Optional<Lote> loteExistente = loteRepository.findByParcelaIdAndManzana_IdAndNumero(
                lote.getParcela().getId(), manzana.getId(), request.getNumero());
            if (loteExistente.isPresent() && !loteExistente.get().getId().equals(loteId)) {
                throw new RuntimeException("Ya existe un lote con el número " + request.getNumero() + " en la manzana " + manzana.getId());
            }
        }
        
        // Validar que el número de partida sea único globalmente (si cambió)
        String numeroPartida = normalizeNumeroPartida(request.getNumeroPartida());
        if (numeroPartida.isEmpty()) {
            throw new RuntimeException("El número de partida es obligatorio.");
        }
        String numeroPartidaActual = normalizeNumeroPartida(lote.getNumeroPartida());
        if (!numeroPartida.equalsIgnoreCase(numeroPartidaActual)) {
            Optional<Lote> loteConPartida = loteRepository.findByNumeroPartidaIgnoreCase(numeroPartida);
            if (loteConPartida.isPresent() && !loteConPartida.get().getId().equals(loteId)) {
                throw new RuntimeException("El número de partida " + numeroPartida + " ya existe y no se puede repetir.");
            }
        }

        BigDecimal precioLote = normalizePrecioLote(request.getPrecioLote());
        
        lote.setNumero(request.getNumero());
        lote.setCalle(request.getCalle());
        lote.setPerimetro(request.getPerimetro());
        lote.setAreaM2(request.getAreaM2());
        lote.setMedidaFrente(request.getMedidaFrente());
        lote.setMedidaIzquierda(request.getMedidaIzquierda());
        lote.setMedidaDerecha(request.getMedidaDerecha());
        lote.setMedidaFondo(request.getMedidaFondo());
        lote.setNumeroPartida(numeroPartida);
        lote.setPrecioLote(precioLote);
        lote.setManzana(manzana);
        
        Lote updated;
        try {
            updated = loteRepository.saveAndFlush(lote);
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException(resolveConstraintMessage(ex, request.getNumero(), numeroPartida));
        }
        log.info("Lote actualizado: {}", updated.getId());
        
        return toResponse(updated);
    }
    
    @Transactional
    public void deleteLote(Long loteId) {
        log.info("Eliminando lote ID: {}", loteId);
        
        if (!loteRepository.existsById(loteId)) {
            throw new RuntimeException("Lote no encontrado con ID: " + loteId);
        }
        
        loteRepository.deleteById(loteId);
        log.info("Lote eliminado: {}", loteId);
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
                .manzana(lote.getManzana() != null ? normalizeManzana(lote.getManzana().getNombre()) : null)
                .parcelaId(lote.getParcela().getId())
                .parcelaNombre(lote.getParcela().getNombre())
                .etapaNumero(lote.getParcela().getEtapa().getNumeroEtapa())
                .projectId(lote.getParcela().getEtapa().getProject().getId())
                .projectNombre(lote.getParcela().getEtapa().getProject().getNombre())
                .build();
    }

    private Optional<Manzana> resolveManzana(Long parcelaId, LoteRequest request) {
        if (request.getManzanaId() != null) {
            return manzanaRepository.findById(request.getManzanaId());
        }

        if (request.getManzana() == null || request.getManzana().trim().isEmpty()) {
            return Optional.empty();
        }

        String input = request.getManzana().trim().toUpperCase(Locale.ROOT);
        List<Manzana> manzanas = manzanaRepository.findByParcelaIdOrderByIdAsc(parcelaId);
        if (manzanas.isEmpty()) {
            return Optional.empty();
        }

        for (Manzana item : manzanas) {
            String normalized = normalizeManzana(item.getNombre());
            if (normalized.equals(input) || ("MANZANA " + normalized).equals(input)) {
                return Optional.of(item);
            }
        }

        int index = parseManzanaIndex(input);
        if (index > 0 && index <= manzanas.size()) {
            return Optional.of(manzanas.get(index - 1));
        }

        return Optional.empty();
    }

    private String normalizeManzana(String nombre) {
        if (nombre == null) {
            return "";
        }
        String value = nombre.trim().toUpperCase(Locale.ROOT);
        if (value.startsWith("MANZANA ")) {
            return value.substring("MANZANA ".length()).trim();
        }
        return value;
    }

    @Transactional(readOnly = true)
    public boolean existsNumeroPartidaGlobal(String numeroPartida, Long excludeLoteId) {
        String normalized = normalizeNumeroPartida(numeroPartida);
        if (normalized.isEmpty()) {
            return false;
        }

        Optional<Lote> lote = loteRepository.findByNumeroPartidaIgnoreCase(normalized);
        if (lote.isEmpty()) {
            return false;
        }

        return excludeLoteId == null || !lote.get().getId().equals(excludeLoteId);
    }

    private String normalizeNumeroPartida(String numeroPartida) {
        return numeroPartida == null ? "" : numeroPartida.trim();
    }

    private BigDecimal normalizePrecioLote(BigDecimal precioLote) {
        if (precioLote == null || precioLote.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El precio del lote es obligatorio y debe ser mayor a 0.");
        }
        return precioLote;
    }

    private String resolveConstraintMessage(DataIntegrityViolationException ex, Integer numeroLote, String numeroPartida) {
        String detail = ex.getMostSpecificCause() != null
            ? ex.getMostSpecificCause().getMessage()
            : ex.getMessage();
        String normalized = detail == null ? "" : detail.toLowerCase(Locale.ROOT);

        if (normalized.contains("numero_partida") || normalized.contains("uk_lotes_numero_partida_global")) {
            return "El número de partida " + numeroPartida + " ya existe y no se puede repetir.";
        }
        return "Ya existe un lote con el número " + numeroLote + " en esta manzana/parcela.";
    }

    private int parseManzanaIndex(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
        }

        if (input.length() == 1 && input.charAt(0) >= 'A' && input.charAt(0) <= 'Z') {
            return (input.charAt(0) - 'A') + 1;
        }

        return -1;
    }
}
