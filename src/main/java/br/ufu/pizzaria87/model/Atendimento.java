package br.ufu.pizzaria87.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "atendimento")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @ManyToOne
    @JoinColumn(name = "numero_ficha", nullable = false)
    private Ficha ficha;

    @ManyToOne
    @JoinColumn(name = "funcionario_abertura_id", nullable = false)
    private Funcionario funcionarioAbertura;

    @ManyToOne
    @JoinColumn(name = "funcionario_fechamento_id")
    private Funcionario funcionarioFechamento;

    public Atendimento() {
    }

    public Atendimento(LocalDateTime dataAbertura, Ficha ficha, Funcionario funcionarioAbertura) {
        this.dataAbertura = dataAbertura;
        this.ficha = ficha;
        this.funcionarioAbertura = funcionarioAbertura;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public Ficha getFicha() {
        return ficha;
    }

    public void setFicha(Ficha ficha) {
        this.ficha = ficha;
    }

    public Funcionario getFuncionarioAbertura() {
        return funcionarioAbertura;
    }

    public void setFuncionarioAbertura(Funcionario funcionarioAbertura) {
        this.funcionarioAbertura = funcionarioAbertura;
    }

    public Funcionario getFuncionarioFechamento() {
        return funcionarioFechamento;
    }

    public void setFuncionarioFechamento(Funcionario funcionarioFechamento) {
        this.funcionarioFechamento = funcionarioFechamento;
    }
}