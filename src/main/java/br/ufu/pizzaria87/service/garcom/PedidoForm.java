package br.ufu.pizzaria87.service.garcom;

import java.util.ArrayList;
import java.util.List;

/**
 * Formulário de lançamento/alteração de pedido (UC002 / UC006).
 * Cada linha corresponde a um item do cardápio exibido na tela; linhas com
 * quantidade 0 (ou vazia) são ignoradas, pois o item não foi selecionado.
 */
public class PedidoForm {

    private List<Linha> linhas = new ArrayList<>();

    public List<Linha> getLinhas() {
        return linhas;
    }

    public void setLinhas(List<Linha> linhas) {
        this.linhas = linhas;
    }

    public static class Linha {

        private Long itemId;
        private Integer quantidade;
        private String observacao;

        public Linha() {
        }

        public Linha(Long itemId, Integer quantidade, String observacao) {
            this.itemId = itemId;
            this.quantidade = quantidade;
            this.observacao = observacao;
        }

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }

        public String getObservacao() {
            return observacao;
        }

        public void setObservacao(String observacao) {
            this.observacao = observacao;
        }
    }
}
