package com.copanexus.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transacoes")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private Double valor;

    @Column(nullable = false)
    private String tipo; //recebe "buy" ou "sell"

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime dataTransacao;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @PrePersist
    protected void onCreate() {
        this.dataTransacao = LocalDateTime.now(); // Salva a data e hora exata automaticamente
    }
}