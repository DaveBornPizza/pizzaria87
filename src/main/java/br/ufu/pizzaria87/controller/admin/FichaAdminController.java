package br.ufu.pizzaria87.controller.admin;

import br.ufu.pizzaria87.controller.LoginController;
import br.ufu.pizzaria87.model.Ficha;
import br.ufu.pizzaria87.service.admin.FichaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/fichas")
public class FichaAdminController {

    private final FichaService fichaService;

    public FichaAdminController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    @GetMapping
    public String listar(Model model, HttpSession session) {
        List<Ficha> fichas = fichaService.listarTodas();
        long livres = fichas.stream().filter(f -> "LIVRE".equalsIgnoreCase(f.getStatus())).count();
        long ocupadas = fichas.stream().filter(f -> "OCUPADA".equalsIgnoreCase(f.getStatus())).count();
        long desativadas = fichas.stream().filter(f -> "DESATIVADA".equalsIgnoreCase(f.getStatus())).count();

        model.addAttribute("fichas", fichas);
        model.addAttribute("totalLivres", livres);
        model.addAttribute("totalOcupadas", ocupadas);
        model.addAttribute("totalDesativadas", desativadas);
        model.addAttribute("usuarioLogado", session.getAttribute(LoginController.SESSAO_FUNCIONARIO));
        return "admin/fichas/lista";
    }

    @PostMapping("/salvar")
    public String salvar(@RequestParam(required = false) Integer numeroFicha,
                         RedirectAttributes redirectAttributes) {
        try {
            fichaService.cadastrarFicha(numeroFicha);
            redirectAttributes.addFlashAttribute("sucesso", "Ficha " + numeroFicha + " cadastrada com status LIVRE.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/fichas";
    }

    @PostMapping("/status/{numeroFicha}")
    public String alternarStatus(@PathVariable Integer numeroFicha,
                                 RedirectAttributes redirectAttributes) {
        try {
            String mensagem = fichaService.alternarStatus(numeroFicha);
            redirectAttributes.addFlashAttribute("sucesso", mensagem);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/fichas";
    }
}