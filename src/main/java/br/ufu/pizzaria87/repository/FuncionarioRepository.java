package br.ufu.pizzaria87.repository;

import br.ufu.pizzaria87.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    Optional<Funcionario> findByIdentificadorAndPinAcesso(String identificador, String pinAcesso);
    boolean existsByIdentificador(String identificador);
}