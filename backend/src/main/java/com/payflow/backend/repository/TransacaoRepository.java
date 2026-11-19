package com.payflow.backend.repository;

import com.payflow.backend.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {
    Optional<Transacao> findByIdempotencyKey(String idempotencyKey);
    boolean existsByTransacaoOriginalId(UUID transacaoOriginalId);

    @Query("SELECT t FROM Transacao t WHERE t.contaOrigem.id = :contaId OR t.contaDestino.id = :contaId ORDER BY t.dataHora DESC")
    List<Transacao> findExtratoByContaId(@Param("contaId") UUID contaId);
}