# Matriz de acesso por rota

Documentação viva do controle de acesso da API. Cobre toda rota REST em
`backend/src/main/java/br/com/imoveis/infrastructure/rest/`, indicando quem
pode chamá-la e por qual mecanismo a posse do recurso é verificada.

**Convenção do projeto**: rotas que resolvem um recurso a partir de um
`{id}`/`{imovelId}` de path filtram pelo `proprietarioId`/`inquilinoId` do
`Principal` autenticado *antes* de responder. Quando o chamador não tem posse
do recurso, a resposta é sempre **404 `NaoEncontradoException`** — nunca 403.
Isso é deliberado: 403 confirmaria a existência do recurso para quem não tem
acesso a ele; 404 não revela nada. Ver `GlobalErrorHandler` — nenhuma exceção
do domínio mapeia para 403.

Duas categorias de rota aparecem nas tabelas abaixo:
- **`{id}` de recurso possuído** — precisa (e, após esta auditoria, tem) filtro de posse.
- **`{token}` de convite** — token de convite funciona como credencial de capability (o
  próprio token não-adivinhável É a autorização) para o fluxo de onboarding do
  inquilino, que começa sem sessão. Rotas nessa categoria são intencionalmente
  públicas; não são "buracos" de autorização.

Legenda das colunas de persona: **Prop. dono** = proprietário dono do recurso;
**Prop. outro** = qualquer outro proprietário autenticado; **Inq. parte** =
inquilino que é parte do recurso (contrato/candidatura/chamado); **Inq.
outro** = qualquer outro inquilino autenticado; **Público** = sem
autenticação.

## AuthController (`/auth`)

| Método | Rota | Quem acessa | Observação |
|---|---|---|---|
| POST | `/register/proprietario` | Público | Cria conta nova; não há recurso alheio a proteger. |
| POST | `/login` | Público | — |
| POST | `/senha/esqueci` | Público | Token de redefinição enviado por e-mail. |
| POST | `/senha/redefinir` | Público (token) | Token unívoco de redefinição autoriza. |
| POST | `/senha/trocar` | Autenticado, self | Exige senha atual; opera sobre a própria `contaAcessoId` do principal. |
| POST | `/email/confirmar` | Público (token) | Aceita token de `VERIFICACAO_EMAIL` (cadastro) ou `ALTERACAO_EMAIL` (troca de e-mail via `/me/email`) — mesmo endpoint, finalidade resolvida a partir do token. |
| POST | `/email/reenviar` | Autenticado, self | — |
| POST | `/convites/proprietarios` | Público | Entry-point de auto-cadastro de novo proprietário (onboarding começa por convite, ver CLAUDE.md); não expõe dado de terceiros. Idempotente por e-mail desde o rank 5: reaproveita um convite pendente existente em vez de duplicar. |
| GET | `/convites/{token}` | Público (token) | Capability token — candidato precisa ver a oferta antes de ter conta. |
| POST | `/convites/{token}/aceitar` | Público (token) | Finaliza cadastro do proprietário com e-mail/senha. |
| GET | `/me` | Autenticado, self | — |
| PUT | `/me/cpf-cnpj` | Autenticado, self (proprietário) | 401 se `proprietarioId` nulo (chamador inquilino). |
| PUT | `/me/telefone` | Autenticado, self (proprietário) | Idem. |
| PUT | `/me/nome` | Autenticado, self | Proprietário ou inquilino, resolvido pelo `Principal`. |
| POST | `/me/email` | Autenticado, self | Não troca o e-mail na hora — grava `emailPendente` e manda o link de confirmação pro *novo* endereço; login continua pelo e-mail atual até `/email/confirmar` ser chamado com esse token. |
| POST | `/avatar` | Autenticado, self | `donoId` = próprio principal (proprietário ou inquilino). |
| POST | `/push-token` | Autenticado, self | Registra/atualiza o Expo push token do device; upsert por token (não por id), sempre associado ao `contaAcessoId` do chamador. |
| POST | `/logout` | Autenticado, self | — |
| GET | `/ping` | Público | Health check. |

## ImoveisController (`/imoveis`)

