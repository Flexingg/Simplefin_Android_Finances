import { initializeApp } from 'firebase/app';
import { getAuth, connectAuthEmulator, GoogleAuthProvider } from 'firebase/auth';
import { getFirestore, connectFirestoreEmulator } from 'firebase/firestore';

// Config-driven: values come from env (VITE_FIREBASE_*) or fall back to the
// placeholder. Swap in a real project by setting env vars (see .env.example)
// or editing this object directly. The Firebase web config is NOT a secret.
export const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY ?? 'AIzaSyCU_Ak-RIdUzJPkqWV37PXXT1ahXewa2JQ',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN ?? 'jokarz-finance.firebaseapp.com',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID ?? 'jokarz-finance',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET ?? 'jokarz-finance.firebasestorage.app',
  messagingSenderId: import.meta.env.VITE_FIREBASE_SENDER_ID ?? '294176106740',
  appId: import.meta.env.VITE_FIREBASE_APP_ID ?? '1:294176106740:web:b5d4e43f471404441fd6a1',
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
