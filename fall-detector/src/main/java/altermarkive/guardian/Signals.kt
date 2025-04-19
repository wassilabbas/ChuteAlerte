package altermarkive.guardian

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import android.content.Intent
import android.net.Uri
import com.google.android.material.floatingactionbutton.FloatingActionButton


class Signals : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.signals, container, false)
        // Ajout des onglets dynamiquement
        val tabs: TabLayout = view.findViewById(R.id.tabs)
        activity?.runOnUiThread {
            for (index in Surface.CHARTS.indices) {
                val tab = tabs.newTab()
                tab.text = Surface.CHARTS[index].label
                tabs.addTab(tab, index, index == 0)
            }
        }
        // Gestion du changement d'onglet
        tabs.addOnTabSelectedListener(view.findViewById(R.id.surface))

        return view
    }
}