package br.ufu.pizzaria87.controller.admin;

import br.ufu.pizzaria87.controller.LoginController;
import br.ufu.pizzaria87.model.ItemCardapio;
import br.ufu.pizzaria87.service.admin.CardapioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/cardapio")
public class CardapioController {

    private final CardapioService cardapioService;

    public CardapioController(CardapioService cardapioService) {
        this.cardapioService = cardapioService;
    }

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (!model.containsAttribute("itemForm")) {
            ItemCardapio novo = new ItemCardapio();
            novo.setDisponivel(true);
            model.addAttribute("itemForm", novo);
        }
        model.addAttribute("itens", cardapioService.listarTodos());
        model.addAttribute("usuarioLogado", session.getAttribute(LoginController.SESSAO_FUNCIONARIO));
        return "admin/cardapio/lista";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("itemForm") ItemCardapio item,
                         RedirectAttributes redirectAttributes) {
        try {
            cardapioService.salvar(item);
            redirectAttributes.addFlashAttribute("sucesso", "Item adicionado ao cardápio com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            redirectAttributes.addFlashAttribute("itemForm", item);
        }
        return "redirect:/admin/cardapio";
    }

    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id,
                             Model model,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        return cardapioService.buscarPorId(id)
                .map(item -> {
                    model.addAttribute("itemForm", item);
                    model.addAttribute("itens", cardapioService.listarTodos());
                    model.addAttribute("usuarioLogado", session.getAttribute(LoginController.SESSAO_FUNCIONARIO));
                    model.addAttribute("editando", true);
                    return "admin/cardapio/lista";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("erro", "Item do cardápio não encontrado.");
                    return "redirect:/admin/cardapio";
                });
    }

    @PostMapping("/atualizar/{id}")
    public String atualizar(@PathVariable Long id,
                            @ModelAttribute("itemForm") ItemCardapio item,
                            RedirectAttributes redirectAttributes) {
        try {
            cardapioService.atualizar(id, item);
            redirectAttributes.addFlashAttribute("sucesso", "Item do cardápio atualizado com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/cardapio";
    }

    @PostMapping("/disponibilidade/{id}")
    public String alternarDisponibilidade(@PathVariable Long id,
                                          RedirectAttributes redirectAttributes) {
        try {
            String mensagem = cardapioService.alternarDisponibilidade(id);
            redirectAttributes.addFlashAttribute("sucesso", mensagem);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/cardapio";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id,
                          RedirectAttributes redirectAttributes) {
        try {
            String mensagem = cardapioService.excluirOuDesativar(id);
            redirectAttributes.addFlashAttribute("sucesso", mensagem);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/cardapio";
    }
}