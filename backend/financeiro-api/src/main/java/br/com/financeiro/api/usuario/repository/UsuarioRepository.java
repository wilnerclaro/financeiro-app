package br.com.financeiro.api.usuario.repository;

import br.com.financeiro.api.usuario.entity.Usuario;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {}
