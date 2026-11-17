package br.com.fiap.zelo.repository;

import br.com.fiap.zelo.enums.EstadoAlerta;
import br.com.fiap.zelo.model.Alerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, UUID> {

    Page<Alerta> findByCasaIdAndEstado(UUID casaId, EstadoAlerta estado, Pageable pageable);

    Page<Alerta> findByCasaId(UUID casaId, Pageable pageable);

    Page<Alerta> findByEstado(EstadoAlerta estado, Pageable pageable);

    Optional<Alerta> findFirstByCasaIdAndEstadoInOrderByAbertoEmDesc(UUID casaId, Collection<EstadoAlerta> estadosAbertos);

    List<Alerta> findByCasaIdAndEstadoIn(UUID casaId, Collection<EstadoAlerta> estadosAbertos);

    List<Alerta> findByEstadoIn(Collection<EstadoAlerta> estadosAbertos);
}
