package com.copanexus.api.repository;

import com.copanexus.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Método customizado para buscar por e-mail (usaremos no Login)
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail (String email);
}