import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

admin.initializeApp();
const db = admin.firestore();

// ---------------------------------------------------------------------------
// Configuration & Environment Variables / Secrets
// Set these in Firebase using:
// firebase functions:config:set amazon.client_id="..." amazon.client_secret="..." aws.access_key="..." aws.secret_key="..."
// or defineSecret() in Cloud Functions v2
// ---------------------------------------------------------------------------
const LWA_CLIENT_ID = process.env.LWA_CLIENT_ID || functions.config().amazon?.client_id || "amzn1.application-oa2-client.placeholder";
const LWA_CLIENT_SECRET = process.env.LWA_CLIENT_SECRET || functions.config().amazon?.client_secret || "placeholder_lwa_secret";
const AWS_ACCESS_KEY_ID = process.env.AWS_ACCESS_KEY_ID || functions.config().aws?.access_key || "";
const AWS_SECRET_ACCESS_KEY = process.env.AWS_SECRET_ACCESS_KEY || functions.config().aws?.secret_key || "";
const AWS_REGION = process.env.AWS_REGION || "us-east-1";
const SP_API_ENDPOINT = "sellingpartnerapi-na.amazon.com";

// ===========================================================================
// SECTION 1: AWS SigV4 Request Signer for SP-API
// ===========================================================================

function getSignatureKey(key: string, dateStamp: string, regionName: string, serviceName: string): Buffer {
  const kDate = crypto.createHmac("sha256", "AWS4" + key).update(dateStamp).digest();
  const kRegion = crypto.createHmac("sha256", kDate).update(regionName).digest();
  const kService = crypto.createHmac("sha256", kRegion).update(serviceName).digest();
  return crypto.createHmac("sha256", kService).update("aws4_request").digest();
}

function signAwsRequest(params: {
  method: string;
  path: string;
  queryString: string;
  headers: Record<string, string>;
  payload: string;
  accessKeyId: string;
  secretAccessKey: string;
  region: string;
  service: string;
}): Record<string, string> {
  const { method, path, queryString, headers, payload, accessKeyId, secretAccessKey, region, service } = params;

  const now = new Date();
  const amzDate = now.toISOString().replace(/[:-]|\.\d{3}/g, "");
  const dateStamp = amzDate.substring(0, 8);

  const signedHeadersMap: Record<string, string> = {
    ...headers,
    host: SP_API_ENDPOINT,
    "x-amz-date": amzDate,
  };

  const sortedHeaderKeys = Object.keys(signedHeadersMap).map((k) => k.toLowerCase()).sort();
  const canonicalHeaders = sortedHeaderKeys.map((k) => `${k}:${signedHeadersMap[k].trim()}\n`).join("");
  const signedHeadersStr = sortedHeaderKeys.join(";");

  const payloadHash = crypto.createHash("sha256").update(payload).digest("hex");
  const canonicalRequest = `${method}\n${path}\n${queryString}\n${canonicalHeaders}\n${signedHeadersStr}\n${payloadHash}`;

  const algorithm = "AWS4-HMAC-SHA256";
  const credentialScope = `${dateStamp}/${region}/${service}/aws4_request`;
  const stringToSign = `${algorithm}\n${amzDate}\n${credentialScope}\n${crypto.createHash("sha256").update(canonicalRequest).digest("hex")}`;

  const signingKey = getSignatureKey(secretAccessKey, dateStamp, region, service);
  const signature = crypto.createHmac("sha256", signingKey).update(stringToSign).digest("hex");

  const authorizationHeader = `${algorithm} Credential=${accessKeyId}/${credentialScope}, SignedHeaders=${signedHeadersStr}, Signature=${signature}`;

  return {
    ...signedHeadersMap,
    Authorization: authorizationHeader,
  };
}

// ===========================================================================
// SECTION 2: Amazon OAuth Flow (Firebase Cloud Functions)
// ===========================================================================

/**
 * Callable Function: getAmazonAuthUrl
 * Generates the Login with Amazon / SP-API Consent OAuth URI.
 */
