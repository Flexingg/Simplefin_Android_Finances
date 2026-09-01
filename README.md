# 🪙 SimpleFIN Android Finances

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase%20(Firestore%20%2B%20Functions%20%2B%20Hosting)-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![CI](https://github.com/Flexingg/Simplefin_Android_Finances/actions/workflows/ci.yml/badge.svg)](https://github.com/Flexingg/Simplefin_Android_Finances/actions/workflows/ci.yml)
[![Download APK](https://img.shields.io/badge/Download-APK-3DDC84?logo=android&logoColor=white)](https://github.com/Flexingg/Simplefin_Android_Finances/releases/latest/download/SimpleFin-v1.1.3.apk)

An autonomous, privacy-first, full-stack personal finance application for Android. Built with **Jetpack Compose (Material 3)**, **SimpleFIN Bridge API** for direct multi-bank synchronization, **Firebase Cloud Services**, and a novel **In-App AI Amazon Order Ingestion & Matcher Engine**.

---

## 🌟 Key Features

### 1. 🏦 SimpleFIN Bridge Multi-Bank Synchronization
- **Direct Multi-Institution Aggregation**: Connect checking, savings, credit cards, investments, and loans from thousands of banks and credit unions.
- **Smart 89-Day Historical Batch Windowing**: Syncs complete multi-month transaction histories without triggering API rate limits or missing intermediate transactions.
- **Secure On-Device Storage**: Encrypted credential and token management.

### 2. ⚡ Novel In-App Live AI Amazon Order Ingestion & Splitter
- **Login with Amazon (LWA) OAuth 2.0**: Official Amazon identity linking and token lifecycle management.
- **1-Tap Live DOM & AI Vision Ingest**: Built-in full-screen browser with native touch routing that navigates to your Amazon Orders page and instantly extracts:
  - Real purchased product titles
  - ASINs and thumbnail references
  - Item quantities and prices
  - Delivery dates and order IDs
- **Automatic Transaction Pairing & Category Splitting**: Automatically correlates scanned Amazon items with bank charges within a $\pm 3$-day window, suggests smart subcategories (e.g. *Toys & Games*, *Office Supplies*, *Health & Personal Care*, *Auto Maintenance*), and creates multi-line transaction splits in 1 tap!

### 3. 📊 Analytics, Canvas Charts & Financial Insights
- **Daily Allowance Engine**: Computes dynamic daily discretionary spending limits based on monthly income, recurring commitments, and remaining days in the cycle.
- **Custom Jetpack Compose Canvas Charts**:
  - **Weekly & Monthly Spend Velocity**: Smooth bezier curve area graphs for tracking cash flow burn.
  - **Category Donut Breakdown**: High-resolution interactive slice charts for expense distribution.
  - **Cash Flow Waterfall**: Inflows vs. outflows comparison.

### 4. 🧠 Financial AI Copilot (MCP & LLM Tool Execution)
- Context-aware financial AI assistant powered by **Gemini 1.5** and Model Context Protocol (MCP) tool execution.
- Capable of answering complex spending questions, calculating savings projections, and automatically applying batch categorization proposals.

### 5. 🏷️ Dynamic Auto-Categorization & Rules Engine
- Customizable categorization hierarchy (Income, Housing, Groceries, Dining, Utilities, Subscriptions, Shopping, etc.).
- Regex and wildcard-based automated transaction rules that categorize incoming bank transactions in real time.

### 6. ☁️ Firebase Cloud Sync & Hosted Legal Compliance
- **Cloud Firestore**: Real-time cross-device transaction and category synchronization.
- **Firebase Hosting**: Hosts Amazon LWA-compliant Privacy Policy, Terms of Service, and OAuth redirect handlers.
- **Cloud Functions (Node.js/TypeScript)**: Handles backend token exchanges and AI vision payload processing.

---

## 🏗️ Architecture & Tech Stack

```
com.randallengineering.finances
├── core/
│   ├── ai/              # Financial MCP tools, Gemini interfaces & Prompts
│   ├── network/         # Resource wrapper, HTTP client & Plaid/SimpleFIN/Amazon clients
│   ├── theme/           # Material 3 Expressive theme, typography, color schemes & shapes
│   └── util/            # DateUtils, CurrencyFormatter, AmazonDomExtractor, AmazonTransactionMatcher
├── data/
│   ├── model/           # Room/Firestore entity data models & DTOs
│   └── repository/      # TransactionRepository, SimpleFinRepository, AmazonRepository, RulesRepository
├── domain/
│   ├── model/           # Clean domain models (Transaction, TransactionSplit, MatchedAmazonOrder)
│   └── usecase/         # Financial use cases (Allowance, Insights, AiChatbot, MatchAmazonItems)
└── ui/
    ├── components/      # AmazonOrdersBrowserSheet, AmazonScanReviewDialog, ExpenseBreakdownCard, etc.
    ├── navigation/      # Jetpack Compose Navigation & App Destinations
    └── screens/         # Dashboard, Transactions, Analytics, Chatbot, Categories, Settings
```

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.0 |
| **UI Framework** | Jetpack Compose, Material 3, Jetpack Navigation |
| **Dependency Injection** | Koin |
| **Async & State** | Kotlin Coroutines, StateFlow, Flow |
| **Serialization** | KotlinX Serialization (JSON) |
| **Networking** | OkHttp3, Retrofit / HttpURLConnection |
| **Cloud & Backend** | Firebase Firestore, Cloud Functions (Node.js/TS), Firebase Hosting |
| **Third-Party APIs** | SimpleFIN Bridge REST API, Amazon LWA OAuth 2.0, Google Gemini AI |

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Iguana, Jellyfish, Koala or newer)
- **JDK 17+**
- **Android Device or Emulator** (Android 8.0 / API Level 26 or higher)
- **Firebase Project** (with Firestore, Functions, and Hosting enabled)

---

### Installation & Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Flexingg/Simplefin_Android_Finances.git
   cd Simplefin_Android_Finances
   ```

2. **Add `google-services.json`**:
   - Download your `google-services.json` from the [Firebase Console](https://console.firebase.google.com/) and place it in the `app/` folder.

3. **Deploy Firebase Hosting & Functions**:
   ```bash
   npm install -g firebase-tools
   firebase login
   firebase deploy
   ```

4. **Build and Run the Android App**:
   ```bash
   # Build Debug APK
   ./gradlew assembleDebug

   # Install via ADB to Connected Device
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔑 Configuration & API Keys

### SimpleFIN Bridge
1. Register for an account at [SimpleFIN](https://beta-bridge.simplefin.org/).
2. Generate a Setup Token.
3. In the App, navigate to **Settings ➔ SimpleFIN Sync** and paste your Setup Token. Tap **Claim Token** to start syncing accounts.

### Login with Amazon (LWA) & In-App Scanner
1. Go to the [Amazon Developer Console](https://developer.amazon.com/loginwithamazon/console/site/lwa/overview.html).
2. Create a new Security Profile:
   - **Allowed Origins**: `https://<YOUR_FIREBASE_PROJECT>.web.app`
   - **Allowed Return URLs**: `https://<YOUR_FIREBASE_PROJECT>.web.app/amazonOAuthCallback`
3. Enter your **Client ID** and **Client Secret** into the app's **Settings ➔ Amazon Integration** screen.
4. Use the in-app **"⚡ Live AI Scan Amazon Orders"** button to automatically ingest your orders into bank transactions.

---

## 📱 Wireless & Remote Deployment (ADB)

The project supports fast local Wi-Fi and remote Tailscale VPN deployment:

```bash
# Local Wi-Fi (Replace with your device IP)
adb connect 192.168.1.110:5555
adb -s 192.168.1.110:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.1.110:5555 shell am start -n com.randallengineering.finances/.MainActivity

# Tailscale Remote VPN
adb connect 100.115.62.59:5555
adb -s 100.115.62.59:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
