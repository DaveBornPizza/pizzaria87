package br.ufu.pizzaria87.model;

import jakarta.persistence.*;

@Entity
@Table(name = "funcionario")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String identificador;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cargo; // Ex.: ADMINISTRADOR, GARCOM, COZINHEIRO

    @Column(name = "pin_acesso", nullable = false, length = 4)
    private String pinAcesso;

    @Column(nullable = false)
    private Boolean ativo = true;

    public Funcionario() {
    }

    public Funcionario(String identificador, String nome, String cargo, String pinAcesso, Boolean ativo) {
        this.identificador = identificador;
        this.nome = nome;
        this.cargo = cargo;
        this.pinAcesso = pinAcesso;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getPinAcesso() {
        return pinAcesso;
    }

    public void setPinAcesso(String pinAcesso) {
        this.pinAcesso = pinAcesso;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}