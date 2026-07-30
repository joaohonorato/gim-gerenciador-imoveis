# Handoff: Landing Page — Geim (gestão de imóveis)

## Overview
Landing page de marketing/portfólio para o Geim, sistema de gerenciamento de imóveis. Nome é um trocadilho com "GErenciamento de IMóveis" e com o gênio da lâmpada (Aladim) — a copy usa a metáfora dos "três desejos" ao longo da página (imóveis = lâmpada, proprietário = Aladim, "esfregar a lâmpada" = criar conta).

## About the Design Files
Os arquivos deste pacote (`index.html` + `geim-landing.css`) são **referências de design em HTML/CSS puro** — protótipos de alta fidelidade mostrando a aparência e estrutura pretendidas, não código de produção para copiar diretamente. A tarefa é **recriar este design no ambiente/stack já existente do projeto** (o repositório do produto é React/Expo — ver `frontend/` no repo `gim-gerenciador-imoveis`), usando os componentes e padrões já estabelecidos lá. Se a landing for um site separado sem stack definida, escolher o framework mais adequado (ex.: Next.js estático) e implementar lá.

Não há JavaScript no pacote — nenhuma interatividade está implementada; a página é estática (nav com âncoras `#`, sem menu mobile funcional, sem animações). Qualquer comportamento (menu mobile, scroll suave, validação de formulário caso adicionem captura de e-mail) deve ser implementado pelo desenvolvedor.

## Fidelity
**Alta fidelidade (hifi)**: cores, tipografia, espaçamento e layout são finais. Recrie pixel-perfect usando os tokens abaixo.

## Screens / Views

### Landing Page (single page, seções âncora)
Largura de conteúdo: `max-width: 1080px`, centralizado, `padding: 0 32px` lateral.

**1. Nav** (topo, sticky não implementado — considerar adicionar)
- Flex row, `justify-content: space-between`, `align-items: center`, `padding: 20px 0`
- Logo "GEIM": peso 500, 19px, letter-spacing .02em
- Links: "Funcionalidades" (`#features`), "Como funciona" (`#how`), botão primário "Criar conta grátis" (`#cta`)
- Gap entre itens do menu: 26px, font-size 14px

**2. Hero** (padding `80px 32px 56px`)
- Grid 2 colunas: `1.15fr .85fr`, gap 40px, `align-items: center`
- Coluna esquerda:
  - Tag "Gestão de imóveis, no estilo dos três desejos" (`.tag.tag-accent`)
  - H1: "Seu imóvel merece um gênio. Não uma planilha." — peso 500, 50px, line-height 1.12, margin 20px 0
  - Parágrafo: "O Geim organiza imóveis, contratos e pagamentos de aluguel num só lugar — você só precisa esfregar a lâmpada." — 17px, cor `--color-neutral-400`, max-width 440px
  - Dois botões lado a lado (gap 14px, margin-top 28px): "Criar conta grátis" (`.btn.btn-primary`) e "Ver como funciona" (`.btn.btn-ghost`)
- Coluna direita: ilustração do Geim saindo da lâmpada, `aspect-ratio: 692/512`, `border-radius: var(--radius-lg)`, fundo branco (a imagem já tem fundo branco integrado — ver seção Assets), `object-fit: contain`

**3. Funcionalidades — "Três desejos"** (id `#features`, padding `56px 32px`)
- Tag outline "Três desejos, sempre concedidos"
- H2: "Peça. O Geim cuida do resto." — peso 500, 30px
- Grid 3 colunas iguais, gap 20px — cada item é um `.card.elev-sm` com: ícone (Phosphor, 22×22, cor accent-300), kicker ("Desejo nº1/2/3"), título (`.card-title`), corpo (`.card-body`)
  - Desejo 1 — "Organizar seus imóveis": ícone casa. "Cadastre casas e apartamentos, acompanhe ocupação e histórico — tudo visível de uma vez."
  - Desejo 2 — "Contratos sem cartório": ícone documento. "Envie convites, colete assinaturas e guarde tudo digitalmente. Nada de papelada empoeirada."
  - Desejo 3 — "Pagamentos automáticos": ícone cartão/pagamento. "O Geim gera as cobranças do aluguel todo mês e avisa quando alguém está atrasado."

