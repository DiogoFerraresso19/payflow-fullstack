package com.payflow.backend.security;

import com.payflow.backend.model.Usuario;
import com.payflow.backend.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Busca o cabeçalho "Authorization" da requisição HTTP
        final String authHeader = request.getHeader("Authorization");

        // 2. Se não houver cabeçalho ou não começar com "Bearer ", segue sem autenticar
        // (O SecurityConfig decidirá depois se a rota exige login ou se é pública)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Remove o prefixo "Bearer " (7 caracteres) para isolar apenas o token
        final String jwt = authHeader.substring(7);
        final String userEmail = jwtService.extrairEmail(jwt);

        // 4. Se extraiu um e-mail e o usuário ainda não está autenticado no contexto atual
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Usuario usuario = usuarioRepository.findByEmail(userEmail).orElse(null);

            // 5. Valida o token e registra o usuário autenticado no Spring Security
            if (usuario != null && jwtService.isTokenValido(jwt, usuario.getEmail())) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        usuario, // Guarda o próprio objeto Usuario logado para usarmos nos Controllers!
                        null,
                        Collections.emptyList()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 6. Passa a requisição adiante para o Controller
        filterChain.doFilter(request, response);
    }
}