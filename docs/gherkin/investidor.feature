# language: pt-BR
# Jornada do Investidor — ver docs/jornadas-e-prioridades-negocio.md (seção 1)
# e o diagrama de sequência correspondente.
#
# O Investidor usa a mesma conta/autenticação do Proprietário — não é um tipo de
# conta separado no sistema, é uma lente de uso sobre os mesmos dados (ver nota de
# mapeamento de persona no documento de jornadas). Por isso os cenários abaixo
# reaproveitam autenticação de proprietário.
#
# Tags:
#   @implementado — comportamento já existe hoje.
#   @pendente     — especificação de comportamento ainda não construído
#                   (ver docs/jornadas-e-prioridades-negocio.md). Não automatizar
#                   como teste de regressão antes da funcionalidade existir.

Funcionalidade: Jornada do Investidor
  Como investidor com uma carteira de imóveis
  Quero enxergar rapidamente se a carteira está rendendo
  Para decidir se compro, mantenho ou vendo um imóvel sem depender de planilha

  @implementado
  Cenário: Contadores agregados na lista de imóveis
    Dado que estou autenticado e tenho 4 imóveis cadastrados, 1 alugado e 3 vagos
    Quando eu abro o hub de Imóveis
    Então vejo os contadores "4 cadastrados / 1 alugado / 3 vagos" no topo da lista

  @implementado
  Cenário: Consulta de pagamentos por contrato
    Dado que tenho um contrato assinado com pagamentos gerados
    Quando eu abro o hub de Pagamentos filtrado por esse contrato
    Então vejo a lista de pagamentos com status "PENDENTE", "PAGO" ou "ATRASADO"

  @implementado
  Cenário: Extrato de pagamentos permanece em ordem cronológica
    Dado que um contrato tem 12 pagamentos mensais gerados
    Quando o primeiro e o segundo pagamento são confirmados como "PAGO"
    Então a lista de pagamentos continua ordenada por data de vencimento
    # findByContratoIdOrderByVencimentoAsc — correção imediata (concluída)

  @implementado
  Cenário: Painel consolidado de visão de carteira
    Dado que tenho múltiplos imóveis com contratos ativos e pagamentos históricos
    Quando eu abro o painel de visão de carteira
    Então vejo renda mensal recorrente da carteira inteira
    E vejo taxa de ocupação (imóveis alugados ÷ total)
    E vejo inadimplência do mês
    # faixa de KPIs no topo do hub de Imóveis — StatCard "Renda mensal
    # recorrente"/"Taxa de ocupação"/"Inadimplência do mês"

  @pendente
  Cenário: Despesas do imóvel disponíveis para cálculo de rentabilidade líquida
    Dado que um imóvel tem despesas registradas (IPTU, condomínio, água, luz)
    Quando eu abro a tela de detalhe do imóvel
    Então vejo o total de despesas do período
    E consigo comparar receita menos despesa, não só receita bruta
    # a lista de despesas (item 3) e o painel de carteira (item 11) já
    # existem — falta só a agregação cruzada receita-menos-despesa por
    # imóvel; ver docs/jornadas-e-prioridades-negocio.md, seção 6

  @implementado
  Cenário: Exportação do extrato de pagamentos
    Dado que estou no hub de Pagamentos com um período selecionado
    Quando eu clico em "Exportar CSV"
    Então um arquivo CSV é baixado com todos os pagamentos do período filtrado
    # botão "Exportar CSV" em (owner)/pagamentos/index.tsx, gerado no client
