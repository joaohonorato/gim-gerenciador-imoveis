# Plataforma de Gestão de Imóveis — Especificação para Implementação

## 1. Contexto

SaaS multi-tenant para proprietários de imóveis gerenciarem sua carteira de locação (pagamentos, contas, manutenção, contratos). Múltiplos proprietários independentes usam a mesma plataforma, cada um só enxerga sua própria carteira.

**Modelo de negócio do MVP:** convite fechado — o proprietário cadastra o imóvel e convida um inquilino específico; não há descoberta pública de imóveis. **Roadmap:** evoluir para marketplace aberto (candidaturas públicas) sem redesenhar o core. Por isso, decisões de arquitetura abaixo já preparam esse caminho — implemente respeitando essas costuras.

## 2. Stack técnica

- **Backend:** Java 21 + Micronaut 4 (REST API), arquitetura hexagonal (`domain/` / `application/` / `infrastructure/`)
- **Frontend:** React Native + Expo 57 (compila para Web, iOS e Android a partir de um único código)
- **Roteamento:** Expo Router (file-based)
- **Estilização:** NativeWind (Tailwind para React Native), consumindo os tokens da seção 6
- **Banco de dados:** Postgres (local via Docker, produção via Supabase); schema é dono do **Flyway** (`backend/src/main/resources/db/migration/`), não do Hibernate — `hibernate.hbm2ddl.auto=validate` só confere entidade x schema real, nunca altera; toda mudança de schema exige um `Vn__descricao.sql` novo. Campos flexíveis por tipo de garantia em coluna `jsonb`/`json`
- **Armazenamento de arquivos:** Azure Blob Storage — avatares de usuário, fotos de imóvel e documentos de contrato/garantia (ver ARQUIVO na seção 3)
- **Autenticação:** login por e-mail e senha, com onboarding iniciado por convite por e-mail
- **Assinatura eletrônica:** integração externa, nível avançado (ver seção 8) — ainda stub (`AssinaturaProvider`) no MVP

## 3. Modelo de dados

```
PROPRIETARIO 1---N IMOVEL
IMOVEL       1---N UNIDADE       # toda IMOVEL ganha 1 UNIDADE padrão automaticamente na criação
UNIDADE      1---N CONVITE       # convite de locação para uma unidade específica
CONVITE      1---1 CANDIDATURA   # candidatura do inquilino que aceitou o convite
UNIDADE      1---N CONTRATO
INQUILINO    1---N CONTRATO      # um inquilino pode ter vários contratos simultâneos
CONTRATO     1---1 GARANTIA
CONTRATO     1---N PAGAMENTO
CONTRATO     1---N ARQUIVO       # documento do próprio contrato + documentos de garantia
UNIDADE      1---N CONTA         # despesa (IPTU/condomínio/água/luz/...), tipo em catálogo por proprietário
IMOVEL       1---N CHAMADO
IMOVEL       1---N ARQUIVO       # fotos do imóvel
PROPRIETARIO 1---1 ARQUIVO       # avatar (opcional)
INQUILINO    1---1 ARQUIVO       # avatar (opcional)
PROPRIETARIO 1---N TIPO_CONTA        # catálogo de tipos de conta, livre por proprietário
PROPRIETARIO 1---N CATEGORIA_CHAMADO # catálogo de categorias de chamado, livre por proprietário
CONTA_ACESSO 1---N PUSH_TOKEN        # device(s) registrado(s) pra push notification
                                      # EVENTO_AUDITORIA referencia CONVITE/CONVITE_ACESSO por
                                      # chave polimórfica (entidade_tipo + entidade_id), sem FK
```

