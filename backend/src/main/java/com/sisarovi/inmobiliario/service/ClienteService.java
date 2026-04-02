package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.ClienteLoteOptionResponse;
import com.sisarovi.inmobiliario.dto.ClienteProjectSummaryResponse;
import com.sisarovi.inmobiliario.dto.ClienteRequest;
import com.sisarovi.inmobiliario.dto.ClienteResponse;
import com.sisarovi.inmobiliario.dto.ClienteAdquisicionRequest;
import com.sisarovi.inmobiliario.dto.PropietarioRequest;
import com.sisarovi.inmobiliario.entity.Cliente;
import com.sisarovi.inmobiliario.entity.Lote;
import com.sisarovi.inmobiliario.entity.Project;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.LoteRepository;
import com.sisarovi.inmobiliario.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;
    private final ProjectRepository projectRepository;
    private final CronogramaService cronogramaService;

    @Transactional(readOnly = true)
    public List<ClienteResponse> getAllClientes(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return clienteRepository.findAllOrdered().stream()
                .filter(cliente -> normalizedQuery.isEmpty() || matches(cliente, normalizedQuery))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteProjectSummaryResponse> getProjectSummaries() {
        List<Project> projects = projectRepository.findAll();

        return projects.stream()
                .map(project -> ClienteProjectSummaryResponse.builder()
                        .projectId(project.getId())
                        .projectNombre(project.getNombre())
                        .cantidadClientes(clienteRepository.findByProjectIdOrdered(project.getId()).size())
                        .build())
                .sorted(Comparator.comparing(ClienteProjectSummaryResponse::getProjectNombre, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> getClientesByProject(Long projectId) {
        return clienteRepository.findByProjectIdOrdered(projectId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteLoteOptionResponse> getLotesDisponibles(Long projectId, Long clienteId) {
        return loteRepository.findByProjectIdForClientes(projectId)
                .stream()
                .map(this::toLoteOption)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ClienteResponse> createClientesPorAdquisicion(ClienteAdquisicionRequest request) {
        List<PropietarioRequest> propietarios = request.getPropietarios();

        if (propietarios == null || propietarios.isEmpty()) {
            throw new RuntimeException("Debe registrar al menos un propietario");
        }

        if (propietarios.size() > 4) {
            throw new RuntimeException("Solo se permiten hasta 4 propietarios por lote");
        }

        String tipoOperacion = sanitize(request.getTipoOperacion()).toUpperCase(Locale.ROOT);
        if (!"CONTADO".equals(tipoOperacion) && !"CREDITO".equals(tipoOperacion) && !"SEPARACION".equals(tipoOperacion)) {
            throw new RuntimeException("Tipo de operación inválido");
        }

        String tipoRelacion = "SEPARACION".equals(tipoOperacion) ? "SEPARACION" : "ADQUISICION";

        Lote lote = loteRepository.findById(request.getLoteId())
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));

        validarLoteDisponibleParaAdquisicion(lote.getId());

        List<ClienteResponse> creados = new ArrayList<>();
        List<Cliente> clientesCreados = new ArrayList<>();
        for (PropietarioRequest propietario : propietarios) {
            ClienteRequest clienteRequest = new ClienteRequest(
                    sanitize(propietario.getNombres()),
                    sanitize(propietario.getApellidos()),
                    sanitize(propietario.getDni()),
                    sanitizeNullable(propietario.getEmail()),
                    sanitize(propietario.getTelefono()),
                    sanitize(propietario.getDireccion()),
                    lote.getId(),
                    tipoRelacion,
                    request.getFechaOperacion()
            );

            Cliente cliente = Cliente.builder()
                    .nombres(clienteRequest.getNombres())
                    .apellidos(clienteRequest.getApellidos())
                    .dni(clienteRequest.getDni())
                    .email(clienteRequest.getEmail())
                    .telefono(clienteRequest.getTelefono())
                    .direccion(clienteRequest.getDireccion())
                    .tipoRelacion(clienteRequest.getTipoRelacion())
                    .clienteDesde(clienteRequest.getClienteDesde())
                    .lote(lote)
                    .build();

            Cliente clienteCreado = clienteRepository.save(cliente);
            clientesCreados.add(clienteCreado);
            creados.add(toResponse(clienteCreado));
        }

        // Para lotes compartidos, todos los propietarios comparten un solo cronograma.
        if (!clientesCreados.isEmpty()) {
            cronogramaService.crearDesdeAdquisicion(clientesCreados.get(0), request);
        }

        return creados;
    }

    @Transactional
    public ClienteResponse createCliente(ClienteRequest request) {
        Lote lote = loteRepository.findById(request.getLoteId())
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));

        validarLoteDisponibleParaAdquisicion(lote.getId());

        Cliente cliente = Cliente.builder()
                .nombres(sanitize(request.getNombres()))
                .apellidos(sanitize(request.getApellidos()))
                .dni(sanitize(request.getDni()))
                .email(sanitizeNullable(request.getEmail()))
                .telefono(sanitize(request.getTelefono()))
                .direccion(sanitize(request.getDireccion()))
                .tipoRelacion(normalizeTipoRelacion(request.getTipoRelacion()))
                .clienteDesde(request.getClienteDesde())
                .lote(lote)
                .build();

        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse updateCliente(Long clienteId, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Lote lote = loteRepository.findById(request.getLoteId())
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));

        if (cliente.getLote() == null || !cliente.getLote().getId().equals(lote.getId())) {
            validarLoteDisponibleParaAdquisicion(lote.getId());
        }

        cliente.setNombres(sanitize(request.getNombres()));
        cliente.setApellidos(sanitize(request.getApellidos()));
        cliente.setDni(sanitize(request.getDni()));
        cliente.setEmail(sanitizeNullable(request.getEmail()));
        cliente.setTelefono(sanitize(request.getTelefono()));
        cliente.setDireccion(sanitize(request.getDireccion()));
        cliente.setTipoRelacion(normalizeTipoRelacion(request.getTipoRelacion()));
        cliente.setClienteDesde(request.getClienteDesde());
        cliente.setLote(lote);

        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void deleteCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RuntimeException("Cliente no encontrado");
        }
        clienteRepository.deleteById(clienteId);
    }

    @Transactional(readOnly = true)
    public ClienteResponse getClienteById(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return toResponse(cliente);
    }

    private boolean matches(Cliente cliente, String query) {
        String fullName = (sanitize(cliente.getNombres()) + " " + sanitize(cliente.getApellidos())).toLowerCase(Locale.ROOT);
        String dni = sanitize(cliente.getDni()).toLowerCase(Locale.ROOT);
        return fullName.contains(query) || dni.contains(query);
    }

    private ClienteResponse toResponse(Cliente cliente) {
        Lote lote = cliente.getLote();

        return ClienteResponse.builder()
                .id(cliente.getId())
                .nombres(cliente.getNombres())
                .apellidos(cliente.getApellidos())
                .dni(cliente.getDni())
                .email(cliente.getEmail())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .tipoRelacion(cliente.getTipoRelacion())
                .clienteDesde(cliente.getClienteDesde())
                .loteId(lote.getId())
                .loteNumero(lote.getNumero())
                .manzana(lote.getManzana() != null ? lote.getManzana().getNombre() : "")
                .parcelaNombre(lote.getParcela().getNombre())
                .etapaNumero(lote.getParcela().getEtapa().getNumeroEtapa())
                .projectId(lote.getParcela().getEtapa().getProject().getId())
                .projectNombre(lote.getParcela().getEtapa().getProject().getNombre())
                .build();
    }

    private ClienteLoteOptionResponse toLoteOption(Lote lote) {
        return ClienteLoteOptionResponse.builder()
                .loteId(lote.getId())
                .loteNumero(lote.getNumero())
                .manzana(lote.getManzana() != null ? lote.getManzana().getNombre() : "")
                .parcelaNombre(lote.getParcela().getNombre())
                .etapaNumero(lote.getParcela().getEtapa().getNumeroEtapa())
                .projectId(lote.getParcela().getEtapa().getProject().getId())
                .projectNombre(lote.getParcela().getEtapa().getProject().getNombre())
                .build();
    }

    private String normalizeTipoRelacion(String value) {
        String normalized = sanitize(value).toUpperCase(Locale.ROOT);
        if (!"ADQUISICION".equals(normalized) && !"SEPARACION".equals(normalized)) {
            throw new RuntimeException("El tipo de relación debe ser ADQUISICION o SEPARACION");
        }
        return normalized;
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private String sanitizeNullable(String value) {
        String normalized = sanitize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private void validarLoteDisponibleParaAdquisicion(Long loteId) {
        if (clienteRepository.existsByLoteId(loteId)) {
            throw new RuntimeException("Este lote ya está adquirido y no puede ser adquirido nuevamente.");
        }
    }
}
     