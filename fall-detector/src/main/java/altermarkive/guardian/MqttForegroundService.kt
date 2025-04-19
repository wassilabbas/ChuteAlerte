package altermarkive.guardian

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MqttForegroundService : Service() {
    private lateinit var mqttHelper: MqttHelper
    private val CHANNEL_ID = "MQTTServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, getNotification()) // ✅ Démarrage immédiat du service en premier plan
        mqttHelper = MqttHelper(applicationContext)
        Log.d("MQTT", "🚀 Service MQTT démarré")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MQTT Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MQTT Service")
            .setContentText("Connexion MQTT active")
            .build()
    }
}
