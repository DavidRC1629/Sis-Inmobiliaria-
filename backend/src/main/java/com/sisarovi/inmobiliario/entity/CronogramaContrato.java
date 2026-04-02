package com.sisarovi.inmobiliario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cronograma_contratos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CronogramaContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "tipo_operacion", nullable = false, length = 20)
    private String tipoOperacion;

    @Column(nullable = false, length = 40)
    private String estado;

    @Column(name = "fecha_operacion", nullable = false)
    private LocalDate fechaOperacion;

    @Column(name = "fecha_inicio_cronograma")
    private LocalDate fechaInicioCronograma;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "asesor", length = 120)
    private String asesor;

    @Column(name = "monto_pagado_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagadoTotal;

    @Column(name = "monto_separacion_objetivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoSeparacionObjetivo;

    @Column(name = "monto_separacion_acumulado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoSeparacionAcumulado;

    @Column(name = "saldo_financiar_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoFinanciarInicial;

    @Column(name = "plazo_meses", nullable = false)
    private Integer plazoMeses;

    @Column(name = "interes_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal interesPorcentaje;

    @Column(name = "monto_cuota_referencial", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoCuotaReferencial;

    @Builder.Default
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CronogramaCuota> cuotas = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CronogramaPagoSeparacion> pagosSeparacion = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
