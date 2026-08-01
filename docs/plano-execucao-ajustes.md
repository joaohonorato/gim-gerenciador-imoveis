# Plano de Execução dos Ajustes e Melhorias

## 1. Objetivo
Executar os ajustes levantados no journey map com foco em:
- alto impacto de negócio
- redução de risco operacional
- fechamento de lacunas críticas de experiência

Critério de ranqueamento adotado:
- prioridade maior para itens com alta complexidade e alto impacto

---

## 2. Escala de priorização
- Impacto: 1 (baixo) a 5 (muito alto)
- Complexidade: 1 (baixa) a 5 (muito alta)
- Risco de não fazer: 1 (baixo) a 5 (muito alto)
- Prioridade final: ordenação por impacto e complexidade, com desempate por risco

---

## 3. Backlog priorizado (ranking executivo)

| Rank | Frente | Impacto | Complexidade | Risco | Prioridade | Andamento |
|---|---|---:|---:|---:|---|---|
| 1 | Jornada completa do inquilino no frontend | 5 | 5 | 5 | P0 | Maior parte concluída — falta chamados e visão de pagamentos do inquilino (ver 4.1) |
| 2 | Instrumentação de funil por persona (telemetria) | 5 | 4 | 5 | P0 | Não iniciado |
| 3 | Padronização de erros por etapa da jornada | 4 | 4 | 4 | P1 | Não iniciado |
| 4 | Painel de status acionável pós-assinatura | 4 | 3 | 4 | P1 | Parcial — hubs de Contratos/Pagamentos/Convites/Candidaturas existem para o proprietário (navegação por Tabs); sem alertas de vencimento nem priorização por urgência ainda |
| 5 | Fluxo robusto de recuperação de acesso/convites | 4 | 3 | 4 | P1 | Parcial — reenvio e revogação de convite já existem (`POST /convites/{token}/reenviar`, `/revogar`); sem playbook de suporte nem fluxo dedicado de token expirado |
| 6 | Wizard guiado no fluxo operacional do proprietário | 3 | 3 | 3 | P2 | Não iniciado |
| 7 | Hardening de contratos de API e permissões por persona | 3 | 3 | 3 | P2 | Não iniciado |
| 8 | Ajustes finos de UX em logout e mensagens contextuais | 2 | 2 | 2 | P3 | Não iniciado |

**Trabalho concluído fora deste backlog original**: feature completa de upload de arquivos (avatares de proprietário/inquilino, fotos de imóvel, documento de contrato e documentos de garantia — Azure Blob Storage, containers públicos para avatar/foto e privados com URL assinada para documentos) e fechamento do ciclo de vida do `CONVITE` (`recusado`/`revogado`/`consumido` automaticamente ao assinar o contrato). Nenhum dos dois estava rankeado aqui porque surgiu de pedidos diretos durante a execução, não do journey map original — mas ambos reduzem risco/impacto de itens já rankeados (upload de documentos cobre parte do gap de "documentação" citado no rank 1; fechamento do ciclo do convite é a base do rank 5).

---

## 4. Plano detalhado por frente

## 4.1 Rank 1 — Jornada completa do inquilino no frontend (P0) — **em andamento, maior parte concluída**
Escopo:
- ✅ telas de cadastro do inquilino por convite (`(contrato)/locacao/[token].tsx`)
- ✅ fluxo de envio/confirmação de garantia (tipo + dados JSON na candidatura)
- ✅ acompanhamento de candidatura (status visível em `(tenant)/tenant/index.tsx` e na própria tela de convite)
- ✅ revisão e assinatura do contrato pelo inquilino (`(contrato)/[id]/revisar.tsx`), incluindo upload de documento do contrato e de garantia
- ✅ visão de contratos do inquilino (home do tenant)
- ✅ perfil com avatar (`(tenant)/perfil.tsx`)
- ❌ **pendente**: abertura e acompanhamento de chamados pelo inquilino (backend 100% coberto, sem tela)
- ❌ **pendente**: upload dos documentos comprobatórios da garantia já na etapa de candidatura/convite (hoje só existe upload de documento depois, na revisão do contrato — ver B1/B2 em `docs/journey-map.md`)
- ❌ **pendente**: visão de pagamentos do inquilino (proprietário já tem hub de pagamentos; inquilino ainda não tem uma visão equivalente dos próprios pagamentos)

Dependências:
- validação de endpoints existentes
- alinhamento de payloads e estados

Entregáveis:
- novas rotas em app de tenant
- integração fim a fim frontend + backend
- testes E2E cobrindo a jornada

Critério de pronto:
- inquilino conclui jornada convite -> assinatura sem usar API manual (**atingido** para o caminho principal — cadastro, garantia, aprovação, assinatura)
- chamados e visão de pagamentos do inquilino também com UI própria (**não atingido**)
- testes E2E estáveis e verdes (**atingido** — golden-path cobre o caminho principal ponta a ponta)

