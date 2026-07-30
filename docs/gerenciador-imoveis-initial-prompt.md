# Plataforma de Gestão de Imóveis — Especificação para Implementação

## 1. Contexto

SaaS multi-tenant para proprietários de imóveis gerenciarem sua carteira de locação (pagamentos, contas, manutenção, contratos). Múltiplos proprietários independentes usam a mesma plataforma, cada um só enxerga sua própria carteira.

**Modelo de negócio do MVP:** convite fechado — o proprietário cadastra o imóvel e convida um inquilino específico; não há descoberta pública de imóveis. **Roadmap:** evoluir para marketplace aberto (candidaturas públicas) sem redesenhar o core. Por isso, decisões de arquitetura abaixo já preparam esse caminho — implemente respeitando essas costuras.

## 2. Stack técnica

- **Backend:** Java + Micronaut (REST API)
- **Frontend:** React Native + Expo (compila para Web, iOS e Android a partir de um único código)
- **Roteamento:** Expo Router (file-based)
- **Estilização:** NativeWind (Tailwind para React Native), consumindo os tokens da seção 6
- **Banco de dados:** relacional (Postgres recomendado); campos flexíveis por tipo de garantia em coluna `jsonb`/`json`
- **Autenticação:** login por e-mail e senha, com onboarding iniciado por convite por e-mail
- **Assinatura eletrônica:** integração externa, nível avançado (ver seção 8)

## 3. Modelo de dados

```
PROPRIETARIO 1---N IMOVEL
IMOVEL       1---N UNIDADE       # toda IMOVEL ganha 1 UNIDADE padrão automaticamente na criação
UNIDADE      1---N CONTRATO
INQUILINO    1---N CONTRATO      # um inquilino pode ter vários contratos simultâneos
CONTRATO     1---1 GARANTIA
CONTRATO     1---N PAGAMENTO
IMOVEL       1---N CONTA
IMOVEL       1---N CHAMADO
```

| Entidade | Campos principais |
|---|---|
| **PROPRIETARIO** | id (uuid, PK), nome, cpf_cnpj (unique), perfil (enum: owner, admin), criado_em |
| **IMOVEL** | id (PK), proprietario_id (FK), endereco, cidade, matricula, visibilidade (enum: privado, publicado — default `privado`) |
| **UNIDADE** | id (PK), imovel_id (FK), nome (ex.: "Imóvel completo", "Quarto 1"), padrao (boolean — `true` = representa o imóvel inteiro), status (enum: vago, reservado, alugado, manutencao) |
| **CONTRATO** | id (PK), unidade_id (FK), inquilino_id (FK), data_inicio, data_fim, tipo (enum: residencial, temporada), valor_aluguel, indice_reajuste, status_assinatura (enum: pendente, assinado) |
| **INQUILINO** | id (PK), nome, cpf (unique), email, criado_em |
| **GARANTIA** | id (PK), contrato_id (FK), tipo (enum: caucao, fiador, seguro_fianca), vencimento, dados_especificos (jsonb — estrutura varia por tipo) |
| **PAGAMENTO** | id (PK), contrato_id (FK), vencimento, pago_em, valor, status (enum: pendente, pago, atrasado) |
| **CONTA** | id (PK), imovel_id (FK), tipo (enum: iptu, condominio, agua, luz), vencimento, status (enum: pendente, pago) |
| **CHAMADO** | id (PK), imovel_id (FK), aberto_por (FK inquilino), categoria (enum: eletrica, hidraulica, estrutural, outro), descricao, status (enum: aberto, em_andamento, resolvido), aberto_em |

