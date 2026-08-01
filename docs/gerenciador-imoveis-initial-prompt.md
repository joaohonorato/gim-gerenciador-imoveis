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
IMOVEL       1---N CONTA
IMOVEL       1---N CHAMADO
IMOVEL       1---N ARQUIVO       # fotos do imóvel
PROPRIETARIO 1---1 ARQUIVO       # avatar (opcional)
INQUILINO    1---1 ARQUIVO       # avatar (opcional)
```

| Entidade | Campos principais |
|---|---|
| **PROPRIETARIO** | id (uuid, PK), nome, cpf_cnpj (unique), perfil (enum: owner, admin), criado_em |
| **IMOVEL** | id (PK), proprietario_id (FK), endereco, cidade, matricula, numero, bairro, complemento, tipo_imovel (enum: casa, apartamento, comercial, outro), visibilidade (enum: privado, publicado — default `privado`) |
| **UNIDADE** | id (PK), imovel_id (FK), nome (ex.: "Imóvel completo", "Quarto 1"), padrao (boolean — `true` = representa o imóvel inteiro), status (enum: vago, reservado, alugado, manutencao) |
| **CONVITE** | id (PK), imovel_id (FK), unidade_id (FK), proprietario_id (FK), token (unique), expira_em, condições (tipo_contrato, valor_aluguel, período sugerido, garantia_aceita), status (enum: pendente, em_analise, consumido, expirado, recusado, revogado), candidatura_id (FK, opcional), dados do último envio (canal, status, destino, tentativas) |
| **CANDIDATURA** | id (PK), convite_id (FK), inquilino_id (FK), status (enum: pendente, aprovada, recusada), garantia_escolhida, criada_em |
| **CONTRATO** | id (PK), unidade_id (FK), inquilino_id (FK), convite_id (FK, opcional — rastreia o convite de origem para fechar o ciclo de vida dele ao assinar), data_inicio, data_fim, tipo (enum: residencial, temporada), valor_aluguel, indice_reajuste, status_assinatura (enum: pendente, assinado) |
| **INQUILINO** | id (PK), nome, cpf (unique), email, criado_em |
| **GARANTIA** | id (PK), contrato_id (FK), tipo (enum: caucao, fiador, seguro_fianca, titulo_capitalizacao, nenhuma), vencimento, dados_especificos (jsonb — estrutura varia por tipo) |
| **PAGAMENTO** | id (PK), contrato_id (FK), vencimento, pago_em, valor, status (enum: pendente, pago, atrasado) |
| **CONTA** | id (PK), imovel_id (FK), tipo (enum: iptu, condominio, agua, luz), vencimento, status (enum: pendente, pago) |
| **CHAMADO** | id (PK), imovel_id (FK), aberto_por (FK inquilino), categoria (enum: eletrica, hidraulica, estrutural, outro), descricao, status (enum: aberto, em_andamento, resolvido), aberto_em |
| **ARQUIVO** | id (PK), tipo (enum: avatar_proprietario, avatar_inquilino, foto_imovel, documento_contrato, documento_garantia), dono_id (proprietario_id / inquilino_id / imovel_id / contrato_id, dependendo do tipo), blob_key, nome_original, content_type, tamanho_bytes, criado_em — tabela única polimórfica, não uma tabela por tipo |

**Regras de integridade:**
- Um `CONTRATO` só pode ter **uma** `GARANTIA` (nunca combinar tipos).
- `INQUILINO` é entidade independente, nunca embutida no contrato — permite múltiplos contratos por CPF e reaproveitamento de cadastro.
- `IMOVEL.visibilidade` sempre `privado` no MVP; campo já existe para a fase de marketplace.
- **`UNIDADE` generaliza imóvel inteiro vs. subdividido**: no MVP, todo `IMOVEL` recebe automaticamente 1 `UNIDADE` com `padrao = true` no momento do cadastro — a UI do proprietário não expõe esse conceito, o fluxo parece idêntico a "alugar o imóvel inteiro". A estrutura já suporta, sem migração futura, subdividir um imóvel em múltiplas unidades (quartos, vagas) caso vire requisito.
- **Não sobreposição**: dois `CONTRATO` com `status_assinatura = assinado` na mesma `UNIDADE` não podem ter períodos (`data_inicio`–`data_fim`) sobrepostos. Validar no momento da aprovação do candidato.
- Contas (`CONTA`) permanecem no nível do `IMOVEL`, não da `UNIDADE` — são custos do imóvel inteiro (IPTU, condomínio), independente de subdivisão.
- **Ciclo de vida do `CONVITE`**: sai de `pendente` para `em_analise` quando uma `CANDIDATURA` é criada; vira `consumido` quando o `CONTRATO` gerado a partir dele é totalmente assinado (por ambas as partes); pode ser encerrado antes disso como `recusado` (pelo inquilino) ou `revogado` (pelo proprietário); `expirado` é automático após `expira_em`. Uma vez fora de `pendente`/`em_analise`, o convite sai da lista de "ativos" do proprietário.
- **Privacidade de `ARQUIVO`**: avatares e fotos de imóvel ficam em containers **públicos** (URL direta, cacheável); documentos de contrato/garantia ficam em container **privado**, acessados só via URL assinada (SAS) de curta duração (5–10 min), gerada sob demanda e checando que quem pede é uma das partes do contrato dono do arquivo.
- **Documento do contrato é 1:1** (novo upload substitui o anterior); **documentos de garantia são 1:N** (cada upload se soma aos anteriores — ex.: fiador precisa de RG + comprovante de renda).

## 4. Regras de negócio (motor de estados)

| Evento | Ação automática |
|---|---|
| Imóvel cadastrado | Cria `UNIDADE` padrão (`padrao = true`, `status = vago`); `IMOVEL.visibilidade = privado` |
| Convite gerado | Cria token único (expira em 7 dias) para onboarding de acesso ou para uma `UNIDADE` com condições de locação (valor, tipo, garantia aceita); `CONVITE.status = pendente` |
| Inquilino envia documentos | Cria/atualiza `INQUILINO` por CPF (reaproveita se já existir) |
| Usuário aceita convite | Confirma e-mail, cria senha e ativa a conta de acesso |
| Inquilino se candidata a um convite de locação | Cria `CANDIDATURA` (`status = pendente`); `CONVITE.status → em_analise` |
| Proprietário revoga convite ainda ativo | `CONVITE.status → revogado`; sai da lista de convites ativos do proprietário |
| Inquilino recusa convite/candidatura | `CONVITE.status → recusado` |
| Proprietário aprova candidato | Valida que não há `CONTRATO` assinado com período sobreposto na mesma `UNIDADE`; `CANDIDATURA.status → aprovada`; gera `CONTRATO` (`status_assinatura = pendente`, `convite_id` apontando para o convite de origem) a partir de template com cláusulas condicionais por tipo de garantia e tipo de locação; `UNIDADE.status → reservado` |
| Proprietário recusa candidato | `CANDIDATURA.status → recusada` |
| Ambas as partes assinam | `CONTRATO.status_assinatura = assinado`; `UNIDADE.status → alugado`; se o contrato tem `convite_id` e o convite ainda está `em_analise`, `CONVITE.status → consumido`; gera `PAGAMENTO` (um por mês do prazo); agenda alerta de vencimento da `GARANTIA` (30 dias antes) |
| Pagamento não confirmado até vencimento + 1 dia | `PAGAMENTO.status = atrasado`; notifica ambas as partes |
| Conta a 5 dias do vencimento | Notifica proprietário |
| Chamado criado | `status = aberto`; notifica proprietário |
| Chamado atualizado pelo proprietário | `status = em_andamento` → `resolvido` (com timestamp) |
| Contrato a 60 dias do fim | Notifica proprietário (renovação/reajuste) |
| Contrato encerrado sem renovação | `UNIDADE.status → vago` |
| Usuário envia avatar | Substitui `ARQUIVO` anterior do mesmo dono/tipo, se houver (1:1) |
| Proprietário envia foto de imóvel | Cria novo `ARQUIVO` (`tipo = foto_imovel`), soma às fotos existentes (1:N) |
| Parte envia documento do contrato | Substitui o `ARQUIVO` anterior do mesmo contrato, se houver (1:1) |
| Parte envia documento de garantia | Cria novo `ARQUIVO` (`tipo = documento_garantia`), soma aos existentes (1:N) |

## 5. API (REST) — estado atual implementado

Papéis: `[P]` proprietário, `[I]` inquilino, `[Ambos]` qualquer autenticado dono do recurso, `[público]` sem autenticação.

```
POST   /auth/register/proprietario           registro self-service direto (fora do fluxo de convite)
POST   /auth/login
POST   /auth/logout                          [Ambos]
GET    /auth/me                              [Ambos]
POST   /auth/avatar                          [Ambos] multipart — avatar do usuário autenticado
POST   /auth/convites/proprietarios          convite de onboarding do proprietário
GET    /auth/convites/{token}                [público]
POST   /auth/convites/{token}/aceitar        [público] conclui cadastro com e-mail/senha
GET    /auth/ping                            health check

