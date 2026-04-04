package com.copanexus.api.controller;

import com.copanexus.api.dto.UsuarioResponseDTO;
import com.copanexus.api.dto.UsuarioUpdateDTO;
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

import java.util.*;
import java.util.stream.Collectors;

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
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody Usuario usuario) {
        // Verifica se o e-mail já existe
        if (repository.findByEmail(usuario.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Criptografia
        usuario.setSenha(encoder.encode(usuario.getSenha()));

        // Salva no banco
        Usuario salvo = repository.save(usuario);

        // Senha morre no banco e nunca retorna para o front
        UsuarioResponseDTO resposta = UsuarioResponseDTO.transformar(salvo);

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
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

    @GetMapping("/{id}/progresso")
    public ResponseEntity<Map<String, Object>> obterProgresso(@PathVariable Long id) {
        return repository.findById(id).map(usuario -> {
            int totalFigurinhas = 980;
            int minhasFigurinhas = usuario.getColecao().size();

            // Cálculo da porcentagem (Regra de 3 básica)
            double porcentagem = (double) minhasFigurinhas / totalFigurinhas * 100;

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", totalFigurinhas);
            stats.put("obtidas", minhasFigurinhas);
            stats.put("porcentagem", Math.round(porcentagem * 100.0) / 100.0);

            return ResponseEntity.ok(stats);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/perfil")
    public ResponseEntity<?> atualizarPerfil(@PathVariable Long id, @RequestBody UsuarioUpdateDTO dto) {
        return repository.findById(id).map(usuario -> {

            // 1. Sanitização de Nome (Previne XSS)
            if (dto.nome() != null && !dto.nome().isBlank()) {
                usuario.setNome(dto.nome().replaceAll("<[^>]*>", ""));
            }

            // 2. Validação de E-mail
            if (dto.email() != null && !dto.email().equals(usuario.getEmail())) {
                // Garante que o novo e-mail não pertence a outro usuário
                if (repository.findByEmail(dto.email()).isPresent()) {
                    return ResponseEntity.badRequest().body("E-mail já cadastrado por outro usuário.");
                }
                usuario.setEmail(dto.email());
            }

            // 3. Troca de Senha com Validação de Identidade
            if (dto.novaSenha() != null && !dto.novaSenha().isBlank()) {
                // O usuário TEM que mandar a senha atual para mudar para uma nova
                if (dto.senhaAntiga() == null || !encoder.matches(dto.senhaAntiga(), usuario.getSenha())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Senha atual incorreta.");
                }

                // Valida o tamanho mínimo da nova senha
                if (dto.novaSenha().length() < 6) {
                    return ResponseEntity.badRequest().body("A nova senha deve ter no mínimo 6 caracteres.");
                }

                usuario.setSenha(encoder.encode(dto.novaSenha()));
            }

            repository.save(usuario);
            return ResponseEntity.ok("Perfil atualizado com sucesso!");

        }).orElse(ResponseEntity.notFound().build());
    }
}