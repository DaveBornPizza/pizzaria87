package br.ufu.pizzaria87.service.admin;

import br.ufu.pizzaria87.model.Funcionario;
import br.ufu.pizzaria87.repository.AtendimentoRepository;
import br.ufu.pizzaria87.repository.FuncionarioRepository;
import br.ufu.pizzaria87.repository.PedidoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final PedidoRepository pedidoRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository,
                              AtendimentoRepository atendimentoRepository,
                              PedidoRepository pedidoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public List<Funcionario> listarTodos() {
        return funcionarioRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public Optional<Funcionario> buscarPorId(Long id) {
        return funcionarioRepository.findById(id);
    }

    @Transactional
    public void salvar(Funcionario funcionario) {
        validarCamposObrigatorios(funcionario);

        String identificadorLimpo = funcionario.getIdentificador().trim();
        String pinLimpo = funcionario.getPinAcesso().trim();
        String cargoLimpo = funcionario.getCargo().trim().toUpperCase();

        if (funcionarioRepository.existsByIdentificador(identificadorLimpo)) {
            throw new IllegalArgumentException("Já existe um funcionário cadastrado com o identificador '" + identificadorLimpo + "'.");
        }

        funcionario.setNome(funcionario.getNome().trim());
        funcionario.setIdentificador(identificadorLimpo);
        funcionario.setPinAcesso(pinLimpo);
        funcionario.setCargo(cargoLimpo);

        if (funcionario.getAtivo() == null) {
            funcionario.setAtivo(true);
        }

        funcionarioRepository.save(funcionario);
    }

    @Transactional
    public void atualizar(Long id, Funcionario dadosAtualizados, Funcionario usuarioLogado) {
        Funcionario existente = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));

        validarCamposObrigatorios(dadosAtualizados);

        String identificadorLimpo = dadosAtualizados.getIdentificador().trim();
        String pinLimpo = dadosAtualizados.getPinAcesso().trim();
        String cargoLimpo = dadosAtualizados.getCargo().trim().toUpperCase();

        funcionarioRepository.findAll().stream()
                .filter(f -> !f.getId().equals(id) && f.getIdentificador().equalsIgnoreCase(identificadorLimpo))
                .findFirst()
                .ifPresent(f -> {
                    throw new IllegalArgumentException("Já existe outro funcionário utilizando o identificador '" + identificadorLimpo + "'.");
                });

        boolean tentandoDesativar = Boolean.FALSE.equals(dadosAtualizados.getAtivo());
        if (tentandoDesativar && ehMesmoFuncionario(existente, usuarioLogado)) {
            throw new IllegalArgumentException("Você não pode desativar a sua própria conta enquanto estiver logado.");
        }

        existente.setNome(dadosAtualizados.getNome().trim());
        existente.setIdentificador(identificadorLimpo);
        existente.setPinAcesso(pinLimpo);
        existente.setCargo(cargoLimpo);

        if (dadosAtualizados.getAtivo() != null) {
            existente.setAtivo(dadosAtualizados.getAtivo());
        }

        funcionarioRepository.save(existente);
    }

    @Transactional
    public String alternarStatusAtivo(Long id, Funcionario usuarioLogado) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));

        boolean vaiDesativar = Boolean.TRUE.equals(funcionario.getAtivo());
        if (vaiDesativar && ehMesmoFuncionario(funcionario, usuarioLogado)) {
            throw new IllegalArgumentException("Você não pode desativar a sua própria conta.");
        }

        funcionario.setAtivo(!vaiDesativar);
        funcionarioRepository.save(funcionario);

        return funcionario.getAtivo()
                ? "Funcionário '" + funcionario.getNome() + "' ativado com sucesso."
                : "Funcionário '" + funcionario.getNome() + "' desativado com sucesso.";
    }

    @Transactional
    public String excluirOuDesativar(Long id, Funcionario usuarioLogado) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));

        if (ehMesmoFuncionario(funcionario, usuarioLogado)) {
            throw new IllegalArgumentException("Você não pode excluir a sua própria conta enquanto estiver logado.");
        }

        if (possuiHistorico(funcionario.getId())) {
            funcionario.setAtivo(false);
            funcionarioRepository.save(funcionario);
            return "Funcionário possui histórico de atendimentos ou pedidos: o registro foi desativado para preservar o histórico.";
        }

        funcionarioRepository.delete(funcionario);
        return "Funcionário '" + funcionario.getNome() + "' excluído com sucesso.";
    }

    private boolean possuiHistorico(Long funcionarioId) {
        boolean temAtendimento = atendimentoRepository.findAll().stream()
                .anyMatch(a ->
                        (a.getFuncionarioAbertura() != null && funcionarioId.equals(a.getFuncionarioAbertura().getId()))
                                || (a.getFuncionarioFechamento() != null && funcionarioId.equals(a.getFuncionarioFechamento().getId()))
                );

        if (temAtendimento) {
            return true;
        }

        return pedidoRepository.findAll().stream()
                .anyMatch(p -> p.getFuncionario() != null && funcionarioId.equals(p.getFuncionario().getId()));
    }

    private boolean ehMesmoFuncionario(Funcionario alvo, Funcionario logado) {
        return logado != null && alvo != null && alvo.getId() != null && alvo.getId().equals(logado.getId());
    }

    private void validarCamposObrigatorios(Funcionario funcionario) {
        if (funcionario == null) {
            throw new IllegalArgumentException("Dados do funcionário inválidos.");
        }
        if (funcionario.getNome() == null || funcionario.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do funcionário é obrigatório.");
        }
        if (funcionario.getIdentificador() == null || funcionario.getIdentificador().isBlank()) {
            throw new IllegalArgumentException("O identificador de acesso é obrigatório.");
        }
        if (funcionario.getPinAcesso() == null || !funcionario.getPinAcesso().trim().matches("\\d{4}")) {
            throw new IllegalArgumentException("O PIN de acesso deve conter exatamente 4 dígitos numéricos.");
        }
        if (funcionario.getCargo() == null || funcionario.getCargo().isBlank()) {
            throw new IllegalArgumentException("O cargo do funcionário é obrigatório.");
        }
    }
}