package com.payflow.backend.dto;

import com.payflow.backend.model.enums.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Representa cada item do extrato exibido no React
public record TransacaoResponse(
        UUID id,
        TipoTransacao tipo,
        BigDecimal valor,
        String direcao, // "ENTRADA" ou "SAIDA" para facilitar a cor verde/vermelha no React
        String nomeContraparte, // Nome de quem enviou ou recebeu
        LocalDateTime dataHora,
        boolean jaEstornada
) {}