**4. Como funciona** (id `#how`) — única seção com fundo diferenciado
- Background: gradiente diagonal 135deg entre `--color-accent-900` e `--color-accent-700` (esta é a "stat band"/seção saturada que o Nocturne reserva para um único destaque de página, conforme a design guide)
- Padding `64px 32px`, margin-top 24px
- Tag "Como funciona" sobre fundo translúcido branco (`rgba(255,255,255,.12)`)
- H2 branco: "Três passos até a mágica"
- Grid 3 colunas, gap 24px, texto em `rgba(255,255,255,.9)`, títulos brancos:
  1. "Esfregue a lâmpada" — "Convide seu inquilino em poucos cliques."
  2. "Assine o contrato" — "Proprietário e inquilino assinam direto pela plataforma."
  3. "Deixe a mágica acontecer" — "Cobranças automáticas, todo mês, sem você precisar pedir."
- Citação em itálico abaixo (margin-top 40px, `rgba(255,255,255,.75)`): "Você é o Aladim da sua carteira de imóveis. O Geim cuida do resto."

**5. CTA final** (id `#cta`, padding `80px 32px`, alinhado à esquerda)
- H2: "Seu próximo desejo é só clicar aqui." — peso 500, 36px, max-width 520px
- Parágrafo: "Grátis para começar. Sem cartão de crédito." — cor `--color-neutral-400`
- Botão primário "Criar conta grátis"

**6. Footer**
- Flex row `space-between`, padding `28px 32px`, font-size 13px, cor `--color-neutral-500`
- Esquerda: "Geim — o gênio da gestão de imóveis" · Direita: "© 2026"

## Interactions & Behavior
- Nenhuma implementada no protótipo (estático). Recomenda-se ao desenvolvedor:
  - Scroll suave para os links âncora (`#features`, `#how`, `#cta`)
  - Estado `:hover` / `:focus-visible` nos botões e links já vem definido pelo stylesheet do design system (`geim-landing.css` — ver classes `.btn`, `a:hover`)
  - Menu responsivo (a landing não tem breakpoint mobile definido — grid do hero e dos cards precisa colapsar para 1 coluna abaixo de ~768px)
  - Link de "Criar conta grátis" deve apontar para o fluxo de cadastro real do app

## State Management
Página estática, sem estado de UI. Se adicionarem captura de e-mail/waitlist no CTA, será necessário estado de formulário (input + submit + validação).

## Design Tokens
Todos os tokens vêm de `geim-landing.css` (design system "Nocturne"), usados via `var(--...)`. Principais:
- `--color-bg`: #161826 (fundo geral, escuro)
- `--color-text`: #e9e9ed
- `--color-accent`: #9184d9 (blurple) — usado em `--color-accent-300` (links/ícones), `--color-accent-700`/`--color-accent-900` (fundo da seção "Como funciona")
- `--color-neutral-400`/`500`: cinzas para texto secundário
- Tipografia: Inter (`--font-heading` / `--font-body`), peso máx. 500 (nunca bold)
- `--radius-lg`: raio usado na imagem do hero (8px de base, escala 0.7×)
- Ícones: Phosphor, sem preenchimento (stroke), stroke-width 1.6–1.5
- Botões: `.btn-primary` é outline (borda accent, sem fill — nunca preencher), `.btn-ghost` é transparente

Não inventar cores/fontes fora desses tokens — ver `geim-landing.css` para a escala completa (ramps 100–900).

## Assets
- `assets/geim-hero.png` — ilustração do Geim (gênio) saindo da lâmpada, usada no hero. Fundo branco sólido incluído na própria imagem (fundo não é transparente — foi decisão do usuário manter assim). Proporção original 692×512.
- Ícones são inline SVG (Phosphor-style, outline) já embutidos no `index.html` — não são arquivos separados.

## Files
- `index.html` — estrutura completa da landing (HTML puro, sem JS, sem web components)
- `geim-landing.css` — stylesheet do design system Nocturne (tokens + componentes) usado pela página
- `assets/geim-hero.png` — ilustração do hero