GET    /imoveis                              [P]
POST   /imoveis                              [P]
GET    /imoveis/{id}                         [P]
POST   /imoveis/{id}/fotos                   [P] multipart — adiciona foto (1:N)
DELETE /imoveis/{id}/fotos/{fotoId}          [P]

GET    /imoveis/{imovelId}/convites          [P]
POST   /imoveis/{imovelId}/convites          [P] cria convite de locação (condições da unidade padrão)
GET    /convites                             [P] convites ativos do proprietário (todos os imóveis)
GET    /convites/{token}                     [público]
POST   /convites/{token}/reenviar            [P]
POST   /convites/{token}/revogar             [P]
POST   /convites/{token}/cadastro            [público] cadastro do inquilino + candidatura, tudo em uma chamada
POST   /convites/{token}/aceitar-vinculo     [I] vincula convite à conta de inquilino já existente
POST   /convites/{token}/garantia            [público/I] envia tipo + dados da garantia da candidatura
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

POST   /imoveis/{imovelId}/chamados          [I]
GET    /imoveis/{imovelId}/chamados          [Ambos]
GET    /chamados                             [Ambos] chamados do usuário autenticado
PATCH  /chamados/{id}                        [P]

GET    /inquilinos/{id}                      [P]

GET    /test-support/access-invites/{email}  só com app.test-support.enabled=true (E2E) — nunca em produção
```

Use GET/POST/PATCH convencionais (não usar o método HTTP QUERY — padronizado em jun/2026, ainda sem suporte maduro em ferramentas de desenvolvimento).

**Nota sobre `UNIDADE`:** os endpoints acima (`/imoveis/{id}/convites`, aprovação, contrato) operam sobre a `UNIDADE` padrão do imóvel nos bastidores — a API mantém os caminhos como estão (referenciando `imovel_id`) e resolve internamente para a unidade `padrao = true` daquele imóvel, sem expor o conceito na superfície da API do MVP. Se subdivisão em múltiplas unidades virar requisito, adicionar `GET/POST /imoveis/{id}/unidades` e trocar as referências para `unidade_id` explícito nos endpoints de convite/contrato.

**Nota sobre autorização:** não há filtro de autorização por rota — `TokenAuthenticationFilter` só resolve o `Principal` (proprietário ou inquilino) a partir do Bearer token; cada use case compara `proprietarioId`/`inquilinoId` do agregado carregado contra o principal chamador antes de agir. Endpoints de upload/blob (`POST /auth/avatar`, `POST /imoveis/{id}/fotos`, `POST /contratos/{id}/documento`, `POST /contratos/{id}/garantia/documentos`, `GET /arquivos/{id}/url`) rodam em thread pool bloqueante (`@ExecuteOn(TaskExecutors.BLOCKING)`) porque o SDK síncrono do Azure Blob Storage não é compatível com as threads de event-loop do Netty.

## 6. Design system — estilo Bauhaus

```ts
export const color = {
  bg: '#F7F5F0', surface: '#FFFFFF', border: '#111111',
  textPrimary: '#111111', textSecondary: '#8A8A85',
  brand: '#1B3FE0', brandStrong: '#0E2699',
  success: '#2E8B3D', warning: '#F2B705', danger: '#D62828',
} as const;

