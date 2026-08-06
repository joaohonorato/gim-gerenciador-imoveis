# Caso de negócio — por que adotar o Gim Imóveis

Este documento existe para responder três perguntas de negócio: por que trocar a gestão manual (planilha, WhatsApp, papel) por um sistema dedicado; o que o Gim Imóveis entrega hoje que a gestão manual não entrega; e como ele se posiciona frente a alternativas de mercado. Complementa [`jornadas-e-prioridades-negocio.md`](jornadas-e-prioridades-negocio.md) (o que funciona e o que falta, por persona, e o caminho até lá).

---

## 1. O problema que gestão manual não resolve

A esmagadora maioria dos proprietários com 1 a ~20 imóveis administra hoje com uma combinação de: planilha (Excel/Google Sheets) para controlar aluguéis e vencimentos, WhatsApp para comunicação com inquilino, papel ou PDF avulso para contrato, e memória para lembrar quando um contrato vence ou uma garantia precisa ser renovada. Isso funciona até não funcionar — e os pontos de ruptura são previsíveis:

| Dor da gestão manual | Consequência concreta |
|---|---|
| Nenhum registro estruturado de quem aprovou o quê, quando | Disputa sobre condição acordada vira "sua palavra contra a minha" |
| Cálculo de reajuste, vencimento e garantia na cabeça ou em fórmula de planilha | Reajuste esquecido, garantia vencida sem renovação, contrato sobreposto na mesma unidade |
| Comunicação de manutenção via WhatsApp | Sem rastro de quando foi reportado, quando foi resolvido, quem é responsável |
| Documento de contrato/garantia solto em pasta local ou e-mail | Perda de documento, sem controle de versão, sem acesso rápido quando precisa |
| Nenhuma visão agregada de carteira | Decisão de comprar/vender um imóvel feita "no olho", sem número consolidado de rentabilidade |
| Cobrança e confirmação de pagamento manual | Atraso não sinalizado automaticamente, sem histórico auditável |

Nenhum desses problemas é hipotético — são o motivo histórico de existir uma categoria inteira de software ("PropTech" / gestão de locação) no mercado brasileiro e internacional.

## 2. O que o Gim Imóveis já resolve hoje

O caminho principal do produto — convite → candidatura → aprovação → geração de contrato → assinatura das duas partes → geração automática de pagamentos — está implementado e testado de ponta a ponta (E2E cobrindo o fluxo completo), e resolve diretamente boa parte da tabela acima:

- **Registro estruturado e auditável**: cada convite, candidatura e assinatura fica com timestamp e estado no banco — não é mais "sua palavra contra a minha".
- **Geração automática de pagamentos**: ao assinar um contrato, o sistema já gera os pagamentos mensais correspondentes ao período — elimina o cálculo manual de vencimento.
- **Bloqueio de sobreposição de contrato**: aprovar um candidato valida que não existe contrato assinado com período sobreposto na mesma unidade — uma regra que, feita na mão, depende de alguém lembrar de checar.
- **Documentos centralizados e seguros**: contrato e documentos de garantia ficam no Azure Blob Storage, com controle de acesso (URL assinada de curta duração para documentos privados) — não é mais uma pasta local ou anexo de e-mail perdido.
- **Onboarding sem fricção desnecessária**: cadastro básico (nome/e-mail/senha, CPF/CNPJ só quando for assinar contrato de fato) reduz o abandono no primeiro passo — comparado a formulários tradicionais de imobiliária que pedem documentação completa antes de qualquer valor demonstrado.
- **Recuperação de acesso self-service**: esqueci minha senha e verificação de e-mail funcionam sem intervenção manual de suporte.
- **Cadastro de imóvel com dado que sustenta decisão**: quartos, banheiros, vagas, m², IPTU e endereço via CEP — o mínimo para justificar um valor de aluguel, algo que planilha isolada não força ninguém a preencher de forma consistente.
- **Visão de carteira e despesas**: renda mensal recorrente, taxa de ocupação e inadimplência do mês num só painel, mais um catálogo de contas/despesas (IPTU, condomínio, água, luz — livremente nomeadas por proprietário) por unidade, com exportação de extrato em CSV.
- **Ciclo de manutenção fechado**: inquilino abre chamado (só se tiver contrato ativo na unidade), proprietário acompanha e atualiza o status — sem depender de WhatsApp para rastrear.

## 3. O que ainda falta para ser uma ferramenta financeira completa

Sendo direto: o produto **sustenta a operação** (cadastro, convite, contrato, assinatura) e, desde a última rodada de melhorias, também **sustenta parte relevante da decisão de investimento** (renda recorrente, ocupação, inadimplência, despesas por unidade). O que ainda falta é mais fino: comparar receita menos despesa por imóvel (rentabilidade líquida, não só bruta), exportação de extrato pronta para declaração de imposto de renda, e instrumentação que meça o funil de cada persona ao longo do tempo. Essas lacunas estão detalhadas com solução proposta em [`jornadas-e-prioridades-negocio.md`](jornadas-e-prioridades-negocio.md).

Ser transparente sobre isso é parte do argumento de negócio: o produto não promete o que não entrega hoje, e o caminho até entregar é conhecido, priorizado e majoritariamente de baixo esforço (a maior parte dos itens críticos já reaproveitou dado e API que já existiam — foi trabalho de interface, não de arquitetura nova; o que resta segue o mesmo padrão).

