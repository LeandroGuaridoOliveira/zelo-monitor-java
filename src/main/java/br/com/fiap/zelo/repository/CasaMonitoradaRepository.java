package br.com.fiap.zelo.repository;

import br.com.fiap.zelo.model.CasaMonitorada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CasaMonitoradaRepository extends JpaRepository<CasaMonitorada, UUID> {
    Optional<CasaMonitorada> findByIdosoId(UUID idosoId);
}
