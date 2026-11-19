package com.payflow.backend.controller;

import com.payflow.backend.dto.AuthResponse;
import com.payflow.backend.dto.LoginRequest;
import com.payflow.backend.dto.RegisterRequest;
import com.payflow.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // Combina @Controller + @ResponseBody: converte automaticamente os retornos Java em JSON
@RequestMapping("/api/auth") // Define a URL base deste controlador (liberada como pública no SecurityConfig)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * @PostMapping("/register"): Mapeia requisições HTTP POST para /api/auth/register
     * @Valid: Ativa as validações que colocamos no RegisterRequest (@NotBlank, @Email, @Size)
     * @RequestBody: Converte o JSON enviado pelo React no objeto Java RegisterRequest
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registrar(request);
        // Retorna status HTTP 201 (Created), que é a boa prática REST quando um novo recurso é criado
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * @PostMapping("/login"): Mapeia requisições HTTP POST para /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        // Retorna status HTTP 200 (OK) com o token no corpo da resposta
        return ResponseEntity.ok(response);
    }
}