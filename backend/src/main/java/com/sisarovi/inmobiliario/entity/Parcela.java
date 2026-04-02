package com.sisarovi.inmobiliario.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "parcelas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parcela {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false)
    private Integer numManzanas;
    @Column(name = "num_lotes", nullable = false)
    @Builder.Default
    private Integer numLotes = 0;
    @Column(nullable = false, length = 100)
    private String propietario;
    @Column(name = "lotes_disponibles", nullable = false)
    @Builder.Default
    private Integer lotesDisponibles = 0;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id", nullable = false)
    @JsonBackReference
    private Etapa etapa;
    @OneToMany(mappedBy = "parcela", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<Lote> lotes = new ArrayList<>();
    public int getCantidadLotes() {
        return lotes != null ? lotes.size() : 0;
    }
}
