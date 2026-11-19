package com.payflow.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferenciaRequest(
        @NotBlank(message = "A chave Pix de destino é obrigatória")
        String chavePixDestino,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor mínimo para transferência é R$ 0,01")
        BigDecimal valor
) {

}
 
