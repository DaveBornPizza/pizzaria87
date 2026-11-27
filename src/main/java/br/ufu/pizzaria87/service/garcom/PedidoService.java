package br.ufu.pizzaria87.service.garcom;

import br.ufu.pizzaria87.model.Atendimento;
import br.ufu.pizzaria87.model.Funcionario;
import br.ufu.pizzaria87.model.ItemCardapio;
import br.ufu.pizzaria87.model.ItemPedido;
import br.ufu.pizzaria87.model.Pedido;
import br.ufu.pizzaria87.repository.ItemCardapioRepository;
import br.ufu.pizzaria87.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regras de pedido do garçom: UC002 (Registrar Pedido na Ficha) e
 * UC006 (Alterar ou Cancelar Pedido).
 */
@Service
public class PedidoService {

    public static final String STATUS_PENDENTE = "PENDENTE";
    public static final String STATUS_EM_PREPARO = "EM_PREPARO";
    public static final String STATUS_PRONTO = "PRONTO";
    public static final String STATUS_CANCELADO = "CANCELADO";

    private static final int TAMANHO_MAXIMO_OBSERVACAO = 255;

    private final PedidoRepository pedidoRepository;
    private final ItemCardapioRepository itemCardapioRepository;
    private final AtendimentoService atendimentoService;

    public PedidoService(PedidoRepository pedidoRepository,
                         ItemCardapioRepository itemCardapioRepository,
                         AtendimentoService atendimentoService) {
        this.pedidoRepository = pedidoRepository;
        this.itemCardapioRepository = itemCardapioRepository;
        this.atendimentoService = atendimentoService;
    }

    /**
     * Opção exibida na tela de seleção de itens. O índice é a posição da linha no
     * formulário (linhas[indice]) e é único em toda a tela, independente da categoria.
     */
    public record OpcaoItem(int indice,
                            ItemCardapio item,
                            int quantidade,
                            String observacao,
                            boolean disponivel,
                            Integer quantidadeMaxima) {
    }

    // ------------------------------------------------------------------
    // Montagem da tela de seleção de itens
    // ------------------------------------------------------------------

    /** UC002: apenas itens disponíveis, agrupados por categoria. */
    @Transactional(readOnly = true)
    public Map<String, List<OpcaoItem>> opcoesNovoPedido() {
        List<ItemCardapio> disponiveis = ordenar(itemCardapioRepository.findByDisponivelTrue());
        List<OpcaoItem> opcoes = new ArrayList<>();
        int indice = 0;
        for (ItemCardapio item : disponiveis) {
            opcoes.add(new OpcaoItem(indice++, item, 0, null, true, null));
        }
        return agruparPorCategoria(opcoes);
    }

    /**
     * UC006: itens disponíveis + itens que já estão no pedido (mesmo que tenham ficado
     * indisponíveis depois; nesse caso só é possível manter ou reduzir a quantidade).
     */
    @Transactional(readOnly = true)
    public Map<String, List<OpcaoItem>> opcoesEdicao(Pedido pedido) {
        Map<Long, ItemPedido> existentes = indexarPorItem(pedido);

        Map<Long, ItemCardapio> itens = new LinkedHashMap<>();
        itemCardapioRepository.findByDisponivelTrue().forEach(i -> itens.put(i.getId(), i));
        existentes.values().forEach(ip -> itens.putIfAbsent(ip.getItemCardapio().getId(), ip.getItemCardapio()));

        List<OpcaoItem> opcoes = new ArrayList<>();
        int indice = 0;
        for (ItemCardapio item : ordenar(new ArrayList<>(itens.values()))) {
            ItemPedido atual = existentes.get(item.getId());
            boolean disponivel = Boolean.TRUE.equals(item.getDisponivel());
            int quantidade = atual != null ? atual.getQuantidade() : 0;
            String observacao = atual != null ? atual.getObservacao() : null;
            Integer maximo = (!disponivel && atual != null) ? atual.getQuantidade() : null;
            opcoes.add(new OpcaoItem(indice++, item, quantidade, observacao, disponivel, maximo));
        }
        return agruparPorCategoria(opcoes);
    }

