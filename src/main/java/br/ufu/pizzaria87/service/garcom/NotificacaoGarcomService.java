package br.ufu.pizzaria87.service.garcom;

import br.ufu.pizzaria87.model.Notificacao;
import br.ufu.pizzaria87.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Leitura das notificações pelo garçom (sino do cabeçalho).
 * A geração das notificações (PEDIDO_PRONTO / FALTA_INGREDIENTE) é responsabilidade
 * do fluxo da cozinha; aqui apenas consultamos e marcamos como lidas.
 */
@Service
public class NotificacaoGarcomService {

    public static final String TIPO_PEDIDO_PRONTO = "PEDIDO_PRONTO";
    public static final String TIPO_FALTA_INGREDIENTE = "FALTA_INGREDIENTE";

    private static final int LIMITE_LIDAS_RECENTES = 5;

    private final NotificacaoRepository notificacaoRepository;

    public NotificacaoGarcomService(NotificacaoRepository notificacaoRepository) {
        this.notificacaoRepository = notificacaoRepository;
    }

    /** Dados do sino: não lidas (mais recentes primeiro), contador e algumas lidas recentes. */
    public record Sino(List<Notificacao> naoLidas, List<Notificacao> lidasRecentes, long totalNaoLidas) {
    }

    @Transactional(readOnly = true)
    public Sino carregarSino(Long funcionarioId) {
        if (funcionarioId == null) {
            return new Sino(List.of(), List.of(), 0);
        }
        List<Notificacao> todas = notificacaoRepository.findByFuncionarioDestino_IdOrderByDataHoraDesc(funcionarioId);
        List<Notificacao> naoLidas = todas.stream().filter(n -> !Boolean.TRUE.equals(n.getLida())).toList();
        List<Notificacao> lidas = todas.stream()
                .filter(n -> Boolean.TRUE.equals(n.getLida()))
                .limit(LIMITE_LIDAS_RECENTES)
                .toList();
        long contador = notificacaoRepository.countByFuncionarioDestino_IdAndLidaFalse(funcionarioId);
        return new Sino(naoLidas, lidas, contador);
    }

    @Transactional
    public void marcarComoLida(Long notificacaoId, Long funcionarioId) {
        Notificacao notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada."));

        if (notificacao.getFuncionarioDestino() == null
                || !notificacao.getFuncionarioDestino().getId().equals(funcionarioId)) {
            throw new IllegalArgumentException("Esta notificação não é destinada a você.");
        }

        if (!Boolean.TRUE.equals(notificacao.getLida())) {
            notificacao.setLida(true);
            notificacaoRepository.save(notificacao);
        }
    }

    @Transactional
    public int marcarTodasComoLidas(Long funcionarioId) {
        List<Notificacao> naoLidas = notificacaoRepository.findByFuncionarioDestino_IdOrderByDataHoraDesc(funcionarioId)
                .stream()
                .filter(n -> !Boolean.TRUE.equals(n.getLida()))
                .toList();
        naoLidas.forEach(n -> n.setLida(true));
        notificacaoRepository.saveAll(naoLidas);
        return naoLidas.size();
    }
}
