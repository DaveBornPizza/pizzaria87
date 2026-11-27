package br.ufu.pizzaria87.controller.garcom;

import br.ufu.pizzaria87.model.Funcionario;
import br.ufu.pizzaria87.service.garcom.NotificacaoGarcomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ações do sino de notificações do garçom (somente leitura/marcação como lida).
 */
@Controller
@RequestMapping("/garcom/notificacoes")
public class NotificacaoGarcomController {

    private final NotificacaoGarcomService notificacaoGarcomService;

    public NotificacaoGarcomController(NotificacaoGarcomService notificacaoGarcomService) {
        this.notificacaoGarcomService = notificacaoGarcomService;
    }

    @PostMapping("/{id}/lida")
    public String marcarComoLida(@PathVariable Long id,
                                 @RequestParam(required = false) String voltar,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Funcionario logado = AtendimentoController.usuarioLogado(session);
        try {
            notificacaoGarcomService.marcarComoLida(id, logado.getId());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:" + destinoSeguro(voltar);
    }

    @PostMapping("/lidas")
    public String marcarTodasComoLidas(@RequestParam(required = false) String voltar,
                                       HttpSession session) {
        Funcionario logado = AtendimentoController.usuarioLogado(session);
        notificacaoGarcomService.marcarTodasComoLidas(logado.getId());
        return "redirect:" + destinoSeguro(voltar);
    }

    /** Só permite voltar para telas internas do garçom (evita redirecionamento aberto). */
    private static String destinoSeguro(String voltar) {
        if (voltar != null && voltar.startsWith("/garcom/") && !voltar.startsWith("//")
                && !voltar.contains("\\") && !voltar.contains("://")
                && !voltar.startsWith("/garcom/notificacoes")) {
            return voltar;
        }
        return "/garcom/painel";
    }
}
