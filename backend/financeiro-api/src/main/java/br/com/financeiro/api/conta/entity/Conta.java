package br.com.financeiro.api.conta.entity;

import br.com.financeiro.api.conta.enums.TipoConta;
import br.com.financeiro.api.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "contas")
public class Conta {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(nullable = false, length = 100)
  private String nome;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoConta tipo;

  @Column(name = "saldo_inicial", nullable = false, precision = 19, scale = 2)
  private BigDecimal saldoInicial;

  @Column(name = "saldo_atual", nullable = false, precision = 19, scale = 2)
  private BigDecimal saldoAtual;

  @Column(nullable = false)
  private Boolean ativa;

  @Column(name = "dia_fechamento")
  private Integer diaFechamento;

  @Column(name = "dia_vencimento")
  private Integer diaVencimento;

  @Version
  @Column(nullable = false)
  private Long versao;

  @CreationTimestamp
  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @UpdateTimestamp
  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;

  public void creditar(BigDecimal valor) {
    saldoAtual = saldoAtual.add(valor);
  }

  public void debitar(BigDecimal valor) {
    saldoAtual = saldoAtual.subtract(valor);
  }

  public boolean ehCartaoDeCredito() {
    return TipoConta.CARTAO_CREDITO.equals(tipo);
  }

  public LocalDate vencimentoDaFatura(LocalDate dataCompra) {
    YearMonth mesDaCompra = YearMonth.from(dataCompra);
    YearMonth mesDoFechamento =
        dataCompra.getDayOfMonth() <= diaFechamento ? mesDaCompra : mesDaCompra.plusMonths(1);
    return mesDoVencimento(mesDoFechamento).atDay(diaVencimento);
  }

  public LocalDate fechamentoDaFatura(YearMonth mesDoVencimento) {
    YearMonth mesDoFechamento =
        diaVencimento > diaFechamento ? mesDoVencimento : mesDoVencimento.minusMonths(1);
    return mesDoFechamento.atDay(diaFechamento);
  }

  private YearMonth mesDoVencimento(YearMonth mesDoFechamento) {
    return diaVencimento > diaFechamento ? mesDoFechamento : mesDoFechamento.plusMonths(1);
  }
}
