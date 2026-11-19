package com.payflow.backend.service;

import com.payflow.backend.dto.AuthResponse;
import com.payflow.backend.dto.LoginRequest;
import com.payflow.backend.dto.RegisterRequest;
import com.payflow.backend.model.ChavePix;
import com.payflow.backend.model.Conta;
import com.payflow.backend.model.Transacao;
import com.payflow.backend.model.Usuario;
import com.payflow.backend.model.enums.TipoChavePix;
import com.payflow.backend.model.enums.TipoTransacao;
import com.payflow.backend.repository.ChavePixRepository;
import com.payflow.backend.repository.ContaRepository;
import com.payflow.backend.repository.TransacaoRepository;
import com.payflow.backend.repository.UsuarioRepository;
import com.payflow.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Service // Indica ao Spring que esta classe contém regras de negócio e pode ser injetada em outras
@RequiredArgsConstructor // Lombok gera automaticamente o construtor para todos os campos "final" (Injeção de Dependência)
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ContaRepository contaRepository;
    private final ChavePixRepository chavePixRepository;
    private final TransacaoRepository transacaoRepository;
    private final PasswordEncoder passwordEncoder; // Bean do BCrypt configurado no SecurityConfig
    private final JwtService jwtService;

    /**
     * @Transactional: Garante a atomicidade (tudo ou nada).
     * Como este método salva em 4 tabelas diferentes (Usuario, Conta, Transacao e ChavePix),
     * se ocorrer qualquer erro na 3ª ou 4ª etapa, o banco desfaz automaticamente as anteriores (Rollback),
     * evitando que exista um usuário sem conta ou uma conta sem histórico de saldo inicial.
     */
    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        // 1. Regras de unicidade: evita erro bruto de constraint do banco e dá mensagem clara
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }
        if (usuarioRepository.existsByCpf(request.cpf())) {
            throw new IllegalArgumentException("Este CPF já está cadastrado.");
        }

        // 2. Criação do Usuário: a senha nunca é salva em texto puro; o BCrypt gera um hash irreversível
        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .cpf(request.cpf())
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .build();
        usuarioRepository.save(usuario);

        // 3. Criação da Conta: usamos BigDecimal com String ("1000.00") para evitar falhas de arredondamento binário do Double
        BigDecimal saldoInicial = new BigDecimal("1000.00");
        Conta conta = Conta.builder()
                .numeroConta(gerarNumeroConta())
                .saldo(saldoInicial)
                .usuario(usuario)
                .build();
        contaRepository.save(conta);

        // 4. Registro no Ledger (Livro-Razão): toda entrada de dinheiro precisa ter origem rastreável.
        // Como é um bônus de boas-vindas do banco, a contaOrigem fica nula (null) e o tipo é DEPOSITO.
        Transacao depositoInicial = Transacao.builder()
                .idempotencyKey("INIT-" + UUID.randomUUID()) // Chave única obrigatória na tabela transacoes
                .contaOrigem(null)
                .contaDestino(conta)
                .valor(saldoInicial)
                .tipo(TipoTransacao.DEPOSITO)
                .build();
        transacaoRepository.save(depositoInicial);

        // 5. Chave Pix padrão: já cadastra o CPF do usuário para ele poder receber transferências imediatamente
        ChavePix chaveCpf = ChavePix.builder()
                .chave(usuario.getCpf())
                .tipo(TipoChavePix.CPF)
                .conta(conta)
                .build();
        chavePixRepository.save(chaveCpf);

        // 6. Emite o Token JWT para o usuário já sair autenticado após o cadastro
        String token = jwtService.gerarToken(usuario.getEmail());
        return new AuthResponse(token, usuario.getNome(), conta.getNumeroConta());
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Busca o usuário pelo e-mail; se não achar, lança exceção genérica (por segurança, não revelamos se foi o e-mail ou a senha que errou)
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos."));

        // 2. O BCrypt pega a senha digitada, aplica o mesmo algoritmo e compara com o hash salvo no banco
        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new IllegalArgumentException("E-mail ou senha inválidos.");
        }

        // 3. Recupera os dados da conta para exibir o número da conta no dashboard do React
        Conta conta = contaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("Conta não encontrada para este usuário."));

        // 4. Gera um novo token válido por 24h e devolve ao frontend
        String token = jwtService.gerarToken(usuario.getEmail());
        return new AuthResponse(token, usuario.getNome(), conta.getNumeroConta());
    }

    // Método auxiliar privado para gerar um número de conta único de 6 dígitos
    private String gerarNumeroConta() {
        int numero = 100000 + new Random().nextInt(900000); // Gera número entre 100000 e 999999
        String numeroStr = String.valueOf(numero);
        
        // Enquanto existir alguma conta com esse número no banco, sorteia outro
        while (contaRepository.findByNumeroConta(numeroStr).isPresent()) {
            numero = 100000 + new Random().nextInt(900000);
            numeroStr = String.valueOf(numero);
        }
        return numeroStr;
    }
}