| Entidade | Campos principais |
|---|---|
| **PROPRIETARIO** | id (uuid, PK), nome, cpf_cnpj (unique, opcional até assinar um contrato), telefone (opcional), perfil (enum: owner, admin), criado_em |
| **IMOVEL** | id (PK), proprietario_id (FK), endereco, cidade, matricula, numero, bairro, complemento, tipo_imovel (enum: casa, apartamento, comercial, outro), visibilidade (enum: privado, publicado — default `privado`), criado_em |
| **UNIDADE** | id (PK), imovel_id (FK), nome (ex.: "Imóvel completo", "Quarto 1"), padrao (boolean — `true` = representa o imóvel inteiro), status (enum: vago, reservado, alugado, manutencao) |
| **CONVITE** | id (PK), imovel_id (FK), unidade_id (FK), proprietario_id (FK), token (unique), expira_em, condições (tipo_contrato, valor_aluguel, período sugerido, garantia_aceita), status (enum: pendente, em_analise, consumido, expirado, recusado, revogado), candidatura_id (FK, opcional), dados do último envio (canal, status, destino, tentativas) |
| **CANDIDATURA** | id (PK), convite_id (FK), inquilino_id (FK), status (enum: pendente, aprovada, recusada), garantia_escolhida, criada_em |
| **CONTRATO** | id (PK), unidade_id (FK), inquilino_id (FK), convite_id (FK, opcional — rastreia o convite de origem para fechar o ciclo de vida dele ao assinar), data_inicio, data_fim, tipo (enum: residencial, temporada), valor_aluguel, indice_reajuste, status_assinatura (enum: pendente, assinado) |
| **INQUILINO** | id (PK), nome, cpf (unique), email, criado_em |
| **GARANTIA** | id (PK), contrato_id (FK), tipo (enum: caucao, fiador, seguro_fianca, titulo_capitalizacao, nenhuma), vencimento, dados_especificos (jsonb — estrutura varia por tipo) |
| **PAGAMENTO** | id (PK), contrato_id (FK), vencimento, pago_em, valor, status (enum: pendente, pago, atrasado) |
| **TIPO_CONTA** | id (PK), proprietario_id (FK), nome (livre, único por proprietário), criado_em — catálogo do proprietário, reutilizável em qualquer imóvel dele |
| **CONTA** | id (PK), unidade_id (FK), tipo_conta_id (FK), responsavel (enum: proprietario, inquilino), contrato_id (FK, obrigatório só quando `responsavel = inquilino`), vencimento, valor, status (enum: pendente, pago) |
| **CATEGORIA_CHAMADO** | id (PK), proprietario_id (FK), nome (livre, único por proprietário), criado_em — catálogo do proprietário |
| **CHAMADO** | id (PK), imovel_id (FK), aberto_por (FK inquilino), categoria_id (FK), descricao, status (enum: aberto, em_andamento, resolvido), aberto_em, resolvido_em |
| **ARQUIVO** | id (PK), tipo (enum: avatar_proprietario, avatar_inquilino, foto_imovel, documento_contrato, documento_garantia), dono_id (proprietario_id / inquilino_id / imovel_id / contrato_id, dependendo do tipo), blob_key, nome_original, content_type, tamanho_bytes, criado_em — tabela única polimórfica, não uma tabela por tipo |
| **PUSH_TOKEN** | id (PK), conta_acesso_id (FK), token (Expo push token, unique — upsert por token, não por id), criado_em |
| **EVENTO_AUDITORIA** | id (PK), entidade_tipo (enum: convite, convite_acesso), entidade_id, tipo_evento (criado, enviado, renovado, candidatura_criada, consumido, aceito, revogado, recusado, tentativa_com_token_expirado, tentativa_com_token_invalido_ou_consumido), detalhe (texto livre, opcional), criado_em — append-only, sem FK (mantém o registro mesmo que a entidade original saia do escopo de retenção) |

