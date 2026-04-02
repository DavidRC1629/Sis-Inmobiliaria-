package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.TerrenoPropioRequest;
import com.sisarovi.inmobiliario.dto.TerrenoPropioResponse;
import com.sisarovi.inmobiliario.entity.Cliente;
import com.sisarovi.inmobiliario.entity.TerrenoPropio;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.TerrenoPropioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import com.sisarovi.inmobiliario.service.CronogramaService;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
public class TerrenoPropioService {
    private final TerrenoPropioRepository terrenoPropioRepository;
    private final ClienteRepository clienteRepository;
    @Autowired
    private RegistroAuditoriaService registroAuditoriaService;
    @Autowired
    private CronogramaService cronogramaService;

    public List<TerrenoPropioResponse> getAll() {
        return terrenoPropioRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Optional<TerrenoPropioResponse> getById(Long id) {
        return terrenoPropioRepository.findById(id).map(this::toResponse);
    }

    @Transactional
    public TerrenoPropioResponse create(TerrenoPropioRequest request) {
        if (terrenoPropioRepository.existsByNumeroPartida(request.getNumeroPartida())) {
            throw new IllegalArgumentException("El número de partida ya existe en el sistema");
        }
        Cliente propietario = clienteRepository.findById(request.getPropietarioId())
                .orElseThrow(() -> new IllegalArgumentException("Propietario no encontrado"));
        TerrenoPropio terreno = TerrenoPropio.builder()
                .numeroLote(request.getNumeroLote())
                .calle(request.getCalle())
                .areaM2(request.getAreaM2())
                .perimetro(request.getPerimetro())
                .medidaFrente(request.getMedidaFrente())
                .medidaFondo(request.getMedidaFondo())
                .medidaIzquierda(request.getMedidaIzquierda())
                .medidaDerecha(request.getMedidaDerecha())
                .numeroPartida(request.getNumeroPartida())
                .precio(request.getPrecio())
                .propietario(propietario)
                .imagenUrl(request.getImagenUrl())
                .estado("DISPONIBLE")
                .build();
        terrenoPropioRepository.save(terreno);
        return toResponse(terreno);
    }

    public boolean existsByNumeroPartida(String numeroPartida) {
        return terrenoPropioRepository.existsByNumeroPartida(numeroPartida);
    }

    private TerrenoPropioResponse toResponse(TerrenoPropio t) {
        return new TerrenoPropioResponse(
                t.getId(),
                t.getNumeroLote(),
                t.getCalle(),
                t.getAreaM2(),
                t.getPerimetro(),
                t.getMedidaFrente(),
                t.getMedidaFondo(),
                t.getMedidaIzquierda(),
                t.getMedidaDerecha(),
                t.getNumeroPartida(),
                t.getPrecio(),
                // Mapear propietario a ClienteResponse si es necesario
                null,
                t.getImagenUrl(),
                t.getEstado()
        );
    }

    public String uploadImagen(Long id, MultipartFile file) {
        TerrenoPropio terreno = terrenoPropioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Terreno propio no encontrado"));
        String folder = "uploads/terrenos_propios/";
        String filename = "terreno_" + id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(folder);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);
            String url = "/" + folder + filename;
            terreno.setImagenUrl(url);
            terrenoPropioRepository.save(terreno);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen", e);
        }
    }

    public void deleteImagen(Long id) {
        TerrenoPropio terreno = terrenoPropioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Terreno propio no encontrado"));
        if (terreno.getImagenUrl() != null) {
            try {
                Path filePath = Paths.get(terreno.getImagenUrl().replaceFirst("/", ""));
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log y continuar
            }
            terreno.setImagenUrl(null);
            terrenoPropioRepository.save(terreno);
        }
    }

    @Transactional
    public void adquirirTerreno(Long terrenoId, Long clienteId, String formaPago, int cuotas, double interes) {
        TerrenoPropio terreno = terrenoPropioRepository.findById(terrenoId)
                .orElseThrow(() -> new IllegalArgumentException("Terreno propio no encontrado"));
        if (!"DISPONIBLE".equals(terreno.getEstado())) {
            throw new IllegalStateException("El terreno no está disponible para adquisición");
        }
        terreno.setEstado("VENDIDO");
        terrenoPropioRepository.save(terreno);
        // Registrar ingreso en auditoría
        registroAuditoriaService.registrarAccion(
            String.valueOf(clienteId),
            "INGRESO",
            String.format("Ingreso por adquisición de Terreno Propio (Partida: %s)", terreno.getNumeroPartida())
        );
        // Crear cronograma (al contado o en cuotas)
        cronogramaService.crearCronogramaParaTerrenoPropio(terreno, clienteId, formaPago, cuotas, interes);
    }
}
