package com.example.contactwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews

class QuickContactWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            WidgetConfigManager.deleteConfig(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val pendingConfig = WidgetConfigManager.loadPendingConfig(context)
            if (pendingConfig.contactUri.isNotEmpty()) {
                val newConfig = pendingConfig.copy(widgetId = appWidgetId)
                WidgetConfigManager.saveConfig(context, newConfig)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            var config = WidgetConfigManager.loadConfig(context, appWidgetId)
            
            // If this widget ID doesn't have a saved config yet, check pending config
            if (config.contactUri.isEmpty()) {
                val pendingConfig = WidgetConfigManager.loadPendingConfig(context)
                if (pendingConfig.contactUri.isNotEmpty()) {
                    config = pendingConfig.copy(widgetId = appWidgetId)
                    WidgetConfigManager.saveConfig(context, config)
                }
            }

            val views = RemoteViews(context.packageName, R.layout.widget_contact)

            // 1. Background color
            views.setInt(R.id.widget_container, "setBackgroundColor", config.backgroundColor)

            // 2. Display mode (Both, Image Only, Name Only)
            when (config.displayMode) {
                "IMAGE_ONLY" -> {
                    views.setViewVisibility(R.id.widget_image, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_name, View.GONE)
                }
                "NAME_ONLY" -> {
                    views.setViewVisibility(R.id.widget_image, View.GONE)
                    views.setViewVisibility(R.id.widget_name, View.VISIBLE)
                }
                else -> { // BOTH
                    views.setViewVisibility(R.id.widget_image, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_name, View.VISIBLE)
                }
            }

            // 3. Name Text
            views.setTextViewText(R.id.widget_name, config.contactName)

            // 4. Icon Size Scaling (Density Independent 80dp for crisp 48dp ImageView)
            val density = context.resources.displayMetrics.density
            val targetSizePx = (80 * density).toInt()

            // 5. Contact Image & Shape with Action Badge & DisplayMode support
            val bitmap = ContactHelper.loadContactBitmap(context, config.photoUri, config.shape, targetSizePx, config.singleClickAction, config.displayMode, config.contactName)
            views.setImageViewBitmap(R.id.widget_image, bitmap)

            // 6. Click Action PendingIntent (Routes through WidgetClickActivity for single and double-tap detection)
            val intent = Intent(context, WidgetClickActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