**Regras de integridade:**
- Um `CONTRATO` só pode ter **uma** `GARANTIA` (nunca combinar tipos).
- `INQUILINO` é entidade independente, nunca embutida no contrato — permite múltiplos contratos por CPF e reaproveitamento de cadastro.
- `IMOVEL.visibilidade` sempre `privado` no MVP; campo já existe para a fase de marketplace.
- **`UNIDADE` generaliza imóvel inteiro vs. subdividido**: todo `IMOVEL` recebe automaticamente 1 `UNIDADE` com `padrao = true` no momento do cadastro — pra quem nunca subdivide, o fluxo continua idêntico a "alugar o imóvel inteiro", sem nenhuma tela extra. O proprietário pode adicionar mais unidades depois via `POST /imoveis/{id}/unidades` (uma por vez ou em lote — ex.: terreno com várias casas, cada uma com vários apartamentos), cada uma com nome livre e seu próprio ciclo de vida (`UnidadeStatus`). `CONTRATO`, `CONVITE`, `CANDIDATURA` e `CONTA` já são todos referenciados por `unidade_id` (não por `imovel_id`), então nenhum deles precisou de migração pra suportar isso — só `CHAMADO` ganhou `unidade_id` (antes só tinha `imovel_id`), derivado automaticamente do contrato do inquilino que abre o chamado (não pedido explicitamente na UI).
- **Não sobreposição**: dois `CONTRATO` com `status_assinatura = assinado` na mesma `UNIDADE` não podem ter períodos (`data_inicio`–`data_fim`) sobrepostos. Validado no momento da aprovação do candidato.
- **`TIPO_CONTA` e `CATEGORIA_CHAMADO` são catálogos por proprietário, não enums fixos**: cada proprietário cadastra livremente seus próprios nomes (ex.: "IPTU", "Condomínio", mas também qualquer coisa específica dele), reutilizáveis em qualquer imóvel/chamado seu. Um catálogo padrão é semeado automaticamente na criação do proprietário (`SemearCatalogosPadrao`).
- **`CONTA` é escopada por `UNIDADE`, não por `IMOVEL`**, e tem um `responsavel` (proprietário ou inquilino): quando é do inquilino, ela compõe — junto com o `PAGAMENTO` do mês — o total que ele deve naquele mês, e exige um `contrato_id` (validado contra a unidade e o proprietário).
- **Ciclo de vida do `CONVITE`**: sai de `pendente` para `em_analise` quando uma `CANDIDATURA` é criada; vira `consumido` quando o `CONTRATO` gerado a partir dele é totalmente assinado (por ambas as partes); pode ser encerrado antes disso como `recusado` (pelo inquilino) ou `revogado` (pelo proprietário); `expirado` é automático após `expira_em`. Uma vez fora de `pendente`/`em_analise`, o convite sai da lista de "ativos" do proprietário.
- **Privacidade de `ARQUIVO`**: avatares e fotos de imóvel ficam em containers **públicos** (URL direta, cacheável); documentos de contrato/garantia ficam em container **privado**, acessados só via URL assinada (SAS) de curta duração (5–10 min), gerada sob demanda e checando que quem pede é uma das partes do contrato (ou, se o contrato ainda não existir, uma das partes da candidatura/convite) dono do arquivo.
- **Documento do contrato é 1:1** (novo upload substitui o anterior); **documentos de garantia são 1:N** (cada upload se soma aos anteriores — ex.: fiador precisa de RG + comprovante de renda).
- **`dono_id` de `documento_garantia` pode ser uma `candidaturaId` ou uma `contratoId`**: o comprobatório pode ser enviado ainda na candidatura (antes de existir contrato, via `/convites/{token}/garantia/documentos`) ou depois, na revisão do contrato já aprovado (via `/contratos/{id}/garantia/documentos`) — os dois caminhos usam o mesmo tipo de arquivo e a mesma infraestrutura, só mudando o que é o "dono" em cada etapa. `GET /contratos/{id}/documentos` reconcilia os dois: busca por `contratoId` e, se o contrato tiver `convite_id`, também busca pela `candidaturaId` daquele convite, juntando os dois conjuntos — nenhum documento enviado na candidatura "some" depois que o contrato é criado.
- **Alertas de vencimento (`CONTRATO.alerta_vencimento_enviado`/`alerta_garantia_enviado`, `CONTA.alerta_enviado`)**: cada flag marca que o alerta proativo (e-mail + push) daquele item já foi disparado, evitando reenvio duplicado. O job diário (`AlertasVencimentoScheduler`) consulta por uma **janela** (hoje até N dias à frente), não uma data exata — um dia em que o job não rodou não perde o alerta, ele só é pego na próxima execução dentro da janela.
- **`CONVITE`/`CONVITE_ACESSO` nunca persistem um status "expirado" de verdade**: o domínio só grava `expira_em`; se o link ainda está válido é sempre calculado comparando `expira_em` com "agora" na hora da resposta (`Convite.expirado(Instant)`/`ConviteAcesso.expirado(Instant)`), nunca um valor gravado no banco. `POST /convites/{token}/reenviar` estende `expira_em` em mais 7 dias (`Convite.renovar`) quando detecta um convite `PENDENTE` e expirado, sem trocar o token — o mesmo link volta a funcionar.
- **`POST /auth/convites/proprietarios` é idempotente por e-mail**: se já existe um `CONVITE_ACESSO` pendente (não consumido, não expirado) pra aquele e-mail, devolve o mesmo token em vez de criar um novo — evita links de onboarding órfãos e ambíguos.