export const getAmazonAuthUrl = functions.https.onCall(async (data, context) => {
  const userId = context.auth?.uid || "default_user";
  const redirectUri = data.redirectUri || `https://${process.env.GCLOUD_PROJECT || "randall-finances"}.web.app/amazonOAuthCallback`;
  
  // State includes userId and timestamp for CSRF verification
  const state = Buffer.from(JSON.stringify({ userId, timestamp: Date.now() })).toString("base64url");

  // Amazon Login with Amazon authorization endpoint (Standard LWA scope)
  const authUrl = `https://www.amazon.com/ap/oa?client_id=${encodeURIComponent(
    data.clientId || LWA_CLIENT_ID
  )}&scope=profile&response_type=code&redirect_uri=${encodeURIComponent(
    redirectUri
  )}&state=${encodeURIComponent(state)}`;

  return {
    authUrl,
    state,
  };
});

/**
 * HTTP Function: amazonOAuthCallback
 * OAuth redirect handler that exchanges auth code for refresh_token and persists to Firestore.
 */
export const amazonOAuthCallback = functions.https.onRequest(async (req, res) => {
  const code = (req.query.code || req.query.spapi_oauth_code) as string;
  const stateRaw = req.query.state as string;

  if (!code) {
    res.status(400).send("<h1>Error: Missing authorization code from Amazon</h1>");
    return;
  }

  try {
    let userId = "default_user";
    if (stateRaw) {
      try {
        const decodedState = JSON.parse(Buffer.from(stateRaw, "base64url").toString("utf-8"));
        if (decodedState.userId) userId = decodedState.userId;
      } catch (e) {
        // Fallback to default
      }
    }

    // Exchange authorization code for LWA tokens
    const tokenResponse = await fetch("https://api.amazon.com/auth/o2/token", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
      },
      body: new URLSearchParams({
        grant_type: "authorization_code",
        code: code,
        client_id: LWA_CLIENT_ID,
        client_secret: LWA_CLIENT_SECRET,
      }).toString(),
    });

    if (!tokenResponse.ok) {
      const errText = await tokenResponse.text();
      functions.logger.error("Failed to exchange Amazon OAuth code", errText);
      res.status(500).send(`<h1>Failed to connect Amazon account</h1><p>${errText}</p>`);
      return;
    }

    const tokenData = await tokenResponse.json();
    const refreshToken = tokenData.refresh_token;

    // Save refresh_token securely into Firestore user config
    await db.collection("users").doc(userId).collection("config").doc("amazon").set(
      {
        connected: true,
        refreshToken: refreshToken,
        connectedAt: admin.firestore.FieldValue.serverTimestamp(),
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    // Return friendly success HTML
    res.send(`
      <!DOCTYPE html>
      <html>
      <head>
        <title>Amazon Connected - Randall Finances</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0F172A; color: #E1E1E1; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }
          .card { background: #1E293B; padding: 36px; border-radius: 16px; border: 1px solid #334155; box-shadow: 0 4px 20px rgba(0,0,0,0.5); max-width: 420px; }
          .icon { font-size: 48px; margin-bottom: 16px; }
          h2 { color: #4CAF50; margin: 0 0 12px 0; }
          p { color: #94A3B8; line-height: 1.5; font-size: 15px; }
        </style>
      </head>
      <body>
        <div class="card">
          <div class="icon">📦</div>
          <h2>Amazon Account Linked!</h2>
          <p>Your Amazon account is now authorized with <strong>Randall Finances</strong>.</p>
          <p>You can close this tab and return to the app to view item breakdowns for your Amazon transactions.</p>
        </div>
      </body>
      </html>
    `);
  } catch (error: any) {
    functions.logger.error("Error in amazonOAuthCallback", error);
    res.status(500).send(`<h1>Authentication Error</h1><p>${error.message}</p>`);
  }
});

/**
 * HTTP Function: privacyPolicy
 * Serves the Amazon-compliant Privacy Policy HTML directly.
 */
