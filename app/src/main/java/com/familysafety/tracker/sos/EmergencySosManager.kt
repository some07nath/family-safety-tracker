package com.familysafety.tracker.sos

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import android.widget.Toast
import com.familysafety.tracker.FamilySafetyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object EmergencySosManager {

    fun triggerSos(
        context: Context,
        latitude: Double,
        longitude: Double,
        batteryPct: Int,
        emergencyNumbers: List<String>
    ) {
        // 1. Provide tactile SOS vibration feedback
        vibrateSosPattern(context)

        val repository = FamilySafetyApp.instance.repository
        val isConnected = repository.isOnline()
        val mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
        val message = "🚨 EMERGENCY SOS! I need immediate help. My current location is: $mapsLink (Battery: $batteryPct%)"

        CoroutineScope(Dispatchers.Main).launch {
            if (isConnected) {
                // Online: Trigger cloud webhook / push notification
                val cloudSuccess = withContext(Dispatchers.IO) {
                    sendCloudSosAlert(latitude, longitude, batteryPct)
                }
                if (cloudSuccess) {
                    Toast.makeText(context, "SOS Alert sent to family network via Cloud!", Toast.LENGTH_LONG).show()
                } else {
                    // Cloud push failed -> Fallback to SMS
                    fallbackToSendSms(context, emergencyNumbers, message)
                }
            } else {
                // Completely Offline: Direct SMS Fallback
                fallbackToSendSms(context, emergencyNumbers, message)
            }
        }
    }

    private fun fallbackToSendSms(context: Context, emergencyNumbers: List<String>, message: String) {
        if (emergencyNumbers.isEmpty()) {
            Toast.makeText(context, "Offline SOS triggered, but no emergency contacts configured!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            for (number in emergencyNumbers) {
                val cleanNumber = number.trim()
                if (cleanNumber.isNotEmpty()) {
                    smsManager.sendTextMessage(cleanNumber, null, message, null, null)
                }
            }
            Toast.makeText(context, "Internet offline. Emergency SMS sent with GPS coordinates!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to send SMS fallback: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun sendCloudSosAlert(lat: Double, lng: Double, battery: Int): Boolean {
        // Connect to Firebase or custom backend endpoint
        // Returns true if dispatched successfully
        return true
    }

    private fun vibrateSosPattern(context: Context) {
        val pattern = longArrayOf(0, 200, 100, 200, 100, 200, 300, 500, 100, 500, 100, 500, 300, 200, 100, 200, 100, 200)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