## 4. Regras de negócio (motor de estados)

| Evento | Ação automática |
|---|---|
| Proprietário cadastrado | Semeia catálogos padrão de `TIPO_CONTA` e `CATEGORIA_CHAMADO` (`SemearCatalogosPadrao`) |
| Imóvel cadastrado | Cria `UNIDADE` padrão (`padrao = true`, `status = vago`); `IMOVEL.visibilidade = privado` |
| Convite gerado | Cria token único (expira em 7 dias) para onboarding de acesso ou para uma `UNIDADE` com condições de locação (valor, tipo, garantia aceita); `CONVITE.status = pendente` |
| Inquilino envia documentos | Cria/atualiza `INQUILINO` por CPF (reaproveita se já existir) |
| Usuário aceita convite | Confirma e-mail, cria senha e ativa a conta de acesso |
| Inquilino se candidata a um convite de locação | Cria `CANDIDATURA` (`status = pendente`); `CONVITE.status → em_analise` |
| Proprietário revoga convite ainda ativo | `CONVITE.status → revogado`; sai da lista de convites ativos do proprietário |
| Inquilino recusa convite/candidatura | `CONVITE.status → recusado` |
| Proprietário aprova candidato | Valida que não há `CONTRATO` assinado com período sobreposto na mesma `UNIDADE`; `CANDIDATURA.status → aprovada`; gera `CONTRATO` (`status_assinatura = pendente`, `convite_id` apontando para o convite de origem) a partir de template com cláusulas condicionais por tipo de garantia e tipo de locação; `UNIDADE.status → reservado` |
| Proprietário recusa candidato | `CANDIDATURA.status → recusada` |
| Ambas as partes assinam | `CONTRATO.status_assinatura = assinado`; `UNIDADE.status → alugado`; se o contrato tem `convite_id` e o convite ainda está `em_analise`, `CONVITE.status → consumido`; gera `PAGAMENTO` (um por mês do prazo) |
| Pagamento não confirmado até vencimento + 1 dia | `PAGAMENTO.status = atrasado`; notifica ambas as partes |
| Conta a 5 dias ou menos do vencimento | Job diário notifica por e-mail + push o responsável (proprietário ou inquilino, conforme `CONTA.responsavel`) — `AlertasVencimentoScheduler`/`NotificarContasProximasDoVencimento` |
| Chamado criado | `status = aberto`; notifica proprietário — só permitido a um inquilino com contrato na unidade do imóvel |
| Chamado atualizado pelo proprietário | `status = em_andamento` → `resolvido` (com timestamp) |
| Contrato a 60 dias ou menos do fim | Sinalizado visualmente no hub de Contratos (cálculo client-side sobre `dataFim`) **e** job diário notifica o proprietário por e-mail + push — `NotificarContratosProximosDoVencimento` |
| Garantia a 30 dias ou menos do vencimento | Job diário notifica o proprietário por e-mail + push — `NotificarGarantiasProximasDoVencimento` |
| Contrato encerrado sem renovação | `UNIDADE.status → vago` |
| Usuário envia avatar | Substitui `ARQUIVO` anterior do mesmo dono/tipo, se houver (1:1) |
| Proprietário envia foto de imóvel | Cria novo `ARQUIVO` (`tipo = foto_imovel`), soma às fotos existentes (1:N) |
| Parte envia documento do contrato | Substitui o `ARQUIVO` anterior do mesmo contrato, se houver (1:1) |
| Parte envia documento de garantia | Cria novo `ARQUIVO` (`tipo = documento_garantia`), soma aos existentes (1:N) |

## 5. API (REST) — estado atual implementado

Papéis: `[P]` proprietário, `[I]` inquilino, `[Ambos]` qualquer autenticado dono do recurso, `[público]` sem autenticação (inclui rotas por `{token}` de convite — capability token, ver nota de autorização abaixo).

