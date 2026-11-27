package br.ufu.pizzaria87.controller.garcom;

import br.ufu.pizzaria87.controller.LoginController;
import br.ufu.pizzaria87.model.Funcionario;
import br.ufu.pizzaria87.service.garcom.NotificacaoGarcomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Disponibiliza, em todas as telas do garçom, os dados do sino de notificações
 * e o usuário logado. Restrito aos controllers do pacote garcom, então as telas
 * de admin e da cozinha não são afetadas.
 */
@ControllerAdvice(basePackageClasses = SinoNotificacoesAdvice.class)
public class SinoNotificacoesAdvice {

    private final NotificacaoGarcomService notificacaoGarcomService;

    public SinoNotificacoesAdvice(NotificacaoGarcomService notificacaoGarcomService) {
        this.notificacaoGarcomService = notificacaoGarcomService;
    }

    @ModelAttribute
    public void adicionarDadosDoCabecalho(Model model, HttpSession session, HttpServletRequest request) {
        Object logado = session.getAttribute(LoginController.SESSAO_FUNCIONARIO);
        if (logado instanceof Funcionario funcionario) {
            model.addAttribute("usuarioLogado", funcionario);
            model.addAttribute("sino", notificacaoGarcomService.carregarSino(funcionario.getId()));
        }
        // Usado pelo sino para voltar à mesma tela depois de marcar uma notificação como lida.
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        String query = request.getQueryString();
        model.addAttribute("uriAtual", query != null ? uri + "?" + query : uri);
    }
}
