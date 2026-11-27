package br.ufu.pizzaria87.controller.garcom;

import br.ufu.pizzaria87.controller.LoginController;
import br.ufu.pizzaria87.model.Atendimento;
import br.ufu.pizzaria87.model.Funcionario;
import br.ufu.pizzaria87.service.garcom.AtendimentoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Painel do garçom, UC005 (Abrir Ficha) e UC004 (Consultar e Fechar Ficha).
 */
@Controller
@RequestMapping("/garcom")
public class AtendimentoController {

    private final AtendimentoService atendimentoService;

    public AtendimentoController(AtendimentoService atendimentoService) {
        this.atendimentoService = atendimentoService;
    }

    @GetMapping({"", "/", "/painel"})
    public String painel(@RequestParam(required = false) String filtro,
                         @RequestParam(required = false) Integer numero,
                         Model model) {
        try {
            model.addAttribute("painel", atendimentoService.montarPainel(filtro, numero));
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("painel", atendimentoService.montarPainel(filtro, null));
        }
        model.addAttribute("numeroBuscado", numero);
        return "garcom/painel";
    }

    // UC005 – Abrir Ficha
    @PostMapping("/fichas/{numero}/abrir")
    public String abrirFicha(@PathVariable Integer numero,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            atendimentoService.abrirFicha(numero, usuarioLogado(session));
            redirectAttributes.addFlashAttribute("sucesso", "Ficha " + numero + " aberta. Bom atendimento!");
            return "redirect:/garcom/fichas/" + numero;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/garcom/painel";
        }
    }

    // Detalhe da ficha ocupada: pedidos, status e consumo parcial
    @GetMapping("/fichas/{numero}")
    public String detalheFicha(@PathVariable Integer numero,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("detalhe", atendimentoService.detalharFicha(numero));
            return "garcom/ficha";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/garcom/painel";
        }
    }

    // UC004 – extrato da conta
    @GetMapping("/fichas/{numero}/extrato")
    public String extrato(@PathVariable Integer numero,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("extrato", atendimentoService.gerarExtrato(numero));
            return "garcom/extrato";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/garcom/painel";
        }
    }

    // UC004 – fechamento da conta
    @PostMapping("/fichas/{numero}/fechar")
    public String fecharFicha(@PathVariable Integer numero,
                              @RequestParam(defaultValue = "false") boolean confirmarPendencias,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            Atendimento atendimento = atendimentoService.fecharFicha(numero, confirmarPendencias, usuarioLogado(session));
            redirectAttributes.addFlashAttribute("sucesso", "Conta da ficha " + numero + " fechada. Total: R$ "
                    + formatarMoeda(atendimento.getValorTotal()) + ". A ficha está livre novamente.");
            return "redirect:/garcom/painel";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/garcom/fichas/" + numero + "/extrato";
        }
    }

    // Liberação de ficha aberta por engano (sem nenhum pedido registrado)
    @PostMapping("/fichas/{numero}/liberar")
    public String liberarFicha(@PathVariable Integer numero,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            atendimentoService.liberarFichaSemPedidos(numero, usuarioLogado(session));
            redirectAttributes.addFlashAttribute("sucesso", "Ficha " + numero + " liberada (não havia consumo).");
            return "redirect:/garcom/painel";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/garcom/fichas/" + numero + "/extrato";
        }
    }

    static Funcionario usuarioLogado(HttpSession session) {
        return (Funcionario) session.getAttribute(LoginController.SESSAO_FUNCIONARIO);
    }

    private static String formatarMoeda(BigDecimal valor) {
        BigDecimal v = valor != null ? valor.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        return v.toPlainString().replace('.', ',');
    }
}
