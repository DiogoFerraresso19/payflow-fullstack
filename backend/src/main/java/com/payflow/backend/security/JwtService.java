package com.payflow.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // Lê os valores configurados no application.properties
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Converte a String secreta em uma chave criptográfica HMAC-SHA256
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // Gera um token JWT contendo o e-mail do usuário como "subject" (dono do token)
    public String gerarToken(String email) {
        return Jwts.builder()
                .subject(email) // Identificador principal do usuário no token
                .issuedAt(new Date(System.currentTimeMillis())) // Data de criação
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Data de expiração
                .signWith(getSigningKey()) // Assina o token para evitar falsificação
                .compact();
    }

    // Extrai o e-mail (subject) de dentro de um token recebido na requisição
    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    // Verifica se o token pertence ao e-mail esperado e se ainda não expirou
    public boolean isTokenValido(String token, String emailUsuario) {
        final String emailExtraido = extrairEmail(token);
        return (emailExtraido.equals(emailUsuario) && !isTokenExpirado(token));
    }

    private boolean isTokenExpirado(String token) {
        return extrairClaims(token).getExpiration().before(new Date());
    }

    // Faz o parse (leitura) do token validando a assinatura criptográfica
    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}