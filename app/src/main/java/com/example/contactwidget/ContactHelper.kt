package com.example.contactwidget

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast

object ContactHelper {

    fun getPhoneNumbersForContact(context: Context, contactId: String): List<String> {
        val numbers = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )

        cursor?.use { c ->
            val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                if (numberIndex != -1) {
                    val num = c.getString(numberIndex)
                    if (!num.isNullOrEmpty() && !numbers.contains(num)) {
                        numbers.add(num)
                    }
                }
            }
        }
        return numbers
    }

    fun cropToShape(src: Bitmap, shape: String): Bitmap {
        val width = src.width
        val height = src.height
        val size = width.coerceAtMost(height)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        if (shape == "CIRCLE") {
            canvas.drawOval(rect, paint)
        } else {
            // Square with rounded corners
            val cornerRadius = size * 0.18f
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val srcRect = Rect((width - size) / 2, (height - size) / 2, (width + size) / 2, (height + size) / 2)
        canvas.drawBitmap(src, srcRect, rect, paint)

        return output
    }

    fun getActionDrawable(context: Context, action: String): android.graphics.drawable.Drawable? {
        val pm = context.packageManager

        fun getIconForPackage(pkgs: List<String>): android.graphics.drawable.Drawable? {
            for (pkg in pkgs) {
                try {
                    val icon = pm.getApplicationIcon(pkg)
                    if (icon != null) return icon
                } catch (_: Exception) {}
            }
            return null
        }

        fun getIconForIntent(intent: Intent): android.graphics.drawable.Drawable? {
            return try {
                val resolveInfo = pm.resolveActivity(intent, 0)
                resolveInfo?.loadIcon(pm)
            } catch (_: Exception) {
                null
            }
        }

        val appIcon = when (action) {
            "WHATSAPP" -> getIconForPackage(listOf("com.whatsapp", "com.whatsapp.w4b"))
            "TELEGRAM" -> getIconForPackage(listOf("org.telegram.messenger", "org.telegram.messenger.web"))
            "CALL" -> getIconForIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:000"))) 
                ?: getIconForPackage(listOf("com.google.android.dialer", "com.android.phone", "com.android.dialer", "com.miui.dialer"))
            "SMS" -> getIconForIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:000"))) 
                ?: getIconForPackage(listOf("com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging"))
            "DETAILS" -> getIconForIntent(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)) 
                ?: getIconForPackage(listOf("com.google.android.contacts", "com.android.contacts"))
            "SUN_MENU" -> androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_sun)
            else -> null
        }

        if (appIcon != null) return appIcon

        val fallbackRes = when (action) {
            "WHATSAPP" -> R.drawable.ic_whatsapp
            "CALL" -> R.drawable.ic_call
            "SMS" -> R.drawable.ic_sms
            "TELEGRAM" -> R.drawable.ic_telegram
            "SUN_MENU" -> R.drawable.ic_sun
            "DETAILS" -> R.drawable.ic_person
            else -> 0
        }

        return if (fallbackRes != 0) androidx.core.content.ContextCompat.getDrawable(context, fallbackRes) else null
    }

    fun drawBadgeOnBitmap(context: Context, baseBitmap: Bitmap, actionBadge: String): Bitmap {
        if (actionBadge.isEmpty() || actionBadge == "NONE") return baseBitmap

        val width = baseBitmap.width
        val height = baseBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw base contact image
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)

        val drawable = getActionDrawable(context, actionBadge)

        if (drawable != null) {
            val badgeSize = (width * 0.38f)
            val badgeRadius = badgeSize / 2f
            val badgeCenterX = width - badgeRadius
            val badgeCenterY = height - badgeRadius

            // Draw white border around badge
            val borderPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius + (width * 0.04f), borderPaint)

            // Draw colored background circle if fallback or draw native app icon directly
            val bgColor = when (actionBadge) {
                "WHATSAPP" -> Color.parseColor("#25D366")
                "CALL" -> Color.parseColor("#007AFF")
                "SMS" -> Color.parseColor("#FF9500")
                "TELEGRAM" -> Color.parseColor("#FFCC00")
                "SUN_MENU" -> Color.parseColor("#FFB300")
                "DETAILS" -> Color.parseColor("#AF52DE")
                else -> Color.TRANSPARENT
            }

            val bgPaint = Paint().apply {
                isAntiAlias = true
                color = bgColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, bgPaint)

            val iconPadding = (badgeSize * 0.04f).toInt()
            val left = (badgeCenterX - badgeRadius + iconPadding).toInt()
            val top = (badgeCenterY - badgeRadius + iconPadding).toInt()
            val right = (badgeCenterX + badgeRadius - iconPadding).toInt()
            val bottom = (badgeCenterY + badgeRadius - iconPadding).toInt()
            drawable.setBounds(left, top, right, bottom)
            if (actionBadge == "SUN_MENU") {
                drawable.mutate().setTint(Color.WHITE)
            }
            drawable.draw(canvas)
        }

        return output
    }

    fun createDefaultAvatarBitmap(context: Context, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Light Blue Background (matching user screenshot)
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#8DBCE7")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // 2. White Person Silhouette
        val whitePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        // Head (oval)
        val headCenterX = size / 2f
        val headCenterY = size * 0.36f
        val headRadius = size * 0.22f
        canvas.drawCircle(headCenterX, headCenterY, headRadius, whitePaint)

        // Shoulders / Body
        val bodyPath = Path().apply {
            val bodyTopY = size * 0.56f
            val bodyBottomY = size.toFloat()
            val leftX = size * 0.05f
            val rightX = size * 0.95f

            moveTo(leftX, bodyBottomY)
            lineTo(rightX, bodyBottomY)
            lineTo(rightX, bodyTopY + size * 0.18f)
            quadTo(size * 0.5f, bodyTopY, leftX, bodyTopY + size * 0.18f)
            close()
        }
        canvas.drawPath(bodyPath, whitePaint)

        return output
    }

    fun createNameOnlyBitmap(context: Context, name: String, shape: String, size: Int, actionBadge: String = ""): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Background color / pill
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1D1D1F")
            style = Paint.Style.FILL
        }

        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        if (shape == "CIRCLE") {
            canvas.drawOval(rect, bgPaint)
        } else {
            val cornerRadius = size * 0.18f
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        }

        // Draw Name text centered in bitmap
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = size * 0.24f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val displayName = if (name.length > 7) name.substring(0, 6) + "…" else name
        val textY = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(displayName, size / 2f, textY, textPaint)

        return if (actionBadge.isNotEmpty()) drawBadgeOnBitmap(context, output, actionBadge) else output
    }

    fun loadContactBitmap(
        context: Context,
        photoUriStr: String?,
        shape: String,
        targetSizePx: Int,
        actionBadge: String = "",
        displayMode: String = "BOTH",
        contactName: String = "איש קשר"
    ): Bitmap {
        if (displayMode == "NAME_ONLY") {
            return createNameOnlyBitmap(context, contactName, shape, targetSizePx, actionBadge)
        }

        var bitmap: Bitmap? = null
        if (!photoUriStr.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(photoUriStr)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                }
            } catch (e: Exception) {
                bitmap = null
            }
        }

        val safeBitmap = if (bitmap != null) {
            Bitmap.createScaledBitmap(bitmap, targetSizePx, targetSizePx, true)
        } else {
            createDefaultAvatarBitmap(context, targetSizePx)
        }

        val cropped = cropToShape(safeBitmap, shape)
        return if (actionBadge.isNotEmpty()) drawBadgeOnBitmap(context, cropped, actionBadge) else cropped
    }

    fun getPhoneNumberFromUri(context: Context, contactUriStr: String): String {
        if (contactUriStr.isEmpty()) return ""
        try {
            val uri = Uri.parse(contactUriStr)
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val numIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIndex != -1) {
                        val num = c.getString(numIndex)
                        if (!num.isNullOrEmpty()) return num
                    }
                    val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                    if (idIndex != -1) {
                        val contactId = c.getString(idIndex)
                        val nums = getPhoneNumbersForContact(context, contactId)
                        if (nums.isNotEmpty()) return nums[0]
                    }
                }
            }
        } catch (_: Exception) {}
        return ""
    }

    fun executeAction(context: Context, action: String, phoneNumber: String, contactUriStr: String) {
        var cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")

        // Fallback: If phone number is empty, fetch number from contactUriStr
        if (cleanNumber.isEmpty() && contactUriStr.isNotEmpty()) {
            cleanNumber = getPhoneNumberFromUri(context, contactUriStr).replace("[^0-9+]".toRegex(), "")
        }

        android.util.Log.d("ContactWidget", "ContactHelper.executeAction: action=$action, cleanNumber=$cleanNumber, contactUriStr=$contactUriStr")

        if (cleanNumber.isEmpty() && action != "DETAILS") {
            Toast.makeText(context, "לא מוגדר מספר טלפון לאיש הקשר", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            when (action) {
                "CALL" -> {
                    try {
                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                }
                "SMS" -> {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanNumber")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                "WHATSAPP" -> {
                    val cleanDigits = cleanNumber.replace("[^0-9]".toRegex(), "")
                    val formattedNum = if (cleanDigits.startsWith("0")) {
                        "972" + cleanDigits.substring(1)
                    } else {
                        cleanDigits
                    }

                    val intentsToTry = listOf(
                        Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=$formattedNum")).apply { setPackage("com.whatsapp") },
                        Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=$formattedNum")).apply { setPackage("com.whatsapp.w4b") },
                        Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=$formattedNum")),
                        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$formattedNum")).apply { setPackage("com.whatsapp") },
                        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$formattedNum")).apply { setPackage("com.whatsapp.w4b") },
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formattedNum"))
                    )

                    var launched = false
                    for (intent in intentsToTry) {
                        try {
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            context.startActivity(intent)
                            android.util.Log.d("ContactWidget", "WhatsApp successfully launched with: $intent")
                            launched = true
                            break
                        } catch (e: Exception) {
                            android.util.Log.d("ContactWidget", "WhatsApp intent failed ($intent): ${e.message}")
                        }
                    }

                    if (!launched) {
                        Toast.makeText(context, "אפליקציית WhatsApp אינה מותקנת", Toast.LENGTH_SHORT).show()
                    }
                }
                "TELEGRAM" -> {
                    val cleanDigits = cleanNumber.replace("[^0-9]".toRegex(), "")
                    val formattedNum = if (cleanDigits.startsWith("0")) {
                        "972" + cleanDigits.substring(1)
                    } else {
                        cleanDigits
                    }

                    val intentsToTry = listOf(
                        Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=$formattedNum")).setPackage("org.telegram.messenger"),
                        Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=$formattedNum")),
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+$formattedNum"))
                    )

                    var launched = false
                    for (intent in intentsToTry) {
                        try {
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            context.startActivity(intent)
                            launched = true
                            break
                        } catch (_: Exception) {}
                    }

                    if (!launched) {
                        Toast.makeText(context, "אפליקציית Telegram אינה מותקנת", Toast.LENGTH_SHORT).show()
                    }
                }
                "DETAILS" -> {
                    if (contactUriStr.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contactUriStr)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "שגיאה בפתיחת הפעולה: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
