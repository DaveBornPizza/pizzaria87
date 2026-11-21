package br.ufu.pizzaria87.repository;

import br.ufu.pizzaria87.model.Ficha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FichaRepository extends JpaRepository<Ficha, Integer> {
    List<Ficha> findByStatus(String status);
}