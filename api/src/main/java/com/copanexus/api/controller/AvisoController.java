package com.copanexus.api.controller;

import com.copanexus.api.model.Aviso;
import com.copanexus.api.repository.AvisoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avisos")
public class AvisoController {

    @Autowired
    private AvisoRepository repository;

    @Value("${admin.api.key}")
    private String CHAVE_MESTRE;

    @GetMapping
    public ResponseEntity<List<Aviso>> listar() {
        // Pega os 20 últimos posts
        return ResponseEntity.ok(repository.findAllByOrderByDataCriacaoDesc(PageRequest.of(0, 20)));
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestHeader("X-Admin-Key") String key, @RequestBody Aviso aviso) {
        if (!CHAVE_MESTRE.equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chave inválida!");
        }
        Aviso salvo = repository.save(aviso);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}