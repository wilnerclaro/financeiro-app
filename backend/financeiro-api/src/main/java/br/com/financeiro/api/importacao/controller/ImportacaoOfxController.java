package br.com.financeiro.api.importacao.controller;

import br.com.financeiro.api.importacao.docs.ImportacaoOfxApiDoc;
import br.com.financeiro.api.importacao.dto.ConfirmacaoOfxResponse;
import br.com.financeiro.api.importacao.dto.RevisaoOfxResponse;
import br.com.financeiro.api.importacao.service.ImportacaoOfxService;
import br.com.financeiro.api.security.UsuarioLogado;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/importacoes/ofx")
@RequiredArgsConstructor
public class ImportacaoOfxController implements ImportacaoOfxApiDoc {
  private final ImportacaoOfxService service;

  @PostMapping(value = "/revisar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<RevisaoOfxResponse> revisar(
      @UsuarioLogado UUID usuarioId,
      @RequestParam UUID contaId,
      @RequestParam MultipartFile arquivo) {
    return ResponseEntity.ok(service.revisar(usuarioId, contaId, arquivo));
  }

  @PostMapping(value = "/confirmar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ConfirmacaoOfxResponse> confirmar(
      @UsuarioLogado UUID usuarioId,
      @RequestParam UUID contaId,
      @RequestParam MultipartFile arquivo,
      @RequestParam String selecoes) {
    return ResponseEntity.ok(service.confirmar(usuarioId, contaId, arquivo, selecoes));
  }
}
