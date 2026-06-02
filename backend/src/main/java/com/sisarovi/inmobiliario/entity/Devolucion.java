package com.sisarovi.inmobiliario.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devoluciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Devolucion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loteId;

    @Column(nullable = false)
    private Integer loteNumero;

    @Column(nullable = false)
    private String manzana;

    @Column(nullable = false)
    private String parcelaNombre;

    @Column(nullable = false)
    private Integer etapaNumero;

    @Column(nullable = false)
    private String proyectoNombre;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montoTotal;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer dias;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFinEstimada;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DevolucionPago> pagos = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        fechaActualizacion = ahora;
        if (montoPagado == null) {
            montoPagado = BigDecimal.ZERO;
        }
        if (estado == null || estado.isBlank()) {
            estado = "EN_CURSO";
        }
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}