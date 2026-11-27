package br.ufu.pizzaria87.controller.garcom;

import br.ufu.pizzaria87.model.Pedido;
import br.ufu.pizzaria87.service.garcom.AtendimentoService;
import br.ufu.pizzaria87.service.garcom.PedidoForm;
import br.ufu.pizzaria87.service.garcom.PedidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * UC002 (Registrar Pedido na Ficha) e UC006 (Alterar ou Cancelar Pedido).
 */
@Controller
@RequestMapping("/garcom")
public class PedidoController {

    private final PedidoService pedidoService;
    private final AtendimentoService atendimentoService;

    public PedidoController(PedidoService pedidoService, AtendimentoService atendimentoService) {
        this.pedidoService = pedidoService;
        this.atendimentoService = atendimentoService;
    }

    // UC002 – tela de seleção de itens
    @GetMapping("/fichas/{numero}/pedidos/novo")
    public String novoPedido(@PathVariable Integer numero,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            atendimentoService.buscarAtendimentoAberto(numero);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/garcom/painel";
        }
        model.addAttribute("numeroFicha", numero);
        model.addAttribute("categorias", pedidoService.opcoesNovoPedido());
        model.addAttribute("editando", false);
        return "garcom/novo-pedido";
    }

    // UC002 – registrar pedido
    @PostMapping("/fichas/{numero}/pedidos")
    public String registrarPedido(@PathVariable Integer numero,
                                  @ModelAttribute PedidoForm pedidoForm,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            Pedido pedido = pedidoService.registrarPedido(numero, pedidoForm, AtendimentoController.usuarioLogado(session));
            redirectAttributes.addFlashAttribute("sucesso",
                    "Pedido #" + pedido.getId() + " enviado à cozinha (Pendente).");
            return "redirect:/garcom/fichas/" + numero;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/garcom/fichas/" + numero + "/pedidos/novo";
        }
    }

    // UC006 – tela de alteração (mesmo layout da seleção de itens, pré-preenchido)
    @GetMapping("/pedidos/{id}/editar")
    public String editarPedido(@PathVariable Long id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Pedido pedido;
        try {
            pedido = pedidoService.buscarPedido(id);
            pedidoService.exigirEditavel(pedido);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return redirecionarParaFichaDoPedido(id);
        }
        model.addAttribute("numeroFicha", pedido.getAtendimento().getFicha().getNumeroFicha());
        model.addAttribute("pedido", pedido);
        model.addAttribute("categorias", pedidoService.opcoesEdicao(pedido));
        model.addAttribute("editando", true);
        return "garcom/novo-pedido";
    }

    // UC006 – salvar alterações
    @PostMapping("/pedidos/{id}/alterar")
    public String alterarPedido(@PathVariable Long id,
                                @ModelAttribute PedidoForm pedidoForm,
                                RedirectAttributes redirectAttributes) {
        try {
            Pedido pedido = pedidoService.alterarPedido(id, pedidoForm);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido #" + id + " alterado.");
            return "redirect:/garcom/fichas/" + pedido.getAtendimento().getFicha().getNumeroFicha();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/garcom/pedidos/" + id + "/editar";
        }
    }

    // UC006 – cancelar pedido
    @PostMapping("/pedidos/{id}/cancelar")
    public String cancelarPedido(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            Pedido pedido = pedidoService.cancelarPedido(id);
            redirectAttributes.addFlashAttribute("sucesso",
                    "Pedido #" + id + " cancelado e retirado do total da ficha.");
            return "redirect:/garcom/fichas/" + pedido.getAtendimento().getFicha().getNumeroFicha();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return redirecionarParaFichaDoPedido(id);
        }
    }

    /** Em caso de erro, volta para a ficha do pedido (se ainda estiver aberta) ou para o painel. */
    private String redirecionarParaFichaDoPedido(Long pedidoId) {
        try {
            Pedido pedido = pedidoService.buscarPedido(pedidoId);
            if (pedido.getAtendimento() != null && pedido.getAtendimento().getDataFechamento() == null) {
                return "redirect:/garcom/fichas/" + pedido.getAtendimento().getFicha().getNumeroFicha();
            }
        } catch (IllegalArgumentException ignorada) {
            // pedido inexistente: cai no painel
        }
        return "redirect:/garcom/painel";
    }
}
