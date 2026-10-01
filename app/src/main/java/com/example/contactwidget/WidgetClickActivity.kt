package com.example.contactwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class WidgetClickActivity : Activity() {

    companion object {
        private var lastClickTime: Long = 0
        private var lastWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
        private var pendingHandler: Handler? = null
        private var pendingRunnable: Runnable? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val isShortcut = intent.getBooleanExtra("IS_SHORTCUT", false) || intent.hasExtra("EXTRA_CONTACT_URI") || intent.data?.scheme == "contactwidget"

        var config: WidgetConfig? = null

        if (isShortcut || widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            val shortcutId = intent.getStringExtra("EXTRA_SHORTCUT_ID")
                ?: intent.data?.lastPathSegment
                ?: ""

            if (shortcutId.isNotEmpty()) {
                config = WidgetConfigManager.loadConfigByKey(this, shortcutId)
            }

            if (config == null) {
                val contactUri = intent.getStringExtra("EXTRA_CONTACT_URI") ?: ""
                val phoneNumber = intent.getStringExtra("EXTRA_PHONE_NUMBER") ?: ""
                val contactName = intent.getStringExtra("EXTRA_CONTACT_NAME") ?: "איש קשר"
                val photoUri = intent.getStringExtra("EXTRA_PHOTO_URI") ?: ""
                val singleClick = intent.getStringExtra("EXTRA_SINGLE_CLICK") ?: ""
                val sunActions = intent.getStringArrayExtra("EXTRA_SUN_ACTIONS")?.toList() ?: emptyList()

                val pending = WidgetConfigManager.loadPendingConfig(this)

                if (contactUri.isNotEmpty() || phoneNumber.isNotEmpty() || singleClick.isNotEmpty()) {
                    config = WidgetConfig(
                        widgetId = -1,
                        contactName = if (contactName.isNotEmpty() && contactName != "איש קשר") contactName else pending.contactName,
                        phoneNumber = if (phoneNumber.isNotEmpty()) phoneNumber else pending.phoneNumber,
                        contactUri = if (contactUri.isNotEmpty()) contactUri else pending.contactUri,
                        photoUri = if (photoUri.isNotEmpty()) photoUri else pending.photoUri,
                        singleClickAction = if (singleClick.isNotEmpty()) singleClick else pending.singleClickAction,
                        sunMenuActions = if (sunActions.isNotEmpty()) sunActions else pending.sunMenuActions
                    )
                } else {
                    config = pending
                }
            }
        } else {
            config = WidgetConfigManager.loadConfig(this, widgetId)
        }

        val finalConfig = config ?: WidgetConfigManager.loadPendingConfig(this)
        android.util.Log.d("ContactWidget", "WidgetClickActivity: finalAction=${finalConfig.singleClickAction}, phone=${finalConfig.phoneNumber}, uri=${finalConfig.contactUri}")

        executeClickAction(finalConfig.singleClickAction, widgetId, finalConfig)
        overridePendingTransition(0, 0)
        
        if (finalConfig.singleClickAction == "SUN_MENU") {
            finish()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 150)
        }
    }

    private fun executeClickAction(action: String, widgetId: Int, config: WidgetConfig) {
        if (action == "SUN_MENU") {
            val intent = Intent(this, SunMenuActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("IS_SHORTCUT", true)
                putExtra("EXTRA_CONTACT_URI", config.contactUri)
                putExtra("EXTRA_PHONE_NUMBER", config.phoneNumber)
                putExtra("EXTRA_CONTACT_NAME", config.contactName)
                putExtra("EXTRA_PHOTO_URI", config.photoUri)
                putExtra("EXTRA_SUN_ACTIONS", config.sunMenuActions.toTypedArray())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        } else {
            ContactHelper.executeAction(this, action, config.phoneNumber, config.contactUri)
        }
    }
}
