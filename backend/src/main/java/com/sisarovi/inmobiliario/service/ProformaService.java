package com.sisarovi.inmobiliario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.ProformaRequest;
import com.sisarovi.inmobiliario.dto.ProformaResponse;
import com.sisarovi.inmobiliario.entity.Proforma;
import com.sisarovi.inmobiliario.repository.ProformaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProformaService {

    private final ProformaRepository proformaRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProformaResponse create(ProformaRequest request, String createdBy) {
        Proforma proforma = Proforma.builder()
                .codigo(resolveCodigo(request.getCodigo()))
            .proyecto(trimToMax(request.getProyecto(), 200))
            .clienteNombre(trimToMax(request.getClienteNombre(), 40))
            .clienteDni(trimToMax(request.getClienteDni(), 20))
            .clienteCelular(trimToMax(request.getClienteCelular(), 30))
            .asesor(trimToMax(request.getAsesor(), 150))
                .fechaEmision(parseDate(request.getFechaEmision()))
                .fechaVencimiento(parseDate(request.getFechaVencimiento()))
                .precioContado(request.getPrecioContado())
                .detalleJson(toJson(request.getDetalle() != null ? request.getDetalle() : request))
                .createdBy(createdBy)
                .build();

        Proforma saved = proformaRepository.save(proforma);
        log.info("Proforma creada. Código: {}", saved.getCodigo());
        return toResponse(saved);
    }

    @Transactional
    public ProformaResponse createWithPdf(ProformaRequest request, MultipartFile pdfFile, String createdBy) {
        byte[] pdfBytes;
        try {
            pdfBytes = pdfFile.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el PDF enviado", e);
        }

        Proforma proforma = Proforma.builder()
            .codigo(resolveCodigo(request.getCodigo()))
                .proyecto(trimToMax(request.getProyecto(), 200))
                .clienteNombre(trimToMax(request.getClienteNombre(), 40))
                .clienteDni(trimToMax(request.getClienteDni(), 20))
                .clienteCelular(trimToMax(request.getClienteCelular(), 30))
                .asesor(trimToMax(request.getAsesor(), 150))
                .fechaEmision(parseDate(request.getFechaEmision()))
                .fechaVencimiento(parseDate(request.getFechaVencimiento()))
                .precioContado(request.getPrecioContado())
                .detalleJson(toJson(request.getDetalle() != null ? request.getDetalle() : request))
                .pdfData(pdfBytes)
                .pdfFileName(pdfFile.getOriginalFilename())
                .pdfContentType(pdfFile.getContentType())
                .createdBy(createdBy)
                .build();

        Proforma saved = proformaRepository.save(proforma);
        log.info("Proforma guardada con PDF. Código: {}", saved.getCodigo());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Proforma getById(Long id) {
        return proformaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proforma no encontrada con ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProformaResponse> historial() {
        return proformaRepository.findHistorialLite();
    }

    @Transactional(readOnly = true)
    public List<ProformaResponse> buscar(String tipo, String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 3) {
            return List.of();
        }

        if ("codigo".equalsIgnoreCase(tipo)) {
            String normalized = query.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            return proformaRepository.buscarTop50PorCodigoNormalizado(normalized);
        }

        return proformaRepository.findTop50ByClienteNombreContainingIgnoreCaseOrderByCreatedAtDesc(query);
    }

    private ProformaResponse toResponse(Proforma p) {
        return ProformaResponse.builder()
                .id(p.getId())
                .codigo(p.getCodigo())
                .proyecto(p.getProyecto())
                .clienteNombre(p.getClienteNombre())
                .clienteDni(p.getClienteDni())
                .asesor(p.getAsesor())
                .fechaEmision(p.getFechaEmision())
                .fechaVencimiento(p.getFechaVencimiento())
                .precioContado(p.getPrecioContado())
                .createdAt(p.getCreatedAt())
                .hasPdf(p.getPdfData() != null && p.getPdfData().length > 0)
                .build();
    }

    private String generateCodigo() {
        final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (int attempt = 0; attempt < 200; attempt++) {
            int numericPart = ThreadLocalRandom.current().nextInt(0, 1000);
            String numbers = String.format(Locale.ROOT, "%03d", numericPart);

            StringBuilder textPart = new StringBuilder(3);
            for (int i = 0; i < 3; i++) {
                int idx = ThreadLocalRandom.current().nextInt(letters.length());
                textPart.append(letters.charAt(idx));
            }

            String code = numbers + "-" + textPart;
            if (!proformaRepository.existsByCodigo(code)) {
                return code;
            }
        }

        throw new IllegalStateException("No se pudo generar un código único para la proforma");
    }

    private String resolveCodigo(String requestedCodigo) {
        if (requestedCodigo == null || requestedCodigo.isBlank()) {
            return generateCodigo();
        }

        String normalized = requestedCodigo
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");

        if (normalized.matches("^\\d{3}[A-Z]{3}$")) {
            String formatted = normalized.substring(0, 3) + "-" + normalized.substring(3);
            if (!proformaRepository.existsByCodigo(formatted)) {
                return formatted;
            }
        }

        return generateCodigo();
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date);
    }

    private String trimToMax(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String toJson(Object detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar detalle de proforma", e);
            return "{}";
        }
    }
}
