package br.com.financeiro.api.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "usuarios")
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false, unique = true, length = 180)
  private String email;

  @Column(name = "senha_hash", nullable = false)
  private String senhaHash;

  @Column(nullable = false)
  private Boolean ativo;

  @CreationTimestamp
  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @UpdateTimestamp
  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;
}
