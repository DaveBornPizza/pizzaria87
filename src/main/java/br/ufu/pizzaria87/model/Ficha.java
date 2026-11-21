package br.ufu.pizzaria87.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ficha")
public class Ficha {

    @Id
    @Column(name = "numero_ficha", nullable = false)
    private Integer numeroFicha;

    @Column(nullable = false)
    private String status; // Ex.: LIVRE, OCUPADA, DESATIVADA

    public Ficha() {
    }

    public Ficha(Integer numeroFicha, String status) {
        this.numeroFicha = numeroFicha;
        this.status = status;
    }

    public Integer getNumeroFicha() {
        return numeroFicha;
    }

    public void setNumeroFicha(Integer numeroFicha) {
        this.numeroFicha = numeroFicha;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}