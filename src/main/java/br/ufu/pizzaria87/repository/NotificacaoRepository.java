package br.ufu.pizzaria87.repository;

import br.ufu.pizzaria87.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByFuncionarioDestino_IdOrderByDataHoraDesc(Long funcionarioId);
    long countByFuncionarioDestino_IdAndLidaFalse(Long funcionarioId);
}