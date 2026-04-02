package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.ProformaRequest;
import com.sisarovi.inmobiliario.dto.ProformaResponse;
import com.sisarovi.inmobiliario.entity.Proforma;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.service.ProformaService;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/proformas")
@RequiredArgsConstructor
public class ProformaController {

    private final ProformaService proformaService;
    private final ObjectMapper objectMapper;
    private final RegistroAuditoriaService registroAuditoriaService;

    @PostMapping
    public ResponseEntity<ProformaResponse> create(@RequestBody ProformaRequest request,
                                                   Authentication authentication) {
        String createdBy = authentication != null ? authentication.getName() : null;
        ProformaResponse created = proformaService.create(request, createdBy);
        registroAuditoriaService.registrarAccion(
            createdBy,
            "PROFORMA",
            String.format("Se creó la proforma %s para el cliente '%s'.", created.getCodigo(), created.getClienteNombre())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProformaResponse> createWithPdf(
            @RequestPart("payload") String payload,
            @RequestPart("pdfFile") MultipartFile pdfFile,
            Authentication authentication
    ) throws Exception {
        String createdBy = authentication != null ? authentication.getName() : null;
        ProformaRequest request = objectMapper.readValue(payload, ProformaRequest.class);
        ProformaResponse created = proformaService.createWithPdf(request, pdfFile, createdBy);
        registroAuditoriaService.registrarAccion(
            createdBy,
            "PROFORMA",
            String.format("Se creó la proforma %s con PDF para el cliente '%s'.", created.getCodigo(), created.getClienteNombre())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(created);
    }

    @GetMapping("/historial")
    public ResponseEntity<List<ProformaResponse>> historial() {
        return ResponseEntity.ok(proformaService.historial());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ProformaResponse>> buscar(@RequestParam(defaultValue = "codigo") String tipo,
                                                         @RequestParam String q) {
        return ResponseEntity.ok(proformaService.buscar(tipo, q));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> verPdf(@PathVariable Long id) {
        Proforma proforma = proformaService.getById(id);

        if (proforma.getPdfData() == null || proforma.getPdfData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String fileName = proforma.getPdfFileName() != null ? proforma.getPdfFileName() : (proforma.getCodigo() + ".pdf");
        String contentType = proforma.getPdfContentType() != null ? proforma.getPdfContentType() : MediaType.APPLICATION_PDF_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(proforma.getPdfData());
    }
}
