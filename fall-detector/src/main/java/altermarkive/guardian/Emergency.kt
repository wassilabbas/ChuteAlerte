package altermarkive.guardian

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Emergency : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.emergency, container, false)

        val emergencyButton = view.findViewById<MaterialButton>(R.id.urgence_button)
        emergencyButton.setOnClickListener {
            Alarm.alert(requireContext())
        }

        return view
    }
}
