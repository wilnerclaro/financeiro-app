package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Metas e sonhos")
class MetaIntegracaoTest extends IntegracaoTest {

  private static final String URL = "/api/metas";

  private Sessao usuario;

  @BeforeEach
  void prepararDados() throws Exception {
    usuario = criarUsuario();
  }

  @Test
  @DisplayName("acumulado = investimento inicial + aportes + juros de todos os meses")
  void acumulado() throws Exception {
    UUID id =
        idDe(
            criar("Reserva de emergência", "10000.00", "2000.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acumulado").value(2000.00))
                .andExpect(jsonPath("$.percentualAtingido").value(0.2))
                .andExpect(jsonPath("$.falta").value(8000.00)));

    movimento(id, 2025, 12, "500.00", "0.00").andExpect(status().isOk());
    movimento(id, 2026, 1, "1000.00", "50.00").andExpect(status().isOk());
    movimento(id, 2026, 2, "1000.00", "-20.00")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acumulado").value(4530.00))
        .andExpect(jsonPath("$.percentualAtingido").value(0.453))
        .andExpect(jsonPath("$.meses[1].juros").value(-20.00));

    consultar(URL + "?ano=2026", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalNecessario").value(10000.00))
        .andExpect(jsonPath("$.totalAcumulado").value(4530.00))
        .andExpect(jsonPath("$.metas[0].meses[0].aporte").value(1000.00))
        .andExpect(jsonPath("$.metas[0].meses[11].aporte").value(0.0))
        .andExpect(jsonPath("$.totaisPorMes[0].juros").value(50.00));

    consultar(URL + "?ano=2025", usuario)
        .andExpect(jsonPath("$.totaisPorMes[11].aporte").value(500.00));
  }

  @Test
  @DisplayName("aporte e juros zerados zeram o mês; meta atingida marca atingida")
  void zerarEAtingir() throws Exception {
    UUID id = idDe(criar("Viagem", "1000.00", "0.00"));
    movimento(id, 2026, 3, "1200.00", "0.00")
        .andExpect(jsonPath("$.atingida").value(true))
        .andExpect(jsonPath("$.falta").value(0.0));
    movimento(id, 2026, 3, "0", "0")
        .andExpect(jsonPath("$.acumulado").value(0.0))
        .andExpect(jsonPath("$.atingida").value(false));
  }

  @Test
  @DisplayName("nome repetido é recusado sem diferenciar maiúsculas")
  void nomeRepetido() throws Exception {
    criar("Carro novo", "50000.00", null).andExpect(status().isCreated());
    criar("carro NOVO", "1.00", null).andExpect(status().isBadRequest());
    criar("Sem valor", "0", null).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("meta inativa sai do painel e não recebe aporte até ser ativada")
  void inativar() throws Exception {
    UUID id = idDe(criar("Casa", "300000.00", "0.00"));
    remover(URL + "/" + id, usuario).andExpect(status().isNoContent());
    remover(URL + "/" + id, usuario).andExpect(status().isNoContent());

    consultar(URL, usuario).andExpect(jsonPath("$.metas.length()").value(0));
    consultar(URL + "?incluirInativas=true", usuario)
        .andExpect(jsonPath("$.metas.length()").value(1));
    movimento(id, 2026, 6, "100.00", "0.00").andExpect(status().isBadRequest());

    ativar(URL + "/" + id + "/ativar", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativa").value(true));

    atualizar(
            usuario,
            URL + "/" + id,
            """
            {"nome":"Casa própria","valorNecessario":250000.00,"investimentoInicial":10000.00}
            """)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Casa própria"))
        .andExpect(jsonPath("$.percentualAtingido").value(0.04));
  }

  @Test
  @DisplayName("meta de outro usuário não é encontrada")
  void isolamento() throws Exception {
    UUID id = idDe(criar("Reserva", "1000.00", "0.00"));
    Sessao outro = criarUsuario();
    consultar(URL + "/" + id, outro).andExpect(status().isNotFound());
    movimento(outro, id, 2026, 1, "10.00", "0.00").andExpect(status().isNotFound());
    consultar(URL, outro).andExpect(jsonPath("$.metas.length()").value(0));
  }

  private org.springframework.test.web.servlet.ResultActions criar(
      String nome, String necessario, String inicial) throws Exception {
    return postar(
        usuario,
        URL,
        """
        {"nome":"%s","valorNecessario":%s,"investimentoInicial":%s}
        """
            .formatted(nome, necessario, inicial == null ? "null" : inicial));
  }

  private org.springframework.test.web.servlet.ResultActions movimento(
      UUID id, int ano, int mes, String aporte, String juros) throws Exception {
    return movimento(usuario, id, ano, mes, aporte, juros);
  }

  private org.springframework.test.web.servlet.ResultActions movimento(
      Sessao quem, UUID id, int ano, int mes, String aporte, String juros) throws Exception {
    return atualizar(
        quem,
        URL + "/" + id + "/movimentos/" + ano + "/" + mes,
        "{\"aporte\":" + aporte + ",\"juros\":" + juros + "}");
  }
}
