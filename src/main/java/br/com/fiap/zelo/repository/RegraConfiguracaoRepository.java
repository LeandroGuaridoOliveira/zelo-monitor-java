package br.com.fiap.zelo.repository;

import br.com.fiap.zelo.model.RegraConfiguracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegraConfiguracaoRepository extends JpaRepository<RegraConfiguracao, UUID> {
    Optional<RegraConfiguracao> findByCasaId(UUID casaId);
}
