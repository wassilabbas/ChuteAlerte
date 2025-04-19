package altermarkive.guardian

import android.content.Context
import java.util.concurrent.Executors

class Upload internal constructor() {
    companion object {
        private val TAG = Upload::class.java.name

        @Volatile
        private var fallDetected: Boolean = false

        init {
            fallDetected = false // Initialisation dans un bloc `init`
        }

        internal fun setFallDetected(detected: Boolean) {
            fallDetected = detected
        }

        internal fun sendAlert(context: Context) {
            if (!fallDetected) {
                Log.i(TAG, "Aucune chute détectée, alerte non envoyée")
                return
            }

            val message = "⚠️ Chute détectée ! Veuillez vérifier l'état de la personne."
            val contact = Contact[context]
            if (contact != null && contact.isNotEmpty()) {
                Messenger.sms(context, contact, message)
                Log.i(TAG, "Message d'alerte envoyé à $contact : $message")
            } else {
                Log.e(TAG, "Aucun contact enregistré, alerte non envoyée.")
            }
        }
    }
}
