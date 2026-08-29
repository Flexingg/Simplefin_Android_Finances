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
export declare class FirestoreBridge {
    private db;
    private uid;
    private storage;
    private unsubs;
    isConfigured(): boolean;
    connect(storage: FinanceStorage): Promise<boolean>;
    /** Pull existing Firestore data into storage (only overrides where remote has data). */
    private bootstrap;
    /** Real-time: when remote data changes, update local storage. */
    private subscribe;
    /** Push a written file's data to Firestore (called from FinanceStorage.saveFile). */
    pushFromFile(filePath: string, data: any): Promise<void>;
    disconnect(): Promise<void>;
}
