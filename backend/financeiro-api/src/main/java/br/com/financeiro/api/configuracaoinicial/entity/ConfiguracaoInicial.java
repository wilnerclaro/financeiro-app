package br.com.financeiro.api.configuracaoinicial.entity;

import br.com.financeiro.api.configuracaoinicial.enums.EtapaConfiguracao;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "configuracoes_iniciais")
public class ConfiguracaoInicial {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false, unique = true)
  private Usuario usuario;

  @Column(name = "renda_passiva_desejada", precision = 19, scale = 2)
  private BigDecimal rendaPassivaDesejada;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EtapaConfiguracao etapa;

  @Column(nullable = false)
  private Boolean pausado;

  @Column(nullable = false)
  private Integer ano;

  @Column(nullable = false)
  private Integer mes;

  @CreationTimestamp
  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @UpdateTimestamp
  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;
}
