# Home Assistant Integration — Proposal (Randall Finances)

Status: **DRAFT — for review/tweak before build**
Author: boba · Date: 2026-09-03

## Goal

Expose Randall Finances to your Home Assistant instance (`home.randalls.cc`) so you can:
watch budgets & spend on HA dashboards, get **daily + monthly spend limiters**, see recent
transactions, and chart balances/spending/income over time — without demo data and without
the phone needing to be present.

---

## 1. Recommended architecture (the "easiest" real path)

HA runs Python `custom_components`. It **cannot** call the finance MCP server directly
(that's stdio + a computation engine on `hermes-pc`). The clean, real pattern is a thin
read-only **Finance Bridge** that HA can reach over HTTP + a **HACS custom component** that
defines real HA entities.

```
Android app ──┐
              ├──► Firestore (jokarz-finance) ──► Finance Bridge (HTTP, LAN, token)
MCP engine ───┘                                      │
                                                     ▼
                              HACS custom_component (randall_finances)
                                                     │
                                         sensor / binary_sensor / number / history
                                                     ▼
                                        HA dashboards + automations
```

- **Data source = Firestore REST** (not local JSON on hermes-pc). It's the canonical
  live store the app already writes to, so HA always sees current data with no sync job
  and no phone dependency. (We already learned admin-SDK writes hang — this reads via REST,
  avoiding that path.)
- **Finance Bridge** = small HTTP service (reuses the existing aggregation logic from
  `hermes-mcp-server`: `get_financial_summary`, `list_budgets`, `get_spending_trends`,
  net-worth, `list_transactions`). Read-only by default, bearer-token auth, binds `0.0.0.0`
  on the LAN like the existing bridge, runs as its own systemd service on `hermes-pc`.
- **HACS integration** = Python `custom_components/randall_finances`, installable as a
  custom repository (no Python backend logic in HA — it only polls the bridge and updates
  entities). Sensors keep history automatically, so native HA **Chart / History / Statistic
  cards just work**.

---

## 2. What you get in Home Assistant (entity catalog)

### Budget tracking — overall + per category
| Entity | Type | Purpose |
|---|---|---|
| `sensor.randall_month_spend` / `_month_income` / `_savings_rate` | sensor | overall month-to-date |
| `binary_sensor.randall_over_budget` | binary_sensor | overall over-budget alarm |
| `sensor.randall_budget_<category>_spent` | sensor (auto-created per active budget) | live per-category spend |
| `sensor.randall_budget_<category>_limit` | sensor | that category's monthly limit |
| `sensor.randall_budget_<category>_pct` | sensor | percent of budget used |
| `binary_sensor.randall_<category>_over_budget` | binary_sensor | per-category trip |

### Daily / monthly spend limiters
| Entity | Type | Behavior |
|---|---|---|
| `sensor.randall_daily_spend` / `_monthly_spend` | sensor | accrued today / this month |
| `binary_sensor.randall_daily_over_limit` | binary_sensor | ON when today's spend > allowance → drives an HA automation/notification |
| `binary_sensor.randall_monthly_over_limit` | binary_sensor | same, monthly |
| `number.randall_daily_limit` / `number.randall_monthly_limit` | number | set the cap **from HA**; writes back so the app honors the same number (bidirectional) |

### Recent transactions (last 3 / 5)
- `sensor.randall_recent_transactions` — a clean template/text entity (one line per txn:
  date · merchant · amount · category) rendered via a Markdown card; merchant/amount/category
  also exposed as attributes so you can build custom cards.
- Configurable count (default 5) from the config flow.

### Account value + time series (charts)
- `sensor.randall_net_worth` (total) and `sensor.<account>_balance` per account → native
  **Chart card** shows value-over-time automatically from entity history.
- Per-category spend, `month_spend`/`month_income` → native history/chart cards.
- (Optional, phase 4) `sensor.randall_daily_spend` recorded daily → bar chart of
  spend-over-time.

Everything above also feeds **HA automations** (e.g. "when `daily_over_limit` turns on →
notify phone / flash a light"), which is where the limiters really earn their keep.

---

## 3. Android Settings page — "Home Assistant" section

Serves as the **onboarding + management surface** for whichever client path you pick:

- **Link / status**: shows the bridge URL + connection state ("Connected · N sensors live").
- **HACS install helper**: shows the HACS custom-repo URL + "Add to HACS" badge + step list.
- **Manual sensor setup fallback**: if you ever run HA without HACS, the page lists every
  sensor the integration would register with its `entity_id`, unit, and description so you
  can create matching helpers manually.
- **What to publish**: toggles to include/exclude specific accounts, categories, and
  transactions. Stored to Firestore; the bridge honors it, so app and HA stay consistent.
- **Daily/monthly limits**: read current values and edit them (same store the HA `number`
  entities write to).

---

## 4. HACS distribution

- New GitHub repo under `Flexingg` (e.g. `hass-randall-finances`) with `hacs.json` +
  `manifest.json` (domain `randall_finances`, HA version floor, single config flow, icons,
  optional translations).
- Install: **HACS → ⋮ → Custom repositories → add repo URL → install** → add integration →
  enter Bridge URL + token. ~2 minutes, repeatable per HA instance.
- Config flow validates the bridge before saving (no fake success).

---

## 5. Build phases (each independently verifiable)

1. **Firestore REST read layer + Finance Bridge** (systemd service on `hermes-pc`, LAN,
   bearer token, read-only). Verify with `curl` against live Firestore — real numbers only.
2. **HACS custom component**: config flow, all sensors/binary_sensors/numbers above,
   history accumulation, polling (default 60s). Install on `home.randalls.cc`; confirm
   entities populate from live data.
3. **Android Settings → Home Assistant section**: link, publish toggles, limits, HACS/manual
   helper.
4. **Example dashboard + automations**: a Lovelace YAML/package exposing budget cards,
   charts, and limiter automations you can drop in (charts use native HA cards — zero extra
   dependencies).

---

## 6. Open questions for you (tweak points)

1. **HACS present?** Is HACS already installed on `home.randalls.cc`? If not, one-time HACS
   install is part of this.
2. **Data source** — I recommend **Firestore REST** as the live source (realtime, no phone
   dependency). OK, or prefer the engine's local JSON on `hermes-pc`?
3. **Daily limiter semantics** — reuse the app's dynamic **Daily Allowance Engine** (monthly
   income − recurring − rollover ÷ days left), or a simpler fixed daily cap you set? Recommend
   the engine, with the fixed cap as an override.
4. **Bidirectional limits** — confirm the HA `number` limit entities should write the cap back
   (so app + HA agree). Recommend yes.
5. **Charts** — native HA Chart/Statistic/History cards (recommended, zero deps) vs shipping
   an ApexCharts example package too?
6. **Scope for v1** — everything above, or start with Phase 1+2 (dashboard + limiters) and add
   the Android Settings page (Phase 3) after?
