package br.com.financeiro.api.importacao.docs;

import br.com.financeiro.api.importacao.dto.ConfirmacaoImportacaoResponse;
import br.com.financeiro.api.importacao.dto.RevisaoExtratoResponse;
import br.com.financeiro.api.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Importação de extrato")
public interface ImportacaoExtratoApiDoc {
  @Operation(
      summary = "Revisar extrato bancário (OFX, CSV ou PDF)",
      description =
          "Não grava lançamentos. O formato é reconhecido pelo conteúdo; CSV e PDF dependem de um"
              + " leitor para o banco do arquivo. Aceita BRL, conta corrente ou poupança, até 2 MB"
              + " e 1.000 transações.")
  ResponseEntity<RevisaoExtratoResponse> revisar(
      @UsuarioLogado UUID usuarioId,
      @RequestParam UUID contaId,
      @RequestParam MultipartFile arquivo);

  @Operation(
      summary = "Confirmar transações revisadas",
      description =
          "Reprocessa o arquivo e grava atomicamente os itens selecionados. selecoes é um JSON com"
              + " identificador e categoriaId. Itens já importados são ignorados.")
  ResponseEntity<ConfirmacaoImportacaoResponse> confirmar(
      @UsuarioLogado UUID usuarioId,
      @RequestParam UUID contaId,
      @RequestParam MultipartFile arquivo,
      @RequestParam String selecoes);
}
