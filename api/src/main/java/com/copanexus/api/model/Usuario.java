package com.copanexus.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class Usuario implements UserDetails {

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
    private String codigoRecuperacao;

    @JsonIgnore
    private LocalDateTime validadeCodigo;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_figurinhas",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "figurinha_id")
    )
    private Set<Figurinhas> colecao = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "usuario_repetidas",
            joinColumns = @JoinColumn(name = "usuario_id")
    )
    @MapKeyColumn(name = "figurinha_numero")
    @Column(name = "quantidade")
    private Map<Integer, Integer> repetidas = new HashMap<>();

    @JsonIgnore
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transacao> transacoes;

    @Override
    @JsonIgnore // Evita que as authorities poluam o JSON de resposta
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Define que todo usuário logado tem a permissão básica "USER"
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Conta nunca expira
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Conta nunca bloqueia
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Senha nunca expira
    }

    @Override
    public boolean isEnabled() {
        return true; // Usuário sempre ativo
    }

    @PreRemove
    private void removerVinculos() {
        // Limpa os registros nas tabelas de relacionamento antes de deletar o usuário
        this.colecao.clear();
        this.repetidas.clear();
    }
}