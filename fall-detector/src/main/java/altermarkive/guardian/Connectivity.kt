package altermarkive.guardian

import android.content.BroadcastReceiver
import android.content.Intent
import android.net.ConnectivityManager
import android.content.Context

class Connectivity : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        @Suppress("DEPRECATION")
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val manager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = manager.activeNetworkInfo
            if (info != null && info.isConnected) {
                Log.i(TAG, "Detected internet connectivity")
                Upload.sendAlert(context)
            }
        }
    }

    companion object {
        private val TAG = Connectivity::class.java.name
    }
}