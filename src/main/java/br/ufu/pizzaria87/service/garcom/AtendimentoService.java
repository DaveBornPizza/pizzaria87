package br.ufu.pizzaria87.service.garcom;

import br.ufu.pizzaria87.model.Atendimento;
import br.ufu.pizzaria87.model.Ficha;
import br.ufu.pizzaria87.model.Funcionario;
import br.ufu.pizzaria87.model.ItemPedido;
import br.ufu.pizzaria87.model.Pedido;
import br.ufu.pizzaria87.repository.AtendimentoRepository;
import br.ufu.pizzaria87.repository.FichaRepository;
import br.ufu.pizzaria87.repository.FuncionarioRepository;
import br.ufu.pizzaria87.repository.PedidoRepository;
import br.ufu.pizzaria87.service.admin.FichaService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Regras do fluxo do salão ligadas à ficha e ao atendimento:
 * painel do garçom, UC005 (Abrir Ficha) e UC004 (Consultar e Fechar Ficha).
 */
@Service
public class AtendimentoService {

    public static final String FILTRO_TODAS = "todas";
    public static final String FILTRO_LIVRES = "livres";
    public static final String FILTRO_OCUPADAS = "ocupadas";
    public static final String FILTRO_DESATIVADAS = "desativadas";

    private final FichaRepository fichaRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final PedidoRepository pedidoRepository;
    private final FuncionarioRepository funcionarioRepository;

