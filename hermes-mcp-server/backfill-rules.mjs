// Backfills auto-rule categorization into Firestore using the REST API
// (which returns HTTP 429 immediately on quota exhaustion instead of hanging).
// Reads all rules + transactions, applies the rules to uncategorized
// transactions, and writes the resulting categories back.
import { readFileSync } from 'fs';
import { JWT } from 'google-auth-library';

const SA_PATH = process.env.SA_PATH || '/home/hermes/.hermes/secrets/jokarz-finance-firebase-adminsdk.json';
const PROJECT = process.env.PROJECT || 'jokarz-finance';
const UID = process.env.UID || 'bNERxbPVI8hUS2hLeczUd5LWY4F3';

const sa = JSON.parse(readFileSync(SA_PATH, 'utf8'));
const client = new JWT({ email: sa.client_email, key: sa.private_key, scopes: ['https://www.googleapis.com/auth/datastore'] });
const token = await client.getAccessToken();
const auth = { Authorization: `Bearer ${token.token}` };
const base = `https://firestore.googleapis.com/v1/projects/${PROJECT}/databases/(default)/documents`;

function dec(fields) {
  const out = {};
  for (const [k, v] of Object.entries(fields || {})) {
    if (v.stringValue !== undefined) out[k] = v.stringValue;
    else if (v.integerValue !== undefined) out[k] = Number(v.integerValue);
    else if (v.doubleValue !== undefined) out[k] = v.doubleValue;
    else if (v.booleanValue !== undefined) out[k] = v.booleanValue;
    else out[k] = v;
  }
  return out;
}

async function listDocs(path) {
  const res = await fetch(`${base}/${path}?pageSize=1000`, { headers: auth });
  const data = await res.json();
  if (res.status !== 200) throw new Error(`list ${path}: HTTP ${res.status} ${data.error?.message || ''}`);
  return (data.documents || []).map((doc) => ({ id: doc.name.split('/').pop(), ...dec(doc.fields) }));
}

async function updateDoc(path, fields) {
  const qs = Object.keys(fields).map((f) => `updateMask.fieldPaths=${encodeURIComponent(f)}`).join('&');
  const body = { fields: {} };
  for (const [k, v] of Object.entries(fields)) {
    body.fields[k] = enc(v);
  }
  const res = await fetch(`${base}/${path}?${qs}`, {
    method: 'PATCH',
    headers: { ...auth, 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (res.status === 429) throw Object.assign(new Error('QUOTA_EXCEEDED'), { quota: true });
  if (res.status !== 200) {
    const d = await res.json().catch(() => ({}));
    throw new Error(`update ${path}: HTTP ${res.status} ${d.error?.message || ''}`);
  }
  return true;
}

function enc(v) {
  if (Array.isArray(v)) return { arrayValue: { values: v.map(enc) } };
  if (typeof v === 'number') return { doubleValue: v };
  if (typeof v === 'boolean') return { booleanValue: v };
  return { stringValue: String(v) };
}

function matchRule(tx, rules) {
  if (tx.isSplit) return null;
  for (const rule of rules.filter((r) => r.isActive !== false)) {
    if (!rule.pattern || !rule.pattern.trim()) continue;
    const regex = new RegExp(rule.pattern.trim(), 'i');
    if (regex.test((tx.originalDesc || '') + ' ' + (tx.payee || ''))) {
      if (rule.minAmount != null && Math.abs(tx.amount) < rule.minAmount) continue;
      if (rule.maxAmount != null && Math.abs(tx.amount) > rule.maxAmount) continue;
      return rule;
    }
  }
  return null;
}

const rules = await listDocs(`users/${UID}/rules`);
const txs = await listDocs(`users/${UID}/transactions`);
console.error(`rules: ${rules.length} | transactions: ${txs.length}`);

try {
  let updated = 0;
  for (const tx of txs) {
    const rule = matchRule(tx, rules);
    const isUncat = !tx.category || String(tx.category).toLowerCase() === 'uncategorized';
    if (rule && isUncat) {
      await updateDoc(`users/${UID}/transactions/${encodeURIComponent(tx.id)}`, {
        category: rule.category,
        subCategory: rule.subCategory || '',
        matchedRuleId: rule.id,
      });
      updated++;
    }
  }
  console.log(`Backfilled categorization for ${updated} transaction(s).`);

  // Ensure new categories exist in the hierarchy.
  const newCats = [
    { id: 'Financial Transactions', mainCategory: 'Financial Transactions', subCategories: ['Credit Card Payment'] },
    { id: 'Transportation', mainCategory: 'Transportation', subCategories: ['Car Insurance'] },
    { id: 'Personal & Lifestyle', mainCategory: 'Personal & Lifestyle', subCategories: ['Fun Money'] },
  ];
  for (const c of newCats) {
    await updateDoc(`users/${UID}/categories/${encodeURIComponent(c.id)}`, {
      mainCategory: c.mainCategory,
      subCategories: c.subCategories,
    });
  }
  console.log('Categories ensured.');
  console.log('DONE — historical categorization backfilled successfully.');
} catch (e) {
  if (e && e.quota) {
    // Quiet: no stdout so the cron delivers nothing on still-exhausted days.
    console.error('QUOTA_EXCEEDED: Firestore write quota still exhausted. No changes written.');
    process.exit(0);
  }
  console.error('BACKFILL ERROR:', e.message);
  process.exit(1);
}