**Regras de integridade:**
- Um `CONTRATO` só pode ter **uma** `GARANTIA` (nunca combinar tipos).
- `INQUILINO` é entidade independente, nunca embutida no contrato — permite múltiplos contratos por CPF e reaproveitamento de cadastro.
- `IMOVEL.visibilidade` sempre `privado` no MVP; campo já existe para a fase de marketplace.
- **`UNIDADE` generaliza imóvel inteiro vs. subdividido**: no MVP, todo `IMOVEL` recebe automaticamente 1 `UNIDADE` com `padrao = true` no momento do cadastro — a UI do proprietário não expõe esse conceito, o fluxo parece idêntico a "alugar o imóvel inteiro". A estrutura já suporta, sem migração futura, subdividir um imóvel em múltiplas unidades (quartos, vagas) caso vire requisito.
- **Não sobreposição**: dois `CONTRATO` com `status_assinatura = assinado` na mesma `UNIDADE` não podem ter períodos (`data_inicio`–`data_fim`) sobrepostos. Validar no momento da aprovação do candidato.
- Contas (`CONTA`) permanecem no nível do `IMOVEL`, não da `UNIDADE` — são custos do imóvel inteiro (IPTU, condomínio), independente de subdivisão.

## 4. Regras de negócio (motor de estados)

| Evento | Ação automática |
|---|---|
| Imóvel cadastrado | Cria `UNIDADE` padrão (`padrao = true`, `status = vago`); `IMOVEL.visibilidade = privado` |
| Convite gerado | Cria token único (expira em 7 dias) para onboarding de acesso ou para uma `UNIDADE` com condições de locação (valor, tipo, garantia aceita) |
| Inquilino envia documentos | Cria/atualiza `INQUILINO` por CPF (reaproveita se já existir) |
| Usuário aceita convite | Confirma e-mail, cria senha e ativa a conta de acesso |
| Proprietário aprova candidato | Valida que não há `CONTRATO` assinado com período sobreposto na mesma `UNIDADE`; gera `CONTRATO` (`status_assinatura = pendente`) a partir de template com cláusulas condicionais por tipo de garantia e tipo de locação; `UNIDADE.status → reservado` |
| Ambas as partes assinam | `CONTRATO.status_assinatura = assinado`; `UNIDADE.status → alugado`; gera `PAGAMENTO` (um por mês do prazo); agenda alerta de vencimento da `GARANTIA` (30 dias antes) |
| Pagamento não confirmado até vencimento + 1 dia | `PAGAMENTO.status = atrasado`; notifica ambas as partes |
| Conta a 5 dias do vencimento | Notifica proprietário |
| Chamado criado | `status = aberto`; notifica proprietário |
| Chamado atualizado pelo proprietário | `status = em_andamento` → `resolvido` (com timestamp) |
| Contrato a 60 dias do fim | Notifica proprietário (renovação/reajuste) |
| Contrato encerrado sem renovação | `UNIDADE.status → vago` |

## 5. API (REST)

Papéis: `[P]` proprietário, `[I]` inquilino, `[Ambos]` qualquer autenticado dono do recurso.

```
POST   /auth/login
POST   /auth/convites/proprietarios
GET    /auth/convites/{token}
POST   /auth/convites/{token}/aceitar
GET    /auth/me

GET    /imoveis                          [P]
POST   /imoveis                          [P]
GET    /imoveis/{id}                     [P]
PATCH  /imoveis/{id}                     [P]
POST   /imoveis/{id}/documentos          [P]

POST   /imoveis/{id}/convites            [P]
GET    /convites/{token}                 público

POST   /convites/{token}/cadastro        [I]
POST   /convites/{token}/documentos      [I]
POST   /convites/{token}/garantia        [I]
POST   /candidaturas/{id}/aprovar        [P]
POST   /candidaturas/{id}/recusar        [P]

GET    /contratos/{id}                   [Ambos]
POST   /contratos/{id}/assinar           [Ambos]
GET    /contratos/{id}/status-assinatura [Ambos]

GET    /contratos/{id}/pagamentos        [Ambos]
POST   /pagamentos/{id}/confirmar        [P]

GET    /imoveis/{id}/contas              [P]
POST   /imoveis/{id}/contas              [P]
PATCH  /contas/{id}                      [P]

POST   /imoveis/{id}/chamados            [I]
GET    /imoveis/{id}/chamados            [Ambos]
PATCH  /chamados/{id}                    [P]
```

Use GET/POST/PATCH convencionais (não usar o método HTTP QUERY — padronizado em jun/2026, ainda sem suporte maduro em ferramentas de desenvolvimento).

