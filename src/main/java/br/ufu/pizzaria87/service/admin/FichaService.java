package br.ufu.pizzaria87.service.admin;

import br.ufu.pizzaria87.model.Ficha;
import br.ufu.pizzaria87.repository.FichaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FichaService {

    public static final String STATUS_LIVRE = "LIVRE";
    public static final String STATUS_OCUPADA = "OCUPADA";
    public static final String STATUS_DESATIVADA = "DESATIVADA";

    private final FichaRepository fichaRepository;

    public FichaService(FichaRepository fichaRepository) {
        this.fichaRepository = fichaRepository;
    }

    public List<Ficha> listarTodas() {
        return fichaRepository.findAll(Sort.by(Sort.Direction.ASC, "numeroFicha"));
    }

    @Transactional
    public void cadastrarFicha(Integer numeroFicha) {
        if (numeroFicha == null || numeroFicha <= 0) {
            throw new IllegalArgumentException("Informe um número de ficha válido maior que zero.");
        }

        if (fichaRepository.existsById(numeroFicha)) {
            throw new IllegalArgumentException("A ficha de número " + numeroFicha + " já está cadastrada.");
        }

        Ficha novaFicha = new Ficha();
        novaFicha.setNumeroFicha(numeroFicha);
        novaFicha.setStatus(STATUS_LIVRE);

        fichaRepository.save(novaFicha);
    }

    @Transactional
    public String alternarStatus(Integer numeroFicha) {
        Ficha ficha = fichaRepository.findById(numeroFicha)
                .orElseThrow(() -> new IllegalArgumentException("Ficha não encontrada."));

        String statusAtual = ficha.getStatus() != null ? ficha.getStatus().toUpperCase() : "";

        if (STATUS_OCUPADA.equals(statusAtual)) {
            throw new IllegalArgumentException("A ficha " + numeroFicha + " está OCUPADA e não pode ser desativada enquanto houver atendimento em aberto.");
        }

        if (STATUS_DESATIVADA.equals(statusAtual)) {
            ficha.setStatus(STATUS_LIVRE);
            fichaRepository.save(ficha);
            return "Ficha " + numeroFicha + " ativada (status LIVRE).";
        }

        ficha.setStatus(STATUS_DESATIVADA);
        fichaRepository.save(ficha);
        return "Ficha " + numeroFicha + " desativada com sucesso.";
    }
}