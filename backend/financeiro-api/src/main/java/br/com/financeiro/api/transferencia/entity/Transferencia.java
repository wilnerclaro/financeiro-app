package br.com.financeiro.api.transferencia.entity;

import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
@Table(name = "transferencias")
public class Transferencia {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conta_origem_id", nullable = false)
  private Conta contaOrigem;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conta_destino_id", nullable = false)
  private Conta contaDestino;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal valor;

  @Column(name = "data_transferencia", nullable = false)
  private LocalDate dataTransferencia;

  @Column(length = 180)
  private String descricao;

  @Column(nullable = false)
  private Boolean cancelada;

  @CreationTimestamp
  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @UpdateTimestamp
  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;
}