| Método | Rota | Quem acessa | Mecanismo |
|---|---|---|---|
| GET | `/` | Prop. dono (só os próprios) | `p.requireProprietarioId()` — **corrigido nesta auditoria**: usava `p.proprietarioId()` nullable, deixando um inquilino chamando receber lista vazia em vez de 401. |
| POST | `/` | Prop. dono | `p.requireProprietarioId()` — **corrigido nesta auditoria**: usava valor nullable, causando `NullPointerException`/500 se um inquilino chamasse (`Imovel.cadastrar` faz `Objects.requireNonNull(proprietarioId)`). |
| GET | `/{id}` | Prop. dono | `.filter(i -> i.proprietarioId().equals(p.proprietarioId()))` → 404. |
| POST | `/{id}/fotos` | Prop. dono | Idem. |
| DELETE | `/{id}/fotos/{fotoId}` | Prop. dono | Idem. |
| POST | `/{id}/unidades` | Prop. dono | `AdicionarUnidades` filtra o imóvel por `proprietarioId` → 404. |

## ContratosController (`/contratos`) — padrão de referência

| Método | Rota | Quem acessa | Mecanismo |
|---|---|---|---|
| GET | `/` | Prop. dono (contratos próprios) ou Inq. parte (contratos próprios) | Escopado por `p.inquilinoId()`/`p.requireProprietarioId()`, sem `{id}`. |
| GET | `/{id}` | Prop. dono, Inq. parte | `isOwnerOrTenant(p, contrato)` → 404. |
| POST | `/{id}/assinar` | Prop. dono (se `parte=PROPRIETARIO`), Inq. parte (se `parte=INQUILINO`) | `isOwnerOrTenant` + checagem adicional de que a `parte` do corpo bate com o tipo de conta do chamador → 404. |
| GET | `/{id}/pagamentos` | Prop. dono, Inq. parte | `isOwnerOrTenant` → 404. |
| GET | `/{id}/documentos` | Prop. dono, Inq. parte | `isOwnerOrTenant` → 404. |
| POST | `/{id}/documento` | Prop. dono, Inq. parte | `isOwnerOrTenant` → 404. |
| POST | `/{id}/garantia/documentos` | Prop. dono, Inq. parte | `isOwnerOrTenant` → 404. |

## CandidaturasController (`/candidaturas`)

| Método | Rota | Quem acessa | Mecanismo |
|---|---|---|---|
| GET | `/` | Prop. dono (pendentes dos próprios imóveis) | `requireProprietarioId()`. |
| POST | `/{id}/aprovar` | Prop. dono do convite da candidatura | `AprovarCandidato` compara `convite.proprietarioId()` → 404. |
| POST | `/{id}/recusar` | Prop. dono do convite da candidatura | `RecusarCandidato`, mesmo padrão → 404. |

## ConvitesController (`/convites`, `/imoveis/{imovelId}/convites`)

| Método | Rota | Quem acessa | Mecanismo |
|---|---|---|---|
| GET | `/convites` | Prop. dono (só os próprios) | `requireProprietarioId()`. |
| GET | `/imoveis/{imovelId}/convites` | Prop. dono do imóvel | Filtro por `proprietarioId` do imóvel → 404. |
| POST | `/imoveis/{imovelId}/convites` | Prop. dono do imóvel | `GerarConvite` filtra o imóvel por `proprietarioId` → 404. `unidadeId` opcional no corpo tem que pertencer a esse imóvel (senão 404 "unidade") — sem isso um proprietário poderia gerar convite apontando pra unidade de outro imóvel. |
| POST | `/convites/{token}/revogar` | Prop. dono do convite | `RevogarConvite` compara `convite.proprietarioId()` → 404. |
| GET | `/convites/{token}` | Público (token) | Capability token — candidato vê a oferta antes de ter conta. |
| POST | `/convites/{token}/reenviar` | Prop. dono do convite | Checagem explícita de `convite.proprietarioId()` → 404. Se o convite estiver `PENDENTE` e expirado, renova o prazo (`Convite.renovar`) antes de reenviar — rank 5. |
| GET | `/convites/{token}/eventos` | Prop. dono do convite | Checagem explícita de `convite.proprietarioId()` → 404. Trilha de auditoria mínima (rank 5). |
| POST | `/convites/{token}/cadastro` | Público (token) | Capability token — cria conta+candidatura do inquilino. |
| POST | `/convites/{token}/aceitar-vinculo` | Inquilino autenticado (qualquer) | Ver nota "Decisão intencional" abaixo — não valida que o e-mail do convite bate com o do chamador. |
| POST | `/convites/{token}/garantia` | Público (token) | Capability token — parte do fluxo de candidatura pré-conta. |
| POST/GET | `/convites/{token}/garantia/documentos` | Público (token) | Idem; `candidaturaId` resolvido a partir do próprio token. |
| GET | `/convites/me` | Inq. parte (candidaturas próprias) | `requireInquilinoId()`, sem `{id}` de terceiro. |
| POST | `/convites/{token}/assinar` | Público (token) | Capability token — assinatura do inquilino, ainda sem sessão obrigatória nesse ponto do fluxo. |

