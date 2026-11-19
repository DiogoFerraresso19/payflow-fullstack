package com.payflow.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// "record" é uma classe imutável do Java 17+ perfeita para transportar dados de entrada/saída
public record RegisterRequest(
        // @NotBlank impede que o campo venha nulo, vazio ("") ou só com espaços ("   ")
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        // @Size garante o tamanho exato da String antes mesmo de chegar na regra de negócio
        @NotBlank(message = "O CPF é obrigatório")
        @Size(min = 11, max = 11, message = "O CPF deve conter exatamente 11 dígitos numéricos")
        String cpf,

        // @Email valida se o texto segue o padrão usuario@dominio.com
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String senha
) {}