export const colorDark = {
  bg: '#111111', surface: '#1C1C1A', border: '#F7F5F0',
  textPrimary: '#F7F5F0', textSecondary: '#9C9A93',
  brand: '#5B7CFF', brandStrong: '#8AA0FF',
  success: '#4CAF5F', warning: '#F2B705', danger: '#E85C5C',
} as const;

export const font = {
  display: 'SpaceGrotesk-Medium',   // títulos
  body: 'Inter-Regular',            // texto corrido
  bodyMedium: 'Inter-Medium',       // rótulos, botões
} as const;

export const fontSize = { xs: 12, sm: 14, base: 16, lg: 20, xl: 24, '2xl': 28 } as const;
export const space = { xs: 4, sm: 8, md: 16, lg: 24, xl: 32, '2xl': 48 } as const; // grid 8pt
export const radius = { none: 0, sm: 2, md: 4 } as const; // Bauhaus: cantos retos, evitar radius grande
```

**Princípios visuais:** superfícies planas, sem sombra/gradiente; bordas pretas sólidas (não cinza sutil); indicadores de status usam **cor + forma geométrica** juntas — círculo = ok, quadrado = atenção, triângulo = urgente (garante leitura mesmo para daltônicos). Ícones: biblioteca outline única e consistente (Phosphor, Lucide ou Tabler), um ícone = um significado, nunca reusar.

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
│   │   ├── index.tsx               # lista com filtros (busca, cidade, status, tipo) + seção de inquilino se alugado
│   │   ├── novo.tsx                # cadastro de imóvel
│   │   └── [id]/
│   │       ├── index.tsx           # detalhe: dados, unidade, galeria de fotos, inquilino atual
│   │       └── convite.tsx         # gerar convite de locação para a unidade padrão
│   ├── contratos/index.tsx         # hub: todos os contratos do proprietário
│   ├── pagamentos/index.tsx        # hub: todos os pagamentos do proprietário
│   ├── convites/
│   │   ├── index.tsx               # hub: convites ativos (reenviar/revogar)
│   │   └── novo.tsx
│   ├── candidaturas/index.tsx      # hub: candidaturas pendentes (aprovar/recusar)
│   ├── inquilinos/[id].tsx         # detalhe do inquilino (link:null — acessível, fora da tab bar)
│   └── perfil/index.tsx            # avatar + dados (link:null — acessível via avatar no header)
├── (tenant)/
│   ├── tenant/index.tsx            # home: contratos, convites, dados do imóvel/proprietário, aceite de convite
│   └── perfil.tsx                  # avatar + dados
└── (contrato)/
    ├── [id]/revisar.tsx            # revisão + assinatura + documentos (contrato e garantia)
    └── locacao/[token].tsx         # fluxo do convite de locação: cadastro/vínculo, garantia, acompanhamento
```

