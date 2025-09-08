// Modified SettingsActivity.kt
package dubrowgn.wattz

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText // Added: Import for EditText
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView

const val settingsName = "settings"
const val settingsUpdateInd = "$namespace.settings-update-ind"

class SettingsActivity : Activity() {
    private val batteryReceiver = BatteryDataReceiver()

    private lateinit var charging: TextView
    private lateinit var currentScalar: RadioGroup
    private lateinit var indicatorUnits: RadioLayout
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var invertCurrent: Switch
    private lateinit var power: TextView

    // Added: EditTexts for notification colors (assume added to activity_settings.xml with ids notification_background_color and notification_text_color)
    private lateinit var notificationBackgroundColor: EditText
    private lateinit var notificationTextColor: EditText

    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }

    inner class BatteryDataReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            debug("BatteryDataReceiver.onReceive()")

            if (intent == null)
                return

            val ind = getString(R.string.indeterminate)

            charging.text = intent.getStringExtra("charging") ?: ind
            power.text = intent.getStringExtra("power") ?: ind
        }
    }

    private fun loadPrefs() {
        val settings = getSharedPreferences(settingsName, MODE_PRIVATE)
        currentScalar.check(
            when (settings.getFloat("currentScalar", -1f)) {
                1000f -> R.id.currentScalar1000
                .001f -> R.id.currentScalar0_001
                else -> R.id.currentScalar1
            }
        )
        indicatorUnits.check(
            when (settings.getString("indicatorUnits", null)) {
                "A" -> R.id.indicatorA
                "Ah" -> R.id.indicatorAh
                "C" -> R.id.indicatorC
                "V" -> R.id.indicatorV
                "Wh" -> R.id.indicatorWh
                "%" -> R.id.indicatorPerc
                else -> R.id.indicatorW
            }
        )
        invertCurrent.isChecked = settings.getBoolean("invertCurrent", false)

        // Added: Load notification color preferences
        notificationBackgroundColor.setText(settings.getString("notificationBackgroundColor", "#FFFFFF"))
        notificationTextColor.setText(settings.getString("notificationTextColor", "#000000"))
    }

    @SuppressLint("ApplySharedPref")
    private fun onChange() {
        getSharedPreferences(settingsName, MODE_PRIVATE)
            .edit()
            .putBoolean("invertCurrent", invertCurrent.isChecked)
            .putFloat(
                "currentScalar",
                when(currentScalar.checkedRadioButtonId) {
                    R.id.currentScalar1000 -> 1000f
                    R.id.currentScalar0_001 -> 0.001f
                    else -> 1f
                }
            )
            .putString(
                "indicatorUnits",
                when (indicatorUnits.checkedRadioButtonId) {
                    R.id.indicatorA -> "A"
                    R.id.indicatorAh -> "Ah"
                    R.id.indicatorC -> "C"
                    R.id.indicatorV -> "V"
                    R.id.indicatorWh -> "Wh"
                    R.id.indicatorPerc -> "%"
                    else -> "W"
                }
            )
            // Added: Save notification colors
            .putString("notificationBackgroundColor", notificationBackgroundColor.text.toString())
            .putString("notificationTextColor", notificationTextColor.text.toString())
            .commit()

        sendBroadcast(Intent().setPackage(packageName).setAction(settingsUpdateInd))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        debug("onCreate()")

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        charging = findViewById(R.id.charging)
        currentScalar = findViewById(R.id.currentScalar)
        indicatorUnits = findViewById(R.id.indicatorUnits)
        invertCurrent = findViewById(R.id.invertCurrent)
        power = findViewById(R.id.power)

        // Added: Initialize EditTexts for colors
        notificationBackgroundColor = findViewById(R.id.notification_background_color)
        notificationTextColor = findViewById(R.id.notification_text_color)

        loadPrefs()

        currentScalar.setOnCheckedChangeListener { _, _ -> onChange() }
        indicatorUnits.checkChangedCallback = { _ -> onChange() }
        invertCurrent.setOnCheckedChangeListener { _, _ -> onChange() }

        // Added: TextWatchers to trigger onChange when color inputs change
        notificationBackgroundColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { onChange() }
        })
        notificationTextColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { onChange() }
        })
    }

    override fun onPause() {
        debug("onPause()")

        unregisterReceiver(batteryReceiver)

        super.onPause()
    }

    override fun onResume() {
        debug("onResume()")

        super.onResume()

        registerReceiver(batteryReceiver, IntentFilter(batteryDataResp), RECEIVER_NOT_EXPORTED)
        sendBroadcast(Intent().setPackage(packageName).setAction(batteryDataReq))
    }

    override fun onDestroy() {
        debug("onDestroy()")

        super.onDestroy()
    }

    fun onBack(view: View) {
        finish()
    }
}
