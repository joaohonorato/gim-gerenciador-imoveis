package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.CategoriaChamadoRepository;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.TipoContaRepository;
import br.com.imoveis.domain.chamado.CategoriaChamado;
import br.com.imoveis.domain.imovel.TipoConta;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;

// Dá um catálogo inicial pronto de uso pro proprietário recém-criado —
// tanto tipos de conta quanto categorias de chamado nascem vazios (o
// proprietário pode renomear, adicionar ou simplesmente ignorar), mas
// começar do zero absoluto é um atrito desnecessário de onboarding.
// Chamado uma única vez na criação do Proprietario (ver os 3 pontos de
// criação: RegistrarProprietarioAcesso, AceitarConviteAcesso,
// SolicitarMagicLink). Idempotente — só cria o que ainda não existir — pra
// ser seguro chamar mais de uma vez sem duplicar linhas.
@Singleton
public class SemearCatalogosPadrao {

    private static final List<String> TIPOS_CONTA_PADRAO = List.of("Luz", "Água", "IPTU", "Condomínio");
    private static final List<String> CATEGORIAS_CHAMADO_PADRAO = List.of("Elétrica", "Hidráulica", "Estrutural", "Outro");

    private final TipoContaRepository tipoContaRepository;
    private final CategoriaChamadoRepository categoriaChamadoRepository;
    private final Clock clock;

    public SemearCatalogosPadrao(TipoContaRepository tipoContaRepository,
                                  CategoriaChamadoRepository categoriaChamadoRepository,
                                  Clock clock) {
        this.tipoContaRepository = tipoContaRepository;
        this.categoriaChamadoRepository = categoriaChamadoRepository;
        this.clock = clock;
    }

    public void execute(UUID proprietarioId) {
        for (String nome : TIPOS_CONTA_PADRAO) {
            if (tipoContaRepository.findByProprietarioIdAndNome(proprietarioId, nome).isEmpty()) {
                tipoContaRepository.save(TipoConta.registrar(proprietarioId, nome, clock.now()));
            }
        }
        for (String nome : CATEGORIAS_CHAMADO_PADRAO) {
            if (categoriaChamadoRepository.findByProprietarioIdAndNome(proprietarioId, nome).isEmpty()) {
                categoriaChamadoRepository.save(CategoriaChamado.registrar(proprietarioId, nome, clock.now()));
            }
        }
    }
}
