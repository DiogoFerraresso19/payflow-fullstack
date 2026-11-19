package com.payflow.backend.dto;

import java.math.BigDecimal;
import java.util.List;

// Devolve tudo que a tela principal do React precisa em uma única chamada
public record DashboardResponse(
        String nomeUsuario,
        String numeroConta,
        BigDecimal saldo,
        List<String> chavesPix,
        List<TransacaoResponse> extrato
) {

}
 
