package com.payflow.backend.dto;

// DTO de saída: envia ao React apenas o crachá digital (token) e dados seguros de exibição
public record AuthResponse(
        String token,
        String nome,
        String numeroConta
) {}