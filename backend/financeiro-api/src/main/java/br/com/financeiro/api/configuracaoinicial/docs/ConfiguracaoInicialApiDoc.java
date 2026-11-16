package br.com.financeiro.api.configuracaoinicial.docs;

import br.com.financeiro.api.configuracaoinicial.dto.AtualizarConfiguracaoInicialRequest;
import br.com.financeiro.api.configuracaoinicial.dto.ConfiguracaoInicialResponse;
import br.com.financeiro.api.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Configuração inicial", description = "Objetivo e progresso dos primeiros passos")
public interface ConfiguracaoInicialApiDoc {

  @Operation(
      summary = "Consultar configuração inicial",
      description = "Sem configuração salva, retorna boas-vindas e a competência atual.")
  @ApiResponse(responseCode = "200", description = "Configuração do usuário autenticado")
  ResponseEntity<ConfiguracaoInicialResponse> buscar(@UsuarioLogado UUID usuarioId);

  @Operation(
      summary = "Salvar configuração inicial",
      description = "Salva objetivo, etapa e pausa. Não cria contas, orçamento ou lançamentos.")
  @ApiResponse(responseCode = "200", description = "Configuração salva")
  @ApiResponse(responseCode = "400", description = "Competência, objetivo ou etapa inválida")
  ResponseEntity<ConfiguracaoInicialResponse> atualizar(
      @UsuarioLogado UUID usuarioId,
      @Valid @RequestBody AtualizarConfiguracaoInicialRequest request);
}