```
POST   /auth/register/proprietario           registro self-service direto (fora do fluxo de convite)
POST   /auth/login
POST   /auth/logout                          [Ambos]
GET    /auth/me                              [Ambos]
PUT    /auth/me/cpf-cnpj                     [P]
PUT    /auth/me/telefone                     [P]
PUT    /auth/me/nome                         [Ambos]
POST   /auth/me/email                        [Ambos] não aplica na hora — grava email pendente, dispara confirmação
POST   /auth/senha/esqueci                   [público]
POST   /auth/senha/redefinir                 [público] token de e-mail
POST   /auth/senha/trocar                    [Ambos] autenticado, valida senha atual
POST   /auth/email/confirmar                 [público] token de e-mail — cadastro (VERIFICACAO_EMAIL) ou troca de e-mail (ALTERACAO_EMAIL)
POST   /auth/email/reenviar                  [Ambos]
POST   /auth/avatar                          [Ambos] multipart — avatar do usuário autenticado
POST   /auth/push-token                      [Ambos] registra/atualiza o Expo push token do device autenticado
POST   /auth/convites/proprietarios          [público] convite de onboarding do proprietário
GET    /auth/convites/{token}                [público]
POST   /auth/convites/{token}/aceitar        [público] conclui cadastro com e-mail/senha
GET    /auth/ping                            health check

GET    /imoveis                              [P]
POST   /imoveis                              [P]
GET    /imoveis/{id}                         [P]
POST   /imoveis/{id}/fotos                   [P] multipart — adiciona foto (1:N)
DELETE /imoveis/{id}/fotos/{fotoId}          [P]
POST   /imoveis/{id}/unidades                [P] adiciona 1 ou mais UNIDADE (individual = lista de 1 nome; lote = vários)

GET    /imoveis/{imovelId}/convites          [P]
POST   /imoveis/{imovelId}/convites          [P] cria convite de locação; unidadeId opcional no corpo (default: unidade padrão)
GET    /convites                             [P] convites ativos do proprietário (todos os imóveis)
GET    /convites/{token}                     [público]
POST   /convites/{token}/reenviar            [P] renova o prazo (7 dias) se estiver expirado e ainda PENDENTE
GET    /convites/{token}/eventos             [P] trilha de auditoria do convite (criado, envios, tentativas com token morto, etc.)
POST   /convites/{token}/revogar             [P]
POST   /convites/{token}/cadastro            [público] cadastro do inquilino + candidatura, tudo em uma chamada
POST   /convites/{token}/aceitar-vinculo     [I] vincula convite à conta de inquilino já existente
POST   /convites/{token}/garantia            [público/I] envia tipo + dados da garantia da candidatura
POST   /convites/{token}/garantia/documentos [público/I] multipart — documento comprobatório da garantia, ainda na candidatura (1:N, soma; dono_id = candidaturaId, não contratoId — ver nota acima)
GET    /convites/{token}/garantia/documentos [público/I] lista os documentos comprobatórios já enviados nesta candidatura
POST   /convites/{token}/assinar             [I] assinatura do inquilino via token (sem exigir sessão)
GET    /convites/me                          [I] convites/candidaturas vinculados ao inquilino autenticado

GET    /candidaturas                         [P]
POST   /candidaturas/{id}/aprovar            [P] gera o CONTRATO
POST   /candidaturas/{id}/recusar            [P]

GET    /contratos                            [Ambos] contratos do usuário autenticado
GET    /contratos/{id}                       [Ambos]
POST   /contratos/{id}/assinar               [Ambos]
GET    /contratos/{id}/pagamentos            [Ambos]
GET    /contratos/{id}/documentos            [Ambos] lista o documento do contrato + documentos de garantia
POST   /contratos/{id}/documento             [Ambos] multipart — documento do contrato (1:1, substitui)
POST   /contratos/{id}/garantia/documentos   [Ambos] multipart — documento de garantia (1:N, soma)

GET    /pagamentos                           [P] pagamentos de todos os contratos do proprietário
POST   /pagamentos/{id}/confirmar            [P]

GET    /arquivos/{id}/url                    [Ambos] URL pública direta (avatar/foto) ou SAS temporária (documento privado)

GET    /imoveis/{imovelId}/contas            [P]
POST   /imoveis/{imovelId}/contas            [P]
PATCH  /contas/{id}                          [P]
GET    /tipos-conta                          [P] catálogo de tipos de conta do proprietário
POST   /tipos-conta                          [P] cria um tipo de conta no catálogo do proprietário

POST   /imoveis/{imovelId}/chamados          [I] só se o inquilino tiver contrato na unidade do imóvel
GET    /imoveis/{imovelId}/chamados          [P]
GET    /imoveis/{imovelId}/categorias-chamado [Ambos] dono do imóvel ou inquilino com contrato na unidade
GET    /chamados                             [Ambos] chamados do usuário autenticado
PATCH  /chamados/{id}                        [P]
GET    /categorias-chamado                   [P] catálogo de categorias de chamado do proprietário
POST   /categorias-chamado                   [P] cria uma categoria no catálogo do proprietário

GET    /inquilinos/{id}                      [P] só se houver relação (contrato ou candidatura) com o inquilino

GET    /test-support/access-invites/{email}  só com app.test-support.enabled=true (E2E) — nunca em produção
GET    /test-support/magic-links/{email}     idem — legado, mantido só por transição
```

