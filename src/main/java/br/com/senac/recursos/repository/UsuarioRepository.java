package br.com.senac.recursos.repository;

import br.com.senac.recursos.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    List<Usuario> findAllByOrderByIdAsc();
}
