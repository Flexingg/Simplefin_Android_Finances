package com.randallengineering.finances.core.firebase

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Local-development emulator hook (config-driven, no real Firebase project needed).
 *
 * When the app is built against the placeholder/demo project id (the checked-in
 * `google-services.json` uses `randall-finances-demo`), point Firestore at the local
 * Firebase emulator so the full sync loop is testable on a developer machine —
 * matching the Web and MCP clients' `USE_FIREBASE_EMULATOR` behaviour.
 *
 * Production: swap in a real `google-services.json` (real `project_id`) and this
 * helper no-ops automatically; the app talks to the real Cloud Firestore.
 */
object FirebaseEmulator {

    /** The placeholder project id from the checked-in demo google-services.json. */
    private const val PLACEHOLDER_PROJECT_ID = "randall-finances-demo"

    /**
     * Host/port the Android app uses to reach the local emulator.
     * 10.0.2.2 = the host machine's loopback as seen from the Android emulator.
     * For a physical device on your LAN, override with the host machine's IP.
     */
    private const val FIRESTORE_EMULATOR_HOST = "10.0.2.2"
    private const val FIRESTORE_EMULATOR_PORT = 8080

    /** True when the configured project is the placeholder → use the emulator. */
    fun isPlaceholderProject(): Boolean =
        FirebaseFirestore.getInstance().app.options.projectId == PLACEHOLDER_PROJECT_ID

    /** Point Firestore at the local emulator when running against the placeholder project. */
    fun FirebaseFirestore.connectEmulatorIfPlaceholder() {
        if (app.options.projectId == PLACEHOLDER_PROJECT_ID) {
            useEmulator(FIRESTORE_EMULATOR_HOST, FIRESTORE_EMULATOR_PORT)
        }
    }
}
