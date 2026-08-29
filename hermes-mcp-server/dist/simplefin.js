import axios from 'axios';
export class SimpleFinClient {
    /**
     * Claims a base64 encoded setup token from bridge.simplefin.org
     */
    static async claimSetupToken(tokenBase64) {
        try {
            const claimUrl = Buffer.from(tokenBase64.trim(), 'base64').toString('utf-8').trim();
            const response = await axios.post(claimUrl, {}, {
                headers: { 'Content-Length': '0' },
                timeout: 15000
            });
            return response.data.trim();
        }
        catch (e) {
            throw new Error(`Failed to claim SimpleFIN token: ${e.response?.data || e.message}`);
        }
    }
    /**
     * Fetches accounts and transactions across an 89-day rolling window batch
     */
    static async fetchAccountsAndTransactions(accessUrl, daysBack = 90) {
        try {
            const urlObj = new URL(accessUrl);
            const authHeader = 'Basic ' + Buffer.from(`${urlObj.username}:${urlObj.password}`).toString('base64');
            const cleanUrl = `${urlObj.protocol}//${urlObj.host}${urlObj.pathname}`;
            const nowSeconds = Math.floor(Date.now() / 1000);
            const startSeconds = nowSeconds - (daysBack * 86400);
            const response = await axios.get(cleanUrl, {
                params: {
                    'start-date': startSeconds,
                    'end-date': nowSeconds
                },
                headers: {
                    Authorization: authHeader
                },
                timeout: 25000
            });
            const data = response.data;
            const accounts = [];
            const transactions = [];
            if (data && Array.isArray(data.accounts)) {
                for (const acc of data.accounts) {
                    accounts.push({
                        id: acc.id,
                        name: acc.name || 'Account',
                        currency: acc.currency || 'USD',
                        balance: parseFloat(acc.balance || '0'),
                        availableBalance: acc['available-balance'] ? parseFloat(acc['available-balance']) : undefined,
                        type: acc.type
                    });
                    if (Array.isArray(acc.transactions)) {
                        for (const tx of acc.transactions) {
                            const amountNum = parseFloat(tx.amount || '0');
                            transactions.push({
                                id: tx.id,
                                accountId: acc.id,
                                postedEpochSeconds: tx.posted || nowSeconds,
                                amount: amountNum,
                                originalDesc: tx.description || tx.payee || 'Transaction',
                                payee: tx.payee || '',
                                category: amountNum > 0 ? 'Income' : 'Uncategorized',
                                notes: tx.memo || '',
                                pending: Boolean(tx.pending)
                            });
                        }
                    }
                }
            }
            return { accounts, transactions };
        }
        catch (e) {
            throw new Error(`SimpleFIN API error: ${e.response?.status ? `HTTP ${e.response.status}` : ''} ${e.message}`);
        }
    }
}
