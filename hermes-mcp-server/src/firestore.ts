import { initializeApp, getApps, cert } from 'firebase-admin/app';
import { getFirestore, Firestore } from 'firebase-admin/firestore';
import * as path from 'path';
import * as fs from 'fs';
import { FinanceStorage } from './storage.js';

/**
 * Bridges the MCP server's FinanceStorage to a shared Cloud Firestore database,
 * so the MCP server and the Android/web/desktop clients stay in sync for the
 * same user (uid).
 *
 * Config (env):
 *   FIREBASE_PROJECT_ID            (required to enable sync)
 *   FIREBASE_UID                   (the target user's uid; required)
 *   FIREBASE_EMULATOR_HOST         (e.g. "127.0.0.1:8080" to use the local emulator)
 *   GOOGLE_APPLICATION_CREDENTIALS (service-account JSON for production)
 */
export class FirestoreBridge {
  private db: Firestore | null = null;
  private uid: string | null = null;
  private storage: FinanceStorage | null = null;
  private unsubs: (() => void)[] = [];

  isConfigured(): boolean {
    return !!(process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_UID);
  }

  /** Run a callback against local storage WITHOUT echoing back to Firestore. */
  private silent(fn: () => void) {
    this.storage!.setSilentPush(true);
    try {
      fn();
    } finally {
      this.storage!.setSilentPush(false);
    }
  }

  async connect(storage: FinanceStorage): Promise<boolean> {
    if (!this.isConfigured()) {
      console.error('[firestore-sync] disabled (set FIREBASE_PROJECT_ID + FIREBASE_UID)');
      return false;
    }
    this.storage = storage;
    this.uid = process.env.FIREBASE_UID!;

    if (process.env.FIREBASE_EMULATOR_HOST) {
      process.env.FIRESTORE_EMULATOR_HOST = process.env.FIREBASE_EMULATOR_HOST;
    }

    if (getApps().length === 0) {
      const opts: any = { projectId: process.env.FIREBASE_PROJECT_ID! };
      if (process.env.GOOGLE_APPLICATION_CREDENTIALS && fs.existsSync(process.env.GOOGLE_APPLICATION_CREDENTIALS)) {
        opts.credential = cert(JSON.parse(fs.readFileSync(process.env.GOOGLE_APPLICATION_CREDENTIALS, 'utf-8')));
      }
      initializeApp(opts, 'mcp-firestore');
    }
    this.db = getFirestore(getApps().find((a) => a.name === 'mcp-firestore') || getApps()[0]);

    await this.bootstrap(storage);
    this.subscribe();
    console.error(`[firestore-sync] connected as uid=${this.uid}`);
    return true;
  }

  /** Pull existing Firestore data into storage (only overrides where remote has data). */
  private async bootstrap(storage: FinanceStorage) {
    const kinds: { col: string; fn: (docs: any[]) => void }[] = [
      { col: 'transactions', fn: (d) => storage.saveTransactions(d) },
      { col: 'budgets', fn: (d) => d.forEach((b) => storage.saveBudget(b)) },
      { col: 'rules', fn: (d) => storage.saveRules(d) },
      { col: 'categories', fn: (d) => d.forEach((c) => storage.addOrUpdateCategory(c.mainCategory, undefined)) },
      { col: 'goals', fn: (d) => d.forEach((g) => storage.saveGoal(g)) },
    ];
    for (const { col, fn } of kinds) {
      try {
        const snap = await this.db!.collection(`users/${this.uid}/${col}`).get();
        const docs = snap.docs.map((x) => ({ id: x.id, ...x.data() }));
        if (docs.length > 0) this.silent(() => fn(docs));
      } catch (e) {
        console.error(`[firestore-sync] bootstrap ${col} failed:`, e);
      }
    }
    // SimpleFIN config
    try {
      const p = await this.db!.doc(`users/${this.uid}`).get();
      if (p.exists && p.data()?.simplefin) {
        this.silent(() => storage.saveConfig({ ...storage.getConfig(), ...p.data()!.simplefin }));
      }
    } catch (e) {}
  }

  /** Real-time: when remote data changes, update local storage. */
  private subscribe() {
    const kinds: { col: string; fn: (docs: any[]) => void }[] = [
      { col: 'transactions', fn: (d) => this.storage!.saveTransactions(d) },
      { col: 'budgets', fn: (d) => d.forEach((b) => this.storage!.saveBudget(b)) },
      { col: 'rules', fn: (d) => this.storage!.saveRules(d) },
      { col: 'goals', fn: (d) => d.forEach((g) => this.storage!.saveGoal(g)) },
    ];
    for (const { col, fn } of kinds) {
      const un = this.db!.collection(`users/${this.uid}/${col}`)
        .onSnapshot((snap) => {
          const docs = snap.docs.map((x) => ({ id: x.id, ...x.data() }));
          this.silent(() => fn(docs));
        }, (err) => console.error(`[firestore-sync] ${col} listener error:`, err));
      this.unsubs.push(un);
    }
  }

  /** Push a written file's data to Firestore (called from FinanceStorage.saveFile). */
  async pushFromFile(filePath: string, data: any) {
    if (!this.db || !this.uid) return;
    try {
      const name = path.basename(filePath);
      const now = Math.floor(Date.now() / 1000);
      if (name === 'config.json') {
        await this.db.doc(`users/${this.uid}`).set({ simplefin: data, updatedAt: now }, { merge: true });
      } else {
        // Collection files: transactions, budgets, rules, categories, goals
        const col = name.replace('.json', '');
        const ref = this.db.collection(`users/${this.uid}/${col}`);
        const batch = this.db.batch();
        (data as any[]).forEach((d) => {
          const id = d?.id ?? (col === 'categories' ? d?.mainCategory : undefined);
          if (id) batch.set(ref.doc(id), { ...d, id, updatedAt: now });
        });
        await batch.commit();
      }
    } catch (e) {
      console.error('[firestore-sync] push failed:', e);
    }
  }

  async disconnect() {
    this.unsubs.forEach((u) => u());
    this.unsubs = [];
  }
}
