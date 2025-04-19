package altermarkive.guardian

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.*

class Sampler private constructor(private val guardian: Guardian) : SensorEventListener {
    private val context: Context = guardian.applicationContext
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val wakeLock: PowerManager.WakeLock =
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Guardian:SamplerLock")
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val INTERVAL_MS = 20
    private val DURATION_S = 10
    private val N = DURATION_S * 1000 / INTERVAL_MS
    private val SPAN_FALLING = 1000 / INTERVAL_MS
    private val SPAN_IMPACT = 2000 / INTERVAL_MS
    private val SPAN_AVERAGING = 400/ INTERVAL_MS

    private val G = 1.0
    private val LYING_AVERAGE_Z_LPF = 0.5
    private val FALLING_WAIST_SV_TOT = 1.0
    private val IMPACT_WAIST_SV_TOT = 2.0
    private val IMPACT_WAIST_SV_D = 1.7
    private val IMPACT_WAIST_Z_2 = 1.5


    private val FILTER_LPF_GAIN = 4.143204922e+03
    private val FILTER_HPF_GAIN = 1.022463023e+00
    private val FILTER_FACTOR_0 = -0.9565436765
    private val FILTER_FACTOR_1 = 1.9555782403

    private var buffers = Array(10) { DoubleArray(N) { Double.NaN } }
    private val BUFFER_X = 0
    private val BUFFER_Y = 1
    private val BUFFER_Z = 2
    private val BUFFER_X_LPF = 3
    private val BUFFER_Y_LPF = 4
    private val BUFFER_Z_LPF = 5
    private val BUFFER_X_HPF = 6
    private val BUFFER_Y_HPF = 7
    private val BUFFER_Z_HPF = 8
    private val BUFFER_Z2 = 9

    private var pos = 0
    private var timeoutFalling = -1
    private var timeoutImpact = -1
    private var isLying = false


    private val xLpfXV = DoubleArray(3) { 0.0 }
    private val xLpfYV = DoubleArray(3) { 0.0 }
    private val yLpfXV = DoubleArray(3) { 0.0 }
    private val yLpfYV = DoubleArray(3) { 0.0 }
    private val zLpfXV = DoubleArray(3) { 0.0 }
    private val zLpfYV = DoubleArray(3) { 0.0 }
    private val xHpfXV = DoubleArray(3) { 0.0 }
    private val xHpfYV = DoubleArray(3) { 0.0 }
    private val yHpfXV = DoubleArray(3) { 0.0 }
    private val yHpfYV = DoubleArray(3) { 0.0 }
    private val zHpfXV = DoubleArray(3) { 0.0 }
    private val zHpfYV = DoubleArray(3) { 0.0 }

    init {
        Log.d("Sampler", "✅ Sampler a été initialisé !")
        initiate()
    }

    @SuppressLint("WakelockTimeout")
    private fun initiate() {
        if (!wakeLock.isHeld) wakeLock.acquire()
        sensors()
    }