Use GET/POST/PATCH convencionais (não usar o método HTTP QUERY — padronizado em jun/2026, ainda sem suporte maduro em ferramentas de desenvolvimento).

**Nota sobre `UNIDADE`:** `POST /imoveis/{id}/unidades` adiciona unidades a um imóvel já cadastrado, além da `padrao = true` automática — mesmo endpoint pra uma unidade só ou várias de uma vez (`nomes: string[]`, sem convenção de nomenclatura imposta pela API; a UI é quem decide se gera nomes tipo "Casa 1 - Apto N" ou deixa o usuário digitar cada um). `POST /imoveis/{imovelId}/convites` aceita um `unidadeId` opcional no corpo — omitido, resolve pra `padrao` (mantém o caso comum de imóvel com uma unidade só sem exigir escolha); informado, tem que pertencer àquele imóvel. A partir daí, `CANDIDATURA`/`CONTRATO`/pagamentos seguem o `unidade_id` gravado no convite sem nenhuma resolução adicional — já eram assim antes desta unidade "extra" existir. `POST /imoveis/{imovelId}/chamados` não recebe `unidadeId` no corpo: é derivado automaticamente do contrato do inquilino chamador naquele imóvel (`AbrirChamado`), já que um inquilino só tem contrato numa unidade por vez ali.

**Nota sobre autorização:** não há filtro de autorização por rota — `TokenAuthenticationFilter` só resolve o `Principal` (proprietário ou inquilino) a partir do Bearer token; cada use case compara `proprietarioId`/`inquilinoId` do agregado carregado contra o principal chamador antes de agir, respondendo **404** (não 403) quando o chamador não tem posse do recurso — ver [`matriz-acesso-por-rota.md`](matriz-acesso-por-rota.md) para o mapeamento completo rota × persona, auditado e coberto por teste de integração. Rotas por `{token}` de convite são a exceção deliberada: o próprio token funciona como credencial de capability para o onboarding do inquilino, que começa sem sessão. Endpoints de upload/blob (`POST /auth/avatar`, `POST /imoveis/{id}/fotos`, `POST /contratos/{id}/documento`, `POST /contratos/{id}/garantia/documentos`, `GET /arquivos/{id}/url`) rodam em thread pool bloqueante (`@ExecuteOn(TaskExecutors.BLOCKING)`) porque o SDK síncrono do Azure Blob Storage não é compatível com as threads de event-loop do Netty.

**Nota sobre erros:** toda resposta de erro segue `{ code, message }` — ver [`catalogo-erros-api.md`](catalogo-erros-api.md) para o catálogo completo de códigos e a mensagem de frontend correspondente a cada um.

## 6. Design system — estado atual implementado

```ts
// frontend/src/design/tokens.ts
export const colors = {
  primary: '#111827',
  surface: '#F5F6F8',
  card: '#FFFFFF',
  border: '#E5E7EB',
  accent: '#2563EB',
  success: '#16A34A',
  warning: '#D97706',
  danger: '#DC2626',
  muted: '#6B7280',
} as const;

export const spacing = { xs: 4, sm: 8, md: 16, lg: 24, xl: 32 } as const;
export const radius = 10;
```

`radius` não é referenciado de forma sistemática pelos componentes — `Card`/`Button` usam a classe Tailwind `rounded-xl` (12px) e `Pill` usa `rounded-lg` (8px), hardcoded via `className` em vez de ler o token. Na prática o raio de canto varia entre 8-12px dependendo do componente, sem uma única fonte de verdade.