## 4. Comparação com alternativas de mercado

**Aviso de escopo**: a tabela abaixo é uma comparação qualitativa de posicionamento, baseada em conhecimento público geral sobre a categoria — não é um benchmark de preço ou feature auditado diretamente contra cada fornecedor. Antes de usar em material comercial externo, validar preço e feature atual direto com cada concorrente (esses produtos mudam de plano/preço com frequência).

| Alternativa | Perfil de quem usa | Onde ganha do Gim Imóveis hoje | Onde o Gim Imóveis ganha |
|---|---|---|---|
| **Planilha + WhatsApp** (baseline real da maioria do mercado de 1-20 imóveis) | Proprietário individual, sem orçamento de software | Zero custo direto, zero curva de aprendizado | Elimina todo o risco de erro manual desta tabela (seção 1); tem assinatura de contrato e trilha de auditoria, que planilha nunca terá |
| **Superlógica Imobiliária** (suíte ERP+imobiliária, mercado brasileiro estabelecido) | Imobiliárias de médio/grande porte, administradoras com volume alto de carteira | Suíte muito mais ampla (contábil, cobrança, boletos bancários integrados, módulos de condomínio) — maturidade de produto de anos | Onboarding mais simples e rápido para operação pequena; sem a sobrecarga de um ERP completo para quem só quer gerir contratos |
| **Kenlo Locação** (mercado brasileiro, foco em imobiliárias) | Imobiliárias de médio porte com equipe de corretores | Integrações de mercado mais maduras (portais de anúncio, emissão de boleto) | Foco mais direto no ciclo contrato→assinatura→pagamento sem exigir todo o aparato de uma imobiliária tradicional |
| **Ferramentas internacionais tipo AppFolio/Buildium** (fora do escopo geográfico direto, mas referência de categoria) | Property managers nos EUA, portfólios maiores | Ecossistema de integrações e maturidade de anos de operação em escala | Desenhado desde o início para as regras brasileiras (CPF/CNPJ, tipos de garantia do modelo de locação brasileiro) — essas ferramentas não nascem com isso |

**Leitura de posicionamento**: o Gim Imóveis não compete hoje pelo mesmo cliente que uma imobiliária de porte médio já atendida por Superlógica/Kenlo — compete pelo espaço que essas ferramentas deixam vago, que é justamente o **proprietário individual ou pequena administradora que hoje usa planilha porque as opções de mercado são pesadas (e caras) demais para o volume dele**. É o mesmo público-alvo do MVP descrito em [`especificacao-produto.md`](especificacao-produto.md).

## 5. Público-alvo (ICP) e motivação de adoção por persona

- **Proprietário individual ou pequena administradora (1-20 imóveis)**: motivação primária — sair da planilha sem pagar o preço/complexidade de um ERP de imobiliária. É quem mais sente o ganho imediato do fluxo de convite→contrato→assinatura, e agora também dos totais/toggles de atenção nos hubs.
- **Investidor de imóveis para aluguel**: motivação primária — decisão de portfólio com dado, não com "achismo". A lacuna estrutural (painel de carteira) foi fechada nesta rodada; o que resta (rentabilidade líquida por imóvel, exportação fiscal) é refinamento, não ausência total.
- **Inquilino**: não é quem decide adotar o produto, mas é quem determina se a experiência do proprietário funciona de verdade — um convite com fricção ou uma assinatura confusa vira ligação para o proprietário reclamar, não para o suporte do produto. A qualidade da jornada do inquilino é retenção indireta do proprietário.

## 6. Risco de não adotar (ou de adotar tarde)

Fora do argumento de conveniência, existe um argumento de risco direto:

- **Sobreposição de contrato na mesma unidade** é um erro caro e evitável — o sistema bloqueia isso estruturalmente; planilha não bloqueia nada, só registra depois do fato.
- **Isolamento de dados entre contas** é auditado e coberto por teste de integração (ver [`matriz-acesso-por-rota.md`](matriz-acesso-por-rota.md)) — um proprietário nunca vê dado de inquilino/contrato de outro proprietário. Replicar essa garantia numa planilha compartilhada ou numa pasta de Drive é, na prática, impossível.
- **LGPD**: isolamento de dados entre proprietários é uma regra de arquitetura do sistema, não uma política de boas intenções.

## 7. Resumo executivo

O Gim Imóveis já entrega, hoje, o suficiente para substituir com vantagem clara a combinação planilha+WhatsApp+papel que é a alternativa real da maior parte do público-alvo — não a suíte completa de uma Superlógica ou Kenlo, que atende um público diferente e mais pesado. O argumento de adoção não é "temos mais feature que o concorrente"; é "resolvemos o problema real de quem hoje não usa nenhum sistema, com a estrutura jurídica e de dados que uma planilha nunca vai ter" — e, desde a última rodada de melhorias, isso já inclui uma leitura básica de carteira, não só operação. O que resta do caminho é majoritariamente refinamento e instrumentação, não construção de zero — ver [`jornadas-e-prioridades-negocio.md`](jornadas-e-prioridades-negocio.md).
