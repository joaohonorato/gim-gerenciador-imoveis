---
name: expo-screen
description: Add or restyle a screen in the Gestão de Imóveis Expo Router frontend (owner, tenant, auth, or contrato flows). Use whenever a task touches frontend/app or frontend/src/design, before creating a screen file, to follow the existing routing, API-client, and design-system conventions instead of improvising new ones.
---

# Adding/changing a frontend screen (Expo Router)

Read `frontend/AGENTS.md` first if you haven't recently — Expo 57 changed enough from prior versions that older-Expo assumptions will be wrong; it points to the versioned docs to check instead.

## Where the file goes

Route groups under `frontend/app/`:
- `(auth)/` — login, register, invite acceptance (`convite/[token].tsx`, `convite/manual.tsx`) — unauthenticated.
- `(owner)/` — proprietário-only screens (imóveis list/create, convites, per-imóvel actions). Layout at `(owner)/_layout.tsx`.
- `(tenant)/` — inquilino-only screens: `tenant/index.tsx` (home: contratos, convites, owner/imóvel info, invite-by-token acceptance, logout), `tenant/pagamentos.tsx`, `tenant/chamados.tsx`, `tenant/perfil.tsx`. The full tenant journey (per `docs/jornadas-e-backlog-tecnico.md` rank 1) is built — treat this group as an established pattern to follow, not a gap to fill.
- `(contrato)/` — contract review/signing, both owner (`[id]/revisar.tsx`) and token-based tenant signing (`locacao/[token].tsx`).

Match the existing route group instead of inventing a new one unless the screen genuinely doesn't belong to owner/tenant/auth/contrato.

## API calls

Use `apiFetch<T>` from `src/api/client.ts`, not raw `fetch`. It auto-attaches the bearer token from `src/api/session.ts` unless you pass `{ auth: false }` (needed for public/token-based screens like convite acceptance or `(contrato)/locacao/[token].tsx`). It throws `ApiException` (has `.status` and a typed `ApiError`) on non-2xx. In the catch block, use `getErrorMessage(e, fallback)` (also from `src/api/client.ts`) to get an action-oriented message — it maps `ApiException`'s error code to the catalog in `docs/catalogo-erros-api.md`, handles `ApiNetworkException` (timeout/offline), and falls back to your `fallback` string otherwise. Never show `e.message`/`error.error.message` raw — that's the backend's internal exception text, not a user-facing message. Add new response shapes to `src/api/types.ts` rather than inlining `any`.

`EXPO_PUBLIC_API_URL` overrides the backend base URL (default `http://localhost:8080`) — don't hardcode the host.

## Design system

Use the components in `src/design/` (`Button`, `Card`, `StatusBadge`) and tokens in `src/design/tokens.ts` — don't hand-roll styling that duplicates them. Current baseline is a Bauhaus aesthetic (see `docs/especificacao-produto.md` §6): flat surfaces, no shadows/gradients, solid black borders, 8pt spacing grid, sharp corners. Status must be readable by **color and shape together** (circle/square/triangle in `StatusBadge`), not color alone — colorblind-safe by design, so don't collapse a new status to a color-only dot.

There may be a local, unversioned design handoff at `design_handoff_portal_redesign/` (gitignored — won't exist on a fresh clone) proposing a softer, lighter-border visual direction that supersedes the tokens above. If it's present, treat it as the current intent for any screen it covers and flag the discrepancy with CLAUDE.md/the spec rather than silently picking one.

## Mobile-first, even for owner/desktop-shaped ideas

This is a phone-width React Native app (Expo Web included) — if a design reference or mock was built desktop-first, translate rather than port literally:
- Persistent sidebar → tab bar or top app bar, not a fixed side column.
- Multi-column split screens → stack vertically, hero/brand block on top.
- Data tables → the existing `Card`-per-item list pattern (see `(owner)/imoveis/index.tsx`), not a grid with columns.

## Loading/validation conventions already in use

- Button loading state: disable + `ActivityIndicator` via `Button`'s `loading` prop, not manual text-swapping, for consistency with existing screens.
- Client-side validation: check required fields before hitting the API, show inline error text near the field/submit button; don't rely on the API round-trip as the only validation signal.

## Testing

If the change affects the golden path (owner or tenant), re-run `frontend/e2e/golden-path.spec.ts` — see the `run` skill for how to launch it (`./run.ps1 -Target e2e`).
