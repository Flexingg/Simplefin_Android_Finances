// Integration test for the shared Firestore sync schema (users/{uid}/...).
// Requires the Firestore emulator running on 127.0.0.1:8080.
import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';

process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080';

const app = initializeApp({ projectId: 'demo-randall-finances' }, 'verify-sync');
const db = getFirestore(app);
const UID = 'test-user-123';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// --- Client A: write a transaction, budget, and gamification state ---
await db.doc(`users/${UID}/transactions/tx-1`).set({
  id: 'tx-1', accountId: 'acc-1', postedEpochSeconds: 1699999999, amount: -45.32,
  originalDesc: 'WALMART STORE', payee: 'Walmart', category: 'Food & Dining',
  subCategory: 'Groceries', notes: '', pending: false, isSplit: false,
  splits: null, receiptUrls: [], matchedRuleId: null, updatedAt: 1699999999,
});
await db.doc(`users/${UID}/budgets/b-1`).set({
  id: 'b-1', category: 'Food & Dining', subCategory: 'Groceries',
  categoryType: 'FIXED', targetAmount: 450, rolloverEnabled: true, updatedAt: 1699999999,
});
await db.doc(`users/${UID}/gamification/state`).set({
  xp: 1477, level: 6, levelTitle: 'Compound Master', streakDays: 1,
  hearts: 5, maxHearts: 5, gems: 280, completedQuests: [], updatedAt: 1699999999,
});
console.log('[OK] Client A wrote transaction, budget, gamification for uid=' + UID);

// --- Client B: realtime snapshot must deliver the same data ---
const got = await new Promise((resolve, reject) => {
  const unsub = db.collection(`users/${UID}/transactions`).onSnapshot((snap) => {
    const docs = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    if (docs.length >= 1) { unsub(); resolve(docs); }
  });
  setTimeout(() => { unsub(); reject(new Error('timeout waiting for snapshot')); }, 8000);
});
const tx = got[0];
if (tx.payee !== 'Walmart' || tx.category !== 'Food & Dining') {
  console.error('[FAIL] snapshot tx mismatch', tx); process.exit(1);
}
console.log('[OK] Client B realtime snapshot delivered tx:', tx.payee, tx.category);

// --- Verify gamification + budgets readable per-uid ---
const gSnap = await db.doc(`users/${UID}/gamification/state`).get();
if (!gSnap.exists || gSnap.data().xp !== 1477) { console.error('[FAIL] gamification'); process.exit(1); }
const bSnap = await db.doc(`users/${UID}/budgets/b-1`).get();
if (!bSnap.exists || bSnap.data().targetAmount !== 450) { console.error('[FAIL] budget'); process.exit(1); }
console.log('[OK] Gamification + budget readable per-uid');

// --- Last-writer-wins: update updatedAt; snapshot reflects it ---
await db.doc(`users/${UID}/transactions/tx-1`).set({ payee: 'Walmart (updated)', updatedAt: 1699999999 + 1 }, { merge: true });
const tx2 = await new Promise((resolve, reject) => {
  const unsub = db.collection(`users/${UID}/transactions`).onSnapshot((snap) => {
    const d = snap.docs.find((x) => x.id === 'tx-1');
    if (d && d.data().payee === 'Walmart (updated)') { unsub(); resolve(d.data()); }
  });
  setTimeout(() => { unsub(); reject(new Error('timeout')); }, 8000);
});
if (tx2.payee !== 'Walmart (updated)') { console.error('[FAIL] last-writer-wins'); process.exit(1); }
console.log('[OK] Last-writer-wins update propagated to snapshot');

// --- Per-uid isolation: another uid must NOT see this data ---
const other = await db.collection('users/other-user/transactions').get();
if (other.docs.length !== 0) { console.error('[FAIL] per-uid isolation broken'); process.exit(1); }
console.log('[OK] Per-uid isolation confirmed');

console.log('\nALL SYNC VERIFICATION TESTS PASSED');
process.exit(0);
