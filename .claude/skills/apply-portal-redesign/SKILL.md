---
name: apply-portal-redesign
description: Apply the mobile-first portal redesign (design tokens, Button/Card/StatusBadge/Pill, and all 9 screens) from the design handoff package to the Expo/React Native frontend. Use when the user asks to "apply the redesign", "update the UI to match the new design", or references design_handoff_portal_redesign.
---

# Apply Portal Redesign

Restyles the frontend to the mobile-first design described in `design_handoff_portal_redesign/` — a local, gitignored working folder (won't exist on a fresh clone; this skill is the versioned record of what it asked for). If the folder is present, read `Portal Mobile.dc.html` first — it is the source of truth for layout, colors, spacing and copy (open it in a browser; it's a clickable prototype with a dev switcher bar for all 9 screens). `Portal.dc.html` in the same folder is a superseded desktop exploration — token/color reference only, do not port its layout. Prefer the newest `README*.md` in that folder (check file dates — handoffs get dropped in as new numbered copies) over this skill's summary below if they disagree.

## Already done (verify, don't redo)
`frontend/src/design/tokens.ts`, `Button.tsx`, `Card.tsx`, `StatusBadge.tsx`, `Pill.tsx` and the screens `app/(auth)/login.tsx`, `app/(owner)/imoveis/index.tsx`, `app/(owner)/imoveis/novo.tsx`, `app/(contrato)/[id]/revisar.tsx` already match the new tokens (`#F5F6F8` / `#E5E7EB` / `#2563EB` / `radius:10`). Confirm they still match before moving on — if `tokens.ts` has drifted, fix it first since every other screen depends on it.

## Component conventions established by this redesign
- `TextInput` style: `className="bg-card px-4 py-3 text-primary rounded-xl"` + `style={{ borderWidth: 1.5, borderColor: '#E5E7EB', fontSize: 14 }}` + `placeholderTextColor="#9CA3AF"` — replaces the old `border-2 border-border rounded px-3 py-3 bg-card text-primary`.
- `Button` has a `dark` variant (`background:'#111827'`) for CTAs that must read as distinct from both the accent-blue primary and outline pills (e.g. a "Buscar" action next to accent-colored filter pills) — don't reuse `primary` for that case.
- `Pill` (`src/design/Pill.tsx`) is the compact toggle for filters/single-choice options (status, tipo, garantia, city): active = accent fill + white text, inactive = `#E5E7EB` border, `radius:8`. Distinct from `Button`'s pill-shaped-but-larger primary/outline — don't reach for `Button` when the mock shows the smaller filter-pill treatment.
- No native `<select>` in RN/Expo Web and no picker dependency in this project — a dropdown in the HTML mock (e.g. city filter) becomes a `Pill` list built from real fetched data (e.g. distinct cities already in the owner's imóveis), not a hardcoded list and not a new dependency.

## Steps for a new/updated handoff
1. **Read the newest README in `design_handoff_portal_redesign/`** for the full screen-by-screen spec (colors, copy, component notes, mobile-layout rules) and note which screens it says are already done vs still pending — verify that claim against the actual files before trusting it.
2. **Restyle the screens it flags as pending** to the tokens/components above, aligning spacing/copy to the mock.
3. **Preserve existing `testID`s** on any screen touched by `frontend/e2e/golden-path.spec.ts` (currently: `input-register-*`, `btn-register-owner`, `input-email`, `input-password`, `btn-login`, `btn-novo-imovel`, `input-endereco`, `input-cidade`, `btn-salvar-imovel`, `btn-assinar`) — a restyle must not change these strings.
4. **Don't change business logic** while restyling — screens like `locacao/[token].tsx` drive real conditional UI state (`precisaCadastro`, `precisaGarantia`, pending-approval, `contratoPendenteAssinatura`) that a mock may represent as a simple stage toggle for demo purposes; map to the real state, don't replace it.
5. **Verify**: run the app (`run` skill) and compare each restyled screen against the HTML mock; run the golden-path E2E to confirm nothing broke for the owner flow it covers.
