package br.com.financeiro.api.categoria.service;

import br.com.financeiro.api.categoria.repository.CategoriaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriasPadraoService {

  private final CategoriaRepository categoriaRepository;

  @Transactional(propagation = Propagation.MANDATORY)
  public void criarPara(UUID usuarioId) {
    categoriaRepository.copiarGruposPadrao(usuarioId);
    categoriaRepository.copiarSubcategoriasPadrao(usuarioId);
  }
}
