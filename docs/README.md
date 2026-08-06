# Documentação — Gim Imóveis

Índice da documentação do produto. Esta pasta substitui `docs-antigos/` (mantida no repo só como histórico da rodada anterior de documentação — não referenciar nem editar; toda atualização futura acontece aqui).

## Produto

- **[especificacao-produto.md](especificacao-produto.md)** — fonte da verdade de regras de negócio: modelo de dados, motor de estados, superfície de API atual, design system, estrutura de telas, restrições legais. Ler antes de implementar qualquer comportamento novo de domínio.
- **[caso-de-negocio.md](caso-de-negocio.md)** — por que adotar o produto: problema que resolve, comparação com alternativas de mercado, público-alvo, risco de não adotar.

## Jornadas e backlog

Dois documentos complementares, mesmo conteúdo de origem visto por dois critérios de priorização diferentes:

- **[jornadas-e-backlog-tecnico.md](jornadas-e-backlog-tecnico.md)** — visão de **4 personas** (inclui Admin/Suporte, operacionais/internas); backlog ranqueado por **impacto × complexidade × risco** — responde "em que ordem construímos".
- **[jornadas-e-prioridades-negocio.md](jornadas-e-prioridades-negocio.md)** — visão de **3 personas de negócio** (Investidor/Proprietário/Inquilino); backlog ranqueado por **impacto no negócio, pesado por persona** — responde "o que importa mais pra quem paga a conta e pra quem opera o produto todo dia". Inclui prompts de implementação prontos pra copiar para os itens ainda em aberto.

Use o documento técnico para sequenciar sprint; use o de negócio para justificar prioridade a quem financia o produto.

## Engenharia (referência viva)

- **[matriz-acesso-por-rota.md](matriz-acesso-por-rota.md)** — matriz rota × persona: quem pode chamar cada endpoint e por qual mecanismo de posse. Atualizar sempre que a autorização de uma rota mudar; consultar antes de adicionar uma rota `{id}`/`{imovelId}` nova.
- **[catalogo-erros-api.md](catalogo-erros-api.md)** — catálogo `code → mensagem de frontend` para todo erro que a API pode devolver. Consultar antes de adicionar um código de erro novo.
- **[playbook-suporte-acesso.md](playbook-suporte-acesso.md)** — runbook pra diagnosticar e resolver "convite não funciona"/"não consigo entrar", pros dois tipos de convite (onboarding de proprietário e locação). Não há painel de admin — o roteiro usa o que já está exposto ao próprio usuário mais consulta direta ao banco.
- **[gherkin/](gherkin/)** — cenários Gherkin por persona (pt-BR), tags `@implementado`/`@pendente`. Checar aqui antes de escrever um teste de integração novo para um fluxo de persona — o cenário pode já estar especificado.

## Infraestrutura

- **[deploy-azure.md](deploy-azure.md)** — runbook operacional: ordem de setup, Bicep, domínio customizado, variáveis de ambiente, GitHub Secrets, gotchas.
- **[provisionamento-e-custos.md](provisionamento-e-custos.md)** — visão executiva de infraestrutura: arquitetura de build→deploy, quais recursos existem, por que existem, quanto custam.

## Design

- **[design-handoff-landing-page/](design-handoff-landing-page/)** — handoff de design (HTML/CSS de referência + tokens) da landing page de marketing. Protótipo de alta fidelidade para recriar no stack real, não código de produção.

---

## Convenção

Cada documento cita os outros pelo nome do arquivo (não pelo caminho completo) sempre que o link já deixa claro que está dentro de `docs/`. Ao renomear ou remover um arquivo desta pasta, atualizar as referências cruzadas nos demais — um `grep -rn "nome-antigo.md" docs/` antes de finalizar a mudança pega a maioria dos casos.