    @Transactional(readOnly = true)
    public Pedido buscarPedido(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido #" + pedidoId + " não encontrado."));
        pedido.getItens().size();
        return pedido;
    }

    // ------------------------------------------------------------------
    // UC002 – Registrar Pedido na Ficha
    // ------------------------------------------------------------------

    @Transactional
    public Pedido registrarPedido(Integer numeroFicha, PedidoForm form, Funcionario usuarioLogado) {
        Atendimento atendimento = atendimentoService.buscarAtendimentoAberto(numeroFicha);
        List<PedidoForm.Linha> linhas = linhasSelecionadas(form);

        Funcionario garcom = atendimentoService.carregarFuncionario(usuarioLogado);
        Pedido pedido = new Pedido(LocalDateTime.now(), STATUS_PENDENTE, atendimento, garcom);

        for (PedidoForm.Linha linha : linhas) {
            ItemCardapio item = carregarItem(linha.getItemId());
            exigirDisponivel(item);
            // Congela o preço praticado no momento da venda.
            pedido.getItens().add(new ItemPedido(linha.getQuantidade(), item.getPreco(),
                    limparObservacao(linha.getObservacao()), pedido, item));
        }

        return pedidoRepository.save(pedido);
    }

    // ------------------------------------------------------------------
    // UC006 – Alterar ou Cancelar Pedido
    // ------------------------------------------------------------------

