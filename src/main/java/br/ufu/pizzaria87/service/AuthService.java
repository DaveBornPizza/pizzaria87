package br.ufu.pizzaria87.service;

import br.ufu.pizzaria87.model.Funcionario;
import br.ufu.pizzaria87.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final FuncionarioRepository funcionarioRepository;

    public AuthService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    public ResultadoAutenticacao autenticar(String identificador, String pinAcesso) {
        if (identificador == null || identificador.isBlank() || pinAcesso == null || pinAcesso.isBlank()) {
            return ResultadoAutenticacao.erro("Informe o identificador e o PIN de 4 dígitos.");
        }

        Optional<Funcionario> encontrado = funcionarioRepository
                .findByIdentificadorAndPinAcesso(identificador.trim(), pinAcesso.trim());

        if (encontrado.isEmpty()) {
            return ResultadoAutenticacao.erro("Identificador ou PIN inválido.");
        }

        Funcionario funcionario = encontrado.get();

        if (Boolean.FALSE.equals(funcionario.getAtivo())) {
            return ResultadoAutenticacao.erro("Cadastro inativo. Procure o administrador.");
        }

        return ResultadoAutenticacao.sucesso(funcionario);
    }

    public record ResultadoAutenticacao(boolean autenticado, Funcionario funcionario, String mensagemErro) {
        public static ResultadoAutenticacao sucesso(Funcionario funcionario) {
            return new ResultadoAutenticacao(true, funcionario, null);
        }

        public static ResultadoAutenticacao erro(String mensagemErro) {
            return new ResultadoAutenticacao(false, null, mensagemErro);
        }
    }
}