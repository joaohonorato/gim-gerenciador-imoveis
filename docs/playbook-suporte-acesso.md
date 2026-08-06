# Playbook de suporte — recuperação de acesso e convites

Runbook pra quem atende "meu convite não funciona" / "não consigo entrar" — proprietário ou inquilino. Não existe painel de admin nesta plataforma; toda investigação aqui usa o que já está exposto ao próprio usuário (self-service) ou consulta direta ao banco (para quem tem acesso operacional). O objetivo do produto é que a maior parte destes casos **nunca chegue ao suporte** — ver seção "Antes de escalar" — mas quando chegar, aqui está o roteiro.

## Antes de escalar: o que o próprio usuário já consegue resolver

| Sintoma | Quem resolve sozinho | Como |
|---|---|---|
| Esqueci minha senha | Proprietário ou inquilino | `POST /auth/senha/esqueci` (tela "Esqueci minha senha" no login) |
| Link de onboarding (proprietário) expirou ou sumiu | Prospect | Cadastro direto em `/register` — não depende de convite nenhum |
| Link de locação (inquilino) expirou | Inquilino | **Não tem self-service** — precisa pedir ao proprietário pra reenviar (ver abaixo). O reenvio renova o prazo automaticamente. |
| Proprietário quer reenviar um convite de locação parado | Proprietário | Hub de Convites → botão "Reenviar" no card do convite (funciona mesmo se já estiver expirado — renova o prazo em 7 dias sem trocar o link) |
| Proprietário quer entender por que um convite não "pegou" | Proprietário | Hub de Convites → botão "Ver histórico" no card — mostra a trilha de eventos (criado, envios, tentativas com token expirado/consumido, etc.) |

Se o usuário chegou até você, é porque um desses caminhos não existe pro caso dele (ex.: perdeu acesso à própria conta de proprietário e não tem mais o convite de onboarding à mão) ou porque ele não sabia que o caminho existia — a segunda situação normalmente se resolve só apontando pro botão certo.

## Diagnóstico: qual dos dois convites é

- **Convite de onboarding de proprietário** (`ConviteAcesso`) — usado uma vez, no primeiro acesso à plataforma. Rota: `/auth/convites/{token}`.
- **Convite de locação** (`Convite`) — usado pelo inquilino para se candidatar a um imóvel específico. Rota: `/convites/{token}`.

Se o usuário não souber qual dos dois tem, pergunte: "você está tentando criar sua conta na plataforma pela primeira vez, ou está tentando se candidatar a um imóvel que um proprietário te convidou?". Primeira resposta = onboarding; segunda = locação.

## Convite de onboarding do proprietário (`ConviteAcesso`)

Estados possíveis e o que cada um significa (resposta de `GET /auth/convites/{token}` traz `consumido` e `expirado`; tentar `POST /auth/convites/{token}/aceitar` num convite ruim sempre dá `401 AUTH_INVALID`, com a mensagem indicando qual dos três é):

| Situação | O que aconteceu | O que orientar |
|---|---|---|
| `consumido: true` | A conta já foi criada com esse convite | Pedir pra tentar fazer login normalmente; se não lembra a senha, `/auth/senha/esqueci` |
| `expirado: true` | Passaram mais de 7 dias desde a criação do convite | Orientar `/register` (cadastro direto, não depende de convite nenhum) — é sempre a saída mais rápida |
| Token não existe (`GET` retorna 401) | Link digitado errado, ou nunca existiu | Confirmar o e-mail correto; se a pessoa é uma prospect legítima, ela pode simplesmente ir em `/register` |

Chamar `POST /auth/convites/proprietarios` de novo pro mesmo e-mail **não cria um convite duplicado** — se já existir um pendente e ainda dentro do prazo pra aquele e-mail, a API devolve o mesmo (mesmo token). Isso evita links órfãos/ambíguos; não é necessário (nem possível) "cancelar" um convite antigo antes de gerar outro.

## Convite de locação (`Convite`)

`GET /convites/{token}` sempre retorna `200` com o estado do convite (nunca 401/404 pra um token que já existiu — só 404 se o token nunca existiu). Os campos que importam: `status` e `expirado`.

| `status` | `expirado` | O que significa | O que orientar |
|---|---|---|---|
| `PENDENTE` | `false` | Convite válido, aguardando o inquilino agir | Normal — pedir pro inquilino tentar de novo, checar se o e-mail/senha batem |
| `PENDENTE` | `true` | Prazo de 7 dias passou sem o inquilino agir | **Proprietário** vai no hub de Convites e clica "Reenviar" — isso renova o prazo automaticamente, sem precisar recriar o convite nem gerar um link novo |
| `EM_ANALISE` | — | Já existe candidatura, aguardando o proprietário aprovar/recusar | Orientar o proprietário a checar o hub de Candidaturas |
| `CONSUMIDO` | — | Contrato já foi assinado pelas duas partes | Convite cumpriu o papel dele; inquilino deve acessar a própria área (`/tenant`), não o link antigo |
| `RECUSADO` | — | Inquilino recusou a candidatura, ou proprietário recusou | Se ainda há interesse, proprietário precisa gerar um convite novo (não dá pra "reabrir" um recusado) |
| `REVOGADO` | — | Proprietário revogou o convite | Mesma orientação: gerar um convite novo, se for o caso |

Pra ver a trilha completa desse convite (todos os envios, tentativas de acesso com token já morto, etc.), oriente o proprietário a abrir "Ver histórico" no card do convite no hub — não existe visão equivalente pro lado do inquilino nem pra suporte fora do banco.

## Consulta direta (quando o usuário não consegue nem chegar no hub — ex. perdeu acesso à própria conta)

Sem painel de admin, a única via é consulta direta ao banco (ambiente com acesso operacional):

```sql
-- Convite de locação por e-mail do inquilino (histórico de envio fica na própria linha)
SELECT token, status, expira_em, ultimo_destino_envio, ultimo_status_envio, tentativas_envio
FROM convites WHERE ultimo_destino_envio = 'email@exemplo.com';

-- Convite de onboarding por e-mail do proprietário
SELECT token, consumido, expira_em FROM convites_acesso WHERE email = 'email@exemplo.com';

-- Trilha de auditoria completa de um convite específico (troque entidade_tipo
-- por CONVITE_ACESSO pro convite de onboarding)
SELECT tipo_evento, detalhe, criado_em FROM eventos_auditoria
WHERE entidade_tipo = 'CONVITE' AND entidade_id = '<id do convite>'
ORDER BY criado_em;
```

Em ambiente de teste/E2E (nunca em produção — guardado por `app.test-support.enabled`), `GET /test-support/access-invites/{email}` devolve o token de onboarding mais recente sem precisar de acesso ao banco.

## O que NÃO fazer

- Não peça pra alguém "revogar e recriar" um convite de locação só porque expirou — isso gera um link novo desnecessariamente quando "Reenviar" já resolve renovando o mesmo link.
- Não confirme a existência de um convite/conta pra quem pergunta sobre o e-mail de outra pessoa — nenhuma rota deste sistema faz isso por design (ver `docs/matriz-acesso-por-rota.md`, convenção 404-não-403); suporte deve seguir a mesma lógica manualmente.
