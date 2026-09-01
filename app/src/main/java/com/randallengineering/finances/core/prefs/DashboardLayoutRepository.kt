package com.randallengineering.finances.core.prefs

import android.content.Context
import com.randallengineering.finances.domain.model.DashboardCardType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the user's dashboard layout: which cards are shown and in what
 * order. Stored as a JSON array in SharedPreferences so it survives restarts
 * and is cheap to read/write on every toggle or reorder.
 */
class DashboardLayoutRepository(context: Context) {

    private val prefs = context.getSharedPreferences("dashboard_layout", Context.MODE_PRIVATE)
    private val key = "layout_v1"

    /** Ordered list of (card, enabled). Falls back to all cards enabled in enum order. */
    fun getLayout(): List<Pair<DashboardCardType, Boolean>> {
        val raw = prefs.getString(key, null) ?: return defaultLayout()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Pair<DashboardCardType, Boolean>>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val type = DashboardCardType.entries.firstOrNull { it.name == o.optString("id") } ?: continue
                list.add(type to o.optBoolean("enabled", true))
            }
            list.ifEmpty { defaultLayout() }
        } catch (e: Exception) {
            defaultLayout()
        }
    }

    fun saveLayout(layout: List<Pair<DashboardCardType, Boolean>>) {
        val arr = JSONArray()
        layout.forEach { (type, enabled) ->
            arr.put(JSONObject().put("id", type.name).put("enabled", enabled))
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun defaultLayout(): List<Pair<DashboardCardType, Boolean>> =
        DashboardCardType.entries.map { it to true }
}
