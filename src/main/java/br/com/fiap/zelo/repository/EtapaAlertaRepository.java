package br.com.fiap.zelo.repository;

import br.com.fiap.zelo.model.EtapaAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EtapaAlertaRepository extends JpaRepository<EtapaAlerta, UUID> {
    List<EtapaAlerta> findByAlertaIdOrderByEnviadoEmAsc(UUID alertaId);
}
