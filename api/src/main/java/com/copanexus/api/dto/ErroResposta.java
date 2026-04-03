package com.copanexus.api.dto;

public class ErroResposta {
    private String mensagem;
    private int status;
    private long timestamp;

    public ErroResposta(String mensagem, int status) {
        this.mensagem = mensagem;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }
    // Getters e Setters
}