package br.com.financeiro.api.conta.entity;

import br.com.financeiro.api.conta.enums.TipoConta;
import br.com.financeiro.api.usuario.entity.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;
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

  @CreationTimestamp
  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @UpdateTimestamp
  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;
}