---

## 4.2 Rank 2 — Instrumentação de funil por persona (P0)
Escopo:
- eventos de entrada/saída por etapa crítica
- correlação por persona e jornada
- indicadores de abandono e tempo por etapa

Dependências:
- definição de taxonomia de eventos
- decisão de destino de logs/métricas

Entregáveis:
- catálogo de eventos versionado
- eventos disparados em pontos críticos do frontend e backend
- visão inicial de funil por persona

Critério de pronto:
- KPIs do journey calculáveis automaticamente
- suporte consegue localizar etapa de falha com evidência

---

## 4.3 Rank 3 — Padronização de erros por etapa (P1)
Escopo:
- contrato único de erro no backend
- mensagens de frontend orientadas a ação
- mapeamento de erro técnico para erro de jornada

Dependências:
- revisão do GlobalErrorHandler
- padronização no cliente API

Entregáveis:
- catálogo de códigos de erro por domínio
- textos de erro por etapa
- guideline de tratamento para novos fluxos

Critério de pronto:
- redução de mensagens genéricas
- suporte consegue orientar usuário com base no código de erro

---

## 4.4 Rank 4 — Painel de status acionável pós-assinatura (P1)
Escopo:
- visão consolidada de pagamentos (pendente, pago, atrasado)
- alertas de vencimento e ações sugeridas
- status de contrato e chamados no mesmo contexto

Dependências:
- dados de pagamento consistentes
- telemetria mínima habilitada

Entregáveis:
- cards de status priorizados por urgência
- filtros por imóvel/contrato

Critério de pronto:
- proprietário identifica próximos passos em menos de 30 segundos

---

## 4.5 Rank 5 — Recuperação de acesso/convites (P1)
Escopo:
- fluxo claro para token inválido/expirado
- reemissão/reabertura de convite (política controlada)
- mensagens e caminhos de suporte

Dependências:
- regra de segurança para reemissão
- trilha de auditoria mínima

Entregáveis:
- rotas e telas de recuperação
- playbook de suporte

Critério de pronto:
- usuário destrava fluxo sem intervenção técnica direta

---

## 4.6 Rank 6 — Wizard operacional do proprietário (P2)
Escopo:
- guia de primeiros passos
- navegação orientada: imóvel -> convite -> candidatura -> contrato

Dependências:
- jornada principal estável

Entregáveis:
- checklist operacional
- marcadores de progresso

Critério de pronto:
- redução de abandono no primeiro uso

---

## 4.7 Rank 7 — Hardening de contratos e permissões (P2)
Escopo:
- revisão de contratos de API por persona
- validação de isolamento de dados
- documentação viva de rotas e permissões

Dependências:
- mapa de personas consolidado

Entregáveis:
- matriz rota x persona
- testes de autorização/isolamento

Critério de pronto:
- cobertura de segurança funcional para rotas críticas

---

## 4.8 Rank 8 — Ajustes finos de UX no logout e contexto (P3)
Escopo:
- feedback mais explícito de estado
- consistência visual de mensagens de sucesso/falha

Dependências:
- base de erros padronizada

Entregáveis:
- microinterações e mensagens revisadas

Critério de pronto:
- consistência em todos os pontos de saída de sessão

---

## 5. Sequenciamento sugerido (ondas)

Onda 1 (P0):
- Rank 1
- Rank 2

Onda 2 (P1):
- Rank 3
- Rank 4
- Rank 5

Onda 3 (P2/P3):
- Rank 6
- Rank 7
- Rank 8

---

## 6. Plano de execução por sprint

Sprint 1:
- base da jornada inquilino (cadastro + convite + garantia)
- desenho e implementação da taxonomia de eventos

Sprint 2:
- assinatura e visão inicial do inquilino
- telemetria de abandono por etapa
- padronização de erros (núcleo)

Sprint 3:
- painel de status acionável do proprietário
- recuperação de acesso/convites

Sprint 4:
- wizard operacional
- hardening de permissões
- refinamentos de UX

---

## 7. Riscos e mitigação

Risco: divergência entre fluxo frontend e regras de domínio
- Mitigação: contratos de API versionados + testes E2E por persona

Risco: baixa observabilidade para medir ganho real
- Mitigação: instrumentar eventos antes de fechar a onda 1

Risco: aumento de escopo durante construção da jornada do inquilino
- Mitigação: dividir em incrementos verticais e feature flags

---

## 8. Métricas de sucesso do plano

- taxa de conclusão convite -> assinatura (inquilino)
- tempo médio login -> convite -> contrato assinado (proprietário)
- queda de tickets de acesso/convite
- estabilidade da suíte E2E dos fluxos críticos

---

## 9. Decisão de go-live por onda

Critério de go-live de cada onda:
- testes de regressão verdes
- critérios de pronto atendidos em todas as frentes da onda
- validação de métricas mínimas por 7 dias após deploy