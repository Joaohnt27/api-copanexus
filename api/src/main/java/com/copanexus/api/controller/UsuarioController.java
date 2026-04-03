package com.copanexus.api.controller;

import com.copanexus.api.model.Figurinhas;
import com.copanexus.api.model.Usuario;
import com.copanexus.api.repository.FigurinhasRepository;
import com.copanexus.api.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*") // Libera o acesso para o Front-end React
public class UsuarioController {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private FigurinhasRepository figurinhasRepository;

    @Autowired
    private PasswordEncoder encoder;

    // ROTA: GET http://localhost:8080/api/usuarios
    @GetMapping
    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    // ROTA: POST http://localhost:8080/api/usuarios/registrar
    @PostMapping("/registrar")
    public ResponseEntity<Usuario> registrar(@Valid @RequestBody Usuario usuario) {
        // Verifica se o e-mail já existe
        if (repository.findByEmail(usuario.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Criptografia
        String senhaCriptografada = encoder.encode(usuario.getSenha());
        usuario.setSenha(senhaCriptografada);

        Usuario salvo = repository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario loginData) {
        // Busca o usuário pelo e-mail
        Optional<Usuario> usuarioOpt = repository.findByEmail(loginData.getEmail());

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            // BCrypt compara a senha pura com o Hash do banco
            if (encoder.matches(loginData.getSenha(), usuario.getSenha())) {
                // Retorna os dados do usuário (menos a senha por segurança)
                usuario.setSenha(null);
                return ResponseEntity.ok(usuario);
            }
        }

        // Se o e-mail não existir ou a senha estiver errada
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("E-mail ou senha inválidos.");
    }

    // ROTA: GET http://localhost:8080/api/usuarios/{id}/colecao
    @GetMapping("/{id}/colecao")
    public ResponseEntity<Set<Integer>> buscarColecao(@PathVariable Long id) {
        return repository.findById(id)
                .map(usuario -> {
                    Set<Integer> numerosObtidos = usuario.getColecao().stream()
                            .map(Figurinhas::getNumero)
                            .collect(Collectors.toSet());
                    return ResponseEntity.ok(numerosObtidos);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ROTA: POST http://localhost:8080/api/usuarios/{id}/colar/{numero}
    @PostMapping("/{id}/colar/{numero}")
    public ResponseEntity<?> colarFigurinha(@PathVariable Long id, @PathVariable Integer numero) {
        var usuarioOpt = repository.findById(id);
        var figOpt = figurinhasRepository.findByNumero(numero);

        if (usuarioOpt.isPresent() && figOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            u.getColecao().add(figOpt.get());
            repository.save(u);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("Erro ao colar figurinha.");
    }
}