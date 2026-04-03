package com.copanexus.api.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "figurinhas")
public class Figurinhas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Integer numero; // De 0 a 979

    public Figurinhas() {}

    public Figurinhas(Integer numero) {
        this.numero = numero;
    }
}