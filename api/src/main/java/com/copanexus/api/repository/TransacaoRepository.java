package com.copanexus.api.repository;

import com.copanexus.api.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    // Traz o extrato do usuário já ordenado do mais recente para o mais antigo
    List<Transacao> findByUsuarioIdOrderByDataTransacaoDesc(Long usuarioId);
}