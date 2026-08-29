import {
  collection,
  doc,
  getDocs,
  onSnapshot,
  setDoc,
  deleteDoc,
  Unsubscribe,
  CollectionReference,
  DocumentData,
} from 'firebase/firestore';
import {
  signInWithPopup,
  signOut as fbSignOut,
  onAuthStateChanged,
  User,
} from 'firebase/auth';
import { auth, db, googleProvider } from './config';
import { appState } from '../services/storage';
import type {
  Transaction, Budget, Rule, CategoryHierarchy, Goal, GamificationState,
} from '../types';

export type SyncKind = 'transactions' | 'budgets' | 'rules' | 'categories' | 'goals';

const KINDS: SyncKind[] = ['transactions', 'budgets', 'rules', 'categories', 'goals'];

type Entity = Transaction | Budget | Rule | Goal;

class FirebaseSync {
  private uid: string | null = null;
  private unsubs: Unsubscribe[] = [];
  private authUnsub: Unsubscribe | null = null;
  private pushTimer: ReturnType<typeof setTimeout> | null = null;

  /** Returns true when a real Firebase project is configured (vs placeholder). */
  get isProductionReady(): boolean {
    return !(db.app.options.apiKey ?? '').includes('DemoPlaceholder');
  }

  init() {
    this.authUnsub = onAuthStateChanged(auth, (user) => {
      if (user) {
        this.attach(user.uid);
      } else {
        this.detach();
      }
    });
  }

  async signInWithGoogle(): Promise<User> {
    const res = await signInWithPopup(auth, googleProvider);
    return res.user;
  }

  async signOut() {
    await fbSignOut(auth);
  }

  /** Read a collection into an array of documents. */
  private async readCollection(ref: CollectionReference<DocumentData>): Promise<any[]> {
    const snap = await getDocs(ref);
    return snap.docs.map((d) => ({ id: d.id, ...d.data() }));
  }

  /**
   * Attach sync for a uid: hydrate Firestore -> appState, subscribe to live
   * snapshots, and push local mutations up.
   */
  async attach(uid: string) {
    this.detach();
    this.uid = uid;

    // Bootstrap: pull everything from Firestore; if empty, migrate local state up.
    for (const kind of KINDS) {
      const ref = collection(db, 'users', uid, kind);
      const docs = await this.readCollection(ref);
      if (docs.length > 0) {
        // Hydrate from remote into a Partial for appState.hydrate()
        appState.hydrate(this.toPartial(kind, docs));
      } else {
        // First login: push the local (seeded) data up so all devices see it.
        const current = this.currentFor(kind);
        if (current && current.length > 0) {
          await this.pushCollection(kind);
        }
      }
    }

    // Realtime listeners for every collection.
    for (const kind of KINDS) {
      const ref = collection(db, 'users', uid, kind);
      const un = onSnapshot(ref, (snap) => {
        const docs = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        appState.hydrate(this.toPartial(kind, docs));
      });
      this.unsubs.push(un);
    }

    // Push local mutations to Firestore.
    appState.subscribe(() => {
      if (!this.uid || !appState.isDirty()) return;
      if (this.pushTimer) clearTimeout(this.pushTimer);
      this.pushTimer = setTimeout(() => this.pushAll(), 250);
    });

    // Profile (SimpleFIN config) + gamification listeners.
    const profileRef = doc(db, 'users', uid);
    const unP = onSnapshot(profileRef, (d) => {
      const data = d.data();
      if (data?.simplefin) {
        // Surface remote SimpleFIN config (used by onboarding/settings).
        try { localStorage.setItem('rf_simplefin_remote', JSON.stringify(data.simplefin)); } catch {}
      }
    });
    this.unsubs.push(unP);

    const gRef = doc(db, 'users', uid, 'gamification', 'state');
    const unG = onSnapshot(gRef, (d) => {
      if (d.exists()) {
        const data = d.data() as Partial<GamificationState>;
        appState.hydrate({ gamification: { ...appState.getGamification(), ...data } as GamificationState });
      } else {
        // Push local gamification up if remote has none.
        setDoc(gRef, { ...appState.getGamification(), updatedAt: Math.floor(Date.now() / 1000) });
      }
    });
    this.unsubs.push(unG);
  }

  detach() {
    this.unsubs.forEach((u) => u());
    this.unsubs = [];
    this.uid = null;
  }

  /** Map a kind to the current array in appState (using its getters). */
  private currentFor(kind: SyncKind): Entity[] | CategoryHierarchy[] {
    switch (kind) {
      case 'transactions': return appState.getTransactions();
      case 'budgets': return appState.getBudgets();
      case 'rules': return appState.getRules();
      case 'categories': return appState.getCategories();
      case 'goals': return appState.getGoals();
    }
  }

  private toPartial(kind: SyncKind, docs: any[]) {
    switch (kind) {
      case 'transactions': return { transactions: docs };
      case 'budgets': return { budgets: docs };
      case 'rules': return { rules: docs };
      case 'categories': return { categories: docs };
      case 'goals': return { goals: docs };
    }
  }

  private async pushAll() {
    if (!this.uid) return;
    for (const kind of KINDS) {
      await this.pushCollection(kind);
    }
  }

  /** Write the current collection array to Firestore (write-through). */
  private async pushCollection(kind: SyncKind) {
    if (!this.uid) return;
    const ref = collection(db, 'users', this.uid, kind);
    const docs = this.currentFor(kind) as any[];
    const now = Math.floor(Date.now() / 1000);
    for (const d of docs) {
      const id = d?.id ?? (kind === 'categories' ? (d as CategoryHierarchy)?.mainCategory : `${kind}-${now}`);
      if (!id) continue;
      await setDoc(doc(ref, id), { ...d, id, updatedAt: now });
    }
  }

  /** Persist the SimpleFIN config into the user profile doc. */
  async saveSimplefin(accessUrl: string, configured = true) {
    if (!this.uid) return;
    const ref = doc(db, 'users', this.uid);
    await setDoc(ref, {
      simplefin: {
        accessUrl,
        accessUrlConfigured: configured,
        lastSyncTimestamp: Math.floor(Date.now() / 1000),
      },
      updatedAt: Math.floor(Date.now() / 1000),
    }, { merge: true });
  }
}

export const firebaseSync = new FirebaseSync();
