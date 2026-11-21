package br.ufu.pizzaria87.service.admin;

import br.ufu.pizzaria87.model.ItemCardapio;
import br.ufu.pizzaria87.repository.ItemCardapioRepository;
import br.ufu.pizzaria87.repository.ItemPedidoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CardapioService {

    private final ItemCardapioRepository itemCardapioRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    public CardapioService(ItemCardapioRepository itemCardapioRepository,
                           ItemPedidoRepository itemPedidoRepository) {
        this.itemCardapioRepository = itemCardapioRepository;
        this.itemPedidoRepository = itemPedidoRepository;
    }

    public List<ItemCardapio> listarTodos() {
        return itemCardapioRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public Optional<ItemCardapio> buscarPorId(Long id) {
        return itemCardapioRepository.findById(id);
    }

    @Transactional
    public void salvar(ItemCardapio item) {
        validarCampos(item);

        String nomeLimpo = item.getNome().trim();
        if (itemCardapioRepository.existsByNomeIgnoreCase(nomeLimpo)) {
            throw new IllegalArgumentException("Já existe um item no cardápio com o nome '" + nomeLimpo + "'.");
        }

        item.setNome(nomeLimpo);
        item.setDescricao(item.getDescricao() != null ? item.getDescricao().trim() : "");
        item.setCategoria(item.getCategoria().trim());
        item.setPreco(normalizarPreco(item.getPreco()));

        if (item.getDisponivel() == null) {
            item.setDisponivel(true);
        }

        itemCardapioRepository.save(item);
    }

    @Transactional
    public void atualizar(Long id, ItemCardapio dadosAtualizados) {
        ItemCardapio existente = itemCardapioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item do cardápio não encontrado."));

        validarCampos(dadosAtualizados);

        String nomeLimpo = dadosAtualizados.getNome().trim();
        boolean nomeDuplicado = itemCardapioRepository.findAll().stream()
                .anyMatch(i -> i.getId() != null && !i.getId().equals(id)
                        && i.getNome() != null && i.getNome().equalsIgnoreCase(nomeLimpo));

        if (nomeDuplicado) {
            throw new IllegalArgumentException("Já existe outro item no cardápio com o nome '" + nomeLimpo + "'.");
        }

        existente.setNome(nomeLimpo);
        existente.setDescricao(dadosAtualizados.getDescricao() != null ? dadosAtualizados.getDescricao().trim() : "");
        existente.setCategoria(dadosAtualizados.getCategoria().trim());
        existente.setPreco(normalizarPreco(dadosAtualizados.getPreco()));

        if (dadosAtualizados.getDisponivel() != null) {
            existente.setDisponivel(dadosAtualizados.getDisponivel());
        }

        itemCardapioRepository.save(existente);
    }

    @Transactional
    public String alternarDisponibilidade(Long id) {
        ItemCardapio item = itemCardapioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item do cardápio não encontrado."));

        boolean novoStatus = !Boolean.TRUE.equals(item.getDisponivel());
        item.setDisponivel(novoStatus);
        itemCardapioRepository.save(item);

        return novoStatus
                ? "Item '" + item.getNome() + "' marcado como disponível."
                : "Item '" + item.getNome() + "' marcado como indisponível.";
    }

    @Transactional
    public String excluirOuDesativar(Long id) {
        ItemCardapio item = itemCardapioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item do cardápio não encontrado."));

        if (possuiHistoricoEmPedidos(item.getId())) {
            item.setDisponivel(false);
            itemCardapioRepository.save(item);
            return "Item já possui histórico em pedidos: foi marcado como indisponível (disponivel = false) para preservar o histórico.";
        }

        itemCardapioRepository.delete(item);
        return "Item '" + item.getNome() + "' excluído com sucesso.";
    }

    private boolean possuiHistoricoEmPedidos(Long itemId) {
        return itemPedidoRepository.findAll().stream()
                .anyMatch(ip -> ip.getItemCardapio() != null && itemId.equals(ip.getItemCardapio().getId()));
    }

    private void validarCampos(ItemCardapio item) {
        if (item == null) {
            throw new IllegalArgumentException("Dados do item inválidos.");
        }
        if (item.getNome() == null || item.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do item é obrigatório.");
        }
        if (item.getCategoria() == null || item.getCategoria().isBlank()) {
            throw new IllegalArgumentException("A categoria do item é obrigatória.");
        }
        if (item.getPreco() == null || item.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço do item deve ser maior que zero.");
        }
    }

    private BigDecimal normalizarPreco(BigDecimal preco) {
        return preco;
    }
}