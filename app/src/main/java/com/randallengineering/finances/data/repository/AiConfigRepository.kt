package com.randallengineering.finances.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AiProviderMode {
    CUSTOM_KEY,       // Custom Gemini API Key provided by user
    BUILTIN_VERTEX    // Google account / Firebase Vertex AI
}

data class AiConfig(
    val apiKey: String = "",
    val providerMode: AiProviderMode = AiProviderMode.CUSTOM_KEY,
    val selectedModel: String = "gemini-2.5-flash"
) {
    val isKeyConfigured: Boolean
        get() = apiKey.isNotBlank()
}

class AiConfigRepository(
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("randall_ai_config", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<AiConfig> = _configFlow.asStateFlow()

    private fun loadConfig(): AiConfig {
        val key = prefs.getString("gemini_api_key", "").orEmpty()
        val modeStr = prefs.getString("ai_provider_mode", AiProviderMode.CUSTOM_KEY.name)
        val mode = try { AiProviderMode.valueOf(modeStr ?: AiProviderMode.CUSTOM_KEY.name) } catch (e: Exception) { AiProviderMode.CUSTOM_KEY }
        val model = prefs.getString("selected_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        return AiConfig(apiKey = key, providerMode = mode, selectedModel = model)
    }

    fun getApiKey(): String = _configFlow.value.apiKey

    fun getProviderMode(): AiProviderMode = _configFlow.value.providerMode

    fun getSelectedModel(): String = _configFlow.value.selectedModel

    fun saveConfig(apiKey: String, providerMode: AiProviderMode, selectedModel: String = "gemini-2.5-flash") {
        prefs.edit()
            .putString("gemini_api_key", apiKey.trim())
            .putString("ai_provider_mode", providerMode.name)
            .putString("selected_model", selectedModel)
            .apply()
        _configFlow.value = AiConfig(apiKey = apiKey.trim(), providerMode = providerMode, selectedModel = selectedModel)
    }

    fun setApiKey(apiKey: String) {
        saveConfig(apiKey, _configFlow.value.providerMode, _configFlow.value.selectedModel)
    }

    fun setProviderMode(mode: AiProviderMode) {
        saveConfig(_configFlow.value.apiKey, mode, _configFlow.value.selectedModel)
    }
}
