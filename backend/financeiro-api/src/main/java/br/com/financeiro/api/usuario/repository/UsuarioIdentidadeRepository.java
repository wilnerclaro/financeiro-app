package br.com.financeiro.api.usuario.repository;

import br.com.financeiro.api.usuario.entity.UsuarioIdentidade;
import br.com.financeiro.api.usuario.enums.ProvedorIdentidade;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioIdentidadeRepository extends JpaRepository<UsuarioIdentidade, UUID> {

  @Query(
      """
      SELECT i FROM UsuarioIdentidade i JOIN FETCH i.usuario
      WHERE i.provedor = :provedor AND i.sujeito = :sujeito
      """)
  Optional<UsuarioIdentidade> buscarComUsuario(
      @Param("provedor") ProvedorIdentidade provedor, @Param("sujeito") String sujeito);
}
