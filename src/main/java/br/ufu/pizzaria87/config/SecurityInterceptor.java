package br.ufu.pizzaria87.config;

import br.ufu.pizzaria87.controller.LoginController;
import br.ufu.pizzaria87.model.Funcionario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);

        Funcionario funcionario = (session != null)
                ? (Funcionario) session.getAttribute(LoginController.SESSAO_FUNCIONARIO)
                : null;

        if (funcionario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        if (Boolean.FALSE.equals(funcionario.getAtivo())) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String cargo = funcionario.getCargo() != null ? funcionario.getCargo().toUpperCase() : "";

        if (uri.startsWith(request.getContextPath() + "/admin") && !"ADMINISTRADOR".equals(cargo)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso restrito a administradores.");
            return false;
        }

        if (uri.startsWith(request.getContextPath() + "/garcom") && !"GARCOM".equals(cargo) && !"ADMINISTRADOR".equals(cargo)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso restrito a garçons.");
            return false;
        }

        if (uri.startsWith(request.getContextPath() + "/cozinha") && !"COZINHEIRO".equals(cargo) && !"ADMINISTRADOR".equals(cargo)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso restrito à cozinha.");
            return false;
        }

        return true;
    }
}