    public AtendimentoService(FichaRepository fichaRepository,
                              AtendimentoRepository atendimentoRepository,
                              PedidoRepository pedidoRepository,
                              FuncionarioRepository funcionarioRepository) {
        this.fichaRepository = fichaRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    // ------------------------------------------------------------------
    // Visões (somente leitura) usadas pelas telas
    // ------------------------------------------------------------------

    /** Cartão de uma ficha na grade do painel do garçom. */
    public record FichaResumo(Integer numero,
                              String status,
                              BigDecimal consumoParcial,
                              int quantidadePedidos,
                              LocalDateTime dataAbertura,
                              String garcomResponsavel) {
    }

    /** Dados do painel: fichas filtradas e totais por status (sempre sobre todas as fichas). */
    public record Painel(List<FichaResumo> fichas,
                         String filtro,
                         long total,
                         long livres,
                         long ocupadas,
                         long desativadas) {
    }

    /** Pedido com subtotal já calculado e indicação se ainda pode ser alterado (UC006). */
    public record PedidoResumo(Pedido pedido, BigDecimal subtotal, boolean editavel) {
    }

    /** Tela de detalhe da ficha ocupada (lista de pedidos e consumo parcial). */
    public record DetalheFicha(Ficha ficha,
                               Atendimento atendimento,
                               List<PedidoResumo> pedidos,
                               BigDecimal consumoParcial) {
    }

    /** Linha consolidada do extrato: mesmo item com o mesmo preço congelado é somado. */
    public record LinhaExtrato(String item, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    }

    /** Extrato da conta (UC004). */
    public record Extrato(Ficha ficha,
                          Atendimento atendimento,
                          List<LinhaExtrato> linhas,
                          BigDecimal total,
                          int totalPedidos,
                          int pedidosCancelados,
                          int pedidosEmAberto) {

        public boolean semPedidos() {
            return totalPedidos == 0;
        }

        public boolean possuiPendencias() {
            return pedidosEmAberto > 0;
        }
    }

    // ------------------------------------------------------------------
    // Painel do garçom
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Painel montarPainel(String filtro, Integer numeroBuscado) {
        String filtroNormalizado = normalizarFiltro(filtro);
        List<Ficha> todas = fichaRepository.findAll(Sort.by(Sort.Direction.ASC, "numeroFicha"));

        long livres = todas.stream().filter(f -> statusIgual(f, FichaService.STATUS_LIVRE)).count();
        long ocupadas = todas.stream().filter(f -> statusIgual(f, FichaService.STATUS_OCUPADA)).count();
        long desativadas = todas.stream().filter(f -> statusIgual(f, FichaService.STATUS_DESATIVADA)).count();

        if (numeroBuscado != null && todas.stream().noneMatch(f -> numeroBuscado.equals(f.getNumeroFicha()))) {
            throw new IllegalArgumentException("A ficha " + numeroBuscado + " não existe.");
        }

        List<FichaResumo> resumos = new ArrayList<>();
        for (Ficha ficha : todas) {
            if (numeroBuscado != null && !numeroBuscado.equals(ficha.getNumeroFicha())) {
                continue;
            }
            if (numeroBuscado == null && !passaNoFiltro(ficha, filtroNormalizado)) {
                continue;
            }
            resumos.add(resumir(ficha));
        }

        return new Painel(resumos, filtroNormalizado, todas.size(), livres, ocupadas, desativadas);
    }

    private FichaResumo resumir(Ficha ficha) {
        if (!statusIgual(ficha, FichaService.STATUS_OCUPADA)) {
            return new FichaResumo(ficha.getNumeroFicha(), statusDe(ficha), BigDecimal.ZERO, 0, null, null);
        }

        Optional<Atendimento> aberto = atendimentoRepository
                .findFirstByFicha_NumeroFichaAndDataFechamentoIsNullOrderByDataAberturaDesc(ficha.getNumeroFicha());

        if (aberto.isEmpty()) {
            return new FichaResumo(ficha.getNumeroFicha(), statusDe(ficha), BigDecimal.ZERO, 0, null, null);
        }

        Atendimento atendimento = aberto.get();
        List<Pedido> pedidos = pedidosDoAtendimento(atendimento.getId());
        int ativos = (int) pedidos.stream().filter(p -> !PedidoService.STATUS_CANCELADO.equals(p.getStatusPedido())).count();
        String garcom = atendimento.getFuncionarioAbertura() != null ? atendimento.getFuncionarioAbertura().getNome() : null;

        return new FichaResumo(ficha.getNumeroFicha(), statusDe(ficha), valorConsumido(pedidos), ativos,
                atendimento.getDataAbertura(), garcom);
    }

    // ------------------------------------------------------------------
    // UC005 – Abrir Ficha
    // ------------------------------------------------------------------

    @Transactional
    public Atendimento abrirFicha(Integer numeroFicha, Funcionario usuarioLogado) {
        if (numeroFicha == null) {
            throw new IllegalArgumentException("Informe o número da ficha.");
        }

        Ficha ficha = fichaRepository.findById(numeroFicha)
                .orElseThrow(() -> new IllegalArgumentException("A ficha " + numeroFicha + " não existe. Informe outro número."));

        if (statusIgual(ficha, FichaService.STATUS_DESATIVADA)) {
            throw new IllegalArgumentException("A ficha " + numeroFicha + " está desativada. Informe outro número.");
        }
        if (!statusIgual(ficha, FichaService.STATUS_LIVRE)) {
            throw new IllegalArgumentException("A ficha " + numeroFicha + " já está em uso. Informe outro número.");
        }
        if (atendimentoRepository.findFirstByFicha_NumeroFichaAndDataFechamentoIsNullOrderByDataAberturaDesc(numeroFicha).isPresent()) {
            throw new IllegalArgumentException("A ficha " + numeroFicha + " já possui um atendimento em aberto.");
        }

        Funcionario garcom = carregarFuncionario(usuarioLogado);

        ficha.setStatus(FichaService.STATUS_OCUPADA);
        fichaRepository.save(ficha);

        Atendimento atendimento = new Atendimento(LocalDateTime.now(), ficha, garcom);
        return atendimentoRepository.save(atendimento);
    }

    // ------------------------------------------------------------------
    // Consulta da ficha ocupada (detalhe) – base para UC002, UC006 e UC004
    // ------------------------------------------------------------------

    /**
     * Retorna o atendimento em aberto da ficha, validando que ela existe e está OCUPADA
     * (extensões 2a de UC002, UC004 e UC006).
     * Sem @Transactional próprio: participa da transação de quem chama.
     */
    public Atendimento buscarAtendimentoAberto(Integer numeroFicha) {
        Ficha ficha = fichaRepository.findById(numeroFicha)
                .orElseThrow(() -> new IllegalArgumentException("A ficha " + numeroFicha + " não existe."));

        if (!statusIgual(ficha, FichaService.STATUS_OCUPADA)) {
            throw new IllegalArgumentException("A ficha " + numeroFicha + " não está ocupada (status atual: "
                    + statusDe(ficha) + ").");
        }

        return atendimentoRepository
                .findFirstByFicha_NumeroFichaAndDataFechamentoIsNullOrderByDataAberturaDesc(numeroFicha)
                .orElseThrow(() -> new IllegalArgumentException("A ficha " + numeroFicha + " não possui atendimento em aberto."));
    }

    @Transactional(readOnly = true)
    public DetalheFicha detalharFicha(Integer numeroFicha) {
        Atendimento atendimento = buscarAtendimentoAberto(numeroFicha);
        List<Pedido> pedidos = pedidosDoAtendimento(atendimento.getId());

        List<PedidoResumo> resumos = pedidos.stream()
                .map(p -> new PedidoResumo(p, subtotal(p), PedidoService.STATUS_PENDENTE.equals(p.getStatusPedido())))
                .toList();

        return new DetalheFicha(atendimento.getFicha(), atendimento, resumos, valorConsumido(pedidos));
    }

    // ------------------------------------------------------------------
    // UC004 – Consultar e Fechar Ficha
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Extrato gerarExtrato(Integer numeroFicha) {
        Atendimento atendimento = buscarAtendimentoAberto(numeroFicha);
        List<Pedido> pedidos = pedidosDoAtendimento(atendimento.getId());

        Map<String, LinhaExtrato> consolidado = new LinkedHashMap<>();
        for (Pedido pedido : pedidos) {
            if (PedidoService.STATUS_CANCELADO.equals(pedido.getStatusPedido())) {
                continue;
            }
            for (ItemPedido ip : pedido.getItens()) {
                String nome = ip.getItemCardapio() != null ? ip.getItemCardapio().getNome() : "Item";
                Long itemId = ip.getItemCardapio() != null ? ip.getItemCardapio().getId() : null;
                String chave = itemId + "|" + ip.getPrecoUnitario().stripTrailingZeros().toPlainString();

                LinhaExtrato atual = consolidado.get(chave);
                int quantidade = (atual != null ? atual.quantidade() : 0) + ip.getQuantidade();
                BigDecimal subtotal = ip.getPrecoUnitario().multiply(BigDecimal.valueOf(quantidade));
                consolidado.put(chave, new LinhaExtrato(nome, quantidade, ip.getPrecoUnitario(), subtotal));
            }
        }

        int cancelados = (int) pedidos.stream().filter(p -> PedidoService.STATUS_CANCELADO.equals(p.getStatusPedido())).count();

        return new Extrato(atendimento.getFicha(), atendimento, new ArrayList<>(consolidado.values()),
                valorConsumido(pedidos), pedidos.size(), cancelados, contarEmAberto(pedidos));
    }

    /**
     * Fecha a conta: exige ao menos um pedido registrado e, havendo pedidos PENDENTE/EM_PREPARO,
     * exige a confirmação explícita do garçom. Grava dataFechamento, valorTotal e
     * funcionarioFechamento, e devolve a ficha para LIVRE.
     */
    @Transactional
    public Atendimento fecharFicha(Integer numeroFicha, boolean confirmouPendencias, Funcionario usuarioLogado) {
        Atendimento atendimento = buscarAtendimentoAberto(numeroFicha);
        List<Pedido> pedidos = pedidosDoAtendimento(atendimento.getId());

        if (pedidos.isEmpty()) {
            throw new IllegalArgumentException("A ficha " + numeroFicha
                    + " não possui nenhum pedido registrado e não pode ter a conta fechada. "
                    + "Se ela foi aberta por engano, use \"Liberar ficha\".");
        }

        int emAberto = contarEmAberto(pedidos);
        if (emAberto > 0 && !confirmouPendencias) {
            throw new IllegalArgumentException("Há " + emAberto
                    + " pedido(s) Pendente(s) ou Em Preparo nesta ficha. Confirme para fechar mesmo assim.");
        }

        Funcionario garcom = carregarFuncionario(usuarioLogado);

        atendimento.setDataFechamento(LocalDateTime.now());
        atendimento.setValorTotal(valorConsumido(pedidos));
        atendimento.setFuncionarioFechamento(garcom);
        atendimentoRepository.save(atendimento);

        Ficha ficha = atendimento.getFicha();
        ficha.setStatus(FichaService.STATUS_LIVRE);
        fichaRepository.save(ficha);

        return atendimento;
    }

    /**
     * Libera uma ficha aberta por engano, sem nenhum pedido registrado.
     * Não é um fechamento de conta: o atendimento é encerrado com valor total zero
     * (o registro é mantido para preservar o histórico) e a ficha volta para LIVRE.
     */
    @Transactional
    public void liberarFichaSemPedidos(Integer numeroFicha, Funcionario usuarioLogado) {
        Atendimento atendimento = buscarAtendimentoAberto(numeroFicha);
        List<Pedido> pedidos = pedidosDoAtendimento(atendimento.getId());

        if (!pedidos.isEmpty()) {
            throw new IllegalArgumentException("A ficha " + numeroFicha
                    + " possui pedidos registrados. Use \"Fechar conta\".");
        }

        Funcionario garcom = carregarFuncionario(usuarioLogado);

        atendimento.setDataFechamento(LocalDateTime.now());
        atendimento.setValorTotal(BigDecimal.ZERO);
        atendimento.setFuncionarioFechamento(garcom);
        atendimentoRepository.save(atendimento);

        Ficha ficha = atendimento.getFicha();
        ficha.setStatus(FichaService.STATUS_LIVRE);
        fichaRepository.save(ficha);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    List<Pedido> pedidosDoAtendimento(Long atendimentoId) {
        List<Pedido> pedidos = new ArrayList<>(pedidoRepository.findByAtendimento_Id(atendimentoId));
        pedidos.sort(Comparator.comparing(Pedido::getDataHora).thenComparing(Pedido::getId));
        // Inicializa a coleção LAZY de itens dentro da transação.
        pedidos.forEach(p -> p.getItens().size());
        return pedidos;
    }

    /** Soma quantidade x preço unitário congelado dos itens do pedido. */
    public static BigDecimal subtotal(Pedido pedido) {
        return pedido.getItens().stream()
                .map(ip -> ip.getPrecoUnitario().multiply(BigDecimal.valueOf(ip.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Consumo do atendimento: soma dos pedidos que não foram cancelados. */
    public static BigDecimal valorConsumido(List<Pedido> pedidos) {
        return pedidos.stream()
                .filter(p -> !PedidoService.STATUS_CANCELADO.equals(p.getStatusPedido()))
                .map(AtendimentoService::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int contarEmAberto(List<Pedido> pedidos) {
        return (int) pedidos.stream()
                .filter(p -> PedidoService.STATUS_PENDENTE.equals(p.getStatusPedido())
                        || PedidoService.STATUS_EM_PREPARO.equals(p.getStatusPedido()))
                .count();
    }

    Funcionario carregarFuncionario(Funcionario usuarioLogado) {
        if (usuarioLogado == null || usuarioLogado.getId() == null) {
            throw new IllegalArgumentException("Sessão expirada. Faça login novamente.");
        }
        return funcionarioRepository.findById(usuarioLogado.getId())
                .orElseThrow(() -> new IllegalArgumentException("Funcionário logado não encontrado."));
    }

    private static String normalizarFiltro(String filtro) {
        if (filtro == null) {
            return FILTRO_TODAS;
        }
        return switch (filtro.trim().toLowerCase()) {
            case FILTRO_LIVRES, FILTRO_OCUPADAS, FILTRO_DESATIVADAS -> filtro.trim().toLowerCase();
            default -> FILTRO_TODAS;
        };
    }

    private static boolean passaNoFiltro(Ficha ficha, String filtro) {
        return switch (filtro) {
            case FILTRO_LIVRES -> statusIgual(ficha, FichaService.STATUS_LIVRE);
            case FILTRO_OCUPADAS -> statusIgual(ficha, FichaService.STATUS_OCUPADA);
            case FILTRO_DESATIVADAS -> statusIgual(ficha, FichaService.STATUS_DESATIVADA);
            default -> true;
        };
    }

    private static boolean statusIgual(Ficha ficha, String status) {
        return ficha.getStatus() != null && status.equalsIgnoreCase(ficha.getStatus().trim());
    }

    private static String statusDe(Ficha ficha) {
        return ficha.getStatus() != null ? ficha.getStatus().trim().toUpperCase() : "";
    }
}
