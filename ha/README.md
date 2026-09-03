# Home Assistant — Discretionary Spending Dashboard (ApexCharts)

The Android app (v1.2.2+) maintains a monthly **discretionary-spending setpoint**
and pushes its current state to Firestore at:

```
users/{uid}/discretionary/state
```

Document shape:

```json
{
  "setpoint": 500.0,
  "necessaryCategories": ["Rent", "Utilities", "Groceries"],
  "month": "2026-09",
  "monthlySpend": 310.42,
  "remaining": 189.58,
  "spentByCategory": { "Dining": 120.0, "Shopping": 190.42 },
  "updatedEpochSeconds": 1767390000
}
```

This folder has two ready-to-use Home Assistant artifacts:

- `rest-sensors.yaml` — HA **REST sensors** (or a command_line/one push via an
  automation) that fetch that document as JSON so ApexCharts can chart it.
- `lovelace-discretionary.yaml` — a **Lovelace view** using the HACS
  `apexcharts-card` you already have installed: a "remaining vs setpoint" stat,
  this-month spend by category, and a recent-months trend series.

## Wiring it to your data path

The ApexCharts card draws HA **entities** (sensors) — it does not read Firestore
directly. You need ONE bridge that turns the Firestore doc into HA sensors:

1. **Recommended (no extra add-on):** your finance backend (the same server that
   serves the randall-finances tools) exposes the doc as JSON at an HTTP URL, or
   you add a tiny HA REST sensor that calls a Firebase REST read with your key.
2. Point the REST sensors in `rest-sensors.yaml` at that URL (`YOUR_JSON_URL`).
3. Import `lovelace-discretionary.yaml` as a new dashboard (or paste the view
   into your main dashboard).

Because this is the read path into your specific Home Assistant instance
(`home.randalls.cc`) and I can't reach it from this session, I left the URL and
entity-id placeholders for you to fill. If you tell me what serves your finance
data over HTTP on the HA side, I'll finish the exact REST sensor block and you
can drop it straight in.
