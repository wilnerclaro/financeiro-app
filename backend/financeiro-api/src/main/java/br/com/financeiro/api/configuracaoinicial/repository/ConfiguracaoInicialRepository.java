package br.com.financeiro.api.configuracaoinicial.repository;

import br.com.financeiro.api.configuracaoinicial.entity.ConfiguracaoInicial;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoInicialRepository extends JpaRepository<ConfiguracaoInicial, UUID> {

  Optional<ConfiguracaoInicial> findByUsuarioId(UUID usuarioId);
}
