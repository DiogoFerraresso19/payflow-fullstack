package com.payflow.backend.service;

import com.payflow.backend.dto.DashboardResponse;
import com.payflow.backend.dto.TransacaoResponse;
import com.payflow.backend.dto.TransferenciaRequest;
import com.payflow.backend.model.ChavePix;
import com.payflow.backend.model.Conta;
import com.payflow.backend.model.Transacao;
import com.payflow.backend.model.Usuario;
import com.payflow.backend.model.enums.TipoTransacao;
import com.payflow.backend.repository.ChavePixRepository;
import com.payflow.backend.repository.ContaRepository;
import com.payflow.backend.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PixService {

    private final ContaRepository contaRepository;
    private final ChavePixRepository chavePixRepository;
    private final TransacaoRepository transacaoRepository;

    /**
     * Carrega todos os dados da tela principal (Saldo, Chaves Pix e Extrato)
     */
    @Transactional(readOnly = true) // Otimiza a consulta no banco pois não haverá escrita
    public DashboardResponse obterDashboard(Usuario usuarioLogado) {
        Conta conta = contaRepository.findByUsuarioId(usuarioLogado.getId())
                .orElseThrow(() -> new IllegalStateException("Conta não encontrada."));

        // Busca todas as chaves Pix vinculadas à conta
        List<String> chaves = chavePixRepository.findByContaId(conta.getId())
                .stream()
                .map(ChavePix::getChave)
                .toList();

        // Busca todas as movimentações onde a conta participou (enviando ou recebendo)
        List<TransacaoResponse> extrato = transacaoRepository.findExtratoByContaId(conta.getId())
                .stream()
                .map(t -> mapearParaResponse(t, conta.getId()))
                .toList();

        return new DashboardResponse(
                usuarioLogado.getNome(),
                conta.getNumeroConta(),
                conta.getSaldo(),
                chaves,
                extrato
        );
    }

    /**
     * Realiza uma transferência Pix com blindagem contra duplicidade e concorrência.
     */
    @Transactional // Garante que débito, crédito e registro no Ledger ocorram juntos ou falhem juntos
    public TransacaoResponse transferir(Usuario usuarioLogado, String idempotencyKey, TransferenciaRequest request) {
        // 1. PROTEÇÃO DE IDEMPOTÊNCIA:
        // Se o cliente clicar 2x no botão ou a internet oscilar e reenviar a mesma requisição,
        // encontramos a chave já salva e devolvemos o recibo anterior sem debitar o saldo novamente!
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("O cabeçalho Idempotency-Key é obrigatório.");
        }
        Optional<Transacao> transacaoExistente = transacaoRepository.findByIdempotencyKey(idempotencyKey);
        if (transacaoExistente.isPresent()) {
            Conta contaOrigem = contaRepository.findByUsuarioId(usuarioLogado.getId()).orElseThrow();
            return mapearParaResponse(transacaoExistente.get(), contaOrigem.getId());
        }

        // 2. Busca a conta de origem do usuário logado e a chave Pix de destino
        Conta contaOrigemSemLock = contaRepository.findByUsuarioId(usuarioLogado.getId())
                .orElseThrow(() -> new IllegalStateException("Conta de origem não encontrada."));

        ChavePix chaveDestino = chavePixRepository.findByChave(request.chavePixDestino())
                .orElseThrow(() -> new IllegalArgumentException("Chave Pix de destino não encontrada."));

        Conta contaDestinoSemLock = chaveDestino.getConta();

        // 3. Regra de negócio: impede transferir para si mesmo
        if (contaOrigemSemLock.getId().equals(contaDestinoSemLock.getId())) {
            throw new IllegalArgumentException("Você não pode fazer uma transferência Pix para a sua própria conta.");
        }

        // 4. BLOQUEIO PESSIMISTA (PESSIMISTIC_WRITE) + PREVENÇÃO DE DEADLOCK:
        // Travamos as duas linhas no PostgreSQL ("SELECT ... FOR UPDATE") para que nenhuma outra
        // requisição simultânea altere o saldo enquanto esta transferência não terminar.
        // Ordenamos os IDs para que, se A transferir para B ao mesmo tempo que B transfere para A,
        // o banco trave sempre na mesma ordem e nunca ocorra Deadlock!
        UUID primeiroId = contaOrigemSemLock.getId().compareTo(contaDestinoSemLock.getId()) < 0
                ? contaOrigemSemLock.getId() : contaDestinoSemLock.getId();
        UUID segundoId = primeiroId.equals(contaOrigemSemLock.getId())
                ? contaDestinoSemLock.getId() : contaOrigemSemLock.getId();

        contaRepository.findByIdWithLock(primeiroId);
        contaRepository.findByIdWithLock(segundoId);

        // Recarrega as contas já devidamente travadas
        Conta contaOrigem = contaRepository.findById(contaOrigemSemLock.getId()).orElseThrow();
        Conta contaDestino = contaRepository.findById(contaDestinoSemLock.getId()).orElseThrow();

        // 5. Valida se há saldo suficiente usando compareTo (padrão seguro para BigDecimal)
        // compareTo retorna < 0 quando o saldo é menor que o valor solicitado
        if (contaOrigem.getSaldo().compareTo(request.valor()) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar a transferência.");
        }

        // 6. Atualiza os saldos (BigDecimal é imutável, por isso reatribuímos com subtract e add)
        contaOrigem.setSaldo(contaOrigem.getSaldo().subtract(request.valor()));
        contaDestino.setSaldo(contaDestino.getSaldo().add(request.valor()));

        contaRepository.save(contaOrigem);
        contaRepository.save(contaDestino);

        // 7. Grava o registro imutável no Ledger (Livro-Razão)
        Transacao transacao = Transacao.builder()
                .idempotencyKey(idempotencyKey)
                .contaOrigem(contaOrigem)
                .contaDestino(contaDestino)
                .valor(request.valor())
                .tipo(TipoTransacao.TRANSFERENCIA)
                .build();

        transacaoRepository.save(transacao);
        return mapearParaResponse(transacao, contaOrigem.getId());
    }

    /**
     * Estorna uma transferência recebida gerando uma transação inversa (nunca apaga o histórico).
     */
    @Transactional
    public TransacaoResponse estornar(Usuario usuarioLogado, UUID transacaoId) {
        Conta minhaConta = contaRepository.findByUsuarioId(usuarioLogado.getId())
                .orElseThrow(() -> new IllegalStateException("Conta não encontrada."));

        Transacao original = transacaoRepository.findById(transacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada."));

        // Validações de segurança do estorno
        if (original.getTipo() != TipoTransacao.TRANSFERENCIA) {
            throw new IllegalArgumentException("Apenas transferências podem ser estornadas.");
        }
        if (!original.getContaDestino().getId().equals(minhaConta.getId())) {
            throw new IllegalArgumentException("Você só pode estornar transferências que recebeu.");
        }
        if (transacaoRepository.existsByTransacaoOriginalId(original.getId())) {
            throw new IllegalArgumentException("Esta transferência já foi estornada anteriormente.");
        }

        // Trava as contas no banco antes de devolver o dinheiro
        Conta contaQueDevolve = contaRepository.findByIdWithLock(minhaConta.getId()).orElseThrow();
        Conta contaQueRecebeDeVolta = contaRepository.findByIdWithLock(original.getContaOrigem().getId()).orElseThrow();

        if (contaQueDevolve.getSaldo().compareTo(original.getValor()) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar o estorno.");
        }

        // Movimenta os valores de volta
        contaQueDevolve.setSaldo(contaQueDevolve.getSaldo().subtract(original.getValor()));
        contaQueRecebeDeVolta.setSaldo(contaQueRecebeDeVolta.getSaldo().add(original.getValor()));

        contaRepository.save(contaQueDevolve);
        contaRepository.save(contaQueRecebeDeVolta);

        // Cria o novo registro de ESTORNO apontando para a transação original
        Transacao estorno = Transacao.builder()
                .idempotencyKey("ESTORNO-" + original.getId())
                .contaOrigem(contaQueDevolve)
                .contaDestino(contaQueRecebeDeVolta)
                .valor(original.getValor())
                .tipo(TipoTransacao.ESTORNO)
                .transacaoOriginalId(original.getId())
                .build();

        transacaoRepository.save(estorno);
        return mapearParaResponse(estorno, contaQueDevolve.getId());
    }

    // Método auxiliar que converte a Entidade Transacao no DTO seguro para o React
    private TransacaoResponse mapearParaResponse(Transacao t, UUID minhaContaId) {
        boolean souDestino = t.getContaDestino().getId().equals(minhaContaId);
        String direcao = souDestino ? "ENTRADA" : "SAIDA";

        String nomeContraparte;
        if (t.getTipo() == TipoTransacao.DEPOSITO) {
            nomeContraparte = "Bônus de Boas-Vindas PayFlow";
        } else if (souDestino) {
            nomeContraparte = t.getContaOrigem().getUsuario().getNome();
        } else {
            nomeContraparte = t.getContaDestino().getUsuario().getNome();
        }

        boolean jaEstornada = transacaoRepository.existsByTransacaoOriginalId(t.getId());

        return new TransacaoResponse(
                t.getId(),
                t.getTipo(),
                t.getValor(),
                direcao,
                nomeContraparte,
                t.getDataHora(),
                jaEstornada
        );
    }
}
