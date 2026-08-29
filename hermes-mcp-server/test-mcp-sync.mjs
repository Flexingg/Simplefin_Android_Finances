// Verifies MCP server <-> Firestore sync against the local emulator.
// Expects emulator on 127.0.0.1:8080 and a seeded user (see verify-sync.mjs: tx-1 Walmart for test-user-123).
import { spawn } from 'child_process';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';
import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';

process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080';
process.env.FIREBASE_PROJECT_ID = 'demo-randall-finances';
process.env.FIREBASE_UID = 'test-user-123';
process.env.FIREBASE_EMULATOR_HOST = '127.0.0.1:8080';
const UID = 'test-user-123';

const serverPath = new URL('./dist/index.js', import.meta.url).pathname;
const transport = new StdioClientTransport({
  command: 'node',
  args: [serverPath],
  env: {
    ...process.env,
    FIREBASE_PROJECT_ID: 'demo-randall-finances',
    FIREBASE_UID: 'test-user-123',
    FIREBASE_EMULATOR_HOST: '127.0.0.1:8080',
    FIRESTORE_EMULATOR_HOST: '127.0.0.1:8080',
  },
});
const client = new Client({ name: 'verify-mcp-sync', version: '1.0.0' });
await client.connect(transport);

// 1) Bootstrap check: transactions seeded in Firestore should be visible via the MCP server.
const tx = await client.callTool({ name: 'list_transactions', arguments: {} });
const txText = tx.content[0].text;
const txParsed = JSON.parse(txText);
const walmart = (txParsed.transactions || []).find((t) => String(t.payee).includes('Walmart'));
if (!walmart) {
  console.error('[FAIL] MCP did not bootstrap the Walmart tx from Firestore. Got:', txText);
  process.exit(1);
}
console.log('[OK] MCP bootstrapped transaction from Firestore:', walmart.payee, walmart.category);

// 2) Mutation -> push: create an auto-rule; it must land in Firestore.
const rule = await client.callTool({ name: 'create_auto_rule', arguments: { pattern: 'Kroger', category: 'Food & Dining', subCategory: 'Groceries' } });
console.log('[INFO] create_auto_rule result:', rule.content[0].text.slice(0, 120));

await new Promise((r) => setTimeout(r, 1500));

// 3) Read back from Firestore directly (admin) to confirm the push.
const adminApp = initializeApp({ projectId: 'demo-randall-finances' }, 'verify-mcp-sync-admin');
const db = getFirestore(adminApp);
const rulesSnap = await db.collection(`users/${UID}/rules`).get();
const kroger = rulesSnap.docs.map((d) => ({ id: d.id, ...d.data() })).find((r) => r.pattern === 'Kroger');
if (!kroger) {
  console.error('[FAIL] MCP mutation did NOT reach Firestore. rules=', rulesSnap.docs.map((d) => d.id));
  process.exit(1);
}
console.log('[OK] MCP auto-rule pushed to Firestore:', kroger.id, kroger.category);

await client.close();
console.log('\nMCP <-> FIRESTORE SYNC VERIFIED');
process.exit(0);
