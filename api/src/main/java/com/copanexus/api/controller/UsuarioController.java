package com.copanexus.api.controller;

import com.copanexus.api.dto.UsuarioResponseDTO;
import com.copanexus.api.dto.UsuarioUpdateDTO;
import com.copanexus.api.model.Figurinhas;
import com.copanexus.api.model.Usuario;
import com.copanexus.api.repository.FigurinhasRepository;
import com.copanexus.api.repository.UsuarioRepository;
import com.copanexus.api.security.TokenService;
import com.copanexus.api.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Autowired
    private EmailService emailService;

    @Autowired
    private TokenService tokenService; // Isso mata o erro de "Cannot resolve symbol"

    // ROTA: GET http://localhost:8080/api/usuarios
    @GetMapping
    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    private static final List<String> DOMINIOS_PROIBIDOS = List.of(
            "teste.com",
            "abc.com",
            "123.com",
            "email.com",
            "mailinator.com",
            "tempmail.com",
            "exemplo.com"
    );

    private boolean isEmailValido(String email) {
        if (email == null || !email.contains("@")) return false;

        // Pega tudo que vem depois do @
        String dominio = email.substring(email.indexOf("@") + 1).toLowerCase();

        // Verifica se o domínio está na lista negra
        return !DOMINIOS_PROIBIDOS.contains(dominio);
    }

    // ROTA: POST http://localhost:8080/api/usuarios/registrar
        @PostMapping("/registrar")
        public ResponseEntity<?> registrar(@Valid @RequestBody Usuario usuario) {
            if (!isEmailValido(usuario.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Este domínio de e-mail não é permitido. Use um e-mail válido.");
            }

            // Verifica se o e-mail já existe
            if (repository.findByEmail(usuario.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT) // 409 é melhor que 400 para duplicados
                        .body("Este e-mail já está cadastrado no sistema.");
            }

            // Criptografia
            usuario.setSenha(encoder.encode(usuario.getSenha()));

            // Salva no banco
            Usuario salvo = repository.save(usuario);

            try {
                emailService.enviarEmailBoasVindas(salvo.getEmail(), salvo.getNome());
                System.out.println("E-mail enviado com sucesso!");
            } catch (Exception e) {
                System.err.println("Aviso: Falha ao enviar e-mail de boas-vindas - " + e.getMessage());
            }

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
                // GERA O TOKEN
                String token = tokenService.gerarToken(usuario);

                // Retorna um mapa com o token e os dados básicos
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("usuario", UsuarioResponseDTO.transformar(usuario));

                return ResponseEntity.ok(response);
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

    // ROTA: DELETE http://localhost:8080/api/usuarios/{id}/descolar/{numero}
    @DeleteMapping("/{id}/descolar/{numero}")
    @Transactional
    public ResponseEntity<?> descolarFigurinha(@PathVariable Long id, @PathVariable Integer numero) {
        return repository.findById(id).map(usuario -> {
            // Remove da lista comparando pelo número da figurinha
            boolean removido = usuario.getColecao().removeIf(f -> f.getNumero().equals(numero));

            if (removido) {
                repository.save(usuario);
                return ResponseEntity.ok("Figurinha removida da coleção.");
            }
            return ResponseEntity.badRequest().body("Figurinha não encontrada na coleção.");
        }).orElse(ResponseEntity.notFound().build());
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

            // Proteção contra Null: Só processa se o nome não for nulo
            if (dto.nome() != null && !dto.nome().isBlank()) {
                usuario.setNome(dto.nome().replaceAll("<[^>]*>", ""));
            }

            // SEGURANÇA: Se quer mudar e-mail ou senha, EXIGE a senha antiga
            boolean querMudarEmail = dto.email() != null && !dto.email().isBlank() && !dto.email().equals(usuario.getEmail());
            boolean querMudarSenha = dto.novaSenha() != null && !dto.novaSenha().isBlank();

            if (querMudarEmail || querMudarSenha) {
                // Se não mandou a senha antiga ou ela não bate, bloqueia!
                if (dto.senhaAntiga() == null || !encoder.matches(dto.senhaAntiga(), usuario.getSenha())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Senha atual necessária para alterar dados sensíveis.");
                }

                if (querMudarEmail) {
                    if (repository.findByEmail(dto.email()).isPresent()) {
                        return ResponseEntity.badRequest().body("E-mail já cadastrado.");
                    }
                    usuario.setEmail(dto.email());
                }

                if (querMudarSenha) {
                    usuario.setSenha(encoder.encode(dto.novaSenha()));
                }
            }

            repository.save(usuario);
            return ResponseEntity.ok("Perfil atualizado!");

        }).orElse(ResponseEntity.notFound().build());
    }

    // ROTA: DELETE http://localhost:8080/api/usuarios/{id}
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deletarConta(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Optional<Usuario> usuarioOpt = repository.findById(id);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Usuario usuario = usuarioOpt.get();
            String senhaDigitada = request.get("senha");

            // Trava de Segurança
            if (senhaDigitada == null || !encoder.matches(senhaDigitada, usuario.getSenha())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Senha atual incorreta.");
            }

            // Limpa a mochila de figurinhas
            usuario.getColecao().clear();
            usuario.getRepetidas().clear();

            // O 'saveAndFlush' FORÇA o banco a deletar as relações AGORA MESMO!
            repository.saveAndFlush(usuario);

            // Agora que o usuário está limpo, o banco deixa dar o tiro final
            repository.delete(usuario);

            return ResponseEntity.ok("Conta excluída com sucesso.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro fatal no banco: " + e.getMessage());
        }
    }

    // ROTA: GET http://localhost:8080/api/usuarios/{id}/repetidas
    @GetMapping("/{id}/repetidas")
    public ResponseEntity<Map<Integer, Integer>> buscarRepetidas(@PathVariable Long id) {
        return repository.findById(id)
                .map(usuario -> ResponseEntity.ok(usuario.getRepetidas()))
                .orElse(ResponseEntity.notFound().build());
    }

    // ROTA: POST http://localhost:8080/api/usuarios/{id}/repetidas/adicionar/{numero}
    @PostMapping("/{id}/repetidas/adicionar/{numero}")
    @Transactional
    public ResponseEntity<?> adicionarRepetida(@PathVariable Long id, @PathVariable Integer numero) {
        return repository.findById(id).map(usuario -> {
            Map<Integer, Integer> repetidas = usuario.getRepetidas();

            repetidas.put(numero, repetidas.getOrDefault(numero, 0) + 1);

            // Força o Java a enfiar isso no banco NA MESMA HORA
            repository.saveAndFlush(usuario);

            return ResponseEntity.ok(repetidas);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ROTA: POST http://localhost:8080/api/usuarios/{id}/repetidas/remover/{numero}
    @PostMapping("/{id}/repetidas/remover/{numero}")
    @Transactional
    public ResponseEntity<?> removerRepetida(@PathVariable Long id, @PathVariable Integer numero) {
        return repository.findById(id).map(usuario -> {
            Map<Integer, Integer> repetidas = usuario.getRepetidas();

            if (repetidas.containsKey(numero)) {
                int qtdAtual = repetidas.get(numero);

                if (qtdAtual <= 1) {
                    // Se só tem 1 e o usuário quer remover, a gente apaga a figurinha completamente da tabela
                    repetidas.remove(numero);
                } else {
                    // Se tem mais de 1, apenas diminui a quantidade
                    repetidas.put(numero, qtdAtual - 1);
                }

                repository.save(usuario);
                return ResponseEntity.ok(repetidas);
            }

            return ResponseEntity.badRequest().body("Figurinha não encontrada nas repetidas.");
        }).orElse(ResponseEntity.notFound().build());
    }

    // ROTA 1: O usuário digita o e-mail e pede o código
    @PostMapping("/esqueci-senha")
    public ResponseEntity<?> solicitarRecuperacao(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        return repository.findByEmail(email).map(usuario -> {
            // Gera um código de 6 dígitos aleatório
            String codigo = String.format("%06d", new java.util.Random().nextInt(999999));

            // Salva o código no banco e diz que vale por 15 minutos
            usuario.setCodigoRecuperacao(codigo);
            usuario.setValidadeCodigo(LocalDateTime.now().plusMinutes(15));
            repository.save(usuario);

            // Dispara o e-mail (usando try/catch para não quebrar a API)
            try {
                emailService.enviarEmailRecuperacaoSenha(usuario.getEmail(), codigo);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Erro ao enviar e-mail.");
            }

            return ResponseEntity.ok("Código de recuperação enviado para o e-mail.");
        }).orElse(ResponseEntity.badRequest().body("Se este e-mail existir, um código foi enviado."));
        // Dica de segurança: Nunca diga se o e-mail existe ou não, para evitar hackers testando e-mails!
    }

    // ROTA 2: O usuário digita o código e a senha nova
    @PostMapping("/redefinir-senha")
    public ResponseEntity<?> redefinirSenha(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String codigoDigitado = request.get("codigo");
        String novaSenha = request.get("novaSenha");

        return repository.findByEmail(email).map(usuario -> {
            // Verifica se tem código gerado
            if (usuario.getCodigoRecuperacao() == null || !usuario.getCodigoRecuperacao().equals(codigoDigitado)) {
                return ResponseEntity.badRequest().body("Código inválido.");
            }

            // Verifica se o código expirou
            if (LocalDateTime.now().isAfter(usuario.getValidadeCodigo())) {
                return ResponseEntity.badRequest().body("Este código expirou. Solicite um novo.");
            }

            // Tudo certo! Criptografa a nova senha e apaga o código usado
            usuario.setSenha(encoder.encode(novaSenha));
            usuario.setCodigoRecuperacao(null);
            usuario.setValidadeCodigo(null);
            repository.save(usuario);

            return ResponseEntity.ok("Senha redefinida com sucesso!");
        }).orElse(ResponseEntity.badRequest().body("Usuário não encontrado."));
    }
}