## ChamadosController

| Método | Rota | Quem acessa | Mecanismo |
|---|---|---|---|
| POST | `/imoveis/{imovelId}/chamados` | Inq. com contrato em alguma unidade do imóvel | `requireInquilinoId()` + **corrigido nesta auditoria**: `AbrirChamado` não verificava nenhuma relação entre o inquilino chamador e o imóvel — qualquer inquilino autenticado podia abrir chamado em imóvel de proprietário com quem nunca teve contrato. Agora exige `contratoRepository.findByInquilinoId(...)` conter alguma unidade do imóvel → 404 senão; a `unidade_id` do chamado é a do contrato encontrado (não vem no corpo da requisição). |
| GET | `/imoveis/{imovelId}/chamados` | Prop. dono do imóvel | Filtro por `proprietarioId` → 404. |
| GET | `/imoveis/{imovelId}/categorias-chamado` | Prop. dono do imóvel, Inq. com contrato na unidade | **Corrigido nesta auditoria**: antes, qualquer principal autenticado podia consultar o catálogo de categorias de qualquer `imovelId`, sem checar relação nenhuma. Agora exige dono OU inquilino com contrato na unidade → 404 senão. |
| GET | `/chamados` | Prop. dono (chamados dos próprios imóveis) ou Inq. parte (chamados que abriu) | Escopado por tipo de conta do principal, sem `{id}` alheio; filtros `?inquilinoId=`/`?proprietarioId=` são aplicados só dentro do conjunto já autorizado (`ListarChamados.paraProprietario`/`paraInquilino`). |
| PATCH | `/chamados/{id}` | Prop. dono do imóvel do chamado | `AtualizarChamado` compara `imovel.proprietarioId()` → 404. |
| GET | `/categorias-chamado` | Prop. dono (catálogo próprio) | `requireProprietarioId()`. |
| POST | `/categorias-chamado` | Prop. dono (cria no catálogo próprio) | `requireProprietarioId()`. |

## ContasController

| Método | Rota | Quem acessa | Mecanismo |
|---|---|---|---|
| GET | `/imoveis/{imovelId}/contas` | Prop. dono do imóvel | Filtro por `proprietarioId` → 404. |
| POST | `/imoveis/{imovelId}/contas` | Prop. dono do imóvel | `RegistrarConta` filtra o imóvel por `proprietarioId`, valida que `tipoContaId` e (quando `responsavel=INQUILINO`) `contratoId` também pertencem a esse proprietário → 404. |
| PATCH | `/contas/{id}` | Prop. dono (via unidade → imóvel) | Resolve `conta → unidade → imóvel` e compara `proprietarioId` → 404. |
| GET | `/tipos-conta` | Prop. dono (catálogo próprio) | `requireProprietarioId()`. |
| POST | `/tipos-conta` | Prop. dono (cria no catálogo próprio) | `requireProprietarioId()`. |

## PagamentosController (`/pagamentos`)

| Método | Rota | Quem acessa | Mecanismo |
|---|---|---|---|
| GET | `/` | Prop. dono (pagamentos dos próprios contratos) | `requireProprietarioId()`, sem `{id}` alheio. |
| POST | `/{id}/confirmar` | Prop. dono do contrato do pagamento | `ConfirmarPagamento` resolve `pagamento → contrato` e compara `proprietarioId` → 404. |

## InquilinosController (`/inquilinos`)

| Método | Rota | Quem acessa | Mecanismo |
|---|---|---|---|
| GET | `/{id}` | Prop. que tem/teve relação com o inquilino (contrato ou candidatura) | `BuscarInquilinoDoProprietario` — `temRelacao` via `ContratoRepository`/`ConviteRepository` → 404. |

## ArquivosController (`/arquivos`)

| Método | Rota | Quem acessa | Mecanismo |
|---|---|---|---|
| GET | `/{id}/url` | Público se `!arquivo.tipo().privado()` (ex.: foto de imóvel); senão Prop. dono/Inq. parte do contrato (ou da candidatura, se ainda não há contrato) | `isOwnerOrTenant` sobre o contrato dono do arquivo, com fallback via candidatura para documentos de garantia enviados antes da aprovação → 404. |

## TestSupportController (`/test-support`)