export const privacyPolicy = functions.https.onRequest(async (req, res) => {
  res.sendFile("privacy.html", { root: "../public" }, (err) => {
    if (err) {
      res.send(`
        <!DOCTYPE html>
        <html><head><title>Privacy Policy - Randall Finances</title><style>body{font-family:sans-serif;background:#0F172A;color:#F8FAFC;padding:40px;max-width:800px;margin:auto;line-height:1.6;}</style></head>
        <body><h1>Privacy Policy</h1><p>Randall Finances does not sell or share user data. All financial data is encrypted and used solely for transaction matching.</p></body></html>
      `);
    }
  });
});

/**
 * HTTP Function: dataCollectionPolicy
 * Serves the Amazon-compliant Data Collection Policy & Terms HTML directly.
 */
export const dataCollectionPolicy = functions.https.onRequest(async (req, res) => {
  res.sendFile("terms.html", { root: "../public" }, (err) => {
    if (err) {
      res.send(`
        <!DOCTYPE html>
        <html><head><title>Data Collection Policy - Randall Finances</title><style>body{font-family:sans-serif;background:#0F172A;color:#F8FAFC;padding:40px;max-width:800px;margin:auto;line-height:1.6;}</style></head>
        <body><h1>Data Collection Policy</h1><p>Data is collected strictly to categorize transactions and match Amazon orders.</p></body></html>
      `);
    }
  });
});

/**
 * Callable Function: parseAmazonOrderScreenshot
 * Uses AI Vision to extract structured order JSON from an Amazon screenshot / photo.
 */
export const parseAmazonOrderScreenshot = functions.https.onCall(async (data, context) => {
  const imageBase64 = data.imageBase64;
  if (!imageBase64) {
    throw new functions.https.HttpsError("invalid-argument", "Missing imageBase64 parameter.");
  }

  try {
    const apiKey = process.env.GEMINI_API_KEY || "placeholder_gemini_key";
    // Call Gemini Vision endpoint
    const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [
          {
            parts: [
              {
                text: `Extract all Amazon orders and item lines from this Amazon Orders screenshot.
                Return ONLY valid JSON matching this schema:
                [
                  {
                    "orderId": "114-1234567-1234567",
                    "purchaseDate": "August 20, 2026",
                    "orderTotal": 42.99,
                    "orderStatus": "Delivered",
                    "items": [
                      {
                        "title": "Product Full Name",
                        "asin": "B08PV7NW3H",
                        "quantityOrdered": 1,
                        "itemPrice": 39.99,
                        "itemTax": 3.00,
                        "totalPrice": 42.99
                      }
                    ]
                  }
                ]`
              },
              {
                inlineData: {
                  mimeType: "image/jpeg",
                  data: imageBase64
                }
              }
            ]
          }
        ],
        generationConfig: {
          responseMimeType: "application/json",
          temperature: 0.1
        }
      })
    });

    if (response.ok) {
      const resJson = await response.json();
      const rawText = resJson.candidates?.[0]?.content?.parts?.[0]?.text || "[]";
      const orders = JSON.parse(rawText);
      return { success: true, orders };
    } else {
      return { success: false, orders: [] };
    }
  } catch (err: any) {
    functions.logger.error("Error in parseAmazonOrderScreenshot", err);
    return { success: false, orders: [] };
  }
});

// ===========================================================================
// SECTION 3: SP-API Order Matching (Firebase Callable Function)
// ===========================================================================

export interface AmazonOrderItem {
  title: string;
  asin: string;
  quantityOrdered: number;
  itemPrice: number;
  itemTax: number;
  totalPrice: number;
  imageUrl?: string;
}

export interface MatchedAmazonOrderResult {
  orderId: string;
  purchaseDate: string;
  orderTotal: number;
  orderStatus: string;
  items: AmazonOrderItem[];
}