    private fun sensors() {
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            Log.d("Sampler", "✅ Capteur ACCÉLÉROMÈTRE détecté : ${accelerometer.name}")
            val success = manager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_FASTEST)
            if (!success) Log.e("Sampler", "❌ Échec de l'enregistrement du capteur !")
        } else {
            Log.e("Sampler", "🚨 Aucun capteur ACCÉLÉROMÈTRE trouvé sur l'appareil !")
        }
    }

    private fun lpf(value: Double, xv: DoubleArray, yv: DoubleArray): Double {
        xv[0] = xv[1]; xv[1] = xv[2]; xv[2] = value / FILTER_LPF_GAIN
        yv[0] = yv[1]; yv[1] = yv[2]
        yv[2] = (xv[0] + xv[2]) + 2 * xv[1] + FILTER_FACTOR_0 * yv[0] + FILTER_FACTOR_1 * yv[1]
        return yv[2]
    }

    private fun hpf(value: Double, xv: DoubleArray, yv: DoubleArray): Double {
        xv[0] = xv[1]; xv[1] = xv[2]; xv[2] = value / FILTER_HPF_GAIN
        yv[0] = yv[1]; yv[1] = yv[2]
        yv[2] = (xv[0] + xv[2]) - 2 * xv[1] + FILTER_FACTOR_0 * yv[0] + FILTER_FACTOR_1 * yv[1]
        return yv[2]
    }

    private fun sv(x: Double, y: Double, z: Double) = sqrt(x * x + y * y + z * z)

    private fun average(array: DoubleArray): Double {
        val valid = array.filterNot { it.isNaN() }.takeLast(SPAN_AVERAGING)
        return if (valid.isNotEmpty()) valid.average() else Double.NaN
    }

    private fun expire(timeout: Int): Int = if (timeout > -1) timeout - 1 else -1

    private fun process(x: Double, y: Double, z: Double, time: Long) {
        timeoutFalling = expire(timeoutFalling)
        timeoutImpact = expire(timeoutImpact)

        // Stockage brut
        buffers[BUFFER_X][pos] = x
        buffers[BUFFER_Y][pos] = y
        buffers[BUFFER_Z][pos] = z

        // LPF & HPF
        val xLPF = lpf(x, xLpfXV, xLpfYV)
        val yLPF = lpf(y, yLpfXV, yLpfYV)
        val zLPF = lpf(z, zLpfXV, zLpfYV)

        val xHPF = hpf(x, xHpfXV, xHpfYV)
        val yHPF = hpf(y, yHpfXV, yHpfYV)
        val zHPF = hpf(z, zHpfXV, zHpfYV)

        // Calculs
        val svTot = sv(x, y, z)
        val svD = sv(xHPF, yHPF, zHPF)
        val z2 = (svTot * svTot - svD * svD - G * G) / (2.0 * G)

        // Stockage filtré
        buffers[BUFFER_X_LPF][pos] = xLPF
        buffers[BUFFER_Y_LPF][pos] = yLPF
        buffers[BUFFER_Z_LPF][pos] = zLPF
        buffers[BUFFER_X_HPF][pos] = xHPF
        buffers[BUFFER_Y_HPF][pos] = yHPF
        buffers[BUFFER_Z_HPF][pos] = zHPF
        buffers[BUFFER_Z2][pos] = z2

        // Détection de falling
        val previousPos = if (pos == 0) buffers[0].size - 1 else pos - 1
        val svTotBefore = sv(buffers[BUFFER_X_LPF][previousPos], buffers[BUFFER_Y_LPF][previousPos], buffers[BUFFER_Z_LPF][previousPos])
        var falling = 0.0
        if (svTotBefore >= FALLING_WAIST_SV_TOT && svTot < FALLING_WAIST_SV_TOT) {
            timeoutFalling = SPAN_FALLING
            falling = 1.0
        }
        // Détection d'impact
        var impact = 0.0
        if (timeoutFalling > 0) {
            if (svTot > IMPACT_WAIST_SV_TOT || svD > IMPACT_WAIST_SV_D || z2 > IMPACT_WAIST_Z_2) {
                timeoutImpact = SPAN_IMPACT
                impact = 1.0
                Log.w("ImpactConfirm", "✅ Impact détecté avec svTot=$svTot svD=$svD z2=$z2")
                Guardian.say(context, Log.WARN, "Sampler", "Detected a fall")
                alert(context)
            }
        }

        // Détection de lying (logique Sampler conservée)
        val averageZ = average(buffers[BUFFER_Z_LPF])
        if (svTot < 1.2 && svD < 0.2 && averageZ > LYING_AVERAGE_Z_LPF) {
            isLying = true
        } else if (svTot > 1.5 || svD > 0.4) {
            isLying = false
        }
        val lying = if (isLying) 1.0 else 0.0

        Log.d("LyingDebug", "falling=$falling impact=$impact lying=$lying averageZ=$averageZ svTot=$svTot")

        // Envoi Firebase
        FirebaseDatabase.getInstance("https://detectiondechute-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("bdd").push().setValue(
                mapOf(
                    "timestamp" to time,
                    "x" to x, "y" to y, "z" to z,
                    "x_lpf" to xLPF, "y_lpf" to yLPF, "z_lpf" to zLPF,
                    "x_hpf" to xHPF, "y_hpf" to yHPF, "z_hpf" to zHPF,
                    "sv_tot" to svTot, "sv_d" to svD, "z2" to z2,
                    "falling" to falling,
                    "impact" to impact,
                    "lying" to lying
                )
            )

        pos = (pos + 1) % buffers[0].size
    }

    private var lastProcessedTime = 0L
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastProcessedTime >= 20) {
                lastProcessedTime = currentTime
                val x = event.values[0].toDouble() / SensorManager.STANDARD_GRAVITY
                val y = event.values[1].toDouble() / SensorManager.STANDARD_GRAVITY
                val z = event.values[2].toDouble() / SensorManager.STANDARD_GRAVITY
                process(x, y, z, currentTime)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun alert(context: Context) {
        Alarm.alert(context)
    }
    companion object {
        private val TAG = Sampler::class.java.name

        @Volatile
        internal var instance: Sampler? = null

        @Synchronized
        fun instance(guardian: Guardian): Sampler {
            return instance ?: Sampler(guardian).also { instance = it }
        }
    }
}
