package com.copanexus.api.controller;

import com.copanexus.api.model.Transacao;
import com.copanexus.api.model.Usuario;
import com.copanexus.api.repository.TransacaoRepository;
import com.copanexus.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios/{usuarioId}/transacoes")
@CrossOrigin(origins = "http://localhost:5173")
public class TransacaoController {

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // BUSCAR EXTRATO
    @GetMapping
    public ResponseEntity<List<Transacao>> listarExtrato(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(transacaoRepository.findByUsuarioIdOrderByDataTransacaoDesc(usuarioId));
    }

    // ADICIONAR NOVA TRANSAÇÃO
    @PostMapping
    public ResponseEntity<?> criarTransacao(@PathVariable Long usuarioId, @RequestBody Transacao transacao) {
        return usuarioRepository.findById(usuarioId).map(usuario -> {
            transacao.setUsuario(usuario);
            Transacao salva = transacaoRepository.save(transacao);
            return ResponseEntity.ok(salva);
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETAR TRANSAÇÃO (O ícone de lixeira no seu front)
    @DeleteMapping("/{transacaoId}")
    public ResponseEntity<?> deletarTransacao(@PathVariable Long usuarioId, @PathVariable Long transacaoId) {
        if(transacaoRepository.existsById(transacaoId)) {
            transacaoRepository.deleteById(transacaoId);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}