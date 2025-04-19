package altermarkive.guardian

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttHelper(context: Context) {
    private val serverUri = "tcp://test.mosquitto.org:1883" //juste pour le test
    private val clientId = "GuardianClient-${System.currentTimeMillis()}"
    private val topic = "vehicle"
    private val mqttClient = MqttAndroidClient(context, serverUri, clientId)

    init {
        connectMqtt()
    }

    fun connectMqtt() {
        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = false
        }

        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "✅ Connecté au broker MQTT : $serverUri")
                subscribeToTopic()

            }


            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "❌ Échec de connexion au broker MQTT", exception)
            }
        })
    }

    fun subscribeToTopic() {
        mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "✅ Abonné au topic '$topic'")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "❌ Échec d'abonnement au topic '$topic'", exception)
            }
        })
    }

    fun publishMessage(message: String) {
        try {
            if (!mqttClient.isConnected) { // ✅ Vérifie que le client est bien connecté
                Log.e("MQTT", "❌ Client MQTT NON CONNECTÉ, tentative de reconnexion...")
                connectMqtt()
                return
            }

            val topic = "vehicle"
            val payload = message.toByteArray()

            if (payload.size > 256000) { // ✅ Vérifie la taille du message
                Log.e("MQTT", "❌ Message trop volumineux pour Mosquitto")
                return
            }

            Log.d("MQTT", "📤 Envoi du message sur $topic : $message") // ✅ Debug avant l’envoi
            mqttClient.publish(topic, payload, 0, false)
            Log.d("MQTT", "✅ Message publié avec succès sur $topic")
        } catch (e: Exception) {
            Log.e("MQTT", "❌ Erreur d'envoi MQTT : ${e.message}")
        }
    }


    fun connectionLost(cause: Throwable?) {
        Log.e("MQTT", "⚠️ Connexion MQTT perdue, tentative de reconnexion...")
        Handler(Looper.getMainLooper()).postDelayed({
            connectMqtt()
        }, 5000) // ✅ Reconnecte après 5 secondes
    }


    fun isConnected(): Boolean = mqttClient.isConnected
}
