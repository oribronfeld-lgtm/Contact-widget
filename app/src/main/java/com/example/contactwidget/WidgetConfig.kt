package com.example.contactwidget

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class WidgetConfig(
    val widgetId: Int,
    var contactUri: String = "",
    var contactName: String = "איש קשר",
    var phoneNumber: String = "",
    var photoUri: String = "",
    var displayMode: String = "BOTH", // "BOTH", "IMAGE_ONLY", "NAME_ONLY"
    var shape: String = "CIRCLE",     // "CIRCLE", "SQUARE"
    var size: String = "MEDIUM",       // "SMALL", "MEDIUM", "LARGE"
    var gridPreset: String = "1x1",    // "1x1", "1x2", "2x2", "1x4"
    var backgroundColor: Int = 0x00000000, // ARGB (0 = Transparent)
    var singleClickAction: String = "SUN_MENU", // "SUN_MENU", "CALL", "SMS", "WHATSAPP", "TELEGRAM"
    var doubleClickAction: String = "NONE",     // "NONE", "SUN_MENU", "CALL", "SMS", "WHATSAPP", "TELEGRAM"
    var sunMenuActions: List<String> = listOf("CALL", "SMS", "WHATSAPP", "TELEGRAM", "DETAILS")
)

object WidgetConfigManager {
    private const val PREFS_NAME = "com.example.contactwidget.WIDGET_PREFS"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveConfig(context: Context, config: WidgetConfig) {
        val json = JSONObject().apply {
            put("widgetId", config.widgetId)
            put("contactUri", config.contactUri)
            put("contactName", config.contactName)
            put("phoneNumber", config.phoneNumber)
            put("photoUri", config.photoUri)
            put("displayMode", config.displayMode)
            put("shape", config.shape)
            put("size", config.size)
            put("gridPreset", config.gridPreset)
            put("backgroundColor", config.backgroundColor)
            put("singleClickAction", config.singleClickAction)
            put("doubleClickAction", config.doubleClickAction)
            
            val actionsArray = JSONArray()
            config.sunMenuActions.forEach { actionsArray.put(it) }
            put("sunMenuActions", actionsArray)
        }

        getPrefs(context).edit()
            .putString("cfg_${config.widgetId}", json.toString())
            .apply()
    }

    fun loadConfig(context: Context, widgetId: Int): WidgetConfig {
        val rawJson = getPrefs(context).getString("cfg_$widgetId", null)
        if (rawJson.isNullOrEmpty()) {
            return WidgetConfig(widgetId = widgetId)
        }

        return try {
            val json = JSONObject(rawJson)
            val actionsList = mutableListOf<String>()
            val actionsArray = json.optJSONArray("sunMenuActions")
            if (actionsArray != null) {
                for (i in 0 until actionsArray.length()) {
                    actionsList.add(actionsArray.getString(i))
                }
            } else {
                actionsList.addAll(listOf("CALL", "SMS", "WHATSAPP", "TELEGRAM", "DETAILS"))
            }

            WidgetConfig(
                widgetId = widgetId,
                contactUri = json.optString("contactUri", ""),
                contactName = json.optString("contactName", "איש קשר"),
                phoneNumber = json.optString("phoneNumber", ""),
                photoUri = json.optString("photoUri", ""),
                displayMode = json.optString("displayMode", "BOTH"),
                shape = json.optString("shape", "CIRCLE"),
                size = json.optString("size", "MEDIUM"),
                gridPreset = json.optString("gridPreset", "1x1"),
                backgroundColor = json.optInt("backgroundColor", 0x00000000),
                singleClickAction = json.optString("singleClickAction", "SUN_MENU"),
                doubleClickAction = json.optString("doubleClickAction", "NONE"),
                sunMenuActions = actionsList
            )
        } catch (e: Exception) {
            WidgetConfig(widgetId = widgetId)
        }
    }

    fun savePendingConfig(context: Context, config: WidgetConfig) {
        saveConfig(context, config.copy(widgetId = -1))
    }

    fun loadPendingConfig(context: Context): WidgetConfig {
        return loadConfig(context, -1)
    }

    fun saveConfigByKey(context: Context, key: String, config: WidgetConfig) {
        val json = JSONObject().apply {
            put("widgetId", config.widgetId)
            put("contactUri", config.contactUri)
            put("contactName", config.contactName)
            put("phoneNumber", config.phoneNumber)
            put("photoUri", config.photoUri)
            put("displayMode", config.displayMode)
            put("shape", config.shape)
            put("size", config.size)
            put("gridPreset", config.gridPreset)
            put("backgroundColor", config.backgroundColor)
            put("singleClickAction", config.singleClickAction)
            put("doubleClickAction", config.doubleClickAction)
            
            val actionsArray = JSONArray()
            config.sunMenuActions.forEach { actionsArray.put(it) }
            put("sunMenuActions", actionsArray)
        }

        getPrefs(context).edit()
            .putString("cfg_key_$key", json.toString())
            .apply()
    }

    fun loadConfigByKey(context: Context, key: String): WidgetConfig? {
        val rawJson = getPrefs(context).getString("cfg_key_$key", null)
        if (rawJson.isNullOrEmpty()) return null

        return try {
            val json = JSONObject(rawJson)
            val actionsList = mutableListOf<String>()
            val actionsArray = json.optJSONArray("sunMenuActions")
            if (actionsArray != null) {
                for (i in 0 until actionsArray.length()) {
                    actionsList.add(actionsArray.getString(i))
                }
            } else {
                actionsList.addAll(listOf("CALL", "SMS", "WHATSAPP", "TELEGRAM", "DETAILS"))
            }

            WidgetConfig(
                widgetId = json.optInt("widgetId", -1),
                contactUri = json.optString("contactUri", ""),
                contactName = json.optString("contactName", "איש קשר"),
                phoneNumber = json.optString("phoneNumber", ""),
                photoUri = json.optString("photoUri", ""),
                displayMode = json.optString("displayMode", "BOTH"),
                shape = json.optString("shape", "CIRCLE"),
                size = json.optString("size", "MEDIUM"),
                gridPreset = json.optString("gridPreset", "1x1"),
                backgroundColor = json.optInt("backgroundColor", 0x00000000),
                singleClickAction = json.optString("singleClickAction", "SUN_MENU"),
                doubleClickAction = json.optString("doubleClickAction", "NONE"),
                sunMenuActions = actionsList
            )
        } catch (e: Exception) {
            null
        }
    }

    fun deleteConfig(context: Context, widgetId: Int) {
        getPrefs(context).edit().remove("cfg_$widgetId").apply()
    }
}