Guardado por `app.test-support.enabled` (ligado só em `test`/`default`) — não
existe em produção real. Todas as rotas resolvem por `email`, não por um
recurso possuído, e existem exclusivamente para destravar E2E/testes manuais
sem SMTP real; fora do escopo desta auditoria de posse por `{id}`.

## Vulnerabilidades encontradas e corrigidas nesta auditoria

1. **`AbrirChamado` (POST `/imoveis/{imovelId}/chamados`)** — não existia
   nenhuma verificação de que o inquilino chamador tivesse relação com o
   imóvel informado. Qualquer inquilino autenticado podia abrir chamados em
   imóveis de proprietários com quem nunca teve contrato, poluindo a fila de
   chamados de terceiros. Corrigido exigindo que o inquilino tenha um
   contrato na unidade padrão do imóvel; senão 404.
2. **`GET /imoveis/{imovelId}/categorias-chamado`** — resolvia o catálogo de
   categorias a partir do imóvel sem checar nenhuma relação do chamador com
   ele, expondo o catálogo (nomes de categoria) de qualquer `imovelId` a
   qualquer autenticado. Corrigido exigindo dono OU inquilino com contrato na
   unidade; senão 404. Severidade baixa (dado exposto é só o catálogo de
   nomes de categoria), mas quebra a convenção de posse do projeto.
3. **`ImoveisController` (`GET`/`POST /imoveis`)** — usava
   `p.proprietarioId()` (nullable) em vez de `p.requireProprietarioId()`.
   `POST` causava `NullPointerException`/500 se chamado por um inquilino
   (`Imovel.cadastrar` faz `Objects.requireNonNull`); `GET` retornava lista
   vazia silenciosamente em vez de 401. Não é um vazamento de dado entre
   contas (nenhum dado de terceiro é exposto), mas é uma checagem de papel
   faltante que gerava comportamento inconsistente com o resto da API.
   Corrigido alinhando com o padrão já usado em `ChamadosController`/
   `ContasController` (`requireProprietarioId()` para rotas sem recurso
   pré-existente contra o qual comparar).

Todas as demais rotas com `{id}`/`{imovelId}` auditadas (11 controllers, ver
tabelas acima) já filtravam corretamente por posse antes desta auditoria.

## Decisões de design intencionais (não são gaps)

- **Rotas `/convites/{token}/...` sem `Principal`** (`GET /convites/{token}`,
  `POST /convites/{token}/cadastro`, `/garantia`, `/garantia/documentos`,
  `/assinar`): o token do convite é a própria credencial — modelo de
  capability token já estabelecido no projeto para o fluxo de onboarding do
  inquilino, que começa antes de existir sessão autenticada.
- **`POST /convites/{token}/aceitar-vinculo`** não valida que o e-mail alvo
  do convite bate com o e-mail do inquilino autenticado que está aceitando —
  qualquer inquilino com sessão e posse do token pode vincular-se à
  candidatura. Isso é consistente com o modelo de capability token acima (a
  posse do token, não a identidade, autoriza), mas é o único ponto onde essa
  decisão tem uma superfície um pouco maior: um convite pensado para o
  e-mail X pode ser aceito por um inquilino logado como Y, se Y tiver o
  link. Documentado aqui como decisão consciente, não corrigido nesta
  auditoria — revisar se o produto passar a depender de garantir que só o
  destinatário pretendido do convite possa aceitá-lo.
- **`POST /auth/convites/proprietarios`** é público de propósito: é o
  entry-point de auto-cadastro de novos proprietários (onboarding "começa
  por convite" mesmo para o primeiro acesso, ver `CLAUDE.md`), não uma ação
  administrativa. Não recebe nem expõe `{id}` de terceiros.

## Testes de regressão

`backend/src/test/java/br/com/imoveis/infrastructure/IdorAccessControlIT.java`
monta um cenário com dois proprietários e dois inquilinos sem relação entre
si e cobre, para cada rota de posse relevante, que o acesso cross-tenant
retorna exatamente 404 (nunca 200 com dado vazado, nunca 403):
`GET/POST /contratos/{id}` (detalhe, pagamentos, assinar), `GET /imoveis/{id}`,
`GET /inquilinos/{id}`, `PATCH /contas/{id}`,
`POST /imoveis/{imovelId}/contas`, `PATCH /chamados/{id}`,
`POST /imoveis/{imovelId}/chamados` e
`GET /imoveis/{imovelId}/categorias-chamado` (as duas últimas cobrindo
diretamente as vulnerabilidades corrigidas acima).
