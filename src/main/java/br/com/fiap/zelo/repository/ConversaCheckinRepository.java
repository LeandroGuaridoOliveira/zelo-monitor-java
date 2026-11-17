package br.com.fiap.zelo.repository;

import br.com.fiap.zelo.model.ConversaCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversaCheckinRepository extends JpaRepository<ConversaCheckin, UUID> {
    List<ConversaCheckin> findByAlertaIdOrderByCriadoEmAsc(UUID alertaId);
    List<ConversaCheckin> findByCasaIdOrderByCriadoEmDesc(UUID casaId);
}
