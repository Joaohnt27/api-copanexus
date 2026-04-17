package com.copanexus.api.repository;

import com.copanexus.api.model.Aviso;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {
    // Busca ordenada para o feed ficar sempre com a mais nova no topo
    List<Aviso> findAllByOrderByDataCriacaoDesc(Pageable pageable);
}