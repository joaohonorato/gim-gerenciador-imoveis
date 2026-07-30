# Handoff: Property Portal Redesign

## Overview
Redesign of the property-management app's owner flows (login, imóveis list, novo imóvel, revisar contrato) plus a new tenant invite/sign screen (convite). Moves the visual language from the current app's neo-brutalist thick-black-border style to a softer, data-friendly SaaS look aimed at busy landlords who need practical, at-a-glance information.

## About the design file
`Portal.dc.html` in this folder is a **design reference built in HTML** — a clickable prototype showing intended look and behavior, not production code. Do not copy its markup/CSS into the app. The task is to **recreate this design inside the existing Expo/React Native codebase**, using its existing patterns: NativeWind (`className`), `expo-router`, and the components in `src/design/`.

## Fidelity
**High-fidelity.** Colors, spacing, type scale, and copy below are final — implement pixel-close within RN's layout system.

## Important: web layout → mobile app
The prototype was built as a **desktop web portal** (persistent left sidebar, two-column login split-screen) per the direction given. The target app is **React Native / Expo**, i.e. phone-width screens. Translate the desktop layout choices to mobile idiom rather than porting them literally:
- **Sidebar nav (imóveis / contratos)** → do not build a persistent sidebar on phone widths. Use the existing stack/tab navigation (`app/(owner)/_layout.tsx`); if a persistent nav is wanted, use a bottom tab bar instead. Carry over the sidebar's *color* (`#111827` dark) and *active-state treatment* (small accent dot + highlighted row) to whatever nav pattern you keep, e.g. a top app bar or tab bar.
- **Login split-screen** (dark brand panel left / form right) → stack vertically on phone: dark brand panel as a top hero panel (~35% of height) with the same copy, form below it, single column.
- **Convite screen** (centered card, max-width 520px) → this maps directly to mobile as a full-width screen with the same content order and spacing scaled to phone padding (24px sides instead of 40px).
- Table layout on the Imóveis screen (grid columns) → **replace with the existing `Card`-per-item list pattern** already in `imoveis/index.tsx`, just restyled per the tokens below (this is closer to how the data reads on a narrow screen anyway).

## Screens / Views

### 1. Login (`app/(auth)/login.tsx`)
- Two-stage flow unchanged functionally: email/nome/cpf → magic-link sent → code entry.
- New look: hero panel background `#111827` with a 40×40 accent-colored rounded square mark, heading "Gestão de Imóveis" (34px/800 in the desktop mock; use ~28px/800 on mobile), subtext in `rgba(255,255,255,0.6)`.
- Form fields: 1.5px border `#E5E7EB`, radius 10px, padding ~13px vertical / 16px horizontal, 15px text.
- Primary button: solid accent `#2563EB`, white text, 700 weight, radius 10px, full width, 13–14px vertical padding.
- Outline button ("Voltar"): white bg, 1.5px `#E5E7EB` border, `#374151` text.
- Error text: `#DC2626`, 13px.

