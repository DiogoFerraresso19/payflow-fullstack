package com.payflow.backend.repository;

import com.payflow.backend.model.ChavePix;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChavePixRepository extends JpaRepository<ChavePix, UUID> {
    Optional<ChavePix> findByChave(String chave);
    List<ChavePix> findByContaId(UUID contaId);
    boolean existsByChave(String chave);
}