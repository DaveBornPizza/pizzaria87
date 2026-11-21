package br.ufu.pizzaria87.repository;

import br.ufu.pizzaria87.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByStatusPedidoOrderByDataHoraAsc(String statusPedido);
    List<Pedido> findByAtendimento_Id(Long atendimentoId);
}