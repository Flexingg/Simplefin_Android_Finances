import { useEffect, useState, ReactNode } from 'react';
import { onAuthStateChanged, User } from 'firebase/auth';
import { auth } from '../firebase/config';
import { firebaseSync } from '../firebase/sync';

/**
 * Wraps the app. Shows a Google sign-in screen until authenticated, then renders
 * children. Once signed in, cross-platform Firestore sync is active.
 */
export default function AuthGate({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const un = onAuthStateChanged(auth, (u) => {
      setUser(u);
      setLoading(false);
    });
    return un;
  }, []);

  const signIn = async () => {
    setBusy(true);
    try {
      await firebaseSync.signInWithGoogle();
    } catch (e: any) {
      // Emulator / no-project errors surface here; keep it non-fatal.
      console.warn('Sign-in failed:', e);
      alert('Sign-in failed. If you are not using a real Firebase project, set VITE_USE_FIREBASE_EMULATOR=1 and run the emulator.');
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <div className="h-screen flex items-center justify-center text-slate-400">Loading…</div>;
  }

  if (!user) {
    return (
      <div className="h-screen flex flex-col items-center justify-center gap-6 bg-[#131F24] text-white p-6 text-center">
        <div className="text-5xl">🪙</div>
        <h1 className="text-2xl font-bold">Randall Finances</h1>
        <p className="max-w-md text-slate-300">
          Sign in with your Google account to sync transactions, budgets, and progress
          across your Android, web, and desktop apps.
        </p>
        {!firebaseSync.isProductionReady && (
          <p className="max-w-md text-xs text-amber-400 bg-amber-950/40 border border-amber-700/50 rounded p-3">
            No real Firebase project is configured. Set <code>VITE_USE_FIREBASE_EMULATOR=1</code> and
            run the local emulator, or provide <code>VITE_FIREBASE_*</code> env values.
          </p>
        )}
        <button
          onClick={signIn}
          disabled={busy}
          className="mt-2 px-6 py-3 rounded-full bg-emerald-500 hover:bg-emerald-400 text-white font-semibold disabled:opacity-50"
        >
          {busy ? 'Signing in…' : 'Continue with Google'}
        </button>
      </div>
    );
  }

  return <>{children}</>;
}