**Tipografia:** nenhuma fonte customizada é carregada (não há `useFonts`/`expo-font` no projeto) — todo texto usa a fonte padrão do sistema operacional: San Francisco no iOS, Roboto no Android, a sans-serif padrão do navegador no web. Não há um token de escala de tamanho centralizado; os tamanhos usados informalmente pelos componentes são: título de hub ~24px/800, subtítulo ~14px, texto de card/label ~14-16px, texto auxiliar/legenda ~12-13px, valor de `StatCard` ~28px/800, texto de botão ~15px/bold.

**Modo escuro:** não implementado — `app.json` fixa `userInterfaceStyle: "light"`, ignorando a preferência do sistema operacional.

**Ícones:** nenhuma biblioteca de ícones está em uso em nenhuma tela — toda comunicação visual é por texto e cor (inclusive a tab bar do proprietário, que só tem rótulo, sem ícone).

**Indicadores de status:** `StatusBadge` (`frontend/src/design/StatusBadge.tsx`) usa **cor + nome do status por extenso** (ponto colorido de 8px + texto), garantindo leitura correta mesmo para daltônicos sem depender só da cor. Não há diferenciação por forma geométrica — o ponto é sempre um círculo, independente do tipo de status ou urgência.

**Princípios visuais aplicados na prática:** superfícies brancas/cinza claro (`surface`/`card`), sem sombra nem gradiente; bordas cinza claras (`border: #E5E7EB`), cantos arredondados médios (8-12px, não retos). Inventário de componentes: `Card`, `Button`, `Pill`, `StatusBadge`, `StatCard`, `Avatar`, `HubHeader` (todos em `frontend/src/design/`).

## 7. Estrutura de telas (Expo Router) — estado atual implementado

```
app/
├── index.tsx                       # redireciona conforme sessão (proprietário/inquilino/login)
├── (auth)/
│   ├── login.tsx
│   ├── register.tsx                # registro self-service do proprietário
│   └── convite/
│       ├── [token].tsx             # aceite de convite de onboarding
│       └── manual.tsx              # entrada manual de token de convite
├── (owner)/                        # navegação por Tabs nativo do Expo Router
│   ├── _layout.tsx                 # <Tabs>: imoveis, contratos, pagamentos, convites, candidaturas
│   ├── imoveis/
│   │   ├── index.tsx               # lista com busca + filtros (status, tipo), KPIs de carteira (renda/ocupação/inadimplência), toggle "precisa de atenção" (vago >30 dias), card com aluguel vigente + 1ª foto
│   │   ├── novo.tsx                # cadastro de imóvel
│   │   └── [id]/
│   │       ├── index.tsx           # detalhe: dados, unidade, galeria de fotos, inquilino atual, seção Contas (catálogo de tipos por proprietário), seção Chamados (com ação de mudar status)
│   │       └── convite.tsx         # gerar convite de locação para a unidade padrão
│   ├── contratos/index.tsx         # hub: contratos do proprietário, totais (receita recorrente/assinados/pendentes), toggle "precisa de atenção" (vencendo ≤60 dias)
│   ├── pagamentos/index.tsx        # hub: pagamentos do proprietário, totais do mês, exportação CSV
│   ├── convites/
│   │   ├── index.tsx               # hub: convites ativos (reenviar/revogar)
│   │   └── novo.tsx
│   ├── candidaturas/index.tsx      # hub: candidaturas pendentes (aprovar/recusar)
│   ├── inquilinos/[id].tsx         # detalhe do inquilino (link:null — acessível, fora da tab bar)
│   └── perfil/index.tsx            # avatar, nome/e-mail editáveis (ContaCard), telefone, CPF/CNPJ, troca de senha proativa (link:null — acessível via avatar no header)
├── (tenant)/
│   └── tenant/
│       ├── index.tsx               # home: contratos, convites, dados do imóvel/proprietário, aceite de convite
│       ├── pagamentos.tsx          # meus pagamentos
│       ├── chamados.tsx            # abrir/acompanhar chamados
│       └── perfil.tsx              # avatar, nome/e-mail editáveis (ContaCard) (rota aninhada — evita colidir com (owner)/perfil/index.tsx em /perfil)
└── (contrato)/
    ├── [id]/revisar.tsx            # revisão + assinatura + documentos (contrato e garantia)
    └── locacao/[token].tsx         # fluxo do convite de locação: cadastro/vínculo, garantia, acompanhamento
```

