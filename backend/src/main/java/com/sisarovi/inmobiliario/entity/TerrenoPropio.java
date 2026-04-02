package com.sisarovi.inmobiliario.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "terrenos_propios", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"numero_partida"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerrenoPropio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_lote", nullable = false)
    private Integer numeroLote;

    @Column(length = 255)
    private String calle;

    @Column(name = "area_m2")
    private BigDecimal areaM2;

    private BigDecimal perimetro;
    private BigDecimal medidaFrente;
    private BigDecimal medidaFondo;
    private BigDecimal medidaIzquierda;
    private BigDecimal medidaDerecha;

    @Column(name = "numero_partida", nullable = false, unique = true, length = 100)
    private String numeroPartida;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal precio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Cliente propietario;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(nullable = false, length = 20)
    private String estado; // DISPONIBLE, VENDIDO, EN_CUOTAS, PAGADO

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
