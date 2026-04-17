package com.copanexus.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "avisos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    private TipoAviso tipo;

    private String cidade;
    private String loja;

    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
    }
}