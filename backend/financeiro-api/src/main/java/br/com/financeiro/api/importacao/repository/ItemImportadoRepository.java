package br.com.financeiro.api.importacao.repository;

import br.com.financeiro.api.importacao.entity.ItemImportado;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemImportadoRepository extends JpaRepository<ItemImportado, UUID> {
  boolean existsByUsuarioIdAndContaIdAndIdentificador(
      UUID usuarioId, UUID contaId, String identificador);
}
