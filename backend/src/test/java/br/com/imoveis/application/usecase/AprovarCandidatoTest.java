package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.fakes.FakeContratoRepository;
import br.com.imoveis.application.fakes.FakeConviteRepository;
import br.com.imoveis.application.fakes.FakeImovelRepository;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.contrato.TipoContrato;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.UnidadeStatus;
import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.domain.shared.Periodo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AprovarCandidatoTest {

    private final FakeConviteRepository conviteRepo = new FakeConviteRepository();
    private final FakeContratoRepository contratoRepo = new FakeContratoRepository();
    private final FakeImovelRepository imovelRepo = new FakeImovelRepository();
    private final AprovarCandidato useCase = new AprovarCandidato(conviteRepo, contratoRepo, imovelRepo);

    @Test
    void aprovaQuandoNaoHaSobreposicao() {
        UUID proprietarioId = UUID.randomUUID();
        UUID inquilinoId = UUID.randomUUID();
        Imovel imovel = Imovel.cadastrar(proprietarioId, "Rua A", "SP", "M-1", Instant.now());
        imovelRepo.save(imovel);
        Convite convite = Convite.gerar(imovel.id(), imovel.unidadePadrao().id(), proprietarioId,
            condicoes(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), Instant.now());
        conviteRepo.save(convite);
        Candidatura candidatura = Candidatura.nova(convite.id(), inquilinoId, Instant.now());
        candidatura.definirGarantia(GarantiaTipo.CAUCAO, "{}");
        conviteRepo.saveCandidatura(candidatura);

        Contrato contrato = useCase.execute(candidatura.id(), proprietarioId);

        assertThat(contrato.statusAssinatura()).isEqualTo(ContratoAssinaturaStatus.PENDENTE);
        assertThat(imovelRepo.findUnidadeById(imovel.unidadePadrao().id()).orElseThrow().status())
            .isEqualTo(UnidadeStatus.RESERVADO);
    }

    @Test
    void rejeitaQuandoHaContratoAssinadoSobreposto() {
        UUID proprietarioId = UUID.randomUUID();
        Imovel imovel = Imovel.cadastrar(proprietarioId, "Rua A", "SP", "M-1", Instant.now());
        imovelRepo.save(imovel);

        Contrato existente = Contrato.novo(imovel.unidadePadrao().id(), UUID.randomUUID(), proprietarioId, UUID.randomUUID(),
            new Periodo(LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31)),
            TipoContrato.RESIDENCIAL, Dinheiro.of(2000), "IPCA");
        existente.definirGarantia(GarantiaTipo.CAUCAO, LocalDate.of(2027, 5, 31), "{}");
        existente.assinar(ParteContrato.PROPRIETARIO);
        existente.assinar(ParteContrato.INQUILINO);
        contratoRepo.save(existente);

        Convite convite = Convite.gerar(imovel.id(), imovel.unidadePadrao().id(), proprietarioId,
            condicoes(LocalDate.of(2026, 8, 1), LocalDate.of(2027, 7, 31)), Instant.now());
        conviteRepo.save(convite);
        Candidatura candidatura = Candidatura.nova(convite.id(), UUID.randomUUID(), Instant.now());
        candidatura.definirGarantia(GarantiaTipo.FIADOR, "{}");
        conviteRepo.saveCandidatura(candidatura);

        assertThatThrownBy(() -> useCase.execute(candidatura.id(), proprietarioId))
            .isInstanceOf(ConflitoException.class);
    }

    private Convite.CondicoesConvite condicoes(LocalDate inicio, LocalDate fim) {
        return new Convite.CondicoesConvite(
            TipoContrato.RESIDENCIAL, Dinheiro.of(2500),
            new Periodo(inicio, fim), GarantiaTipo.CAUCAO);
    }
}
