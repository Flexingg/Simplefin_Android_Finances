import { initializeApp } from 'firebase/app';
import { getAuth, connectAuthEmulator, GoogleAuthProvider } from 'firebase/auth';
import { getFirestore, connectFirestoreEmulator } from 'firebase/firestore';

// Config-driven: values come from env (VITE_FIREBASE_*) or fall back to the
// placeholder. Swap in a real project by setting env vars (see .env.example)
// or editing this object directly. The Firebase web config is NOT a secret.
export const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY ?? 'AIzaSyDemoPlaceholder0000000000000000',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN ?? 'randall-finances.firebaseapp.com',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID ?? 'randall-finances',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET ?? 'randall-finances.firebasestorage.app',
  messagingSenderId: import.meta.env.VITE_FIREBASE_SENDER_ID ?? '123456789012',
  appId: import.meta.env.VITE_FIREBASE_APP_ID ?? '1:123456789012:web:placeholder',
};

export const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);

// Local emulator mode for development with no real project.
// Run: firebase emulators:start  (or npx firebase-tools emulators:start)
const USE_EMULATOR = import.meta.env.VITE_USE_FIREBASE_EMULATOR === '1';
if (USE_EMULATOR) {
  connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
  connectFirestoreEmulator(db, '127.0.0.1', 8080);
}

export const googleProvider = new GoogleAuthProvider();
