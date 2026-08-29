# 🔥 Firebase Cross-Platform Sync — Schema & Architecture

Randall Finances syncs **APK (Android), Web/Desktop (Electron), and the Hermes MCP server**
against a single **Cloud Firestore** database, keyed by the signed-in **Google account (Firebase Auth `uid`)**.
Local storage becomes an offline cache; Firestore is the source of truth.

## Auth

- **Android**: Firebase Auth Google Sign-In (native `GoogleSignInClient`).
- **Web/Desktop**: Firebase Auth `signInWithPopup(new GoogleAuthProvider())`.
- **MCP server**: `firebase-admin` (server-side, bypasses user auth) targeting a chosen `uid`.

All clients that operate on a user's data use the same `uid`, so the same Google account sees the
same data everywhere.

## Firestore layout

```
users/{uid}                          # profile + simplefin config (single doc)
users/{uid}/transactions/{txId}      # one document per transaction
users/{uid}/budgets/{budgetId}
users/{uid}/rules/{ruleId}
users/{uid}/categories/{id}
users/{uid}/goals/{goalId}
users/{uid}/gamification             # single gamification state doc
```

Field names mirror the shared TS/Kotlin domain models exactly (so `Transaction`, `Budget`,
`Rule`, `CategoryHierarchy`, `Goal`, `GamificationState`, `SimpleFinConfig` serialize 1:1).

### `users/{uid}` (profile doc)
```jsonc
{
  "displayName": "…",        // from Google profile
  "email": "…",
  "updatedAt": 1699999999,   // epoch seconds
  "simplefin": {
    "accessUrl": "…",         // SimpleFIN bridge access URL (encrypt at rest / via CF)
    "accessUrlConfigured": true,
    "lastSyncTimestamp": 1699999999
  }
}
```

### `users/{uid}/transactions/{txId}`
Mirrors `Transaction`:
```jsonc
{
  "id": "tx-1",
  "accountId": "acct-1",
  "postedEpochSeconds": 1699999999,
  "amount": -45.32,
  "originalDesc": "WALMART STORE 1234",
  "payee": "Walmart",
  "category": "Food & Dining",
  "subCategory": "Groceries",
  "notes": null,
  "pending": false,
  "isSplit": false,
  "splits": null,
  "receiptUrls": [],
  "matchedRuleId": null,
  "updatedAt": 1699999999
}
```

### `users/{uid}/budgets/{id}`, `users/{uid}/rules/{id}`,
`users/{uid}/goals/{id}`, `users/{uid}/categories/{id}` — same field names as their TS/Kotlin models.

### `users/{uid}/gamification`
```jsonc
{ "xp": 1477, "level": 6, "levelTitle": "Compound Master",
  "streakDays": 1, "hearts": 5, "maxHearts": 5, "gems": 280,
  "completedQuests": ["inbox_zero_day1"], "updatedAt": 1699999999 }
```

## Sync strategy (every client)

1. **Authenticate** → obtain `uid`.
2. **Bootstrap**: read all collections into an in-memory map; write any locally-held data the server
   doesn't have (one-time migration on first login).
3. **Subscribe** with Firestore realtime listeners (`onSnapshot`) on every collection so remote edits
   (from another device / the MCP) appear instantly.
4. **Write-through**: every mutation writes to Firestore; local cache is updated from the snapshot.
5. **Conflict policy**: last-writer-wins on the `updatedAt` field; collection writes are atomic per doc.

## Emulator (dev, no real project needed)

`USE_FIREBASE_EMULATOR=1` points all clients at the local Firestore/Auth emulators, so the full sync
loop is testable without a real Firebase project. Production uses the values in `firebaseConfig`.

## Config injection

- **Web/Desktop**: `web/src/firebase/config.ts` reads `import.meta.env` / a checked-in template.
- **Android**: `app/google-services.json` (real one required for production).
- **MCP**: `GOOGLE_APPLICATION_CREDENTIALS` (service-account key) + `FIREBASE_PROJECT_ID`.
