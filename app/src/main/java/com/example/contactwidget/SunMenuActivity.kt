package com.example.contactwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

class SunMenuActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sun_menu)

        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val isShortcut = intent.getBooleanExtra("IS_SHORTCUT", false) || intent.hasExtra("EXTRA_CONTACT_URI")

        var config: WidgetConfig
        if (isShortcut || widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            val contactUri = intent.getStringExtra("EXTRA_CONTACT_URI") ?: ""
            val phoneNumber = intent.getStringExtra("EXTRA_PHONE_NUMBER") ?: ""
            val contactName = intent.getStringExtra("EXTRA_CONTACT_NAME") ?: ""
            val photoUri = intent.getStringExtra("EXTRA_PHOTO_URI") ?: ""
            val sunActions = intent.getStringArrayExtra("EXTRA_SUN_ACTIONS")?.toList() ?: listOf("CALL", "SMS", "WHATSAPP", "TELEGRAM", "DETAILS")

            if (contactUri.isNotEmpty() || phoneNumber.isNotEmpty() || contactName.isNotEmpty()) {
                val pending = WidgetConfigManager.loadPendingConfig(this)
                config = WidgetConfig(
                    widgetId = -1,
                    contactName = if (contactName.isNotEmpty()) contactName else pending.contactName,
                    phoneNumber = if (phoneNumber.isNotEmpty()) phoneNumber else pending.phoneNumber,
                    contactUri = if (contactUri.isNotEmpty()) contactUri else pending.contactUri,
                    photoUri = if (photoUri.isNotEmpty()) photoUri else pending.photoUri,
                    sunMenuActions = sunActions
                )
            } else {
                config = WidgetConfigManager.loadPendingConfig(this)
            }
        } else {
            config = WidgetConfigManager.loadConfig(this, widgetId)
        }

        val root = findViewById<View>(R.id.sun_menu_root)
        val container = findViewById<RelativeLayout>(R.id.sun_menu_container)
        val imgCenter = findViewById<ImageView>(R.id.center_contact_image)
        val txtName = findViewById<TextView>(R.id.center_contact_name)
        val txtPhone = findViewById<TextView>(R.id.center_contact_phone)

        root.setOnClickListener { finish() }

        txtName.text = config.contactName
        txtPhone.text = config.phoneNumber.ifEmpty { "ללא מספר" }

        val avatar = ContactHelper.loadContactBitmap(this, config.photoUri, "CIRCLE", 200)
        imgCenter.setImageBitmap(avatar)

        setupRadialSunMenu(container, config)
    }

    private fun setupRadialSunMenu(container: RelativeLayout, config: WidgetConfig) {
        var actions = config.sunMenuActions.filter { it.isNotEmpty() }
        if (actions.size < 2) {
            actions = listOf("WHATSAPP", "CALL", "SMS", "TELEGRAM", "DETAILS")
        }
        val count = actions.size
        val radiusDp = 95f
        val density = resources.displayMetrics.density
        val radiusPx = (radiusDp * density).toInt()

        for (i in 0 until count) {
            val action = actions[i]
            val angle = (2.0 * Math.PI / count) * i - (Math.PI / 2) // Start top

            val (bgColor, iconRes) = when (action) {
                "WHATSAPP" -> Pair(Color.parseColor("#25D366"), R.drawable.ic_whatsapp)
                "CALL" -> Pair(Color.parseColor("#007AFF"), R.drawable.ic_call)
                "SMS" -> Pair(Color.parseColor("#FF9500"), R.drawable.ic_sms)
                "TELEGRAM" -> Pair(Color.parseColor("#FFCC00"), R.drawable.ic_telegram)
                "DETAILS" -> Pair(Color.parseColor("#AF52DE"), R.drawable.ic_person)
                else -> Pair(Color.parseColor("#007AFF"), R.drawable.ic_person)
            }

            // Create colorful circle drawable dynamically matching screenshot 2
            val circleDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }

            val xOffset = (radiusPx * cos(angle)).toFloat()
            val yOffset = (radiusPx * sin(angle)).toFloat()

            val appDrawable = ContactHelper.getActionDrawable(this, action)

            val actionButton = ImageView(this).apply {
                val btnSize = (64 * density).toInt()
                layoutParams = RelativeLayout.LayoutParams(btnSize, btnSize).apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                }
                translationX = xOffset
                translationY = yOffset
                background = circleDrawable
                val pad = (4 * density).toInt()
                setPadding(pad, pad, pad, pad)
                elevation = 16f
                setImageDrawable(appDrawable)

                setOnClickListener {
                    ContactHelper.executeAction(this@SunMenuActivity, action, config.phoneNumber, config.contactUri)
                    finish()
                }
            }

            container.addView(actionButton)
        }
    }
}
