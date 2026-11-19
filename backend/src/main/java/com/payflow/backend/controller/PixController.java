package com.payflow.backend.controller;

import com.payflow.backend.dto.DashboardResponse;
import com.payflow.backend.dto.TransacaoResponse;
import com.payflow.backend.dto.TransferenciaRequest;
import com.payflow.backend.model.Usuario;
import com.payflow.backend.service.PixService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pix") // Todas as rotas aqui exigem o Header "Authorization: Bearer <token>"
@RequiredArgsConstructor
public class PixController {

    private final PixService pixService;

    // GET http://localhost:8080/api/pix/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> obterDashboard(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(pixService.obterDashboard(usuarioLogado));
    }

    // POST http://localhost:8080/api/pix/transferir
    @PostMapping("/transferir")
    public ResponseEntity<TransacaoResponse> transferir(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferenciaRequest request
    ) {
        TransacaoResponse recibo = pixService.transferir(usuarioLogado, idempotencyKey, request);
        return ResponseEntity.ok(recibo);
    }

    // POST http://localhost:8080/api/pix/estornar/{id}
    @PostMapping("/estornar/{id}")
    public ResponseEntity<TransacaoResponse> estornar(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PathVariable UUID id
    ) {
        TransacaoResponse recibo = pixService.estornar(usuarioLogado, id);
        return ResponseEntity.ok(recibo);
    }
}