### 2. Imóveis list (`app/(owner)/imoveis/index.tsx`)
- Header: "Meus imóveis" (24px/800, `#111827`) + subtitle "Acompanhe status e dados de cada imóvel cadastrado." (14px, `#6B7280`), with a primary "+ Novo imóvel" button (`whiteSpace: nowrap` equivalent — don't let it wrap to two lines).
- **New**: 3 stat tiles above the list — Total cadastrados / Alugados / Vagos — white cards, 1px `#E5E7EB` border, 12px radius, 20px padding, big number 28px/800 (colors: total `#111827`, alugados `#2563EB`, vagos `#16A34A`), label 13px `#6B7280`. Compute these client-side from the fetched list — no new API needed.
- List rows: keep using `Card`, but restyle border to 1px `#E5E7EB` (not 2px black), radius 12px. Show endereço (bold), cidade, matrícula, visibilidade label (Público/Privado), and a status dot + label (color-coded, see tokens below) — replaces the current `StatusBadge` shape system (circle/square/triangle) with a single colored dot for all statuses, label text colored to match.

### 3. Novo Imóvel (`app/(owner)/imoveis/novo.tsx`)
- Header row: outline "← Voltar" button + title "Novo imóvel" (24px/800).
- Form card: white, 1px `#E5E7EB` border, 12px radius, 32px padding. Fields get a small label above each input (13px/600, `#374151`) instead of relying on placeholder-only.
- Visibilidade: replace with a two-option pill toggle (Público / Privado) — selected pill filled with accent color, unselected has `#E5E7EB` border on white.
- Same primary/outline button styles as login.

### 4. Revisar Contrato (`app/(contrato)/[id]/revisar.tsx`)
- Header row: outline "← Voltar" + title "Revisar contrato".
- Two cards side by side on wide screens (stack vertically on phone): "Detalhes" (tipo, aluguel, período, status with colored dot) and "Assinaturas" (proprietário/inquilino rows with Pendente=`#D97706`/Assinado=`#16A34A` text).
- Sign button only shows if `!assinouProprietario`; full-width primary button.
- Success state: light green banner (`#F0FDF4` bg, `#16A34A` text) "Contrato totalmente assinado!" when both signed.

### 5. Convite / Inquilino sign (new screen — no current route; suggest `app/(contrato)/convite/[token].tsx`)
- Public, token-based (no auth) — the tenant opens a link and lands here.
- Centered card: small accent dot + eyebrow label "CONVITE DE LOCAÇÃO" (uppercase, 13px/600, `#6B7280`), heading "Revise e assine seu contrato" (24px/800).
- Detail rows in a bordered box: Tipo de contrato, Aluguel mensal, Período, Garantia — label left (`#6B7280`, 14px) / value right (bold, `#111827`, 14px), each row separated by a 1px `#F0F1F3` divider.
- Checkbox + label: "Li e concordo com os termos do contrato de locação descritos acima." (13px, `#6B7280`).
- Primary button "Assinar como inquilino" disabled until checkbox is checked; loading state "Assinando...".
- Success state (after signing): centered checkmark in a 56px `#DCFCE7` circle, heading "Assinatura registrada!" (20px/800), body copy explaining the owner will countersign and the tenant will get a copy by email.
- Needs new API support: a public `GET /convites/:token` and `POST /convites/:token/assinar` (mirrors the existing `Convite` type in `src/api/types.ts`, which already has the right fields).

## Interactions & Behavior
- All loading states: disable the button, swap label to a "...ing" variant (e.g. "Assinando...", "Salvando...", "Enviando..."), matching existing `Button` component's `loading` prop pattern — keep using `ActivityIndicator` there rather than text-swap if you prefer consistency with the existing `Button.tsx`.
- Form validation: inline error text below fields / above the submit button, `#DC2626`, unchanged from current behavior (client checks for empty required fields before hitting the API).
- No new animations — keep transitions instant/native (RN default), consistent with the current app.

## State Management
No new state shape beyond what's already in each screen file. For Convite, add local state: `token` (route param), `contrato` (fetched), `checked` (boolean), `loading`, `accepted`.

## Design Tokens
Update `src/design/tokens.ts`:
```ts
export const colors = {
  primary: '#111827',
  surface: '#F5F6F8',   // was #F9FAFB
  card: '#FFFFFF',
  border: '#E5E7EB',    // was #111827 (thick black) — now a light hairline
  accent: '#2563EB',
  success: '#16A34A',
  warning: '#D97706',
  danger: '#DC2626',
  muted: '#6B7280',
} as const;

export const radius = 10; // was 4
```
Spacing scale (`xs/sm/md/lg/xl`) is unchanged.

Typography: keep system font stack already in use; scale used in the mock — 24px/800 (screen titles), 15px/700 (section titles), 14px (body), 13px (labels/meta), 12px uppercase/700 (table/section eyebrows).

Component-level changes needed:
- `Button.tsx`: border width 2px → 1.5px, color `border-border` → `#E5E7EB` for outline variant (primary stays solid accent, no border), `rounded` (4px) → `rounded-xl` (~10-12px).
- `Card.tsx`: same border/radius change as Button.
- `StatusBadge.tsx`: simplify — drop the circle/square/triangle shape system, always render a small colored dot (6-8px circle) + label, colored per status (see mapping already in the component; the shapes were the main departure, not the colors).

## Assets
No new images/icons. All marks in the mock are plain colored rounded squares/circles (brand mark, avatar initials) — build these as plain `View`s, not new asset files.

## Files
- `Portal.dc.html` — full interactive HTML reference (open in a browser; has a small dev nav bar at the top to jump between the 5 screens).
