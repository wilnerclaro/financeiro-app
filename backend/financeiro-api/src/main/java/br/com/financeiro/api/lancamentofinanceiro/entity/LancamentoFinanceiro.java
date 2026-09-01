package br.com.financeiro.api.lancamentofinanceiro.entity;

import br.com.financeiro.api.categoria.entity.Categoria;
import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import br.com.financeiro.api.usuario.entity.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "lancamentos_financeiros")
public class LancamentoFinanceiro {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conta_id", nullable = false)
  private Conta conta;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "categoria_id", nullable = false)
  private Categoria categoria;

  @Column(nullable = false, length = 180)
  private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoLancamentoFinanceiro tipo;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal valor;

  @Column(name = "data_competencia", nullable = false)
  private LocalDate dataCompetencia;

  @Column(name = "data_vencimento")
  private LocalDate dataVencimento;

  @Column(name = "data_pagamento")
  private LocalDate dataPagamento;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusLancamentoFinanceiro status;

  @Column(columnDefinition = "TEXT")
  private String observacao;

  @CreationTimestamp
  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @UpdateTimestamp
  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;
}
