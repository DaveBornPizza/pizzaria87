package br.ufu.pizzaria87.repository;

import br.ufu.pizzaria87.model.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {
}