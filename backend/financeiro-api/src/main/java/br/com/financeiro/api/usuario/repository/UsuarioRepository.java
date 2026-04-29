package br.com.financeiro.api.usuario.repository;

import br.com.financeiro.api.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
}
