package com.copanexus.api.dto;

public record UsuarioUpdateDTO (
        String nome,
        String email,
        String senhaAntiga,
        String novaSenha
){}
