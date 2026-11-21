package br.ufu.pizzaria87.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipo; // Ex.: PEDIDO_PRONTO, FALTA_INGREDIENTE

    @Column(nullable = false)
    private String mensagem;

    @Column(nullable = false)
    private Boolean lida = false;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "funcionario_destino_id", nullable = false)
    private Funcionario funcionarioDestino;

    public Notificacao() {
    }

    public Notificacao(String tipo, String mensagem, LocalDateTime dataHora, Pedido pedido, Funcionario funcionarioDestino) {
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.lida = false;
        this.dataHora = dataHora;
        this.pedido = pedido;
        this.funcionarioDestino = funcionarioDestino;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public Boolean getLida() {
        return lida;
    }

    public void setLida(Boolean lida) {
        this.lida = lida;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Funcionario getFuncionarioDestino() {
        return funcionarioDestino;
    }

    public void setFuncionarioDestino(Funcionario funcionarioDestino) {
        this.funcionarioDestino = funcionarioDestino;
    }
}