package br.ufu.pizzaria87.controller.admin;

import br.ufu.pizzaria87.controller.LoginController;
import br.ufu.pizzaria87.model.Funcionario;
import br.ufu.pizzaria87.service.admin.FuncionarioService;
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
@RequestMapping("/admin/funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (!model.containsAttribute("funcionarioForm")) {
            Funcionario novo = new Funcionario();
            novo.setAtivo(true);
            model.addAttribute("funcionarioForm", novo);
        }
        model.addAttribute("funcionarios", funcionarioService.listarTodos());
        model.addAttribute("usuarioLogado", session.getAttribute(LoginController.SESSAO_FUNCIONARIO));
        return "admin/funcionarios/lista";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("funcionarioForm") Funcionario funcionario,
                         RedirectAttributes redirectAttributes) {
        try {
            funcionarioService.salvar(funcionario);
            redirectAttributes.addFlashAttribute("sucesso", "Funcionário cadastrado com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            redirectAttributes.addFlashAttribute("funcionarioForm", funcionario);
        }
        return "redirect:/admin/funcionarios";
    }

    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id,
                             Model model,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        return funcionarioService.buscarPorId(id)
                .map(funcionario -> {
                    model.addAttribute("funcionarioForm", funcionario);
                    model.addAttribute("funcionarios", funcionarioService.listarTodos());
                    model.addAttribute("usuarioLogado", session.getAttribute(LoginController.SESSAO_FUNCIONARIO));
                    model.addAttribute("editando", true);
                    return "admin/funcionarios/lista";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("erro", "Funcionário não encontrado.");
                    return "redirect:/admin/funcionarios";
                });
    }

    @PostMapping("/atualizar/{id}")
    public String atualizar(@PathVariable Long id,
                            @ModelAttribute("funcionarioForm") Funcionario funcionario,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Funcionario logado = (Funcionario) session.getAttribute(LoginController.SESSAO_FUNCIONARIO);
        try {
            funcionarioService.atualizar(id, funcionario, logado);
            redirectAttributes.addFlashAttribute("sucesso", "Dados do funcionário atualizados com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/status/{id}")
    public String alternarStatus(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Funcionario logado = (Funcionario) session.getAttribute(LoginController.SESSAO_FUNCIONARIO);
        try {
            String mensagem = funcionarioService.alternarStatusAtivo(id, logado);
            redirectAttributes.addFlashAttribute("sucesso", mensagem);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/funcionarios";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        Funcionario logado = (Funcionario) session.getAttribute(LoginController.SESSAO_FUNCIONARIO);
        try {
            String mensagem = funcionarioService.excluirOuDesativar(id, logado);
            redirectAttributes.addFlashAttribute("sucesso", mensagem);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/funcionarios";
    }
}