    @Transactional
    public Pedido alterarPedido(Long pedidoId, PedidoForm form) {
        Pedido pedido = buscarPedido(pedidoId);
        exigirEditavel(pedido);

        List<PedidoForm.Linha> linhas = linhasSelecionadas(form);
        Map<Long, ItemPedido> existentes = indexarPorItem(pedido);

        // 1) Valida tudo antes de mexer no pedido.
        for (PedidoForm.Linha linha : linhas) {
            ItemCardapio item = carregarItem(linha.getItemId());
            ItemPedido atual = existentes.get(item.getId());
            boolean disponivel = Boolean.TRUE.equals(item.getDisponivel());
            if (atual == null && !disponivel) {
                exigirDisponivel(item);
            }
            if (atual != null && !disponivel && linha.getQuantidade() > atual.getQuantidade()) {
                throw new IllegalArgumentException("O item '" + item.getNome()
                        + "' ficou indisponível: só é possível manter ou reduzir a quantidade dele.");
            }
        }

        // 2) Aplica as alterações.
        Set<ItemPedido> mantidos = new HashSet<>();
        List<ItemPedido> novos = new ArrayList<>();
        for (PedidoForm.Linha linha : linhas) {
            ItemCardapio item = carregarItem(linha.getItemId());
            ItemPedido atual = existentes.get(item.getId());
            if (atual != null && !mantidos.contains(atual)) {
                // Mantém o preço congelado no lançamento original.
                atual.setQuantidade(linha.getQuantidade());
                atual.setObservacao(limparObservacao(linha.getObservacao()));
                mantidos.add(atual);
            } else {
                novos.add(new ItemPedido(linha.getQuantidade(), item.getPreco(),
                        limparObservacao(linha.getObservacao()), pedido, item));
            }
        }

        pedido.getItens().removeIf(ip -> !mantidos.contains(ip));
        pedido.getItens().addAll(novos);

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido cancelarPedido(Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        exigirEditavel(pedido);
        pedido.setStatusPedido(STATUS_CANCELADO);
        return pedidoRepository.save(pedido);
    }

    /** Bloqueio da camada Service: só pedidos PENDENTE de atendimento em aberto podem mudar. */
    public void exigirEditavel(Pedido pedido) {
        if (!STATUS_PENDENTE.equals(pedido.getStatusPedido())) {
            throw new IllegalArgumentException("Este pedido não pode mais ser alterado (status atual: "
                    + rotuloStatus(pedido.getStatusPedido()) + ").");
        }
        if (pedido.getAtendimento() == null || pedido.getAtendimento().getDataFechamento() != null) {
            throw new IllegalArgumentException("Este pedido pertence a um atendimento já encerrado.");
        }
    }

    public static String rotuloStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_PENDENTE -> "Pendente";
            case STATUS_EM_PREPARO -> "Em Preparo";
            case STATUS_PRONTO -> "Pronto para Entrega";
            case STATUS_CANCELADO -> "Cancelado";
            default -> status;
        };
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /**
     * Mantém apenas as linhas com quantidade informada diferente de zero e valida
     * que todas são positivas (extensão 3b de UC002).
     */
    private List<PedidoForm.Linha> linhasSelecionadas(PedidoForm form) {
        List<PedidoForm.Linha> selecionadas = new ArrayList<>();
        if (form != null && form.getLinhas() != null) {
            for (PedidoForm.Linha linha : form.getLinhas()) {
                if (linha == null || linha.getItemId() == null || linha.getQuantidade() == null
                        || linha.getQuantidade() == 0) {
                    continue;
                }
                if (linha.getQuantidade() < 0) {
                    throw new IllegalArgumentException("Quantidade inválida: informe um valor maior que zero.");
                }
                selecionadas.add(linha);
            }
        }
        if (selecionadas.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um item com quantidade maior que zero. "
                    + "Para retirar todos os itens de um pedido pendente, use \"Cancelar pedido\".");
        }
        return selecionadas;
    }

    private ItemCardapio carregarItem(Long itemId) {
        return itemCardapioRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item do cardápio não encontrado."));
    }

    private static void exigirDisponivel(ItemCardapio item) {
        if (!Boolean.TRUE.equals(item.getDisponivel())) {
            throw new IllegalArgumentException("O item '" + item.getNome() + "' está indisponível. Escolha outro item.");
        }
    }

    private static String limparObservacao(String observacao) {
        if (observacao == null || observacao.isBlank()) {
            return null;
        }
        String limpa = observacao.trim();
        if (limpa.length() > TAMANHO_MAXIMO_OBSERVACAO) {
            throw new IllegalArgumentException("A observação pode ter no máximo " + TAMANHO_MAXIMO_OBSERVACAO + " caracteres.");
        }
        return limpa;
    }

    private static Map<Long, ItemPedido> indexarPorItem(Pedido pedido) {
        Map<Long, ItemPedido> mapa = new LinkedHashMap<>();
        for (ItemPedido ip : pedido.getItens()) {
            if (ip.getItemCardapio() != null) {
                mapa.putIfAbsent(ip.getItemCardapio().getId(), ip);
            }
        }
        return mapa;
    }

    private static List<ItemCardapio> ordenar(List<ItemCardapio> itens) {
        List<ItemCardapio> copia = new ArrayList<>(itens);
        copia.sort(Comparator
                .comparing((ItemCardapio i) -> i.getCategoria() != null ? i.getCategoria() : "", String.CASE_INSENSITIVE_ORDER)
                .thenComparing(i -> i.getNome() != null ? i.getNome() : "", String.CASE_INSENSITIVE_ORDER));
        return copia;
    }

    private static Map<String, List<OpcaoItem>> agruparPorCategoria(List<OpcaoItem> opcoes) {
        Map<String, List<OpcaoItem>> grupos = new LinkedHashMap<>();
        for (OpcaoItem opcao : opcoes) {
            String categoria = opcao.item().getCategoria() != null ? opcao.item().getCategoria() : "Outros";
            grupos.computeIfAbsent(categoria, c -> new ArrayList<>()).add(opcao);
        }
        return grupos;
    }
}
