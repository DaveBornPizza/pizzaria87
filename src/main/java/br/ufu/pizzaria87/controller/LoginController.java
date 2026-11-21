package br.ufu.pizzaria87.controller;

import br.ufu.pizzaria87.model.Funcionario;
import br.ufu.pizzaria87.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    public static final String SESSAO_FUNCIONARIO = "funcionarioLogado";

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping({"/", "/login"})
    public String exibirLogin(HttpSession session) {
        Object logado = session.getAttribute(SESSAO_FUNCIONARIO);
        if (logado instanceof Funcionario funcionario) {
            return "redirect:" + rotaPorCargo(funcionario.getCargo());
        }
        return "login";
    }

    @PostMapping("/login")
    public String processarLogin(@RequestParam String identificador,
                                 @RequestParam String pinAcesso,
                                 HttpSession session,
                                 Model model) {
        AuthService.ResultadoAutenticacao resultado = authService.autenticar(identificador, pinAcesso);

        if (!resultado.autenticado()) {
            model.addAttribute("erro", resultado.mensagemErro());
            model.addAttribute("identificador", identificador);
            return "login";
        }

        Funcionario funcionario = resultado.funcionario();
        session.setAttribute(SESSAO_FUNCIONARIO, funcionario);

        return "redirect:" + rotaPorCargo(funcionario.getCargo());
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private String rotaPorCargo(String cargo) {
        if (cargo == null) {
            return "/login";
        }
        return switch (cargo.toUpperCase()) {
            case "ADMINISTRADOR" -> "/admin/funcionarios";
            case "GARCOM" -> "/garcom/painel";
            case "COZINHEIRO" -> "/cozinha/kds";
            default -> "/login";
        };
    }
}