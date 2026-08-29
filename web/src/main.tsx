import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import { firebaseSync } from './firebase/sync';
import AuthGate from './components/AuthGate';
import './index.css';

// Begin watching Firebase auth; on sign-in this attaches Firestore sync.
firebaseSync.init();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthGate>
      <App />
    </AuthGate>
  </React.StrictMode>
);
