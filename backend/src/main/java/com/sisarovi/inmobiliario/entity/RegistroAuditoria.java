package com.sisarovi.inmobiliario.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String usuario;

    @Column(nullable = false)
    private String accion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column
    private String clienteNombre;

    @Column
    private String clienteDni;

    @Column
    private java.math.BigDecimal monto;

    @Column
    private String medios;

    @Column(length = 150)
    private String item;

    @Column
    private Integer loteNumero;

    @Column
    private String manzanaNombre;

    @Column
    private String parcelaNombre;

    @Column
    private Integer etapaNumero;

    @Column
    private String proyectoNombre;
}
