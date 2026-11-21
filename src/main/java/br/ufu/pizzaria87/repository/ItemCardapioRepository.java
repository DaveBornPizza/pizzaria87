package br.ufu.pizzaria87.repository;

import br.ufu.pizzaria87.model.ItemCardapio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemCardapioRepository extends JpaRepository<ItemCardapio, Long> {
    boolean existsByNomeIgnoreCase(String nome);
    List<ItemCardapio> findByDisponivelTrue();
}