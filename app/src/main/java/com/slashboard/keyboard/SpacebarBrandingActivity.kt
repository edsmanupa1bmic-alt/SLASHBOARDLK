package com.slashboard.keyboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.slashboard.keyboard.data.repository.ThemeEngine

/**
 * Spacebar Branding Activity:
 * Allows the user to personalize the spacebar label with custom text (Max 12 chars)
 * or choose from inspiration preset chips.
 */
class SpacebarBrandingActivity : AppCompatActivity() {

    private val quickIdeas = listOf(
        "Slashboard", "Official", "Sri Lanka",
        "Pro", "Love", "Gamer",
        "Hustle", "Dream", "Focus", "Vibes"
    )

    private val prefsRepository by lazy { SlashboardApp.instance.preferencesRepository }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spacebar_branding)

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val etLabel = findViewById<EditText>(R.id.et_spacebar_label)
        val tvCounter = findViewById<TextView>(R.id.tv_char_counter)
        val tvPreview = findViewById<TextView>(R.id.tv_spacebar_preview)
        val chipsContainer = findViewById<LinearLayout>(R.id.chips_container)
        val btnApply = findViewById<Button>(R.id.btn_apply)

        // Enforce max 12 characters
        etLabel.filters = arrayOf(InputFilter.LengthFilter(12))

        // Read current label
        val currentLabel = prefsRepository.settingsFlow.value.customSpacebarLabel.ifBlank { "Slashboard" }
        etLabel.setText(currentLabel)
        etLabel.setSelection(etLabel.text.length)
        tvCounter.text = "${currentLabel.length} / 12"
        tvPreview.text = currentLabel

        btnBack.setOnClickListener {
            finish()
        }

        // Live text change listener for real-time spacebar preview
        etLabel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvCounter.text = "$len / 12"
                val text = s?.toString()?.trim() ?: ""
                tvPreview.text = if (text.isNotBlank()) text else "Slashboard"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Populate Quick Idea Chips in clean 3-item rows
        chipsContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val chunkedIdeas = quickIdeas.chunked(3)

        for (rowItems in chunkedIdeas) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, (8 * density).toInt())
                }
            }

            for (idea in rowItems) {
                val chip = TextView(this).apply {
                    text = idea
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#E2E8F0"))
                    textSize = 13f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setBackgroundResource(R.drawable.bg_chip_pill)
                    isClickable = true
                    isFocusable = true

                    val padH = (12 * density).toInt()
                    val padV = (10 * density).toInt()
                    setPadding(padH, padV, padH, padV)

                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        val marginH = (4 * density).toInt()
                        setMargins(marginH, 0, marginH, 0)
                    }

                    setOnClickListener {
                        etLabel.setText(idea)
                        etLabel.setSelection(idea.length)
                    }
                }
                rowLayout.addView(chip)
            }
            chipsContainer.addView(rowLayout)
        }

        // Apply Button Action
        btnApply.setOnClickListener {
            val entered = etLabel.text.toString().trim()
            val finalLabel = if (entered.isNotBlank()) entered else "Slashboard"

            // Save to repository and SharedPreferences
            prefsRepository.updateCustomSpacebarLabel(finalLabel)

            // Direct sync with app-wide preferences
            try {
                getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
                    .edit()
                    .putString("custom_spacebar_label", finalLabel)
                    .apply()
                getSharedPreferences("slashboard_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("custom_spacebar_label", finalLabel)
                    .apply()
            } catch (_: Exception) {}

            // Send broadcast to immediately notify running IME to refresh
            try {
                sendBroadcast(Intent(ThemeEngine.ACTION_THEME_CHANGED))
                sendBroadcast(Intent("com.slashboard.keyboard.ACTION_REFRESH_SPACEBAR"))
            } catch (_: Exception) {}

            Toast.makeText(this, "Space bar updated: \"$finalLabel\"", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
