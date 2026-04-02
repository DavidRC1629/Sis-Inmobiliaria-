package com.sisarovi.inmobiliario.config;

import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditTrailInterceptor implements HandlerInterceptor {

    private final RegistroAuditoriaService registroAuditoriaService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }

        String endpoint = request.getRequestURI();
        if (endpoint.startsWith("/api/registro-auditoria")
            || endpoint.startsWith("/api/auth/login")
            || endpoint.startsWith("/api/projects")
            || endpoint.startsWith("/api/proformas")
            || endpoint.startsWith("/api/cronogramas")) {
            return;
        }

        try {
            String usuario = resolverUsuario();
            String entidad = resolverEntidad(handler, endpoint);
            String accion = resolverAccion(method, endpoint, entidad);
            String descripcion = construirDescripcion(method, endpoint, entidad, response.getStatus(), ex);
            registroAuditoriaService.registrarAccion(usuario, accion, descripcion);
        } catch (Exception e) {
            log.warn("No se pudo registrar auditoría para {} {}", method, endpoint, e);
        }
    }

    private String resolverUsuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "ANONIMO";
        }
        String name = authentication.getName();
        if (name == null || name.trim().isEmpty() || "anonymousUser".equalsIgnoreCase(name)) {
            return "ANONIMO";
        }
        return name.trim();
    }

    private String resolverEntidad(Object handler, String endpoint) {
        if (handler instanceof HandlerMethod handlerMethod) {
            String controller = handlerMethod.getBeanType().getSimpleName();
            if (controller.endsWith("Controller")) {
                controller = controller.substring(0, controller.length() - "Controller".length());
            }
            if ("Auth".equalsIgnoreCase(controller)) {
                return "Autenticación";
            }
            if ("Project".equalsIgnoreCase(controller)) {
                return "Proyecto";
            }
            if ("User".equalsIgnoreCase(controller)) {
                return "Usuario";
            }
            if ("Proforma".equalsIgnoreCase(controller)) {
                return "Proforma";
            }
            if ("RegistroAuditoria".equalsIgnoreCase(controller)) {
                return "Auditoría";
            }
            return controller;
        }

        if (endpoint.contains("/proformas")) {
            return "Proforma";
        }
        if (endpoint.contains("/lotes")) {
            return "Lote";
        }
        if (endpoint.contains("/users")) {
            return "Usuario";
        }
        if (endpoint.contains("/projects")) {
            return "Proyecto";
        }
        return "Sistema";
    }

    private String resolverAccion(String method, String endpoint, String entidad) {
        String methodUpper = method == null ? "" : method.toUpperCase();
        String endpointLower = endpoint == null ? "" : endpoint.toLowerCase();

        if ("POST".equals(methodUpper) && "Proforma".equalsIgnoreCase(entidad)) {
            return "PROFORMA";
        }
        if (endpointLower.contains("cronograma")) {
            return "CRONOGRAMA";
        }
        if ("POST".equals(methodUpper) && endpointLower.contains("/auth/login")) {
            return "LOGIN";
        }

        return switch (methodUpper) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "OTHER";
        };
    }

    private String construirDescripcion(String method, String endpoint, String entidad, int status, Exception ex) {
        String tipoOperacion = switch ((method == null ? "" : method.toUpperCase())) {
            case "POST" -> "creó";
            case "PUT", "PATCH" -> "actualizó";
            case "DELETE" -> "eliminó";
            default -> "ejecutó";
        };

        if (status >= 400 || ex != null) {
            return String.format("Se intentó %s %s, pero ocurrió un error (HTTP %d).", tipoOperacion, entidad, status);
        }

        return String.format("Se %s %s correctamente.", tipoOperacion, entidad);
    }
}