/**
 * Callable Function: getAmazonOrderDetailsForTransaction
 * Matches a bank transaction (amount & date) against the user's Amazon Order History.
 */
export const getAmazonOrderDetailsForTransaction = functions.https.onCall(async (data, context) => {
  const userId = context.auth?.uid || "default_user";
  const transactionDateEpoch = data.transactionDateEpoch as number; // epoch seconds
  const targetAmount = Math.abs(parseFloat(data.amount) || 0.0);

  if (!transactionDateEpoch || isNaN(targetAmount)) {
    throw new functions.https.HttpsError("invalid-argument", "transactionDateEpoch and amount are required");
  }

  try {
    // 1. Retrieve user's stored Amazon refresh_token from Firestore
    const amazonConfigDoc = await db.collection("users").doc(userId).collection("config").doc("amazon").get();
    const amazonData = amazonConfigDoc.data();

    if (!amazonData || !amazonData.refreshToken) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Amazon account is not linked. Please connect your Amazon account in Settings."
      );
    }

    const refreshToken = amazonData.refreshToken as string;

    // 2. Exchange refresh_token for short-lived LWA access_token
    const lwaTokenResponse = await fetch("https://api.amazon.com/auth/o2/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        refresh_token: refreshToken,
        client_id: LWA_CLIENT_ID,
        client_secret: LWA_CLIENT_SECRET,
      }).toString(),
    });

    if (!lwaTokenResponse.ok) {
      const err = await lwaTokenResponse.text();
      throw new Error(`Failed to refresh Amazon LWA token: ${err}`);
    }

    const { access_token: accessToken } = await lwaTokenResponse.json();

    // 3. Define time window: 4 days before and 4 days after transaction date
    const txDateMs = transactionDateEpoch * 1000;
    const windowMs = 4 * 24 * 60 * 60 * 1000;
    const createdAfter = new Date(txDateMs - windowMs).toISOString();
    const createdBefore = new Date(txDateMs + windowMs).toISOString();

    // 4. Query SP-API Orders endpoint
    const queryParams = new URLSearchParams({
      CreatedAfter: createdAfter,
      CreatedBefore: createdBefore,
      MarketplaceIds: "ATVPDKIKX0DER", // Amazon.com US Marketplace ID
    });
    const queryString = queryParams.toString();
    const ordersPath = "/orders/v0/orders";

    // Sign SP-API Request with AWS SigV4
    const signedHeaders = AWS_ACCESS_KEY_ID
      ? signAwsRequest({
          method: "GET",
          path: ordersPath,
          queryString: queryString,
          headers: { "x-amz-access-token": accessToken },
          payload: "",
          accessKeyId: AWS_ACCESS_KEY_ID,
          secretAccessKey: AWS_SECRET_ACCESS_KEY,
          region: AWS_REGION,
          service: "execute-api",
        })
      : {
          "x-amz-access-token": accessToken,
          host: SP_API_ENDPOINT,
        };

    const ordersResponse = await fetch(`https://${SP_API_ENDPOINT}${ordersPath}?${queryString}`, {
      method: "GET",
      headers: signedHeaders,
    });

    if (!ordersResponse.ok) {
      const errBody = await ordersResponse.text();
      throw new Error(`SP-API Orders query failed (${ordersResponse.status}): ${errBody}`);
    }

    const ordersData = await ordersResponse.json();
    const ordersList = ordersData?.payload?.Orders || [];

    // 5. Filter to find the order whose OrderTotal amount matches transaction amount (±$0.10 tolerance)
    const matchedOrder = ordersList.find((order: any) => {
      const orderAmount = Math.abs(parseFloat(order.OrderTotal?.Amount) || 0.0);
      return Math.abs(orderAmount - targetAmount) < 0.1;
    });

    if (!matchedOrder) {
      return {
        matched: false,
        message: `No Amazon order matching $${targetAmount.toFixed(2)} found between ${createdAfter.substring(0, 10)} and ${createdBefore.substring(0, 10)}.`,
      };
    }

    const amazonOrderId = matchedOrder.AmazonOrderId;

    // 6. Query SP-API Order Items endpoint
    const itemsPath = `/orders/v0/orders/${amazonOrderId}/orderItems`;
    const itemsSignedHeaders = AWS_ACCESS_KEY_ID
      ? signAwsRequest({
          method: "GET",
          path: itemsPath,
          queryString: "",
          headers: { "x-amz-access-token": accessToken },
          payload: "",
          accessKeyId: AWS_ACCESS_KEY_ID,
          secretAccessKey: AWS_SECRET_ACCESS_KEY,
          region: AWS_REGION,
          service: "execute-api",
        })
      : {
          "x-amz-access-token": accessToken,
          host: SP_API_ENDPOINT,
        };

    const itemsResponse = await fetch(`https://${SP_API_ENDPOINT}${itemsPath}`, {
      method: "GET",
      headers: itemsSignedHeaders,
    });

    const itemsData = itemsResponse.ok ? await itemsResponse.json() : null;
    const rawOrderItems = itemsData?.payload?.OrderItems || [];

    // 7. Format clean response
    const items: AmazonOrderItem[] = rawOrderItems.map((item: any) => {
      const itemPrice = parseFloat(item.ItemPrice?.Amount) || 0.0;
      const itemTax = parseFloat(item.ItemTax?.Amount) || 0.0;
      return {
        title: item.Title || "Amazon Product",
        asin: item.ASIN || "",
        quantityOrdered: item.QuantityOrdered || 1,
        itemPrice: itemPrice,
        itemTax: itemTax,
        totalPrice: itemPrice + itemTax,
      };
    });

    const result: MatchedAmazonOrderResult = {
      orderId: amazonOrderId,
      purchaseDate: matchedOrder.PurchaseDate,
      orderTotal: parseFloat(matchedOrder.OrderTotal?.Amount) || targetAmount,
      orderStatus: matchedOrder.OrderStatus || "Completed",
      items: items,
    };

    return {
      matched: true,
      order: result,
    };
  } catch (error: any) {
    functions.logger.error("Error in getAmazonOrderDetailsForTransaction", error);
    throw new functions.https.HttpsError("internal", error.message || "Failed to retrieve Amazon order details");
  }
});