**Ordem de construção (MVP crítico, telas 1-9 — concluído):** aceite de convite + criação de senha → login → dashboard do proprietário → cadastro de imóvel → gerar convite → cadastro do inquilino com senha → confirmação de garantia → aprovação → revisar contrato → assinar contrato.

**Telas 10+ (concluído):** detalhe do imóvel (galeria de fotos, seção de inquilino, Contas, Chamados), home do inquilino (contratos, convites, aceite, logout, pagamentos, chamados), perfis com avatar/telefone/troca de senha (ambos os lados), hubs de contratos/pagamentos/convites/candidaturas com navegação por Tabs, KPIs de carteira e totais agregados, busca/filtro na lista de imóveis, exportação CSV de pagamentos, documentos de contrato/garantia anexados na tela de revisão. A jornada completa do inquilino no frontend — incluindo chamados e visão de pagamentos — está fechada.

## 8. Restrições legais a respeitar na implementação

- **LGPD:** isolamento de dados entre proprietários (nunca expor dados de um inquilino/contrato a outro proprietário sem consentimento explícito — auditado em [`matriz-acesso-por-rota.md`](matriz-acesso-por-rota.md)); base legal padrão = execução de contrato; política de retenção configurável (default sugerido: 5 anos após fim do contrato).
- **Garantia:** um contrato só pode ter um tipo de garantia (nunca combinar).
- **Assinatura eletrônica:** usar nível avançado (ex.: Gov.br ou provedor tipo Clicksign/D4Sign), não exigir ICP-Brasil. Incluir campo para 2 testemunhas no template do contrato (dá força de título executivo extrajudicial).
- **Custódia de valores:** a plataforma nunca retém dinheiro diretamente (nem aluguel, nem caução). Caução em dinheiro é instruída para conta poupança conjunta entre locador e locatário. Pagamentos são confirmados manualmente no MVP ou via integração com parceiro já licenciado — nunca uma conta própria da plataforma.
- **Modelo de cobrança:** assinatura SaaS fixa por imóvel (não comissão sobre aluguel) — mantém a operação fora do escopo de corretagem/CRECI no MVP.
- **Locação por temporada:** se implementada, cadastro do imóvel deve exigir autorização documentada do condomínio antes de permitir publicação (decisão do STJ de mai/2026 sobre destinação residencial em condomínios).

## 9. Escopo do MVP — prioridades

**Essencial (constrói o esqueleto funcional) — concluído:** convite de onboarding do proprietário; login por e-mail/senha; cadastro de imóvel; convite e cadastro de inquilino com senha; geração de contrato com cláusulas condicionais; dashboard de status por imóvel; avatares e fotos de imóvel; documentos de contrato/garantia anexados; jornada completa do inquilino (candidatura, garantia, assinatura, pagamentos, chamados); catálogos de contas e categorias de chamado por proprietário; hardening de autorização por rota; catálogo de erros padronizado; alertas proativos de vencimento (contrato, garantia, conta) por e-mail e push.

**Ainda stub/fora do MVP real:** assinatura eletrônica de verdade (hoje é um clique que marca `assinado`, atrás da interface `AssinaturaProvider`); envio de e-mail transacional real (Resend configurado, mas fluxo depende de chave/domínio válidos); push notification real (infraestrutura pronta — `PushNotificationSender`/`ExpoPushNotificationSender`, registro de token — mas depende de um projeto EAS provisionado, `extra.eas.projectId` em `app.json`, que ainda não existe; sem ele, o canal de e-mail dos alertas continua funcionando normalmente, só o push é pulado).

**Importante, não bloqueante:** instrumentação de telemetria por persona; wizard guiado de primeiros passos.

**Adiar para fases futuras:** integração bancária automática; análise de crédito automática para seguro-fiança; publicação pública de imóvel, candidaturas e ranking (marketplace); painel de rentabilidade líquida (receita menos despesa) por imóvel.

Ver [`jornadas-e-backlog-tecnico.md`](jornadas-e-backlog-tecnico.md) e [`jornadas-e-prioridades-negocio.md`](jornadas-e-prioridades-negocio.md) para o backlog priorizado do que falta, e `README.md` (seção "O que não está neste MVP") para a lista completa de costuras/stubs.
