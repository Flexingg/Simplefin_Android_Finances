import { Transaction, SimpleFinAccount } from './types.js';
export declare class SimpleFinClient {
    /**
     * Claims a base64 encoded setup token from bridge.simplefin.org
     */
    static claimSetupToken(tokenBase64: string): Promise<string>;
    /**
     * Fetches accounts and transactions across an 89-day rolling window batch
     */
    static fetchAccountsAndTransactions(accessUrl: string, daysBack?: number): Promise<{
        accounts: SimpleFinAccount[];
        transactions: Transaction[];
    }>;
}
