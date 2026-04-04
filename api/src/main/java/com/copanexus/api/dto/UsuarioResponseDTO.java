package com.copanexus.api.dto;

import com.copanexus.api.model.Usuario;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email
) {
    public static UsuarioResponseDTO transformar(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}