// ===========================================================================
// SECTION 4: SimpleFIN Bridge & Batch Sync
// ===========================================================================

export const claimSimpleFinToken = functions.https.onCall(async (data, context) => {
  const userId = context.auth?.uid || "default_user";
  const setupToken = data.setupToken as string;

  if (!setupToken) {
    throw new functions.https.HttpsError("invalid-argument", "setupToken is required");
  }

  try {
    const claimUrl = Buffer.from(setupToken.trim(), "base64").toString("utf-8").trim();

    if (!claimUrl.startsWith("http://") && !claimUrl.startsWith("https://")) {
      throw new functions.https.HttpsError("invalid-argument", "Invalid decoded SimpleFIN claim URL");
    }

    const response = await fetch(claimUrl, {
      method: "POST",
      headers: { "Content-Length": "0" },
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to claim token from SimpleFIN bridge: ${response.status} ${errorText}`);
    }

    const accessUrl = (await response.text()).trim();

    if (!accessUrl.startsWith("http://") && !accessUrl.startsWith("https://")) {
      throw new Error(`Received invalid Access URL: ${accessUrl}`);
    }

    await db.collection("users").doc(userId).collection("config").doc("simplefin").set(
      {
        accessUrl: accessUrl,
        accessUrlConfigured: true,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        errorList: [],
      },
      { merge: true }
    );

    return {
      success: true,
      message: "SimpleFIN access token claimed and configured successfully.",
    };
  } catch (error: any) {
    functions.logger.error("Error in claimSimpleFinToken", error);
    throw new functions.https.HttpsError("internal", error.message || "Failed to claim SimpleFIN token");
  }
});

export const syncTransactionsJob = functions.https.onCall(async (data, context) => {
  const userId = context.auth?.uid || "default_user";
  const requestedDaysBack = typeof data?.daysBack === "number" ? Math.min(Math.max(data.daysBack, 1), 1000) : 90;

  try {
    const configDoc = await db.collection("users").doc(userId).collection("config").doc("simplefin").get();
    const configData = configDoc.data();

    if (!configData || !configData.accessUrl) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "SimpleFIN Access URL is not configured. Please claim a setup token first."
      );
    }

    const accessUrl = configData.accessUrl as string;
    const parsedUrl = new URL(accessUrl);
    const username = parsedUrl.username;
    const password = parsedUrl.password;

    parsedUrl.username = "";
    parsedUrl.password = "";

    let requestEndpoint = parsedUrl.toString();
    if (requestEndpoint.endsWith("/")) {
      requestEndpoint = requestEndpoint.slice(0, -1);
    }
    requestEndpoint += "/accounts?version=2";

    const basicAuthHeader = "Basic " + Buffer.from(`${username}:${password}`).toString("base64");
    const nowEpochSeconds = Math.floor(Date.now() / 1000);
    const dayInSeconds = 24 * 60 * 60;
    const batchWindowDays = 89;

    const allTransactionsMap = new Map<string, any>();
    const errList: string[] = [];

    const totalBatches = Math.max(Math.ceil(requestedDaysBack / batchWindowDays), 1);
    let currentOffsetDays = 0;

    for (let i = 0; i < totalBatches; i++) {
      const endDayOffset = currentOffsetDays;
      const startDayOffset = Math.min(currentOffsetDays + batchWindowDays, requestedDaysBack);

      const endEpoch = nowEpochSeconds - endDayOffset * dayInSeconds;
      const startEpoch = nowEpochSeconds - startDayOffset * dayInSeconds;

      const batchUrl = `${requestEndpoint}&start-date=${startEpoch}&end-date=${endEpoch}`;

      try {
        const response = await fetch(batchUrl, {
          method: "GET",
          headers: {
            Authorization: basicAuthHeader,
            Accept: "application/json",
          },
        });

        if (response.ok) {
          const body: any = await response.json();
          if (body.errlist) errList.push(...body.errlist);
          if (body.errors) errList.push(...body.errors);

          for (const account of body.accounts || []) {
            for (const tx of account.transactions || []) {
              allTransactionsMap.set(tx.id, { tx, accountId: account.id });
            }
          }
        }
      } catch (err: any) {
        errList.push(`Batch ${i + 1} failed: ${err.message}`);
      }

      currentOffsetDays = startDayOffset;
      if (currentOffsetDays >= requestedDaysBack) break;
    }

    const batch = db.batch();
    const transactionsRef = db.collection("transactions");

    for (const [txId, { tx, accountId }] of allTransactionsMap.entries()) {
      const docRef = transactionsRef.doc(txId);
      const amountNum = parseFloat(tx.amount) || 0;

      batch.set(
        docRef,
        {
          id: tx.id,
          accountId: accountId,
          postedEpochSeconds: tx.posted,
          amount: amountNum,
          originalDesc: tx.description || tx.payee || "Transaction",
          payee: tx.payee || "",
          notes: tx.memo || "",
          pending: tx.pending || false,
          category: amountNum > 0 ? "Income" : "Uncategorized",
          subCategory: "",
          isSplit: false,
          receiptUrls: [],
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    }

    await batch.commit();

    await db.collection("users").doc(userId).collection("config").doc("simplefin").update({
      lastSyncTimestamp: admin.firestore.FieldValue.serverTimestamp(),
      errorList: Array.from(new Set(errList)),
    });

    return {
      success: true,
      syncedCount: allTransactionsMap.size,
      errorList: Array.from(new Set(errList)),
    };
  } catch (error: any) {
    functions.logger.error("Error in syncTransactionsJob", error);
    throw new functions.https.HttpsError("internal", error.message || "Failed to sync transactions");
  }
});
