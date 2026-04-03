package com.copanexus.api.repository;

import com.copanexus.api.model.Figurinhas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FigurinhasRepository extends JpaRepository<Figurinhas, Long> {

    // Método para buscar a figurinha pelo número (ex: buscar a 10, a 979)
    Optional<Figurinhas> findByNumero(Integer numero);
}