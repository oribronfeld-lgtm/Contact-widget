package com.example.contactwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val PICK_CONTACT_REQUEST = 1001

    private lateinit var currentConfig: WidgetConfig
    private val phoneNumbersList = mutableListOf<String>()

    // UI elements
    private lateinit var btnPickContact: MaterialButton
    private lateinit var txtSelectedContact: TextView
    private lateinit var spinnerPhoneNumbers: Spinner
    private lateinit var rgDisplayMode: RadioGroup
    private lateinit var rgShape: RadioGroup
    private lateinit var rgSize: RadioGroup
    private lateinit var rgBackground: RadioGroup
    private lateinit var spinnerSingleClick: Spinner
    private lateinit var spinnerDoubleClick: Spinner
    private lateinit var cbCall: CheckBox
    private lateinit var cbSms: CheckBox
    private lateinit var cbWhatsapp: CheckBox
    private lateinit var cbTelegram: CheckBox
    private lateinit var cbDetails: CheckBox
    private lateinit var btnSave: MaterialButton
    private lateinit var btnAddShortcut: MaterialButton

    // Preview elements
    private lateinit var previewContainer: FrameLayout
    private lateinit var previewImage: ImageView
    private lateinit var previewName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        setContentView(R.layout.activity_config)

        // Read appWidgetId
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        currentConfig = WidgetConfigManager.loadConfig(this, appWidgetId)
        if (currentConfig.contactUri.isEmpty()) {
            val pending = WidgetConfigManager.loadPendingConfig(this)
            if (pending.contactUri.isNotEmpty()) {
                currentConfig = pending.copy(widgetId = appWidgetId)
            }
        }

        initViews()
        setupSpinners()
        restoreUIFromConfig()
        setupListeners()
        updatePreview()
    }

    private fun restoreUIFromConfig() {
        if (currentConfig.contactName.isNotEmpty() && currentConfig.contactName != "איש קשר") {
            txtSelectedContact.text = "${currentConfig.contactName} (${currentConfig.phoneNumber})"
        }

        if (currentConfig.contactUri.isNotEmpty()) {
            val uri = Uri.parse(currentConfig.contactUri)
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                    if (idIndex != -1) {
                        val contactId = c.getString(idIndex)
                        phoneNumbersList.clear()
                        phoneNumbersList.addAll(ContactHelper.getPhoneNumbersForContact(this, contactId))

                        if (phoneNumbersList.isNotEmpty()) {
                            val phoneAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, phoneNumbersList)
                            spinnerPhoneNumbers.adapter = phoneAdapter

                            val selectedIndex = phoneNumbersList.indexOf(currentConfig.phoneNumber).coerceAtLeast(0)
                            currentConfig.phoneNumber = phoneNumbersList[selectedIndex]
                            spinnerPhoneNumbers.setSelection(selectedIndex)

                            spinnerPhoneNumbers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                    currentConfig.phoneNumber = phoneNumbersList[position]
                                    txtSelectedContact.text = "${currentConfig.contactName} (${currentConfig.phoneNumber})"
                                }
                                override fun onNothingSelected(parent: AdapterView<*>?) {}
                            }
                        }
                    }
                }
            }
        }

        when (currentConfig.displayMode) {
            "IMAGE_ONLY" -> rgDisplayMode.check(R.id.rb_image_only)
            "NAME_ONLY" -> rgDisplayMode.check(R.id.rb_name_only)
            else -> rgDisplayMode.check(R.id.rb_both)
        }

        if (currentConfig.shape == "SQUARE") rgShape.check(R.id.rb_square) else rgShape.check(R.id.rb_circle)

        when (currentConfig.backgroundColor) {
            Color.parseColor("#CC000000") -> rgBackground.check(R.id.rb_bg_black)
            Color.parseColor("#CCFFFFFF") -> rgBackground.check(R.id.rb_bg_white)
            Color.parseColor("#CC007AFF") -> rgBackground.check(R.id.rb_bg_blue)
            else -> rgBackground.check(R.id.rb_bg_transparent)
        }

        val clickActionMap = arrayOf("SUN_MENU", "CALL", "SMS", "WHATSAPP", "TELEGRAM", "DETAILS", "NONE")
        val singleIdx = clickActionMap.indexOf(currentConfig.singleClickAction).coerceAtLeast(0)
        spinnerSingleClick.setSelection(singleIdx)

        val actions = currentConfig.sunMenuActions
        cbCall.isChecked = actions.contains("CALL")
        cbSms.isChecked = actions.contains("SMS")
        cbWhatsapp.isChecked = actions.contains("WHATSAPP")
        cbTelegram.isChecked = actions.contains("TELEGRAM")
        cbDetails.isChecked = actions.contains("DETAILS")
    }

    private fun initViews() {
        btnPickContact = findViewById(R.id.btn_pick_contact)
        txtSelectedContact = findViewById(R.id.txt_selected_contact)
        spinnerPhoneNumbers = findViewById(R.id.spinner_phone_numbers)
        rgDisplayMode = findViewById(R.id.rg_display_mode)
        rgShape = findViewById(R.id.rg_shape)
        rgBackground = findViewById(R.id.rg_background)
        spinnerSingleClick = findViewById(R.id.spinner_single_click)

        cbCall = findViewById(R.id.cb_sun_call)
        cbSms = findViewById(R.id.cb_sun_sms)
        cbWhatsapp = findViewById(R.id.cb_sun_whatsapp)
        cbTelegram = findViewById(R.id.cb_sun_telegram)
        cbDetails = findViewById(R.id.cb_sun_details)
        btnSave = findViewById(R.id.btn_save_widget)
        btnAddShortcut = findViewById(R.id.btn_add_shortcut)

        previewContainer = findViewById(R.id.preview_container)
        previewImage = findViewById(R.id.preview_image)
        previewName = findViewById(R.id.preview_name)
    }

    private fun setupSpinners() {
        val clickOptions = arrayOf("תפריט שמש", "חיוג טלפוני ישיר", "שליחת SMS", "WhatsApp", "Telegram", "פרטי איש קשר", "ללא")
        val clickActionMap = arrayOf("SUN_MENU", "CALL", "SMS", "WHATSAPP", "TELEGRAM", "DETAILS", "NONE")

        val adapter = ActionAdapter(this, clickOptions, clickActionMap)
        spinnerSingleClick.adapter = adapter

        val singleIdx = clickActionMap.indexOf(currentConfig.singleClickAction).coerceIn(0, 6)
        spinnerSingleClick.setSelection(singleIdx)

        spinnerSingleClick.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentConfig.singleClickAction = clickActionMap[position.coerceIn(0, 6)]
                updatePreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        setupCheckboxIcons()
    }

    private fun setupCheckboxIcons() {
        fun applyIcon(cb: CheckBox, action: String) {
            cb.textDirection = View.TEXT_DIRECTION_RTL
            cb.gravity = android.view.Gravity.RIGHT or android.view.Gravity.CENTER_VERTICAL
            val drawable = ContactHelper.getActionDrawable(this, action)
            if (drawable != null) {
                val density = resources.displayMetrics.density
                val size = (24 * density).toInt()
                drawable.setBounds(0, 0, size, size)
                cb.setCompoundDrawablesRelative(drawable, null, null, null)
                cb.compoundDrawablePadding = (12 * density).toInt()
            }
        }

        applyIcon(cbCall, "CALL")
        applyIcon(cbSms, "SMS")
        applyIcon(cbWhatsapp, "WHATSAPP")
        applyIcon(cbTelegram, "TELEGRAM")
        applyIcon(cbDetails, "DETAILS")
    }

    private fun setupListeners() {
        btnPickContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(intent, PICK_CONTACT_REQUEST)
        }

        rgDisplayMode.setOnCheckedChangeListener { _, checkedId ->
            currentConfig.displayMode = when (checkedId) {
                R.id.rb_image_only -> "IMAGE_ONLY"
                R.id.rb_name_only -> "NAME_ONLY"
                else -> "BOTH"
            }
            updatePreview()
        }

        rgShape.setOnCheckedChangeListener { _, checkedId ->
            currentConfig.shape = if (checkedId == R.id.rb_square) "SQUARE" else "CIRCLE"
            updatePreview()
        }

        rgBackground.setOnCheckedChangeListener { _, checkedId ->
            currentConfig.backgroundColor = when (checkedId) {
                R.id.rb_bg_black -> Color.parseColor("#CC000000")
                R.id.rb_bg_white -> Color.parseColor("#CCFFFFFF")
                R.id.rb_bg_blue -> Color.parseColor("#CC007AFF")
                else -> Color.TRANSPARENT
            }
            updatePreview()
        }

        btnSave.setOnClickListener {
            saveAndFinish()
        }

        btnAddShortcut.setOnClickListener {
            createShortcutAndFinish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val contactUri: Uri? = data.data
            if (contactUri != null) {
                loadSelectedContactData(contactUri)
            }
        }
    }

    private fun loadSelectedContactData(contactUri: Uri) {
        currentConfig.contactUri = contactUri.toString()

        val cursor: Cursor? = contentResolver.query(contactUri, null, null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val photoIndex = c.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

                if (idIndex != -1 && nameIndex != -1) {
                    val contactId = c.getString(idIndex)
                    currentConfig.contactName = c.getString(nameIndex) ?: "איש קשר"
                    currentConfig.photoUri = if (photoIndex != -1) c.getString(photoIndex) ?: "" else ""

                    // Fetch phone numbers
                    phoneNumbersList.clear()
                    phoneNumbersList.addAll(ContactHelper.getPhoneNumbersForContact(this, contactId))

                    if (phoneNumbersList.isNotEmpty()) {
                        val phoneAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, phoneNumbersList)
                        spinnerPhoneNumbers.adapter = phoneAdapter

                        val selectedIndex = if (currentConfig.phoneNumber.isNotEmpty() && phoneNumbersList.contains(currentConfig.phoneNumber)) {
                            phoneNumbersList.indexOf(currentConfig.phoneNumber)
                        } else 0

                        currentConfig.phoneNumber = phoneNumbersList[selectedIndex]
                        spinnerPhoneNumbers.setSelection(selectedIndex)

                        spinnerPhoneNumbers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                currentConfig.phoneNumber = phoneNumbersList[position]
                                txtSelectedContact.text = "${currentConfig.contactName} (${currentConfig.phoneNumber})"
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                    } else {
                        currentConfig.phoneNumber = ""
                        spinnerPhoneNumbers.adapter = null
                    }

                    txtSelectedContact.text = "${currentConfig.contactName} (${currentConfig.phoneNumber})"
                }
            }
        }
        updatePreview()
    }

    private fun updatePreview() {
        previewContainer.setBackgroundColor(if (currentConfig.backgroundColor == Color.TRANSPARENT) Color.parseColor("#33000000") else currentConfig.backgroundColor)

        when (currentConfig.displayMode) {
            "IMAGE_ONLY" -> {
                previewImage.visibility = View.VISIBLE
                previewName.visibility = View.GONE
            }
            "NAME_ONLY" -> {
                previewImage.visibility = View.GONE
                previewName.visibility = View.VISIBLE
            }
            else -> {
                previewImage.visibility = View.VISIBLE
                previewName.visibility = View.VISIBLE
            }
        }

        previewName.text = currentConfig.contactName
        previewName.setTextColor(Color.WHITE)
        previewName.setBackgroundResource(R.drawable.bg_name_pill)
        val density = resources.displayMetrics.density
        previewName.setPadding((8 * density).toInt(), (2 * density).toInt(), (8 * density).toInt(), (2 * density).toInt())

        val targetPx = (80 * density).toInt()

        val bitmap = ContactHelper.loadContactBitmap(this, currentConfig.photoUri, currentConfig.shape, targetPx, currentConfig.singleClickAction, currentConfig.displayMode, currentConfig.contactName)
        previewImage.setImageBitmap(bitmap)
    }

    private fun collectConfigFromUI() {
        if (spinnerPhoneNumbers.adapter != null && spinnerPhoneNumbers.selectedItem != null) {
            currentConfig.phoneNumber = spinnerPhoneNumbers.selectedItem.toString()
        }

        val selectedActions = mutableListOf<String>()
        if (cbCall.isChecked) selectedActions.add("CALL")
        if (cbSms.isChecked) selectedActions.add("SMS")
        if (cbWhatsapp.isChecked) selectedActions.add("WHATSAPP")
        if (cbTelegram.isChecked) selectedActions.add("TELEGRAM")
        if (cbDetails.isChecked) selectedActions.add("DETAILS")

        currentConfig.sunMenuActions = selectedActions

        val clickActionMap = arrayOf("SUN_MENU", "CALL", "SMS", "WHATSAPP", "TELEGRAM", "DETAILS", "NONE")
        currentConfig.singleClickAction = clickActionMap[spinnerSingleClick.selectedItemPosition.coerceIn(0, 6)]
        currentConfig.doubleClickAction = "NONE"

        WidgetConfigManager.saveConfig(this, currentConfig)
        WidgetConfigManager.savePendingConfig(this, currentConfig)
    }

    private fun createShortcutAndFinish() {
        collectConfigFromUI()

        val shortcutShape = "CIRCLE"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val shortcutId = "contact_shortcut_" + System.currentTimeMillis()
                
                WidgetConfigManager.saveConfigByKey(this, shortcutId, currentConfig)
                android.util.Log.d("ContactWidget", "createShortcutAndFinish: shortcutId=$shortcutId, action=${currentConfig.singleClickAction}, phone=${currentConfig.phoneNumber}")

                val cleanNumber = currentConfig.phoneNumber.replace("[^0-9+]".toRegex(), "")
                val cleanDigits = cleanNumber.replace("[^0-9]".toRegex(), "")
                val formattedNum = if (cleanDigits.startsWith("0")) "972" + cleanDigits.substring(1) else cleanDigits

                val targetIntent: Intent = when (currentConfig.singleClickAction) {
                    "WHATSAPP" -> {
                        Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=$formattedNum")).apply {
                            setPackage("com.whatsapp")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    }
                    "CALL" -> {
                        try {
                            Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        } catch (e: Exception) {
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        }
                    }
                    "SMS" -> {
                        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanNumber")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    }
                    "TELEGRAM" -> {
                        Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=$formattedNum")).apply {
                            setPackage("org.telegram.messenger")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    }
                    "DETAILS" -> {
                        if (currentConfig.contactUri.isNotEmpty()) {
                            Intent(Intent.ACTION_VIEW, Uri.parse(currentConfig.contactUri)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        } else {
                            Intent(this, SunMenuActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                putExtra("IS_SHORTCUT", true)
                                putExtra("EXTRA_CONTACT_URI", currentConfig.contactUri)
                                putExtra("EXTRA_PHONE_NUMBER", currentConfig.phoneNumber)
                                putExtra("EXTRA_CONTACT_NAME", currentConfig.contactName)
                                putExtra("EXTRA_PHOTO_URI", currentConfig.photoUri)
                                putExtra("EXTRA_SUN_ACTIONS", currentConfig.sunMenuActions.toTypedArray())
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        }
                    }
                    else -> { // SUN_MENU
                        Intent(this, SunMenuActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra("IS_SHORTCUT", true)
                            putExtra("EXTRA_CONTACT_URI", currentConfig.contactUri)
                            putExtra("EXTRA_PHONE_NUMBER", currentConfig.phoneNumber)
                            putExtra("EXTRA_CONTACT_NAME", currentConfig.contactName)
                            putExtra("EXTRA_PHOTO_URI", currentConfig.photoUri)
                            putExtra("EXTRA_SUN_ACTIONS", currentConfig.sunMenuActions.toTypedArray())
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    }
                }

                val density = resources.displayMetrics.density
                val iconPx = (96 * density).toInt()
                val bitmap = ContactHelper.loadContactBitmap(this, currentConfig.photoUri, shortcutShape, iconPx, currentConfig.singleClickAction, currentConfig.displayMode, currentConfig.contactName)

                val shortcut = android.content.pm.ShortcutInfo.Builder(this, shortcutId)
                    .setShortLabel(currentConfig.contactName)
                    .setLongLabel(currentConfig.contactName)
                    .setIcon(android.graphics.drawable.Icon.createWithBitmap(bitmap))
                    .setIntent(targetIntent)
                    .build()

                shortcutManager.requestPinShortcut(shortcut, null)
                Toast.makeText(this, "אייקון איש הקשר נוצר! כעת תוכל לגרור אותו לאייקונים אחרים ליצירת תיקייה 📁", Toast.LENGTH_LONG).show()
            }
        }
        setResult(RESULT_OK)
        finish()
    }

    private fun saveAndFinish() {
        collectConfigFromUI()

        val appWidgetManager = AppWidgetManager.getInstance(this)

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            QuickContactWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val myProvider = android.content.ComponentName(this, QuickContactWidgetProvider::class.java)

                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val pinnedWidgetCallbackIntent = Intent(this, QuickContactWidgetProvider::class.java)
                    val successCallback = android.app.PendingIntent.getBroadcast(
                        this, 0, pinnedWidgetCallbackIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )

                    appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                    Toast.makeText(this, "אנא אשר את הוספת הווידג'ט למסך הבית בחלון שנפתח", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "אנא הוסף את הווידג'ט דרך לחיצה ארוכה על מסך הבית -> הווידג'טים שלי", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "אנא הוסף את הווידג'ט דרך לחיצה ארוכה על מסך הבית -> הווידג'טים שלי", Toast.LENGTH_LONG).show()
            }
            setResult(RESULT_OK)
            finish()
        }
    }
}

class ActionAdapter(
    context: android.content.Context,
    private val options: Array<String>,
    private val actionKeys: Array<String>
) : ArrayAdapter<String>(context, android.R.layout.select_dialog_item, options) {

    override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
        val view = convertView ?: android.view.LayoutInflater.from(context).inflate(android.R.layout.select_dialog_item, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = options[position]
        textView.textSize = 15f
        textView.setTextColor(Color.parseColor("#1D1D1F"))
        textView.textDirection = View.TEXT_DIRECTION_RTL
        textView.gravity = android.view.Gravity.RIGHT or android.view.Gravity.CENTER_VERTICAL
        
        val action = actionKeys[position]
        val drawable = ContactHelper.getActionDrawable(context, action)
        if (drawable != null) {
            val density = context.resources.displayMetrics.density
            val size = (24 * density).toInt()
            drawable.setBounds(0, 0, size, size)
            if (action == "SUN_MENU") {
                drawable.mutate().setTint(Color.parseColor("#FFB300"))
            }
            textView.setCompoundDrawablesRelative(drawable, null, null, null)
            textView.compoundDrawablePadding = (12 * density).toInt()
        } else {
            textView.setCompoundDrawablesRelative(null, null, null, null)
        }
        return view
    }
}
