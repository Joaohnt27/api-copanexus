package com.copanexus.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@Entity
@Table(name = "users") public class Usuario {

    @PreRemove
    private void removerVinculos() {
        // Antes do Hibernate deletar o usuário, ele limpa os vínculos na tabela 'usuario_figurinhas'
        this.colecao.clear();
        this.repetidas.clear();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nome;

    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Size(min = 6)
    @Column(nullable = false)
    private String senha;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_figurinhas", // Nome da tabela que o Hibernate vai criar no Neon
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "figurinha_id")
    )
    private Set<Figurinhas> colecao = new HashSet<>();

    // Repetidas
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "usuario_repetidas", // O Java vai criar essa tabela no Neon sozinho!
            joinColumns = @JoinColumn(name = "usuario_id")
    )
    @MapKeyColumn(name = "figurinha_numero") // A Chave (Qual é a figurinha)
    @Column(name = "quantidade")             // O Valor (Quantas ele tem repetida)
    private Map<Integer, Integer> repetidas = new HashMap<>();

    public Usuario() {}
}

