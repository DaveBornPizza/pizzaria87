package br.ufu.pizzaria87.repository;

import br.ufu.pizzaria87.model.Atendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {
    Optional<Atendimento> findFirstByFicha_NumeroFichaAndDataFechamentoIsNullOrderByDataAberturaDesc(Integer numeroFicha);
}