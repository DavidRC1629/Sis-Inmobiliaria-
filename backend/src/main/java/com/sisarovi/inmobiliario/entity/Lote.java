package com.sisarovi.inmobiliario.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "lotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Integer numero;
    @Column(length = 100)
    private String calle;
    @Column(precision = 10, scale = 2)
    private BigDecimal perimetro;
    @Column(name = "area_m2", precision = 10, scale = 2)
    private BigDecimal areaM2;
    @Column(precision = 10, scale = 2)
    private BigDecimal medidaFrente;
    @Column(precision = 10, scale = 2)
    private BigDecimal medidaIzquierda;
    @Column(precision = 10, scale = 2)
    private BigDecimal medidaDerecha;
    @Column(precision = 10, scale = 2)
    private BigDecimal medidaFondo;
    @Column(length = 100)
    private String numeroPartida;
    @Column(name = "precio_lote", precision = 12, scale = 2)
    private BigDecimal precioLote;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manzana_id")
    @JsonBackReference
    private Manzana manzana;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcela_id", nullable = false)
    @JsonBackReference
    private Parcela parcela;
}