**Ordem de construção (MVP crítico, telas 1-9 — concluído):** aceite de convite + criação de senha → login → dashboard do proprietário → cadastro de imóvel → gerar convite → cadastro do inquilino com senha → confirmação de garantia → aprovação → revisar contrato → assinar contrato.

**Telas 10+ (concluído/em andamento):** detalhe do imóvel (com galeria de fotos e seção de inquilino), home do inquilino (contratos, convites, aceite, logout), perfis com avatar (ambos os lados), hubs de contratos/pagamentos/convites/candidaturas com navegação por Tabs, documentos de contrato/garantia anexados na tela de revisão. **Ainda pendente** (ver `docs/plano-execucao-ajustes.md`): telas de chamados para o inquilino (abrir/acompanhar) — hoje só o backend está coberto.

## 8. Restrições legais a respeitar na implementação

- **LGPD:** isolamento de dados entre proprietários (nunca expor dados de um inquilino/contrato a outro proprietário sem consentimento explícito); base legal padrão = execução de contrato; política de retenção configurável (default sugerido: 5 anos após fim do contrato).
- **Garantia:** um contrato só pode ter um tipo de garantia (nunca combinar).
- **Assinatura eletrônica:** usar nível avançado (ex.: Gov.br ou provedor tipo Clicksign/D4Sign), não exigir ICP-Brasil. Incluir campo para 2 testemunhas no template do contrato (dá força de título executivo extrajudicial).
- **Custódia de valores:** a plataforma nunca retém dinheiro diretamente (nem aluguel, nem caução). Caução em dinheiro é instruída para conta poupança conjunta entre locador e locatário. Pagamentos são confirmados manualmente no MVP ou via integração com parceiro já licenciado — nunca uma conta própria da plataforma.
- **Modelo de cobrança:** assinatura SaaS fixa por imóvel (não comissão sobre aluguel) — mantém a operação fora do escopo de corretagem/CRECI no MVP.
- **Locação por temporada:** se implementada, cadastro do imóvel deve exigir autorização documentada do condomínio antes de permitir publicação (decisão do STJ de mai/2026 sobre destinação residencial em condomínios).

## 9. Escopo do MVP — prioridades

**Essencial (constrói o esqueleto funcional) — concluído:** convite de onboarding do proprietário; login por e-mail/senha; cadastro de imóvel; convite e cadastro de inquilino com senha; geração de contrato com cláusulas condicionais; dashboard de status por imóvel; avatares e fotos de imóvel; documentos de contrato/garantia anexados.

**Ainda stub/fora do MVP real:** assinatura eletrônica de verdade (hoje é um clique que marca `assinado`, atrás da interface `AssinaturaProvider`); envio de e-mail transacional real (Resend configurado, mas fluxo depende de chave/domínio válidos); chamados de manutenção (backend completo, sem UI para o inquilino abrir/acompanhar).

**Importante, não bloqueante:** controle de pagamentos com alerta de atraso; alertas de vencimento (garantia, reajuste, contas) — pagamentos são gerados na assinatura, mas ainda não há scheduler de alertas.

**Adiar para fases futuras:** integração bancária automática; análise de crédito automática para seguro-fiança; publicação pública de imóvel, candidaturas e ranking (marketplace).

Ver `docs/plano-execucao-ajustes.md` para o backlog priorizado do que falta e `README.md` (seção "O que não está neste MVP") para a lista completa de costuras/stubs.