**Nota sobre `UNIDADE`:** os endpoints acima (`/imoveis/{id}/convites`, aprovação, contrato) operam sobre a `UNIDADE` padrão do imóvel nos bastidores — a API pode manter os caminhos como estão (referenciando `imovel_id`) e resolver internamente para a unidade `padrao = true` daquele imóvel, sem expor o conceito na superfície da API do MVP. Se subdivisão em múltiplas unidades virar requisito, adicionar `GET/POST /imoveis/{id}/unidades` e trocar as referências para `unidade_id` explícito nos endpoints de convite/contrato.

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

## 7. Estrutura de telas (Expo Router)

```
app/
├── (auth)/
│   ├── login.tsx
│   └── convite/[token].tsx
├── (owner)/
│   ├── imoveis/
│   │   ├── index.tsx           # dashboard: cards com status (pagamento/contas/chamados)
│   │   ├── novo.tsx            # cadastro de imóvel (multi-step)
│   │   └── [id]/
│   │       ├── index.tsx       # detalhe do imóvel
│   │       ├── convidar.tsx
│   │       ├── aprovacao.tsx
│   │       ├── chamados.tsx
│   │       └── pagamentos.tsx
│   └── perfil.tsx
├── (tenant)/
│   ├── cadastro.tsx
│   ├── aguardando.tsx
│   ├── imoveis/
│   │   ├── index.tsx           # lista de contratos ativos do inquilino
│   │   └── [id]/
│   │       ├── index.tsx
│   │       └── chamados.tsx
│   └── perfil.tsx
└── (contrato)/
    └── [id]/
        ├── revisar.tsx
        └── assinar.tsx
```

**Ordem de construção (MVP crítico, telas 1-9):** aceite de convite + criação de senha → login → dashboard do proprietário → cadastro de imóvel → gerar convite → cadastro do inquilino com senha → confirmação de garantia → aprovação → revisar contrato → assinar contrato. **Telas 10-13** (detalhe do imóvel, home do inquilino, abrir chamado, lista de chamados) fecham o ciclo de gestão.

## 8. Restrições legais a respeitar na implementação

- **LGPD:** isolamento de dados entre proprietários (nunca expor dados de um inquilino/contrato a outro proprietário sem consentimento explícito); base legal padrão = execução de contrato; política de retenção configurável (default sugerido: 5 anos após fim do contrato).
- **Garantia:** um contrato só pode ter um tipo de garantia (nunca combinar).
- **Assinatura eletrônica:** usar nível avançado (ex.: Gov.br ou provedor tipo Clicksign/D4Sign), não exigir ICP-Brasil. Incluir campo para 2 testemunhas no template do contrato (dá força de título executivo extrajudicial).
- **Custódia de valores:** a plataforma nunca retém dinheiro diretamente (nem aluguel, nem caução). Caução em dinheiro é instruída para conta poupança conjunta entre locador e locatário. Pagamentos são confirmados manualmente no MVP ou via integração com parceiro já licenciado — nunca uma conta própria da plataforma.
- **Modelo de cobrança:** assinatura SaaS fixa por imóvel (não comissão sobre aluguel) — mantém a operação fora do escopo de corretagem/CRECI no MVP.
- **Locação por temporada:** se implementada, cadastro do imóvel deve exigir autorização documentada do condomínio antes de permitir publicação (decisão do STJ de mai/2026 sobre destinação residencial em condomínios).

## 9. Escopo do MVP — prioridades

**Essencial (constrói o esqueleto funcional):** convite de onboarding do proprietário; login por e-mail/senha; cadastro de imóvel; convite e cadastro de inquilino com senha; geração de contrato com cláusulas condicionais; assinatura eletrônica; dashboard de status por imóvel; chamados de manutenção.

**Importante, não bloqueante:** controle de pagamentos com alerta de atraso; alertas de vencimento (garantia, reajuste, contas).

**Adiar para fases futuras:** integração bancária automática; análise de crédito automática para seguro-fiança; publicação pública de imóvel, candidaturas e ranking (marketplace).
